package com.rudimarmoveis.site.model;

import java.util.List;
import java.util.Set;

/**
 * Taxonomia de categorias de produto, agrupada por comodo. Fonte unica usada tanto pelo
 * select de categoria do formulario de produto (admin) quanto pelo filtro do catalogo
 * publico - assim os dois nunca ficam fora de sincronia.
 */
public final class CategoriasCatalogo {

    public static final List<GrupoCategorias> GRUPOS = List.of(
            new GrupoCategorias("Sala de estar", List.of(
                    "Sofá", "Sofá-cama", "Estofado", "Namoradeira", "Chaise", "Divã", "Poltrona", "Puff",
                    "Rack/Painel para TV", "Móvel para TV", "Painel suspenso", "Home Theater",
                    "Mesa de centro", "Mesa de apoio", "Mesa lateral", "Mesa de canto", "Conjunto de mesa",
                    "Estante", "Livreiro", "Sapateira/organizador", "Bar", "Esteira"
            )),
            new GrupoCategorias("Sala de jantar", List.of(
                    "Conjunto de mesas e cadeiras", "Mesa de jantar", "Mesa extensível", "Cadeira de jantar",
                    "Banqueta", "Banco", "Carrinho/bar", "Bar", "Buffet", "Aparador", "Cristaleira"
            )),
            new GrupoCategorias("Quarto", List.of(
                    "Cama", "Conjunto box", "Colchão", "Colchão infantil", "Beliche", "Bicama", "Berço",
                    "Base/Box", "Base baú", "Baú", "Guarda-roupa/Roupeiro", "Cômoda", "Mesa de cabeceira",
                    "Escrivaninha", "Penteadeira", "Cabeceira", "Sapateira", "Espelho", "Multiuso/Organizador"
            )),
            new GrupoCategorias("Cozinha", List.of(
                    "Armário de cozinha", "Armário aéreo", "Armário inferior", "Paneleiro", "Cozinha americana",
                    "Balcão/Bancada", "Torre quente", "Gabinete", "Mesa de cozinha", "Mesa dobrável",
                    "Cadeira de cozinha", "Conjunto de cozinha", "Fruteira", "Cubas e pias",
                    "Escorredor de louças", "Cantinho café", "Kit cozinha", "Kit forno"
            )),
            new GrupoCategorias("Escritório", List.of(
                    "Escrivaninha", "Mesa de escritório", "Mesa gamer", "Mesa em L", "Cadeira de escritório",
                    "Cadeira gamer", "Cadeira executiva", "Estante", "Armário de escritório", "Arquivo",
                    "Gaveteiro"
            )),
            new GrupoCategorias("Banheiro", List.of(
                    "Gabinete para banheiro", "Armário para banheiro", "Espelheira", "Kit banheiro"
            )),
            new GrupoCategorias("Lavanderia", List.of(
                    "Armário para lavanderia", "Balcão para lavanderia", "Tanque", "Gabinete para tanque",
                    "Varal", "Tábua de passar"
            )),
            new GrupoCategorias("Decoração e acessórios", List.of(
                    "Nicho", "Prateleira", "Quadro", "Relógio", "Luminária", "Tapete", "Almofada", "Cortina",
                    "Pendente", "Vaso", "Objetos decorativos", "Calha", "Tampo"
            )),
            new GrupoCategorias("Eletrodomésticos", List.of(
                    "Cooktop"
            ))
    );

    public static final List<String> TAMANHOS = List.of("Solteiro", "Casal", "Queen", "King");

    // Categorias cujo produto costuma variar por tamanho - so nessas o campo "Tamanho"
    // aparece no formulario/filtro (ver admin-produtos.js e catalogo.js, que tem a mesma lista).
    public static final Set<String> CATEGORIAS_COM_TAMANHO = Set.of("Cama", "Conjunto box", "Colchão");

    private CategoriasCatalogo() {
    }
}
