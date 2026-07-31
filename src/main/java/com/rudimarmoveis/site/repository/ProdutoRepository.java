package com.rudimarmoveis.site.repository;

import com.rudimarmoveis.site.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByNomeAsc();

    // usado na home: so os produtos marcados como "destaque" pelo admin
    List<Produto> findByAtivoTrueAndDestaqueTrueOrderByNomeAsc();

    long countByAtivoTrue();
}
