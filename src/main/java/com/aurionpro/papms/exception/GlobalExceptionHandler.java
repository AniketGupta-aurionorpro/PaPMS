package com.aurionpro.papms.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), req.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		return ResponseEntity.badRequest()
				.body(new ApiError(Instant.now(), 400, "Bad Request", ex.getMessage(), req.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
		log.error("Unexpected error", ex);
		return ResponseEntity.internalServerError()
				.body(new ApiError(Instant.now(), 500, "Internal Server Error", ex.getMessage(), req.getRequestURI()));
	}
}
