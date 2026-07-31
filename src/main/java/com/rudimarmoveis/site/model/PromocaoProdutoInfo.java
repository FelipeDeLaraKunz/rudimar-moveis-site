package com.rudimarmoveis.site.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vincula um produto a promocao ativa que da o melhor desconto para ele, com o preco
 * promocional ja calculado. Usada apenas para exibicao (cards, pagina de detalhe, etc.),
 * montada em tempo de requisicao pelo PromocaoService.
 */
public class PromocaoProdutoInfo {

    private final Promocao promocao;
    private final BigDecimal precoPromocional;

    public PromocaoProdutoInfo(Promocao promocao, BigDecimal precoPromocional) {
        this.promocao = promocao;
        this.precoPromocional = precoPromocional;
    }

    public Long getPromocaoId() {
        return promocao.getId();
    }

    public String getTitulo() {
        return promocao.getTitulo();
    }

    public Integer getPercentualDesconto() {
        return promocao.getPercentualDesconto();
    }

    public BigDecimal getPrecoPromocional() {
        return precoPromocional;
    }

    public LocalDate getValidaAte() {
        return promocao.getValidaAte();
    }

    public String getValidaAteIso() {
        return promocao.getValidaAteIso();
    }
}
