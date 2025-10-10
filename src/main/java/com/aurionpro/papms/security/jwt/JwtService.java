package com.aurionpro.papms.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.aurionpro.papms.entity.User; // Import User entity
import com.aurionpro.papms.repository.AppUserRepository; // Import AppUserRepository
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtService {
    private final Environment env;
    private final AppUserRepository userRepository; // Inject repository

    private Key key() {
        String secret = env.getProperty("app.jwt.secret");
        assert secret != null;
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails, long expirationMs) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList());

        // --- START: MODIFICATION FOR PASSWORD CHANGE ---
        // Fetch the full User entity to check the flag
        Optional<User> userEntityOpt = userRepository.findByUsername(userDetails.getUsername());
        userEntityOpt.ifPresent(user -> {
            if (user.getRequiresPasswordChange()) {
                claims.put("requires_password_change", true);
            }
        });
        // --- END: MODIFICATION ---

        return Jwts.builder().subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claims(claims)
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getPayload().getSubject();
    }

    // ADD THIS NEW METHOD to extract any claim
    public <T> T extractClaim(String token, String claimKey, Class<T> claimType) {
        final Claims claims = parse(token).getPayload();
        return claims.get(claimKey, claimType);
    }


    public boolean isValid(String token, UserDetails user) {
        return user.getUsername().equals(extractUsername(token))
                && parse(token).getPayload().getExpiration().after(new Date());
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token);
    }
}