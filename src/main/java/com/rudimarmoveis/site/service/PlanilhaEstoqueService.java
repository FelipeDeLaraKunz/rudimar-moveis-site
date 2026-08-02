package com.rudimarmoveis.site.service;

import com.rudimarmoveis.site.model.Produto;
import com.rudimarmoveis.site.repository.ProdutoRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Le planilhas de estoque em dois formatos possiveis:
 * - modelo simples (sem cabecalho): coluna B = quantidade, C = data de entrada (ignorada),
 *   D = nome tecnico, E = categoria, F = marca. Cria produtos novos como rascunho, ignorando
 *   linhas sem nenhuma unidade em estoque.
 * - modelo completo (o mesmo que o botao "Exportar planilha" gera, reconhecido pelo cabecalho
 *   "ID" na primeira celula): atualiza os produtos existentes casando pelo ID, e cria os que
 *   vierem sem ID. Campos que essa planilha nao cobre (ex: descricao) ficam como estavam.
 * Tambem gera a planilha de exportacao com o estoque completo, incluindo os IDs de todas
 * as fotos de cada produto (para poder reimportar depois de editar em lote).
 */
@Service
public class PlanilhaEstoqueService {

    // ---- colunas do modelo simples (sem cabecalho) ----
    private static final int COL_QUANTIDADE = 1;
    private static final int COL_NOME_TECNICO = 3;
    private static final int COL_CATEGORIA = 4;
    private static final int COL_MARCA = 5;

    // ---- colunas do modelo completo (com cabecalho, igual ao gerado na exportacao) ----
    private static final int COLC_ID = 0;
    private static final int COLC_NOME_INTERNO = 1;
    private static final int COLC_NOME_CLIENTE = 2;
    private static final int COLC_CATEGORIA = 3;
    private static final int COLC_MARCA = 4;
    private static final int COLC_COR = 5;
    private static final int COLC_QUANTIDADE = 6;
    private static final int COLC_PRECO = 7;
    private static final int COLC_PRECO_CARTAO = 8;
    private static final int COLC_ATIVO = 9;
    private static final int COLC_NA_HOME = 10;
    private static final int COLC_FOTOS = 11;

    private final ProdutoRepository produtoRepository;

