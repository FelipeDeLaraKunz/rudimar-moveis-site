package com.rudimarmoveis.site.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * As imagens enviadas pelo admin ficam salvas fora do jar (pasta configurada em
 * app.upload-dir). Este mapeamento faz o Spring servir esses arquivos em /uploads/**,
 * como se fossem estaticos.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String localizacao = "file:" + Paths.get(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(localizacao)
                // cada arquivo tem um nome unico (UUID) e nunca e sobrescrito - trocar a foto de
                // um produto gera um arquivo novo, nunca reaproveita o nome antigo. Por isso da
                // pra cachear "para sempre" no navegador: nao tem risco de mostrar uma foto velha
                // por engano, e evita rebaixar a mesma imagem de novo a cada visita.
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
