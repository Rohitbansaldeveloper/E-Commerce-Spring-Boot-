package com.scaler.productservicefeb25.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex) {
        Map<String,Object> body = base(400, "VALIDATION_ERROR");
        Map<String,String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,Object>> constraint(ConstraintViolationException ex) {
        Map<String,Object> body = base(400, "VALIDATION_ERROR"); body.put("message", ex.getMessage()); return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String,Object>> notFound(ProductNotFoundException ex) { Map<String,Object> b=base(404,"PRODUCT_NOT_FOUND"); b.put("message",ex.getMessage()); return ResponseEntity.status(404).body(b); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> badRequest(IllegalArgumentException ex) { Map<String,Object> b=base(400,"BAD_REQUEST"); b.put("message",ex.getMessage()); return ResponseEntity.badRequest().body(b); }
    private Map<String,Object> base(int status,String error){ Map<String,Object> b=new LinkedHashMap<>(); b.put("timestamp", Instant.now()); b.put("status",status); b.put("error",error); return b; }
}
