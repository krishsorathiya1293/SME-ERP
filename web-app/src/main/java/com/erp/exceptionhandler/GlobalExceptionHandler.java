package com.erp.exceptionhandler;

import com.erp.api.usermanagement.model.ErrorResponse;
import com.erp.exception.EncryptionException;
import com.erp.exception.EntityNotFoundException;
import com.erp.exception.PdfGenerationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.HtmlUtils;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  private static final String TIMESTAMP = "timestamp";
  private static final String EXCEPTION = "exception";

  private record ErrorSpec(HttpStatus status, String code, String defaultMessage) {}

  private static final ErrorSpec NOT_FOUND =
      new ErrorSpec(HttpStatus.NOT_FOUND, "404", "Resource not found");

  private static final ErrorSpec BAD_REQUEST =
      new ErrorSpec(HttpStatus.BAD_REQUEST, "400", "Bad request");

  private static final ErrorSpec UNPROCESSABLE =
      new ErrorSpec(HttpStatus.UNPROCESSABLE_ENTITY, "422", "Validation failed");

  private static final ErrorSpec UNAUTHORIZED =
      new ErrorSpec(HttpStatus.UNAUTHORIZED, "401", "Unauthorized");

  private static final ErrorSpec FORBIDDEN =
      new ErrorSpec(HttpStatus.FORBIDDEN, "403", "Access denied");

  private static final ErrorSpec CONFLICT = new ErrorSpec(HttpStatus.CONFLICT, "409", "Conflict");

  private static final ErrorSpec INTERNAL_ERROR =
      new ErrorSpec(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Internal server error");

  @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(HttpServletRequest req, Exception ex) {

    String message =
        (ex instanceof NoResourceFoundException)
            ? "No endpoint found: " + req.getRequestURI()
            : ex.getMessage();

    return handle(req, ex, NOT_FOUND, message);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
  public ResponseEntity<ErrorResponse> handleBadRequest(HttpServletRequest req, Exception ex) {

    String message =
        (ex instanceof HttpMessageNotReadableException)
            ? "Malformed or missing request body"
            : ex.getMessage();

    return handle(req, ex, BAD_REQUEST, message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      HttpServletRequest req, MethodArgumentNotValidException ex) {

    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));

    return handle(req, ex, UNPROCESSABLE, message);
  }

  @ExceptionHandler({BadCredentialsException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> handleSecurity(HttpServletRequest req, Exception ex) {

    ErrorSpec spec = ex instanceof BadCredentialsException ? UNAUTHORIZED : FORBIDDEN;

    String message =
        ex instanceof BadCredentialsException ? "Invalid username or password" : "Access denied";

    return handle(req, ex, spec, message);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      HttpServletRequest req, DataIntegrityViolationException ex) {
    Throwable root = ex.getRootCause();
    String sqlState = null;
    if (root instanceof org.hibernate.exception.ConstraintViolationException cve
        && cve.getSQLException() != null) {
      sqlState = cve.getSQLException().getSQLState();
    }
    boolean isUniqueViolation = "23505".equals(sqlState);
    boolean isForeignKeyViolation = "23503".equals(sqlState);
    ErrorSpec spec = (isUniqueViolation || isForeignKeyViolation) ? CONFLICT : BAD_REQUEST;
    String message;
    if (isUniqueViolation) {
      message = "Duplicate value already exists";
    } else {
      if (isForeignKeyViolation) message = "Cannot delete. This record is used in another service";
      else message = "Data integrity violation";
    }
    return handle(req, ex, spec, message);
  }

  @ExceptionHandler({
    EncryptionException.class,
    PdfGenerationFailedException.class,
    Exception.class
  })
  public ResponseEntity<ErrorResponse> handleInternal(HttpServletRequest req, Exception ex) {

    String message =
        (ex instanceof EncryptionException || ex instanceof PdfGenerationFailedException)
            ? null
            : "An unexpected error occurred";

    return handle(req, ex, INTERNAL_ERROR, message);
  }

  private ResponseEntity<ErrorResponse> handle(
      HttpServletRequest req, Exception ex, ErrorSpec spec, String overrideMessage) {

    logError(spec.status(), req, ex);

    String message = overrideMessage != null ? overrideMessage : spec.defaultMessage();

    return error(spec.status(), spec.code(), message, detail(ex));
  }

  private ResponseEntity<ErrorResponse> error(
      HttpStatus status, String code, String message, Map<String, Object> detail) {

    ErrorResponse body = new ErrorResponse();
    body.setCode(code);
    body.setMessage(HtmlUtils.htmlEscape(message));

    if (detail != null) {
      Map<String, Object> safeDetail = new HashMap<>();
      detail.forEach(
          (k, v) -> {
            if (v instanceof String s) {
              safeDetail.put(k, HtmlUtils.htmlEscape(s));
            } else {
              safeDetail.put(k, v);
            }
          });
      body.setDetail(safeDetail);
    }

    return ResponseEntity.status(status).body(body);
  }

  private Map<String, Object> detail(Exception ex) {
    return Map.of(
        TIMESTAMP, LocalDateTime.now().toString(),
        EXCEPTION, ex.getClass().getSimpleName());
  }

  private void logError(HttpStatus status, HttpServletRequest req, Exception ex) {
    log.error(
        "[{}] {} {} — {}",
        status.value(),
        req.getMethod(),
        req.getRequestURI(),
        ex.getMessage(),
        ex);
  }
}
