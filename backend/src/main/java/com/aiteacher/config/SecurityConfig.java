package com.aiteacher.config;

import com.aiteacher.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public paths
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/health/detailed").permitAll()
                        .requestMatchers("/api/health/database").permitAll()
                        .requestMatchers("/api/health/redis").permitAll()
                        .requestMatchers("/api/health/minio").permitAll()
                        .requestMatchers("/api/health/ai-providers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/{id}").permitAll()
                        .requestMatchers("/api/payment/channels").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/payment/callback/**").permitAll()
                        // OPTIONS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        System.out.println("[CORS DEBUG] SecurityConfig.corsConfigurationSource() called");
        System.out.println("[CORS DEBUG] corsAllowedOrigins from @Value: " + corsAllowedOrigins);
        System.out.println("[CORS DEBUG] CORS_ALLOWED_ORIGINS env: " + System.getenv("CORS_ALLOWED_ORIGINS"));

        // Allow all origins for preflight OPTIONS — this is the simplest fix for CORS 403
        // Spring Security's CorsFilter processes preflight before any auth rules apply
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}