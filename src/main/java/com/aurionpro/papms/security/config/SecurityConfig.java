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



//@Bean
//public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider authenticationProvider)
//		throws Exception {
//
//	http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource()))
//			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//			.authorizeHttpRequests(auth -> auth
//					.requestMatchers("/auth/**", "/v1/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
//                            "/v3/api-docs/**","/actuator/health","/api/organizations/register")
//					.permitAll()
//
//					 // Endpoint with a specific permission check
//		            .requestMatchers(HttpMethod.GET, "/api/organizations/pending").hasRole("BANK_ADMIN")
//
//
//					// GET -> Any authenticated user with a valid role
//					.requestMatchers(HttpMethod.GET, "/**").hasAnyRole("BANK_ADMIN", "ORG_ADMIN", "EMPLOYEE")
//
//					// GET -> Any authenticated user with a valid authority
//					//.requestMatchers(HttpMethod.GET, "/**").hasAnyAuthority("BANK_ADMIN", "ORG_ADMIN", "EMPLOYEE")
//
//					// POST, PUT, PATCH, DELETE -> Admins only
////					.requestMatchers(HttpMethod.POST, "/**").hasAnyAuthority("BANK_ADMIN", "ORG_ADMIN")
////					.requestMatchers(HttpMethod.PUT, "/**").hasAnyAuthority("BANK_ADMIN", "ORG_ADMIN")
////					.requestMatchers(HttpMethod.PATCH, "/**").hasAnyAuthority("BANK_ADMIN", "ORG_ADMIN")
////					.requestMatchers(HttpMethod.DELETE, "/**").hasAnyAuthority("BANK_ADMIN", "ORG_ADMIN")
//
//
//
//					// POST, PUT, PATCH, DELETE -> Admins only
//					.requestMatchers(HttpMethod.POST, "/**").hasAnyRole("BANK_ADMIN", "ORG_ADMIN")
//					.requestMatchers(HttpMethod.PUT, "/**").hasAnyRole("BANK_ADMIN", "ORG_ADMIN")
//					.requestMatchers(HttpMethod.PATCH, "/**").hasAnyRole("BANK_ADMIN", "ORG_ADMIN")
//					.requestMatchers(HttpMethod.DELETE, "/**").hasAnyRole("BANK_ADMIN", "ORG_ADMIN")
//
//					// Anything else must be authenticated
//					.anyRequest().authenticated())
//
//			.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint())
//					.accessDeniedHandler(accessDeniedHandler()))
//
//			.authenticationProvider(authenticationProvider)
//			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//	return http.build();
//}
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/auth/**",
                                "/api/organizations/register",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        //deposit
                        .requestMatchers(HttpMethod.POST, "/api/deposits/self").hasRole("ORG_ADMIN")


                        //bill vendor
                        .requestMatchers("/api/bills/vendors/**").hasRole("ORG_ADMIN")

                        // Organization endpoints
                        .requestMatchers(HttpMethod.GET, "/api/organizations/pending").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/approve").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/reject").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/suspend").hasRole("BANK_ADMIN")

                        // Employee endpoints
                        .requestMatchers(HttpMethod.POST, "/api/organizations/*/employees/**").hasRole("ORG_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/organizations/*/employees/**").hasRole("ORG_ADMIN")

                        // Document endpoints
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/documents/*/approve").hasRole("BANK_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/*/documents/*/reject").hasRole("BANK_ADMIN")

                        // Vendor endpoints
                        .requestMatchers("/api/vendors/**").hasRole("ORG_ADMIN")

                        // Default authorization
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
