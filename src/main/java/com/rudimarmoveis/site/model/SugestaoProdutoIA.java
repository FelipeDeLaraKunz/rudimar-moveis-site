package com.rudimarmoveis.site.model;

// Resposta da automacao do n8n a partir do nome tecnico interno do produto (ver IaProdutoService).
// O site manda so o nomeInterno pro webhook do n8n; o n8n devolve o resto pronto pro admin revisar.
public record SugestaoProdutoIA(
        String nome,
        String descricao,
        String cor,
        String categoria,
        String marca
) {
}
