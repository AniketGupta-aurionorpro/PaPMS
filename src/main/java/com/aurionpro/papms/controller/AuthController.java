package com.aurionpro.papms.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.AuthRequest;
import com.aurionpro.papms.dto.AuthResponse;
import com.aurionpro.papms.dto.RegisterRequest;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthenticationManager authManager;
	private final JwtService jwtService;
	private final UserDetailsService uds;
	private final PasswordEncoder encoder;
	private final AppUserRepository userRepo;


	@Value("${app.jwt.expiration}")
	private long jwtExp;

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
		if (userRepo.existsByUsername(req.username())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
		}
		User u = User.builder().username(req.username()).password(encoder.encode(req.password())).fullName(req.fullName()).email(req.email()).role(req.role()).organizationId(req.organizationId()).enable(true).build();
		userRepo.save(u);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
		Authentication auth = new UsernamePasswordAuthenticationToken(req.username(), req.password());
		authManager.authenticate(auth);
		var user = uds.loadUserByUsername(req.username());
		String token = jwtService.generateToken(user, jwtExp);
		return ResponseEntity.ok(new AuthResponse(token));
	}
}
