package com.qdauth.exception;

import com.qdauth.controller.AccountController;
import com.qdauth.dto.ErrorResponse;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleConflict(IllegalArgumentException ex) {
    return new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed.", errors);
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNotFound(IllegalStateException ex) {
    return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleSecurity(SecurityException ex) {
    return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleGeneric(Exception ex) {
    log.error(HttpStatus.INTERNAL_SERVER_ERROR.toString(), ex);
    return new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred.");
  }
}
