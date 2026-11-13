package ing.software.dulcemayor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.frontend-url:http://localhost:8081}")
    private String frontendUrl;

    @Value("${app.central-url:http://localhost:5173}")
    private String centralUrl;

    @Value("${app.order-url:http://localhost:5174}")
    private String orderUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.cors.allowed-origins:}")
    private String extraAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/payments/mp/webhook", "/api/payments/mp/verify", "/api/payments/mp/status/**", "/api/payments/config").permitAll()
                        .requestMatchers("/api/checkout/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        Set<String> origins = new LinkedHashSet<>(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174",
                "http://localhost:8081",
                "http://127.0.0.1:8081",
                "http://localhost:3000"
        ));

        addOriginIfPresent(origins, frontendUrl);
        addOriginIfPresent(origins, centralUrl);
        addOriginIfPresent(origins, orderUrl);
        addOriginIfPresent(origins, baseUrl);

        if (extraAllowedOrigins != null && !extraAllowedOrigins.isBlank()) {
            for (String origin : extraAllowedOrigins.split(",")) {
                addOriginIfPresent(origins, origin);
            }
        }

        configuration.setAllowedOrigins(new ArrayList<>(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void addOriginIfPresent(Set<String> origins, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        String trimmed = rawUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.isEmpty()) {
            origins.add(trimmed);
        }
    }
}
