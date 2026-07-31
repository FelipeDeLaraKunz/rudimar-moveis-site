package com.rudimarmoveis.site.service;

import com.rudimarmoveis.site.model.Produto;
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
import java.util.List;

/**
 * Le a planilha de controle de estoque (layout: coluna B = quantidade, C = data de entrada
 * (ignorada aqui), D = nome tecnico, E = categoria, F = marca) e cria os produtos correspondentes
 * como rascunho - sem preco, cor, foto nem nome de venda ainda (o admin completa manualmente depois).
 * Linhas sem nenhuma unidade em estoque sao ignoradas. Tambem gera a planilha de exportacao
 * com o estoque completo.
 */
@Service
public class PlanilhaEstoqueService {

    private static final int COL_QUANTIDADE = 1;
    private static final int COL_NOME_TECNICO = 3;
    private static final int COL_CATEGORIA = 4;
    private static final int COL_MARCA = 5;

    public List<Produto> importar(MultipartFile arquivo) throws IOException {
        List<Produto> produtos = new ArrayList<>();
        DataFormatter formatador = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet planilha = workbook.getSheetAt(0);

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
        }

        return produtos;
    }

    public byte[] exportar(List<Produto> produtos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet planilha = workbook.createSheet("Estoque");

            String[] cabecalho = {
                    "ID", "Nome interno", "Nome (cliente)", "Categoria", "Marca", "Cor",
                    "Quantidade em estoque", "Preco a vista", "Preco no cartao (10x)",
                    "Ativo", "Na home", "ID da foto principal"
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
                linha.createCell(11).setCellValue(idDaFoto(produto.getImagemPrincipal()));
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

    private String vazioSeNulo(String valor) {
        return valor == null ? "" : valor;
    }

    private String idDaFoto(String urlImagem) {
        if (!StringUtils.hasText(urlImagem)) {
            return "";
        }
        int barra = urlImagem.lastIndexOf('/');
        return barra >= 0 ? urlImagem.substring(barra + 1) : urlImagem;
    }
}
