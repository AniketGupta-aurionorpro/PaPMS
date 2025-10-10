package com.aurionpro.papms.security.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.aurionpro.papms.security.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService uds;

    // Password encoder
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // DAO auth provider
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        // Use the non-deprecated constructor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/auth/**",
                                "/auth/login",
                                "/api/organizations/register",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        // Deposit endpoints
                        .requestMatchers(HttpMethod.POST, "/api/deposits/self").hasRole("ORG_ADMIN")

                        // Vendor Bill endpoints
                        .requestMatchers("/api/bills/vendors/**").hasRole("ORG_ADMIN")

                        // Organization endpoints
                        .requestMatchers(HttpMethod.GET, "/api/organizations/pending").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/approve").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/reject").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/suspend").hasRole("BANK_ADMIN")

                        // Employee endpoints
                        .requestMatchers(HttpMethod.POST, "/api/organizations/*/employees/**").hasRole("ORG_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/organizations/*/employees/**").hasRole("ORG_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/organizations/*/employees/payslips/**").hasRole("EMPLOYEE")

                        // Document endpoints
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/documents/*/approve").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/documents/*/reject").hasRole("BANK_ADMIN")

                        // Vendor endpoints
                        .requestMatchers("/api/vendors/**").hasRole("ORG_ADMIN")

                        // Payroll endpoints
                        .requestMatchers(HttpMethod.POST, "/api/organizations/*/payrolls").hasRole("ORG_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/organizations/*/payrolls/**").hasRole("ORG_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payrolls/pending").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payrolls/*").hasAnyRole("BANK_ADMIN", "ORG_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/payrolls/*/approve").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/payrolls/*/reject").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/auth/force-change-password").authenticated()
                        // CATCH-ALL RULE: MUST BE LAST!
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider(passwordEncoder()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    // CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // 401
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication is required or the token is invalid.", request);
    }

    // 403
    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN", "You do not have permission to access this resource.", request);
    }

    private void writeJsonError(HttpServletResponse response, int status, String code, String message,
                                HttpServletRequest request) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        String json = """
				{
				  "timestamp": "%s",
				  "status": %d,
				  "error": "%s",
				  "message": "%s",
				  "path": "%s"
				}
				""".formatted(Instant.now().toString(), status, code, escape(message), request.getRequestURI());
        response.getWriter().write(json);
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}