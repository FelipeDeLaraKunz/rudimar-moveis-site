package com.rudimarmoveis.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Preco promocional de um produto dentro de uma promocao: a vista (obrigatorio) e no
// cartao/10x (opcional), no mesmo espirito do preco/precoCartao normal do Produto.
@Embeddable
public class PrecoPromocional {

    @Column(name = "preco_promocional", nullable = false)
    private BigDecimal precoPromocional;

    @Column(name = "preco_promocional_cartao")
    private BigDecimal precoPromocionalCartao;

    public PrecoPromocional() {
    }

    public BigDecimal getPrecoPromocional() {
        return precoPromocional;
    }

    public void setPrecoPromocional(BigDecimal precoPromocional) {
        this.precoPromocional = precoPromocional;
    }

    public BigDecimal getPrecoPromocionalCartao() {
        return precoPromocionalCartao;
    }

    public void setPrecoPromocionalCartao(BigDecimal precoPromocionalCartao) {
        this.precoPromocionalCartao = precoPromocionalCartao;
    }

    @Transient
    public BigDecimal getParcela10x() {
        return precoPromocionalCartao == null ? null : precoPromocionalCartao.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
    }
}
