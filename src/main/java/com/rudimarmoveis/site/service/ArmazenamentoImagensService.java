package com.rudimarmoveis.site.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Guarda as imagens enviadas pelo admin (produtos e promocoes) em uma pasta no
 * servidor (fora do jar, para poder escrever em tempo de execucao) e devolve o
 * caminho publico (/uploads/arquivo.jpg) para salvar no banco.
 */
@Service
public class ArmazenamentoImagensService {

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
        String extensao = extrairExtensao(arquivo.getOriginalFilename());
        String novoNome = UUID.randomUUID() + extensao;

        try {
            Path destino = diretorioUploads.resolve(novoNome);
            Files.copy(arquivo.getInputStream(), destino);
            return "/uploads/" + novoNome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar imagem enviada", e);
        }
    }

    /**
     * Salva bytes de imagem ja em memoria (ex: baixados de uma URL externa) e retorna o
     * caminho publico (ex: /uploads/uuid.jpg). Usado pela API de automacao (n8n).
     */
    public String salvar(byte[] conteudo, String extensao) {
        String novoNome = UUID.randomUUID() + (extensao == null ? "" : extensao);
        try {
            Path destino = diretorioUploads.resolve(novoNome);
            Files.write(destino, conteudo);
            return "/uploads/" + novoNome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar imagem baixada", e);
        }
    }

    /** Remove um arquivo previamente salvo (usado ao excluir produto/imagem). Falhas sao ignoradas. */
    public void excluir(String caminhoPublico) {
        if (!StringUtils.hasText(caminhoPublico) || !caminhoPublico.startsWith("/uploads/")) {
            return;
        }
        try {
            String nomeArquivo = caminhoPublico.substring("/uploads/".length());
            Files.deleteIfExists(diretorioUploads.resolve(nomeArquivo));
        } catch (IOException ignored) {
            // se nao conseguir apagar o arquivo fisico, nao impede a operacao no banco
        }
    }

    private String extrairExtensao(String nomeOriginal) {
        if (!StringUtils.hasText(nomeOriginal) || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.')).toLowerCase();
    }
}
