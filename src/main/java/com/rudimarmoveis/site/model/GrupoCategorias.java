package com.rudimarmoveis.site.model;

import java.util.List;

// Um grupo de categorias por comodo (ex: "Quarto" -> Cama, Guarda-roupa, Comoda...),
// usado tanto no select de categoria do admin quanto no filtro do catalogo publico.
public record GrupoCategorias(String nome, List<String> categorias) {
}
