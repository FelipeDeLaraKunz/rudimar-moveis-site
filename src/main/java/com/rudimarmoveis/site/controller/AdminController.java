package com.rudimarmoveis.site.controller;

import com.rudimarmoveis.site.model.Produto;
import com.rudimarmoveis.site.model.Promocao;
import com.rudimarmoveis.site.repository.ProdutoRepository;
import com.rudimarmoveis.site.repository.PromocaoRepository;
import com.rudimarmoveis.site.service.ArmazenamentoImagensService;
import com.rudimarmoveis.site.service.ImagemExternaService;
import com.rudimarmoveis.site.service.PlanilhaEstoqueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProdutoRepository produtoRepository;
    private final PromocaoRepository promocaoRepository;
    private final ArmazenamentoImagensService armazenamentoImagens;
    private final PlanilhaEstoqueService planilhaEstoqueService;
    private final ImagemExternaService imagemExterna;

    @Autowired
    public AdminController(ProdutoRepository produtoRepository,
                            PromocaoRepository promocaoRepository,
                            ArmazenamentoImagensService armazenamentoImagens,
                            PlanilhaEstoqueService planilhaEstoqueService,
                            ImagemExternaService imagemExterna) {
        this.produtoRepository = produtoRepository;
        this.promocaoRepository = promocaoRepository;
        this.armazenamentoImagens = armazenamentoImagens;
        this.planilhaEstoqueService = planilhaEstoqueService;
        this.imagemExterna = imagemExterna;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    // ---------- PRODUTOS / ESTOQUE ----------

    @GetMapping("/produtos")
    public String listarProdutos(@RequestParam(value = "novo", required = false) Boolean novo, Model model) {
        List<Produto> produtos = produtoRepository.findAll();
        model.addAttribute("produtos", produtos);
        model.addAttribute("marcasExistentes", extrairMarcas(produtos));
        model.addAttribute("abrirModal", Boolean.TRUE.equals(novo));
        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        return "admin/produtos";
    }

    @PostMapping("/produtos")
    public String salvarProduto(@Valid @ModelAttribute("produto") Produto produto,
                                 BindingResult result,
                                 @RequestParam(value = "novasImagens", required = false) List<MultipartFile> novasImagens,
                                 @RequestParam(value = "imagensRemover", required = false) List<String> imagensRemover,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            List<Produto> produtos = produtoRepository.findAll();
            model.addAttribute("produtos", produtos);
            model.addAttribute("marcasExistentes", extrairMarcas(produtos));
            model.addAttribute("abrirModal", true);
            return "admin/produtos";
        }

        // monta a lista final de imagens: comeca com as que ja existiam (se for edicao)
        List<String> imagensExistentes = new ArrayList<>();
        if (produto.getId() != null) {
            produtoRepository.findById(produto.getId())
                    .ifPresent(existente -> imagensExistentes.addAll(existente.getImagens()));
        }

        // processa as fotos novas ANTES de mexer nas existentes: se alguma falhar, nada foi
        // apagado ainda (nem do disco, nem do banco) e da pra voltar pro formulario sem
        // ter perdido/quebrado nenhuma foto que ja estava la
        List<String> imagensNovas = new ArrayList<>();
        if (novasImagens != null) {
            try {
                for (MultipartFile arquivo : novasImagens) {
                    String url = armazenamentoImagens.salvar(arquivo);
                    if (url != null) {
                        imagensNovas.add(url);
                    }
                }
            } catch (ArmazenamentoImagensService.ImagemInvalidaException e) {
                redirectAttributes.addFlashAttribute("erroUpload", e.getMessage());
                produto.setImagens(imagensExistentes);
                redirectAttributes.addFlashAttribute("produto", produto);
                return produto.getId() != null
                        ? "redirect:/admin/produtos/" + produto.getId() + "/editar"
                        : "redirect:/admin/produtos?novo=true";
            }
        }

        List<String> imagensFinal = new ArrayList<>(imagensExistentes);
        if (imagensRemover != null) {
            imagensRemover.forEach(armazenamentoImagens::excluir);
            imagensFinal.removeAll(imagensRemover);
        }
        imagensFinal.addAll(imagensNovas);
        produto.setImagens(imagensFinal);

        produtoRepository.save(produto);
        return "redirect:/admin/produtos";
    }

    @GetMapping("/produtos/{id}/editar")
    public String editarProduto(@PathVariable Long id, Model model) {
        // se ja veio um "produto" via flash (redirecionado de volta por causa de um erro no
        // salvamento), usa ele em vez de recarregar do banco - senao as edicoes que o admin
        // tinha acabado de fazer no formulario seriam perdidas
        if (!model.containsAttribute("produto")) {
            Produto produto = produtoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado: " + id));
            model.addAttribute("produto", produto);
        }
        List<Produto> produtos = produtoRepository.findAll();
        model.addAttribute("produtos", produtos);
        model.addAttribute("marcasExistentes", extrairMarcas(produtos));
        model.addAttribute("abrirModal", true);
        return "admin/produtos";
    }

    @PostMapping("/produtos/{id}/excluir")
    public String excluirProduto(@PathVariable Long id) {
        produtoRepository.findById(id).ifPresent(produto -> {
            produto.getImagens().forEach(armazenamentoImagens::excluir);
            produtoRepository.delete(produto);
        });
        return "redirect:/admin/produtos";
    }

    // reprocessa (redimensiona/recomprime) as fotos ja enviadas de produtos e promocoes que
    // estao maiores que o necessario - pra melhorar fotos enviadas antes dessa otimizacao
    // existir (fotos novas ja saem otimizadas na hora do upload, ver ArmazenamentoImagensService)
    @PostMapping("/produtos/otimizar-fotos")
    public String otimizarFotos(RedirectAttributes redirectAttributes) {
        List<String> caminhos = new ArrayList<>();
        produtoRepository.findAll().forEach(p -> caminhos.addAll(p.getImagens()));
        promocaoRepository.findAll().forEach(promo -> {
            if (StringUtils.hasText(promo.getImagemUrl())) {
                caminhos.add(promo.getImagemUrl());
            }
        });

        ArmazenamentoImagensService.ResultadoOtimizacao resultado = armazenamentoImagens.otimizarExistentes(caminhos);
        String mensagem;
        if (resultado.otimizadas() == 0) {
            mensagem = "Nenhuma foto precisava ser otimizada - todas já estão no tamanho ideal.";
        } else {
            double economiaMb = (resultado.bytesAntes() - resultado.bytesDepois()) / 1024.0 / 1024.0;
            mensagem = resultado.otimizadas() + " foto(s) otimizada(s), economizando "
                    + String.format(Locale.forLanguageTag("pt-BR"), "%.1f", economiaMb) + " MB no total.";
        }
        if (resultado.comFalha() > 0) {
            mensagem += " " + resultado.comFalha() + " foto(s) não puderam ser processadas (formato não suportado).";
        }
        redirectAttributes.addFlashAttribute("otimizacaoFotos", mensagem);
        return "redirect:/admin/produtos";
    }

    // cria produtos em lote a partir de uma planilha de estoque (quantidade, nome tecnico, categoria e marca).
    // eles entram ocultos do site (sem preco/foto ainda) ate o admin completar o cadastro de cada um.
    @PostMapping("/produtos/importar")
    public String importarProdutos(@RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes redirectAttributes) {
        if (arquivo == null || arquivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("erroImportacao", "Selecione um arquivo de planilha (.xls ou .xlsx).");
            return "redirect:/admin/produtos";
        }
        try {
            List<Produto> produtos = planilhaEstoqueService.importar(arquivo);
            produtoRepository.saveAll(produtos);
            redirectAttributes.addFlashAttribute("importados", produtos.size());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erroImportacao", "Não foi possível importar essa planilha: " + e.getMessage());
        }
        return "redirect:/admin/produtos";
    }

    // gera uma planilha com todo o estoque atual, ja com preco, nome de venda e id da foto principal
    @GetMapping("/produtos/exportar")
    public ResponseEntity<byte[]> exportarProdutos() throws IOException {
        byte[] planilha = planilhaEstoqueService.exportar(produtoRepository.findAll());
        String nomeArquivo = "estoque-rudimar-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(planilha);
    }

    // baixa uma imagem de uma URL externa e devolve os bytes brutos, para o JS anexar
    // como se fosse um arquivo escolhido do computador (mesmo fluxo de preview/remocao/upload)
    @PostMapping("/produtos/baixar-imagem-url")
    @ResponseBody
    public ResponseEntity<?> baixarImagemUrl(@RequestBody BaixarImagemRequest requisicao) {
        if (!StringUtils.hasText(requisicao.url())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Cole o link de uma imagem."));
        }
        try {
            ImagemExternaService.ImagemBaixada imagem = imagemExterna.baixar(requisicao.url());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(imagem.contentType()))
                    .body(imagem.conteudo());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("erro", "Nao foi possivel baixar essa imagem."));
        }
    }

    public record BaixarImagemRequest(String url) {
    }

    // sem isso, um upload que passa do limite configurado (multipart.max-request-size)
    // vira um 403 generico e confuso, em vez de avisar o admin que o(s) arquivo(s) sao
    // grandes demais - ver comentario no application.properties (resolve-lazily) para
    // o motivo do 403.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tratarUploadGrandeDemais(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erroUpload",
                "O(s) arquivo(s) enviado(s) sao grandes demais. Tente enviar fotos menores ou em menor quantidade de cada vez.");
        String destino = request.getRequestURI().contains("/promocoes") ? "/admin/promocoes" : "/admin/produtos";
        return "redirect:" + destino;
    }

    // ---------- PROMOCOES ----------

    @GetMapping("/promocoes")
    public String listarPromocoes(@RequestParam(value = "novo", required = false) Boolean novo, Model model) {
        popularModeloPromocoes(model);
        model.addAttribute("abrirModal", Boolean.TRUE.equals(novo));
        if (!model.containsAttribute("promocao")) {
            model.addAttribute("promocao", new Promocao());
        }
        return "admin/promocoes";
    }

    @PostMapping("/promocoes")
    public String salvarPromocao(@Valid @ModelAttribute("promocao") Promocao promocao,
                                  BindingResult result,
                                  @RequestParam(value = "imagemUpload", required = false) MultipartFile imagemUpload,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            popularModeloPromocoes(model);
            model.addAttribute("abrirModal", true);
            return "admin/promocoes";
        }

        try {
            String novaImagem = armazenamentoImagens.salvar(imagemUpload);
            if (novaImagem != null) {
                promocao.setImagemUrl(novaImagem);
            } else if (promocao.getId() != null) {
                // nao enviou uma nova foto na edicao: mantem a imagem que ja estava salva
                promocaoRepository.findById(promocao.getId())
                        .ifPresent(existente -> promocao.setImagemUrl(existente.getImagemUrl()));
            }
        } catch (ArmazenamentoImagensService.ImagemInvalidaException e) {
            redirectAttributes.addFlashAttribute("erroUpload", e.getMessage());
            redirectAttributes.addFlashAttribute("promocao", promocao);
            return promocao.getId() != null
                    ? "redirect:/admin/promocoes/" + promocao.getId() + "/editar"
                    : "redirect:/admin/promocoes?novo=true";
        }

        promocaoRepository.save(promocao);
        return "redirect:/admin/promocoes";
    }

    @GetMapping("/promocoes/{id}/editar")
    public String editarPromocao(@PathVariable Long id, Model model) {
        // se ja veio uma "promocao" via flash (redirecionado de volta por erro no salvamento),
        // usa ela em vez de recarregar do banco - senao as edicoes do formulario se perderiam
        if (!model.containsAttribute("promocao")) {
            Promocao promocao = promocaoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Promocao nao encontrada: " + id));
            model.addAttribute("promocao", promocao);
        }
        popularModeloPromocoes(model);
        model.addAttribute("abrirModal", true);
        return "admin/promocoes";
    }

    @PostMapping("/promocoes/{id}/excluir")
    public String excluirPromocao(@PathVariable Long id) {
        promocaoRepository.findById(id).ifPresent(promocao -> {
            armazenamentoImagens.excluir(promocao.getImagemUrl());
            promocaoRepository.delete(promocao);
        });
        return "redirect:/admin/promocoes";
    }

    // lista de marcas ja usadas em algum produto, para sugerir no autocomplete do formulario
    private List<String> extrairMarcas(List<Produto> produtos) {
        return produtos.stream()
                .map(Produto::getMarca)
                .filter(marca -> marca != null && !marca.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // monta os atributos comuns as telas de listagem/edicao de promocoes
    private void popularModeloPromocoes(Model model) {
        List<Promocao> promocoes = promocaoRepository.findAll();

        // so oferece para selecionar produtos que estao ativos e com estoque disponivel
        List<Produto> produtosDisponiveis = produtoRepository.findAll().stream()
                .filter(Produto::isAtivo)
                .filter(p -> p.getQuantidadeEstoque() != null && p.getQuantidadeEstoque() > 0)
                .collect(Collectors.toList());

        // nomes dos produtos vinculados a cada promocao, para exibir na tabela de listagem
        Map<Long, String> nomesPorPromocao = new HashMap<>();
        for (Promocao promo : promocoes) {
            if (promo.getProdutoIds() != null && !promo.getProdutoIds().isEmpty()) {
                String nomes = promo.getProdutoIds().stream()
                        .map(id -> produtoRepository.findById(id).map(Produto::getNomeInterno).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                nomesPorPromocao.put(promo.getId(), nomes);
            }
        }

        model.addAttribute("promocoes", promocoes);
        model.addAttribute("produtosDisponiveis", produtosDisponiveis);
        model.addAttribute("nomesPorPromocao", nomesPorPromocao);
    }
}
