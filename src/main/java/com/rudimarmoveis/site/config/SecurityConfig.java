package com.rudimarmoveis.site.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * So existe UM usuario no sistema: o administrador da loja.
 * Os visitantes do site NAO fazem login - so o /admin fica protegido.
 * Usuario e senha vem do application.properties (variaveis de ambiente em producao).
 */
@Configuration
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        // ADMIN_PASSWORD sem valor default no application.properties ja faz o Spring recusar
        // subir se a variavel nao existir - mas se ela existir e vier VAZIA (ex: docker-compose
        // substitui por string vazia quando falta um .env na VPS, sem dar erro), o Spring
        // resolve isso como "definida, e vazia" e passaria direto. Por isso o cheque aqui:
        // a aplicacao tem que recusar subir com uma senha de admin fraca/vazia, nunca abrir
        // o painel administrativo protegido por "nada".
        if (!StringUtils.hasText(adminPassword) || adminPassword.length() < 8) {
            throw new IllegalStateException(
                    "admin.password (variavel de ambiente ADMIN_PASSWORD) nao esta definida ou e "
                            + "curta demais (minimo 8 caracteres). Configure uma senha forte antes de subir a aplicacao.");
        }
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // pagina publica, css e imagens: liberado para todo mundo
                .requestMatchers("/", "/css/**", "/img/**", "/webjars/**").permitAll()
                .requestMatchers("/admin/login").permitAll()
                // qualquer outra rota /admin/** exige login como ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/produtos", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
