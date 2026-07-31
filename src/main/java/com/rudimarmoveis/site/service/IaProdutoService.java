package com.rudimarmoveis.site.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudimarmoveis.site.model.SugestaoProdutoIA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Aciona o gatilho de webhook do n8n a partir do nome tecnico/interno do produto e espera
 * a automacao devolver nome, descricao, cor, categoria e marca ja prontos pro admin revisar.
 * Fotos continuam manuais (upload, colar link ou Ctrl+V) - o n8n nao mexe em imagens.
 */
@Service
public class IaProdutoService {

    private final String webhookUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IaProdutoService(@Value("${n8n.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean disponivel() {
        return StringUtils.hasText(webhookUrl);
    }

    public SugestaoProdutoIA gerarSugestao(String nomeInterno) {
        if (!disponivel()) {
            throw new IllegalStateException("A variavel de ambiente N8N_WEBHOOK_URL nao foi configurada no servidor.");
        }

        try {
            String corpoJson = objectMapper.writeValueAsString(Map.of("nomeInterno", nomeInterno));

            HttpClient client = HttpClient.newBuilder()
                    // forca HTTP/1.1: o cliente Java por padrao tenta um upgrade para HTTP/2 (h2c)
                    // em conexoes http:// simples, e o servidor do n8n nao responde a essa tentativa
                    // (fica pendurado ate estourar o timeout). curl nao tem esse problema por usar HTTP/1.1 direto.
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90)) // a automacao de texto no n8n pode demorar um pouco
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson))
                    .build();

            HttpResponse<String> resposta = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                throw new IllegalStateException("O n8n retornou um erro (HTTP " + resposta.statusCode() + "): " + resposta.body());
            }
            if (!StringUtils.hasText(resposta.body())) {
                throw new IllegalStateException("O n8n nao retornou nenhum dado. Confira se o workflow tem um no \"Respond to Webhook\" no final.");
            }

            return objectMapper.readValue(resposta.body(), SugestaoProdutoIA.class);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Falha ao chamar o webhook do n8n: " + e.getMessage(), e);
        }
    }
}
