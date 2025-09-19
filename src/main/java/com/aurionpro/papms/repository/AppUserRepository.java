package com.aurionpro.papms.repository;

import java.util.Optional;

import com.aurionpro.papms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AppUserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);
}