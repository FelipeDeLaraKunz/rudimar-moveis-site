package com.rudimarmoveis.site.repository;

import com.rudimarmoveis.site.model.Promocao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocaoRepository extends JpaRepository<Promocao, Long> {

    List<Promocao> findByAtivoTrueOrderByIdDesc();
}
