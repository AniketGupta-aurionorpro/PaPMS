package com.aurionpro.papms.security.filter;

import java.io.IOException;

import com.aurionpro.papms.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService uds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = uds.loadUserByUsername(username);
                if (jwtService.isValid(token, user)) {
                    // --- START: ENFORCEMENT LOGIC ---
                    Boolean requiresPasswordChange = jwtService.extractClaim(token, "requires_password_change", Boolean.class);

                    if (Boolean.TRUE.equals(requiresPasswordChange)) {
                        // Allow ONLY the force-change-password endpoint
                        if (!request.getServletPath().equals("/auth/force-change-password")) {
                            sendPasswordChangeRequiredError(response);
                            return; // Stop the filter chain
                        }
                    }
                    // --- END: ENFORCEMENT LOGIC ---

                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // You can log the exception for debugging if needed
        }

        chain.doFilter(request, response);
    }

    private void sendPasswordChangeRequiredError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorDetails = Map.of(
                "error", "Password Change Required",
                "message", "You must change your password before you can access this resource."
        );

        new ObjectMapper().writeValue(response.getWriter(), errorDetails);
    }
}