package com.rudimarmoveis.site.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Baixa uma imagem de uma URL externa (usado pelo admin ao colar um link de foto,
 * e pela API de automacao/n8n ao criar produtos com imagens ja encontradas por fora).
 * Centraliza aqui a validacao de SSRF e os limites de tamanho/tipo, pra nao duplicar
 * essa logica sensivel em mais de um lugar.
 */
@Service
public class ImagemExternaService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024; // mesmo limite do upload por arquivo

    public record ImagemBaixada(byte[] conteudo, String contentType) {
    }

    public ImagemBaixada baixar(String url) throws IOException, InterruptedException {
        URI uri = validarUrlDeImagem(url);

        HttpClient client = HttpClient.newBuilder()
                // forca HTTP/1.1: evita que o cliente tente um upgrade h2c que alguns servidores nao respondem
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Mozilla/5.0 (compatible; RudimarMoveisBot/1.0)")
                .GET()
                .build();
        HttpResponse<byte[]> resposta = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        String tipoConteudo = resposta.headers().firstValue("Content-Type").orElse("");
        if (resposta.statusCode() != 200 || !tipoConteudo.startsWith("image/")) {
            throw new IllegalArgumentException("Esse link nao aponta para uma imagem valida.");
        }
        if (resposta.body().length > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Essa imagem e muito grande.");
        }

        return new ImagemBaixada(resposta.body(), tipoConteudo);
    }

    /** Deriva uma extensao de arquivo (com ponto) a partir do Content-Type retornado. */
    public String extensaoPorContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String subtipo = contentType.split("/")[contentType.contains("/") ? 1 : 0].split("\\+")[0].trim();
        return switch (subtipo) {
            case "jpeg", "jpg" -> ".jpg";
            case "png" -> ".png";
            case "webp" -> ".webp";
            case "gif" -> ".gif";
            default -> "";
        };
    }

    // aceita so http/https e bloqueia enderecos internos/privados, para evitar que esse
    // recurso seja usado para acessar recursos internos da rede (SSRF)
    private URI validarUrlDeImagem(String url) throws IOException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Link invalido.");
        }
        String esquema = uri.getScheme();
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("O link precisa comecar com http:// ou https://");
        }
        InetAddress endereco = InetAddress.getByName(uri.getHost());
        if (endereco.isLoopbackAddress() || endereco.isSiteLocalAddress() || endereco.isLinkLocalAddress() || endereco.isAnyLocalAddress()) {
            throw new IllegalArgumentException("Esse link nao e permitido.");
        }
        return uri;
    }
}
