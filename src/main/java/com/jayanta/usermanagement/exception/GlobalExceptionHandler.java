package com.jayanta.usermanagement.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiError> handleUserException(UserException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(ex.getMessage())
                        .code(ex.getCode())
                        .field(ex.getField())
                        .build());
    }

    //  WRONG PASSWORD - 401
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed - Bad credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.builder()
                        .status(HttpStatus.UNAUTHORIZED)
                        .message("Invalid email or password")
                        .code("INVALID_CREDENTIALS")
                        .field("password")
                        .build());
    }

    //  USER NOT FOUND - 401
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UsernameNotFoundException ex) {
        log.warn("Login failed - User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.builder()
                        .status(HttpStatus.UNAUTHORIZED)
                        .message("Invalid email or password")
                        .code("USER_NOT_FOUND")
                        .field("email")
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.builder()
                        .status(HttpStatus.FORBIDDEN)
                        .message("Access denied")
                        .code("ACCESS_DENIED")
                        .build());
    }

    //  FINAL CATCH-ALL with full logging
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message("Internal server error")
                        .code("INTERNAL_ERROR")
                        .build());
    }
}
