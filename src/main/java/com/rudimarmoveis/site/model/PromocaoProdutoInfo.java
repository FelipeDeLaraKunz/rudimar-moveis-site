package com.rudimarmoveis.site.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vincula um produto a promocao ativa que da o melhor desconto para ele, com o preco
 * promocional (a vista e no cartao) ja resolvido. Usada apenas para exibicao (cards,
 * pagina de detalhe, etc.), montada em tempo de requisicao pelo PromocaoService.
 */
public class PromocaoProdutoInfo {

    private final Promocao promocao;
    private final PrecoPromocional precos;

    public PromocaoProdutoInfo(Promocao promocao, PrecoPromocional precos) {
        this.promocao = promocao;
        this.precos = precos;
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

    public String getCor() {
        return promocao.getCor();
    }

    public BigDecimal getPrecoPromocional() {
        return precos.getPrecoPromocional();
    }

    public BigDecimal getPrecoPromocionalCartao() {
        return precos.getPrecoPromocionalCartao();
    }

    public BigDecimal getParcela10x() {
        return precos.getParcela10x();
    }

    public LocalDate getValidaAte() {
        return promocao.getValidaAte();
    }

    public String getValidaAteIso() {
        return promocao.getValidaAteIso();
    }
}