    public PlanilhaEstoqueService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> importar(MultipartFile arquivo) throws IOException {
        DataFormatter formatador = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet planilha = workbook.getSheetAt(0);

            if (ehModeloCompleto(planilha, formatador)) {
                return importarModeloCompleto(planilha, formatador);
            }
            return importarModeloSimples(planilha, formatador);
        }
    }

    // o modelo completo (gerado pelo proprio "Exportar planilha") sempre tem "ID" na primeira
    // celula da primeira linha; o modelo simples nao tem nenhum cabecalho
    private boolean ehModeloCompleto(Sheet planilha, DataFormatter formatador) {
        Row primeiraLinha = planilha.getRow(planilha.getFirstRowNum());
        if (primeiraLinha == null) {
            return false;
        }
        return "ID".equalsIgnoreCase(valorTexto(primeiraLinha, COLC_ID, formatador).trim());
    }

    private List<Produto> importarModeloSimples(Sheet planilha, DataFormatter formatador) {
        List<Produto> produtos = new ArrayList<>();

        for (Row linha : planilha) {
            String nomeTecnico = valorTexto(linha, COL_NOME_TECNICO, formatador).trim();
            if (!StringUtils.hasText(nomeTecnico)) {
                continue; // linha em branco, de cabecalho ou de formatacao - nao e um produto
            }

            Integer quantidade = valorInteiro(linha, COL_QUANTIDADE, formatador);
            if (quantidade == null || quantidade <= 0) {
                continue; // sem estoque - nao vale a pena cadastrar
            }

            Produto produto = new Produto();
            produto.setNomeInterno(nomeTecnico);
            produto.setNome(nomeTecnico); // ponto de partida; o admin ajusta o nome de venda depois
            produto.setCategoria(valorTexto(linha, COL_CATEGORIA, formatador).trim());
            produto.setMarca(valorTexto(linha, COL_MARCA, formatador).trim());
            produto.setQuantidadeEstoque(quantidade);
            produto.setPreco(BigDecimal.ZERO);
            produto.setAtivo(false); // fica oculto do site ate o admin completar foto e preco
            produtos.add(produto);
        }

        return produtos;
    }

    private List<Produto> importarModeloCompleto(Sheet planilha, DataFormatter formatador) {
        List<Produto> produtos = new ArrayList<>();

        for (Row linha : planilha) {
            if (linha.getRowNum() == planilha.getFirstRowNum()) {
                continue; // linha de cabecalho
            }

            String nomeInterno = valorTexto(linha, COLC_NOME_INTERNO, formatador).trim();
            if (!StringUtils.hasText(nomeInterno)) {
                continue; // linha em branco
            }

            Long id = valorLong(linha, COLC_ID, formatador);
            Produto produto = id != null ? produtoRepository.findById(id).orElse(null) : null;
            if (produto == null) {
                produto = new Produto();
                produto.setPreco(BigDecimal.ZERO);
                produto.setAtivo(false);
            }

            produto.setNomeInterno(nomeInterno);
            String nomeCliente = valorTexto(linha, COLC_NOME_CLIENTE, formatador).trim();
            produto.setNome(StringUtils.hasText(nomeCliente) ? nomeCliente : nomeInterno);
            produto.setCategoria(valorTexto(linha, COLC_CATEGORIA, formatador).trim());
            produto.setMarca(valorTexto(linha, COLC_MARCA, formatador).trim());
            produto.setCor(valorTexto(linha, COLC_COR, formatador).trim());
            produto.setQuantidadeEstoque(valorInteiro(linha, COLC_QUANTIDADE, formatador));

            BigDecimal preco = valorDecimal(linha, COLC_PRECO, formatador);
            produto.setPreco(preco != null ? preco : BigDecimal.ZERO);
            produto.setPrecoCartao(valorDecimal(linha, COLC_PRECO_CARTAO, formatador));

            produto.setAtivo(valorBooleano(linha, COLC_ATIVO, formatador));
            produto.setDestaque(valorBooleano(linha, COLC_NA_HOME, formatador));

            // so mexe nas fotos se a celula trouxer algo - celula vazia preserva as fotos atuais
            List<String> imagens = idsDasFotosDaLinha(linha, formatador);
            if (!imagens.isEmpty()) {
                produto.setImagens(imagens);
            }

            produtos.add(produto);
        }

        return produtos;
    }

    public byte[] exportar(List<Produto> produtos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet planilha = workbook.createSheet("Estoque");

            String[] cabecalho = {
                    "ID", "Nome interno", "Nome (cliente)", "Categoria", "Marca", "Cor",
                    "Quantidade em estoque", "Preco a vista", "Preco no cartao (10x)",
                    "Ativo", "Na home", "IDs das fotos"
            };
            Row linhaCabecalho = planilha.createRow(0);
            for (int i = 0; i < cabecalho.length; i++) {
                linhaCabecalho.createCell(i).setCellValue(cabecalho[i]);
            }

            int numeroLinha = 1;
            for (Produto produto : produtos) {
                Row linha = planilha.createRow(numeroLinha++);
                linha.createCell(0).setCellValue(produto.getId() == null ? 0 : produto.getId().doubleValue());
                linha.createCell(1).setCellValue(vazioSeNulo(produto.getNomeInterno()));
                linha.createCell(2).setCellValue(vazioSeNulo(produto.getNome()));
                linha.createCell(3).setCellValue(vazioSeNulo(produto.getCategoria()));
                linha.createCell(4).setCellValue(vazioSeNulo(produto.getMarca()));
                linha.createCell(5).setCellValue(vazioSeNulo(produto.getCor()));
                linha.createCell(6).setCellValue(produto.getQuantidadeEstoque() == null ? 0 : produto.getQuantidadeEstoque());
                linha.createCell(7).setCellValue(produto.getPreco() == null ? 0 : produto.getPreco().doubleValue());
                linha.createCell(8).setCellValue(produto.getPrecoCartao() == null ? 0 : produto.getPrecoCartao().doubleValue());
                linha.createCell(9).setCellValue(produto.isAtivo() ? "Sim" : "Nao");
                linha.createCell(10).setCellValue(produto.isDestaque() ? "Sim" : "Nao");
                linha.createCell(11).setCellValue(idsDasFotos(produto.getImagens()));
            }

            for (int i = 0; i < cabecalho.length; i++) {
                planilha.autoSizeColumn(i);
            }

            workbook.write(saida);
            return saida.toByteArray();
        }
    }

    private String valorTexto(Row linha, int coluna, DataFormatter formatador) {
        Cell celula = linha.getCell(coluna);
        return celula == null ? "" : formatador.formatCellValue(celula);
    }

    private Integer valorInteiro(Row linha, int coluna, DataFormatter formatador) {
        String texto = valorTexto(linha, coluna, formatador).trim();
        if (!StringUtils.hasText(texto)) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long valorLong(Row linha, int coluna, DataFormatter formatador) {
        String texto = valorTexto(linha, coluna, formatador).trim();
        if (!StringUtils.hasText(texto)) {
            return null;
        }
        try {
            long valor = (long) Double.parseDouble(texto.replace(",", "."));
            return valor > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal valorDecimal(Row linha, int coluna, DataFormatter formatador) {
        String texto = valorTexto(linha, coluna, formatador).trim();
        if (!StringUtils.hasText(texto)) {
            return null;
        }
        try {
            return new BigDecimal(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean valorBooleano(Row linha, int coluna, DataFormatter formatador) {
        String texto = valorTexto(linha, coluna, formatador).trim();
        return texto.equalsIgnoreCase("sim") || texto.equalsIgnoreCase("true") || texto.equals("1");
    }

    private List<String> idsDasFotosDaLinha(Row linha, DataFormatter formatador) {
        String texto = valorTexto(linha, COLC_FOTOS, formatador).trim();
        if (!StringUtils.hasText(texto)) {
            return new ArrayList<>();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(id -> "/uploads/" + id)
                .collect(Collectors.toList());
    }

    private String vazioSeNulo(String valor) {
        return valor == null ? "" : valor;
    }

    // junta os ids de todas as fotos do produto numa unica celula, separados por virgula
    private String idsDasFotos(List<String> imagens) {
        if (imagens == null || imagens.isEmpty()) {
            return "";
        }
        return imagens.stream()
                .map(this::idDaFoto)
                .collect(Collectors.joining(","));
    }

    private String idDaFoto(String urlImagem) {
        if (!StringUtils.hasText(urlImagem)) {
            return "";
        }
        int barra = urlImagem.lastIndexOf('/');
        return barra >= 0 ? urlImagem.substring(barra + 1) : urlImagem;
    }
}
