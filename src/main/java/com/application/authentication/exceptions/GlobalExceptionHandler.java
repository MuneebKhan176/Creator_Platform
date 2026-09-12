package com.application.authentication.exceptions;

import com.application.authentication.dtos.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Invalid request";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // Anything from the data access layer: connection drops, constraint
    // violations, bad SQL, missing columns/tables. Logged in full server-side;
    // the client only learns "it's a database problem", never the query or
    // schema detail (that's an information-disclosure risk otherwise).
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("A database error occurred. Please try again later."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong. Please try again later."));
    }
}