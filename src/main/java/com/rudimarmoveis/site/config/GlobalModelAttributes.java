package com.rudimarmoveis.site.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * Deixa os dados de contato/redes sociais da loja disponiveis em qualquer template,
 * sem precisar repetir em cada controller. Para trocar algum valor, basta editar o
 * application.properties (ou as variaveis de ambiente equivalentes em producao).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${loja.whatsapp}")
    private String lojaWhatsapp;

    @Value("${loja.telefone-exibicao}")
    private String lojaTelefoneExibicao;

    @Value("${loja.endereco}")
    private String lojaEndereco;

    @Value("${loja.instagram}")
    private String lojaInstagram;

    @Value("${loja.facebook}")
    private String lojaFacebook;

    private String lojaMapsUrl;
    private String lojaMapsEmbedUrl;

    @PostConstruct
    public void montarLinkDoMaps() {
        // Junta o nome da loja com o endereco para o Google Maps achar o lugar certo com mais precisao
        String consulta = "Rudimar Moveis, " + lojaEndereco;
        String consultaCodificada = URLEncoder.encode(consulta, StandardCharsets.UTF_8);
        this.lojaMapsUrl = "https://www.google.com/maps/search/?api=1&query=" + consultaCodificada;
        this.lojaMapsEmbedUrl = "https://www.google.com/maps?q=" + consultaCodificada + "&output=embed";
    }

    @ModelAttribute("lojaWhatsapp")
    public String lojaWhatsapp() {
        return lojaWhatsapp;
    }

    @ModelAttribute("lojaTelefoneExibicao")
    public String lojaTelefoneExibicao() {
        return lojaTelefoneExibicao;
    }

    @ModelAttribute("lojaEndereco")
    public String lojaEndereco() {
        return lojaEndereco;
    }

    @ModelAttribute("lojaInstagram")
    public String lojaInstagram() {
        return lojaInstagram;
    }

    @ModelAttribute("lojaFacebook")
    public String lojaFacebook() {
        return lojaFacebook;
    }

    @ModelAttribute("lojaMapsUrl")
    public String lojaMapsUrl() {
        return lojaMapsUrl;
    }

    @ModelAttribute("lojaMapsEmbedUrl")
    public String lojaMapsEmbedUrl() {
        return lojaMapsEmbedUrl;
    }

    // termo digitado na busca da navbar (quando veio de "?busca=..."), para preencher o campo em qualquer pagina
    @ModelAttribute("buscaInicial")
    public String buscaInicial(HttpServletRequest request) {
        String busca = request.getParameter("busca");
        return busca == null ? "" : busca;
    }
}
