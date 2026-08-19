package com.rudimarmoveis.site.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda as imagens enviadas pelo admin (produtos e promocoes) em uma pasta no
 * servidor (fora do jar, para poder escrever em tempo de execucao) e devolve o
 * caminho publico (/uploads/arquivo.jpg) para salvar no banco.
 *
 * Fotos de celular hoje em dia costumam vir enormes (4000px+, 8-10MB) - sem redimensionar,
 * isso deixava o carregamento das paginas lento (e as vezes a foto nem chegava a carregar,
 * em conexoes mais lentas). Toda foto passa por aqui antes de ser salva, entao e o unico
 * lugar que precisa cuidar disso.
 */
@Service
public class ArmazenamentoImagensService {

    // acima disso a foto e redimensionada - de sobra pra tela cheia/zoom, sem o peso de
    // uma foto de celular original
    private static final int DIMENSAO_MAXIMA = 1600;
    private static final float QUALIDADE_JPEG = 0.82f;
    private static final List<String> EXTENSOES_JPEG = List.of(".jpg", ".jpeg");
    // acima disso, rejeita com uma mensagem clara em vez de deixar passar pro limite do
    // Tomcat (multipart.max-file-size) - se deixasse chegar la, a rejeicao vira um 403
    // confuso (ver comentario no application.properties), entao aqui a gente barra antes,
    // com folga, onde da pra responder direito pro admin
    private static final long TAMANHO_MAXIMO_ENTRADA_BYTES = 20L * 1024 * 1024;

    private final Path diretorioUploads;

    public ArmazenamentoImagensService(@Value("${app.upload-dir}") String uploadDir) {
        this.diretorioUploads = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void garantirDiretorioExiste() {
        try {
            Files.createDirectories(diretorioUploads);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel criar a pasta de uploads: " + diretorioUploads, e);
        }
    }

    /**
     * Salva um arquivo enviado e retorna o caminho publico (ex: /uploads/uuid.jpg).
     * Retorna null se o arquivo estiver vazio (usuario nao selecionou nada).
     */
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_ENTRADA_BYTES) {
            throw new ImagemInvalidaException(
                    "Essa foto é grande demais (máximo 20MB). Tente uma foto menor ou já comprimida.");
        }
        String extensao = extrairExtensao(arquivo.getOriginalFilename());
        String novoNome = UUID.randomUUID() + extensao;
        Path destino = diretorioUploads.resolve(novoNome);

