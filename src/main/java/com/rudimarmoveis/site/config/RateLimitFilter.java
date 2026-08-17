package com.rudimarmoveis.site.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit em memoria, por IP, sem depender de Redis ou outro servico externo
 * (o servidor roda com pouca RAM, entao qualquer coisa mais pesada seria desperdicio).
 *
 * Duas regras:
 * - login (/admin/login via POST): poucas tentativas por minuto, para dificultar
 *   um script tentando adivinhar a senha do admin por forca bruta.
 * - geral (qualquer request): um limite mais alto, so para o servidor nao cair
 *   se alguem disparar uma rajada grande de requisicoes.
 *
 * O IP usado e sempre o da conexao TCP (request.getRemoteAddr()) - de proposito NAO
 * confiamos em cabecalhos como X-Forwarded-For aqui, porque sao enviados pelo proprio
 * cliente: se essa aplicacao um dia ficar atras de um proxy/load balancer confiavel,
 * a extracao do IP real deve ser feita via configuracao do proxy (ex.: RemoteIpValve/
 * ForwardedHeaderFilter), nunca lendo o header diretamente, senao o rate limit de
 * login vira inutil (bastaria variar o header a cada tentativa para escapar dele).
 */
@Component
public class RateLimitFilter extends HttpFilter {

    private static final long JANELA_LOGIN_MS = 60_000;
    private static final int LIMITE_LOGIN = 5;

    private static final long JANELA_GERAL_MS = 60_000;
    private static final int LIMITE_GERAL = 300;

    private static final long MAX_IDADE_SEM_USO_MS = JANELA_GERAL_MS * 10;

    private final Map<String, Contador> contadoresLogin = new ConcurrentHashMap<>();
    private final Map<String, Contador> contadoresGerais = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        ScheduledExecutorService limpeza = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "rate-limit-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        limpeza.scheduleAtFixedRate(this::limparEntradasAntigas, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String ip = request.getRemoteAddr();

        if (isTentativaDeLogin(request)) {
            if (!permitir(contadoresLogin, ip, JANELA_LOGIN_MS, LIMITE_LOGIN)) {
                responder429(response, "Muitas tentativas de login. Aguarde 1 minuto e tente novamente.");
                return;
            }
        }

        if (!permitir(contadoresGerais, ip, JANELA_GERAL_MS, LIMITE_GERAL)) {
            responder429(response, "Muitas requisicoes. Aguarde um instante e tente novamente.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isTentativaDeLogin(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + "/admin/login").equals(request.getRequestURI());
    }

    private boolean permitir(Map<String, Contador> contadores, String ip, long janelaMs, int limite) {
        long agora = System.currentTimeMillis();
        Contador contador = contadores.computeIfAbsent(ip, k -> new Contador(agora));

        synchronized (contador) {
            if (agora - contador.inicioJanela >= janelaMs) {
                contador.inicioJanela = agora;
                contador.quantidade.set(0);
            }
            return contador.quantidade.incrementAndGet() <= limite;
        }
    }

    private void limparEntradasAntigas() {
        long agora = System.currentTimeMillis();
        contadoresLogin.entrySet().removeIf(e -> agora - e.getValue().inicioJanela > MAX_IDADE_SEM_USO_MS);
        contadoresGerais.entrySet().removeIf(e -> agora - e.getValue().inicioJanela > MAX_IDADE_SEM_USO_MS);
    }

    private void responder429(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write(mensagem);
    }

    private static final class Contador {
        volatile long inicioJanela;
        final AtomicInteger quantidade = new AtomicInteger(0);

        Contador(long inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }
}
