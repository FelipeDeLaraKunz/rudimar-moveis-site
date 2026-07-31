package com.rudimarmoveis.site.controller;

import com.rudimarmoveis.site.model.Produto;
import com.rudimarmoveis.site.model.Promocao;
import com.rudimarmoveis.site.model.PromocaoProdutoInfo;
import com.rudimarmoveis.site.repository.ProdutoRepository;
import com.rudimarmoveis.site.repository.PromocaoRepository;
import com.rudimarmoveis.site.service.PromocaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ProdutoRepository produtoRepository;
    private final PromocaoRepository promocaoRepository;
    private final PromocaoService promocaoService;

    @Autowired
    public HomeController(ProdutoRepository produtoRepository, PromocaoRepository promocaoRepository,
                           PromocaoService promocaoService) {
        this.produtoRepository = produtoRepository;
        this.promocaoRepository = promocaoRepository;
        this.promocaoService = promocaoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Promocao> promocoes = promocaoService.listarValidas();
        Map<Long, PromocaoProdutoInfo> promoPorProduto = promocaoService.mapearPorProduto(promocoes);
        Map<Long, List<Produto>> produtosPorPromocao = produtosAtivosPorPromocao(promocoes);

        long totalProdutosAtivos = produtoRepository.countByAtivoTrue();
        List<Produto> vitrine = produtoRepository.findByAtivoTrueAndDestaqueTrueOrderByNomeAsc();

        model.addAttribute("promocoes", promocoes);
        model.addAttribute("produtosPorPromocao", produtosPorPromocao);
        model.addAttribute("promoPorProduto", promoPorProduto);
        model.addAttribute("produtos", vitrine);
        model.addAttribute("totalProdutosAtivos", totalProdutosAtivos);
        return "index";
    }

    @GetMapping("/produtos")
    public String catalogo(Model model) {
        List<Produto> produtos = produtoRepository.findByAtivoTrueOrderByNomeAsc();

        model.addAttribute("produtos", produtos);
        model.addAttribute("totalProdutos", produtos.size());
        model.addAttribute("promoPorProduto", promocaoService.mapearPorProduto(promocaoService.listarValidas()));
        // valores realmente em uso pelos produtos ativos, para montar os filtros da lateral
        model.addAttribute("categoriasDisponiveis", extrairValoresDistintos(produtos, Produto::getCategoria));
        model.addAttribute("marcasDisponiveis", extrairValoresDistintos(produtos, Produto::getMarca));
        model.addAttribute("coresDisponiveis", extrairValoresDistintos(produtos, Produto::getCor));
        return "catalogo";
    }

    // lista ordenada e sem duplicatas/vazios de um atributo do produto, usada para montar os filtros do catalogo
    private List<String> extrairValoresDistintos(List<Produto> produtos, Function<Produto, String> extrator) {
        return produtos.stream()
                .map(extrator)
                .filter(valor -> valor != null && !valor.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    @GetMapping("/produtos/{id}")
    public String detalheProduto(@PathVariable Long id, Model model) {
        return produtoRepository.findById(id)
                .filter(Produto::isAtivo)
                .map(produto -> {
                    model.addAttribute("produto", produto);
                    PromocaoProdutoInfo promoInfo = promocaoService.mapearPorProduto(promocaoService.listarValidas())
                            .get(produto.getId());
                    model.addAttribute("promoInfo", promoInfo);
                    return "produto-detalhe";
                })
                // produto nao existe ou foi desativado: volta para o catalogo
                .orElse("redirect:/produtos");
    }

    @GetMapping("/promocoes")
    public String promocoes(Model model) {
        List<Promocao> promocoes = promocaoService.listarValidas();
        Map<Long, PromocaoProdutoInfo> promoPorProduto = promocaoService.mapearPorProduto(promocoes);
        Map<Long, List<Produto>> produtosPorPromocao = produtosAtivosPorPromocao(promocoes);

        model.addAttribute("promocoes", promocoes);
        model.addAttribute("produtosPorPromocao", produtosPorPromocao);
        model.addAttribute("promoPorProduto", promoPorProduto);
        return "promocoes";
    }

    @GetMapping("/promocoes/{id}")
    public String promocaoDetalhe(@PathVariable Long id, Model model) {
        Promocao promocao = promocaoRepository.findById(id).orElse(null);
        if (promocao == null || !promocao.isValidaAgora()) {
            // promocao nao existe, foi ocultada ou ja venceu: nao ha o que mostrar
            return "redirect:/produtos";
        }

        List<Produto> produtos = promocao.getProdutoIds().stream()
                .map(produtoRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(Produto::isAtivo)
                .sorted(Comparator.comparing(Produto::getNome))
                .collect(Collectors.toList());

        Map<Long, PromocaoProdutoInfo> promoPorProduto = new HashMap<>();
        for (Produto produto : produtos) {
            BigDecimal precoPromocional = promocao.getPrecosPromocionais().get(produto.getId());
            promoPorProduto.put(produto.getId(), new PromocaoProdutoInfo(promocao, precoPromocional));
        }

        model.addAttribute("promocao", promocao);
        model.addAttribute("produtos", produtos);
        model.addAttribute("promoPorProduto", promoPorProduto);
        return "promocao-detalhe";
    }

    // produtos ativos de cada promocao valida, para exibir os cards reais na home (nao so o nome)
    private Map<Long, List<Produto>> produtosAtivosPorPromocao(List<Promocao> promocoes) {
        Map<Long, List<Produto>> mapa = new HashMap<>();
        for (Promocao promocao : promocoes) {
            List<Produto> produtos = new ArrayList<>();
            for (Long produtoId : promocao.getProdutoIds()) {
                produtoRepository.findById(produtoId)
                        .filter(Produto::isAtivo)
                        .ifPresent(produtos::add);
            }
            produtos.sort(Comparator.comparing(Produto::getNome));
            mapa.put(promocao.getId(), produtos);
        }
        return mapa;
    }
}
