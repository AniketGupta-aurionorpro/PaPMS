package com.aurionpro.papms.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

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

	private Key key() {
		String secret = env.getProperty("app.jwt.secret");
        assert secret != null;
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(UserDetails user, long expirationMs) {
		Instant now = Instant.now();

		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", user.getAuthorities().stream().map(a -> a.getAuthority()).toList());

		return Jwts.builder().subject(user.getUsername())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.claims(claims)
				.signWith(key())
				.compact();
	}

	public String extractUsername(String token) {
		return parse(token).getPayload().getSubject();
	}

	public boolean isValid(String token, UserDetails user) {
		return user.getUsername().equals(extractUsername(token))
				&& parse(token).getPayload().getExpiration().after(new Date());
	}

	private Jws<Claims> parse(String token) {
		return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token);
	}
}
