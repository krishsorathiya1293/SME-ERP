package com.erp.exceptionhandler;

import com.erp.api.usermanagement.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  private static final String TIMESTAMP = "timestamp";
  private static final String EXCEPTION = "exception";

  private ResponseEntity<ErrorResponse> error(
      HttpStatus status, String code, String message, Map<String, Object> detail) {

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setMessage(message);
    errorResponse.setCode(code);
    if (detail != null && !detail.isEmpty()) {
      errorResponse.setDetail(detail);
    }

    return ResponseEntity.status(status).body(errorResponse);
  }

  @ExceptionHandler({BadCredentialsException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      HttpServletRequest request, Exception ex) {
    logError(HttpStatus.UNAUTHORIZED, request, ex);
    return error(
        HttpStatus.UNAUTHORIZED,
        "401",
        ex.getMessage(),
        Map.of(
            TIMESTAMP, LocalDateTime.now().toString(), EXCEPTION, ex.getClass().getSimpleName()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAll(HttpServletRequest request, Exception ex) {
    logError(HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "500",
        ex.getMessage(),
        Map.of(
            TIMESTAMP, LocalDateTime.now().toString(), EXCEPTION, ex.getClass().getSimpleName()));
  }

  public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest request, Exception ex) {
    logError(HttpStatus.NOT_FOUND, request, ex);
    return error(
        HttpStatus.NOT_FOUND,
        "404",
        ex.getMessage(),
        Map.of(
            TIMESTAMP, LocalDateTime.now().toString(), EXCEPTION, ex.getClass().getSimpleName()));
  }

  public ResponseEntity<ErrorResponse> handleDuplicateEntity(
      HttpServletRequest request, Exception ex) {
    logError(HttpStatus.CONFLICT, request, ex);
    return error(
        HttpStatus.CONFLICT,
        "409",
        ex.getMessage(),
        Map.of(
            TIMESTAMP, LocalDateTime.now().toString(), EXCEPTION, ex.getClass().getSimpleName()));
  }

  public ResponseEntity<ErrorResponse> handleValidationErrors(
      HttpServletRequest request, Exception ex) {
    logError(HttpStatus.UNPROCESSABLE_ENTITY, request, ex);
    return error(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "422",
        ex.getMessage(),
        Map.of(
            TIMESTAMP, LocalDateTime.now().toString(), EXCEPTION, ex.getClass().getSimpleName()));
  }

  private void logError(HttpStatus status, HttpServletRequest request, Exception ex) {
    if (log.isErrorEnabled()) {
      String requestDetails = captureRequestDetails(request);
      log.error(
          "Exception [{}] occurred while processing request [{}]: [{}]",
          status,
          requestDetails,
          ex.getMessage(),
          ex);
    }
  }

  private String captureRequestDetails(HttpServletRequest request) {
    Map<String, String> headers =
        Collections.list(request.getHeaderNames()).stream()
            .collect(Collectors.toMap(headerName -> headerName, request::getHeader));
    return String.format(
        "Method: %s, URI: %s, Headers: %s", request.getMethod(), request.getRequestURI(), headers);
  }
}
