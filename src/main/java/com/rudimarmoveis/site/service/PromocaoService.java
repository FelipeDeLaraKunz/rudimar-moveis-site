package com.rudimarmoveis.site.service;

import com.rudimarmoveis.site.model.Promocao;
import com.rudimarmoveis.site.model.PromocaoProdutoInfo;
import com.rudimarmoveis.site.repository.PromocaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromocaoService {

    private final PromocaoRepository promocaoRepository;

    public PromocaoService(PromocaoRepository promocaoRepository) {
        this.promocaoRepository = promocaoRepository;
    }

    // promocoes visiveis e que ainda nao passaram da data de validade
    public List<Promocao> listarValidas() {
        return promocaoRepository.findByAtivoTrueOrderByIdDesc().stream()
                .filter(Promocao::isValidaAgora)
                .collect(Collectors.toList());
    }

    // produtoId -> promocao que da o maior desconto para ele, dentre as promocoes validas informadas
    public Map<Long, PromocaoProdutoInfo> mapearPorProduto(List<Promocao> promocoesValidas) {
        Map<Long, PromocaoProdutoInfo> mapa = new HashMap<>();
        for (Promocao promocao : promocoesValidas) {
            for (Map.Entry<Long, BigDecimal> entry : promocao.getPrecosPromocionais().entrySet()) {
                Long produtoId = entry.getKey();
                PromocaoProdutoInfo candidata = new PromocaoProdutoInfo(promocao, entry.getValue());
                PromocaoProdutoInfo existente = mapa.get(produtoId);
                if (existente == null || percentualOuZero(candidata) > percentualOuZero(existente)) {
                    mapa.put(produtoId, candidata);
                }
            }
        }
        return mapa;
    }

    // percentualDesconto e opcional (a promocao pode ter descontos variados por produto);
    // trata como 0 aqui so para efeito de desempate entre promocoes validas de um mesmo produto
    private int percentualOuZero(PromocaoProdutoInfo info) {
        Integer percentual = info.getPercentualDesconto();
        return percentual == null ? 0 : percentual;
    }
}
