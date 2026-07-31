package com.rudimarmoveis.site.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome tecnico/interno, usado so no painel admin para controle do estoque (o cliente nunca ve isso)
    @NotBlank(message = "Informe o nome interno do produto")
    @Column(name = "nome_interno", nullable = false)
    private String nomeInterno;

    // Nome exibido para o cliente no site (cards, pagina do produto, busca, etc.)
    @NotBlank(message = "Informe o nome do produto para o cliente")
    @Column(nullable = false)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    // Preco a vista, destacado nos cards e na pagina do produto
    @NotNull(message = "Informe o preco")
    @PositiveOrZero(message = "O preco nao pode ser negativo")
    @Column(nullable = false)
    private BigDecimal preco;

    // Preco total no cartao (opcional); exibido parcelado em 10x sem juros
    @PositiveOrZero(message = "O preco no cartao nao pode ser negativo")
    @Column(name = "preco_cartao")
    private BigDecimal precoCartao;

    private String categoria;

    private String marca;

    private String cor;

    // Quantidade em estoque - visivel SOMENTE no painel admin, nunca exibido ao cliente
    @NotNull(message = "Informe a quantidade em estoque")
    @PositiveOrZero(message = "A quantidade nao pode ser negativa")
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque = 0;

    // Controla se o produto aparece na vitrine da pagina inicial (alem do catalogo completo)
    @Column(nullable = false)
    private boolean destaque = false;

    // Controla se o produto aparece no site (catalogo e vitrine) de forma geral
    @Column(nullable = false)
    private boolean ativo = true;

    // Varias fotos por produto, enviadas via upload no painel admin (ordem preservada)
    @ElementCollection
    @CollectionTable(name = "produto_imagens", joinColumns = @JoinColumn(name = "produto_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "imagem_url", length = 500)
    private List<String> imagens = new ArrayList<>();

    public Produto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeInterno() {
        return nomeInterno;
    }

    public void setNomeInterno(String nomeInterno) {
        this.nomeInterno = nomeInterno;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getPrecoCartao() {
        return precoCartao;
    }

    public void setPrecoCartao(BigDecimal precoCartao) {
        this.precoCartao = precoCartao;
    }

    // Valor de cada uma das 10 parcelas sem juros (ou null se nao houver preco no cartao cadastrado)
    @Transient
    public BigDecimal getParcela10x() {
        return precoCartao == null ? null : precoCartao.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public boolean isDestaque() {
        return destaque;
    }

    public void setDestaque(boolean destaque) {
        this.destaque = destaque;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<String> getImagens() {
        return imagens;
    }

    public void setImagens(List<String> imagens) {
        this.imagens = imagens;
    }

    // Atalho usado nos cards/listagens: a primeira foto cadastrada (ou null se nao houver nenhuma)
    @Transient
    public String getImagemPrincipal() {
        return imagens == null || imagens.isEmpty() ? null : imagens.get(0);
    }
}
