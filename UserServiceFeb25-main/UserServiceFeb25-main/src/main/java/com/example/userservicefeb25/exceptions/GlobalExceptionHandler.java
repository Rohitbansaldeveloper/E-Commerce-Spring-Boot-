package com.example.userservicefeb25.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex){Map<String,Object>b=base(400,"VALIDATION_ERROR");Map<String,String>e=new LinkedHashMap<>();ex.getBindingResult().getFieldErrors().forEach(x->e.put(x.getField(),x.getDefaultMessage()));b.put("errors",e);return ResponseEntity.badRequest().body(b);}
 @ExceptionHandler(ConstraintViolationException.class) public ResponseEntity<Map<String,Object>> constraint(ConstraintViolationException ex){Map<String,Object>b=base(400,"VALIDATION_ERROR");b.put("message",ex.getMessage());return ResponseEntity.badRequest().body(b);}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,Object>> bad(IllegalArgumentException ex){Map<String,Object>b=base(400,"BAD_REQUEST");b.put("message",ex.getMessage());return ResponseEntity.badRequest().body(b);}
 @ExceptionHandler(IllegalStateException.class) public ResponseEntity<Map<String,Object>> state(IllegalStateException ex){Map<String,Object>b=base(409,"CONFLICT");b.put("message",ex.getMessage());return ResponseEntity.status(409).body(b);}
 private Map<String,Object>base(int s,String e){Map<String,Object>b=new LinkedHashMap<>();b.put("timestamp",Instant.now());b.put("status",s);b.put("error",e);return b;}
}
