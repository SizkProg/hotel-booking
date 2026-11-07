package com.example.common;

import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  public record ErrorDTO(Instant timestamp, int status, String message) {}

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ErrorDTO> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(new ErrorDTO(Instant.now(), e.getStatusCode().value(), e.getReason()));
  }

  @ExceptionHandler({
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ErrorDTO> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(new ErrorDTO(Instant.now(), 400, "Некорректный запрос"));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  ResponseEntity<ErrorDTO> conflict(Exception e) {
    return ResponseEntity.status(409).body(new ErrorDTO(Instant.now(), 409, "Конфликт данных"));
  }
}