        try (InputStream entrada = arquivo.getInputStream()) {
            BufferedImage imagem = ImageIO.read(entrada);
            if (imagem == null) {
                // formato que o Java nao consegue decodificar (ex: HEIC direto do iPhone sem
                // conversao) - nesses casos o navegador do cliente tambem nao exibiria a foto,
                // entao e melhor avisar agora do que deixar uma foto "salva" que nunca aparece
                throw new ImagemInvalidaException(
                        "Não foi possível processar essa imagem. Tente salvar como JPEG ou PNG antes de enviar.");
            }
            salvarRedimensionada(imagem, extensao, destino);
            return "/uploads/" + novoNome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar imagem enviada", e);
        }
    }

    // redimensiona (se preciso) e grava no formato de destino; PNG/GIF mantem o formato
    // original (preserva transparencia), so JPEG usa compressao com qualidade reduzida.
    // Sempre escreve num arquivo temporario e substitui por cima do destino no final: ao
    // reotimizar uma foto que ja existe (otimizarExistentes), escrever direto por cima do
    // arquivo antigo NAO trunca o excesso quando o resultado fica menor (RandomAccessFile
    // so sobrescreve os bytes iniciais), entao o arquivo final ficava do mesmo tamanho de
    // antes com lixo sobrando no fim. Substituir o arquivo inteiro evita esse problema.
    private void salvarRedimensionada(BufferedImage original, String extensao, Path destino) throws IOException {
        BufferedImage imagem = redimensionarSePreciso(original);
        String sufixo = extensao.isEmpty() ? ".img" : extensao;
        Path temporario = Files.createTempFile(destino.getParent(), "tmp-", sufixo);

        try {
            if (EXTENSOES_JPEG.contains(extensao.toLowerCase(Locale.ROOT))) {
                salvarComoJpeg(imagem, temporario);
            } else {
                String formato = extensao.isEmpty() ? "png" : extensao.substring(1).toLowerCase(Locale.ROOT);
                if (!ImageIO.write(imagem, formato, temporario.toFile())) {
                    // ImageIO nao tem um writer pra esse formato (raro) - grava como PNG mesmo assim
                    ImageIO.write(imagem, "png", temporario.toFile());
                }
            }
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private BufferedImage redimensionarSePreciso(BufferedImage original) {
        int largura = original.getWidth();
        int altura = original.getHeight();
        if (largura <= DIMENSAO_MAXIMA && altura <= DIMENSAO_MAXIMA) {
            return original;
        }

        double escala = largura >= altura
                ? (double) DIMENSAO_MAXIMA / largura
                : (double) DIMENSAO_MAXIMA / altura;
        int novaLargura = Math.max(1, (int) Math.round(largura * escala));
        int novaAltura = Math.max(1, (int) Math.round(altura * escala));

        BufferedImage redimensionada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = redimensionada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, novaLargura, novaAltura, null);
        g.dispose();
        return redimensionada;
    }

    private void salvarComoJpeg(BufferedImage imagem, Path destino) throws IOException {
        // JPEG nao tem canal alpha - remove a transparencia (se houver) sobre fundo branco
        BufferedImage semAlpha = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = semAlpha.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, imagem.getWidth(), imagem.getHeight());
        g.drawImage(imagem, 0, 0, null);
        g.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam parametros = writer.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(QUALIDADE_JPEG);

        try (ImageOutputStream saida = ImageIO.createImageOutputStream(destino.toFile())) {
            writer.setOutput(saida);
            writer.write(null, new IIOImage(semAlpha, null, null), parametros);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Reprocessa (redimensiona/recomprime, in-place, mantendo o mesmo nome/URL) as fotos ja
     * salvas que ainda estao maiores que o necessario - usado pela acao "Otimizar fotos ja
     * enviadas" do admin, pra melhorar fotos que foram enviadas antes dessa otimizacao
     * existir. Fotos que ja estao pequenas, ou em formato que o Java nao consegue ler, sao
     * puladas (contadas separadamente no resultado).
     */
    public ResultadoOtimizacao otimizarExistentes(List<String> caminhosPublicos) {
        Set<String> unicos = new LinkedHashSet<>(caminhosPublicos);
        int otimizadas = 0;
        int jaOtimas = 0;
        int comFalha = 0;
        long bytesAntes = 0;
        long bytesDepois = 0;

        for (String caminhoPublico : unicos) {
            Path arquivo = resolverDentroDoDiretorio(caminhoPublico);
            if (arquivo == null || !Files.isRegularFile(arquivo)) {
                continue;
            }

            try {
                long tamanhoAntes = Files.size(arquivo);
                BufferedImage imagem = ImageIO.read(arquivo.toFile());
                if (imagem == null) {
                    comFalha++;
                    continue;
                }
                boolean dimensaoOk = imagem.getWidth() <= DIMENSAO_MAXIMA && imagem.getHeight() <= DIMENSAO_MAXIMA;
                if (dimensaoOk) {
                    // ja esta dentro do tamanho ideal - pula pra nao recomprimir (e perder
                    // qualidade) uma foto que rodar essa acao de novo no futuro vai encontrar
                    jaOtimas++;
                    continue;
                }

                String extensao = extrairExtensao(arquivo.getFileName().toString());
                salvarRedimensionada(imagem, extensao, arquivo);
                bytesAntes += tamanhoAntes;
                bytesDepois += Files.size(arquivo);
                otimizadas++;
            } catch (IOException e) {
                comFalha++;
            }
        }

        return new ResultadoOtimizacao(otimizadas, jaOtimas, comFalha, bytesAntes, bytesDepois);
    }

    public record ResultadoOtimizacao(int otimizadas, int jaOtimas, int comFalha, long bytesAntes, long bytesDepois) {
    }

    /** Remove um arquivo previamente salvo (usado ao excluir produto/imagem). Falhas sao ignoradas. */
    public void excluir(String caminhoPublico) {
        Path arquivo = resolverDentroDoDiretorio(caminhoPublico);
        if (arquivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ignored) {
            // se nao conseguir apagar o arquivo fisico, nao impede a operacao no banco
        }
    }

    // resolve um caminho publico (ex: /uploads/xxx.jpg) pro arquivo real, garantindo que o
    // resultado fica DENTRO da pasta de uploads - "caminhoPublico" as vezes vem de um valor
    // guardado no banco que pode ter sido influenciado por fora (ex: coluna de fotos de uma
    // planilha importada), entao nao da pra confiar que nunca vai ter um ".." tentando sair
    // da pasta. Retorna null se o caminho for invalido ou tentar escapar.
    private Path resolverDentroDoDiretorio(String caminhoPublico) {
        if (!StringUtils.hasText(caminhoPublico) || !caminhoPublico.startsWith("/uploads/")) {
            return null;
        }
        String nomeArquivo = caminhoPublico.substring("/uploads/".length());
        Path resolvido = diretorioUploads.resolve(nomeArquivo).normalize();
        return resolvido.startsWith(diretorioUploads) ? resolvido : null;
    }

    // so aceita extensoes curtas e alfanumericas (jpg, jpeg, png, webp, gif...). Qualquer
    // outra coisa (nome de arquivo forjado com "/", "..", etc.) cai no "sem extensao" -
    // o nome final do arquivo salvo e sempre um UUID nosso, nunca o nome original enviado,
    // entao isso so protege contra a extensao "escapar" da pasta de uploads.
    private String extrairExtensao(String nomeOriginal) {
        if (!StringUtils.hasText(nomeOriginal) || !nomeOriginal.contains(".")) {
            return "";
        }
        String extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return extensao.matches("\\.[a-z0-9]{1,6}") ? extensao : "";
    }

    /** Lancada quando o arquivo enviado nao e uma imagem que o servidor consiga processar/exibir. */
    public static class ImagemInvalidaException extends RuntimeException {
        public ImagemInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
