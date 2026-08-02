package com.rudimarmoveis.site.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "promocoes")
public class Promocao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o titulo da promocao")
    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    // Opcional: quando o desconto varia de produto para produto, deixa-se em branco e o
    // selo "-X%" simplesmente nao aparece (o preco promocional de cada produto continua valendo).
    @Positive(message = "O desconto deve ser maior que zero")
    private Integer percentualDesconto;

    // Caminho da imagem enviada via upload no painel admin (ex: /uploads/arquivo.jpg)
    private String imagemUrl;

    // Data em que a promocao deixa de valer (opcional). A promocao vale ate o final desse dia.
    private LocalDate validaAte;

    @Column(nullable = false)
    private boolean ativo = true;

    // Cor de destaque dessa promocao no site (selo, preco riscado, timer, banner).
    // Formato hexadecimal (#rrggbb), escolhida no admin via <input type="color">.
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor invalida")
    @Column(nullable = false, length = 7)
    private String cor = "#C1440E";

    // Produtos participantes da promocao e o preco promocional (a vista + cartao) de cada um
    // (o preco normal do produto, em Produto.preco/precoCartao, nunca e alterado por aqui)
    @ElementCollection
    @CollectionTable(name = "promocao_produtos", joinColumns = @JoinColumn(name = "promocao_id"))
    @MapKeyColumn(name = "produto_id")
    private Map<Long, PrecoPromocional> precosPromocionais = new LinkedHashMap<>();

    public Promocao() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(Integer percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public LocalDate getValidaAte() {
        return validaAte;
    }

    public void setValidaAte(LocalDate validaAte) {
        this.validaAte = validaAte;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public Map<Long, PrecoPromocional> getPrecosPromocionais() {
        return precosPromocionais;
    }

    public void setPrecosPromocionais(Map<Long, PrecoPromocional> precosPromocionais) {
        this.precosPromocionais = precosPromocionais;
    }

    // atalho usado onde so importa quais produtos participam, sem o preco de cada um
    @Transient
    public Set<Long> getProdutoIds() {
        return precosPromocionais.keySet();
    }

    // true quando a promocao esta marcada como visivel e ainda nao passou da data de validade
    @Transient
    public boolean isValidaAgora() {
        return ativo && (validaAte == null || !validaAte.isBefore(LocalDate.now()));
    }

    // momento exato (ISO, meia-noite do dia seguinte a "valida ate") em que a promocao acaba,
    // usado pelo timer em JS. Nulo quando a promocao nao tem data de validade definida.
    @Transient
    public String getValidaAteIso() {
        return validaAte == null ? null : validaAte.plusDays(1).atStartOfDay().toString();
    }
}
