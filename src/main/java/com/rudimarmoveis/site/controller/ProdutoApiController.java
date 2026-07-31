package com.rudimarmoveis.site.controller;

import com.rudimarmoveis.site.model.Produto;
import com.rudimarmoveis.site.repository.ProdutoRepository;
import com.rudimarmoveis.site.service.ArmazenamentoImagensService;
import com.rudimarmoveis.site.service.ImagemExternaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API para automacoes externas (ex: n8n) criarem produtos programaticamente.
 * Autenticacao por chave simples no cabecalho X-Api-Key, separada do login do admin
 * (que usa sessao/formulario). Todo produto criado por aqui entra oculto (ativo=false)
 * ate o admin revisar e completar o que faltar, igual acontece na importacao por planilha.
 *
 * Aceita fotos de duas formas, no mesmo endpoint (o Content-Type escolhe qual metodo roda):
 * - JSON com "imagens": [urls] -> o servidor baixa cada link;
 * - multipart/form-data com arquivos de verdade em "imagens" -> salvos direto, sem download.
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoApiController {

    private final ProdutoRepository produtoRepository;
    private final ArmazenamentoImagensService armazenamentoImagens;
    private final ImagemExternaService imagemExterna;
    private final String chaveApi;

    public ProdutoApiController(ProdutoRepository produtoRepository,
                                 ArmazenamentoImagensService armazenamentoImagens,
                                 ImagemExternaService imagemExterna,
                                 @Value("${automacao.api-key:}") String chaveApi) {
        this.produtoRepository = produtoRepository;
        this.armazenamentoImagens = armazenamentoImagens;
        this.imagemExterna = imagemExterna;
        this.chaveApi = chaveApi;
    }

    // variante JSON: fotos chegam como lista de URLs, o servidor baixa cada uma
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> criarProdutoComLinks(@RequestHeader(value = "X-Api-Key", required = false) String chaveRecebida,
                                                   @RequestBody CriarProdutoRequest requisicao) {
        ResponseEntity<?> erro = validarAcesso(chaveRecebida, requisicao.nomeInterno());
        if (erro != null) {
            return erro;
        }

        Produto produto = montarProduto(requisicao.nomeInterno(), requisicao.nome(), requisicao.descricao(),
                requisicao.cor(), requisicao.categoria(), requisicao.marca(), requisicao.preco(),
                requisicao.precoCartao(), requisicao.quantidadeEstoque());

        List<String> imagensSalvas = new ArrayList<>();
        List<String> imagensComFalha = new ArrayList<>();
        if (requisicao.imagens() != null) {
            for (String url : requisicao.imagens()) {
                try {
                    ImagemExternaService.ImagemBaixada imagem = imagemExterna.baixar(url);
                    String extensao = imagemExterna.extensaoPorContentType(imagem.contentType());
                    imagensSalvas.add(armazenamentoImagens.salvar(imagem.conteudo(), extensao));
                } catch (Exception e) {
                    imagensComFalha.add(url);
                }
            }
        }
        produto.setImagens(imagensSalvas);

        Produto salvo = produtoRepository.save(produto);
        return ResponseEntity.ok(respostaCriacao(salvo, imagensSalvas.size(), imagensComFalha));
    }

    // variante multipart: fotos chegam como arquivos de verdade, salvos direto (sem download)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarProdutoComArquivos(@RequestHeader(value = "X-Api-Key", required = false) String chaveRecebida,
                                                      @RequestParam String nomeInterno,
                                                      @RequestParam(required = false) String nome,
                                                      @RequestParam(required = false) String descricao,
                                                      @RequestParam(required = false) String cor,
                                                      @RequestParam(required = false) String categoria,
                                                      @RequestParam(required = false) String marca,
                                                      @RequestParam(required = false) BigDecimal preco,
                                                      @RequestParam(required = false) BigDecimal precoCartao,
                                                      @RequestParam(required = false) Integer quantidadeEstoque,
                                                      @RequestParam(value = "imagens", required = false) List<MultipartFile> imagens) {
        ResponseEntity<?> erro = validarAcesso(chaveRecebida, nomeInterno);
        if (erro != null) {
            return erro;
        }

        Produto produto = montarProduto(nomeInterno, nome, descricao, cor, categoria, marca, preco, precoCartao, quantidadeEstoque);

        List<String> imagensSalvas = new ArrayList<>();
        if (imagens != null) {
            for (MultipartFile arquivo : imagens) {
                String url = armazenamentoImagens.salvar(arquivo);
                if (url != null) {
                    imagensSalvas.add(url);
                }
            }
        }
        produto.setImagens(imagensSalvas);

        Produto salvo = produtoRepository.save(produto);
        return ResponseEntity.ok(respostaCriacao(salvo, imagensSalvas.size(), List.of()));
    }

    private ResponseEntity<?> validarAcesso(String chaveRecebida, String nomeInterno) {
        if (!StringUtils.hasText(chaveApi)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", "AUTOMACAO_API_KEY nao configurada no servidor."));
        }
        if (chaveRecebida == null || !chaveRecebida.equals(chaveApi)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Chave de API invalida ou ausente (cabecalho X-Api-Key)."));
        }
        if (!StringUtils.hasText(nomeInterno)) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o nome interno do produto."));
        }
        return null;
    }

    private Produto montarProduto(String nomeInterno, String nome, String descricao, String cor,
                                   String categoria, String marca, BigDecimal preco,
                                   BigDecimal precoCartao, Integer quantidadeEstoque) {
        Produto produto = new Produto();
        produto.setNomeInterno(nomeInterno);
        produto.setNome(StringUtils.hasText(nome) ? nome : nomeInterno);
        produto.setDescricao(descricao);
        produto.setCor(cor);
        produto.setCategoria(categoria);
        produto.setMarca(marca);
        produto.setPreco(preco != null ? preco : BigDecimal.ZERO);
        produto.setPrecoCartao(precoCartao);
        produto.setQuantidadeEstoque(quantidadeEstoque != null ? quantidadeEstoque : 0);
        // sempre entra oculto: o admin revisa (preco, categoria, fotos) antes de publicar
        produto.setAtivo(false);
        produto.setDestaque(false);
        return produto;
    }

    private Map<String, Object> respostaCriacao(Produto salvo, int imagensSalvas, List<String> imagensComFalha) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id", salvo.getId());
        resposta.put("imagensSalvas", imagensSalvas);
        resposta.put("imagensComFalha", imagensComFalha);
        return resposta;
    }

    public record CriarProdutoRequest(
            String nomeInterno,
            String nome,
            String descricao,
            String cor,
            String categoria,
            String marca,
            BigDecimal preco,
            BigDecimal precoCartao,
            Integer quantidadeEstoque,
            List<String> imagens
    ) {
    }
}
