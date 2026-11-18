package io.github.broxzx.bookinglab.booking.exceptions.handler;

import io.github.broxzx.bookinglab.booking.exceptions.DeclinedBookingException;
import io.github.broxzx.bookinglab.booking.exceptions.NotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleEntityNotFoundException(NotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(DeclinedBookingException.class)
  public ProblemDetail handleDeclinedBookingException(DeclinedBookingException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLockException(OptimisticLockingFailureException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Seat was booked by another request");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    if (e.getCause() instanceof ConstraintViolationException exception
        && "idx__unique_active_booking".equalsIgnoreCase(exception.getConstraintName())) {
      return ProblemDetail.forStatusAndDetail(
          HttpStatus.CONFLICT, "Seat was booked by another request");
    }

    log.error("DataIntegrityViolationException occurred: ", e);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, try again");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(
          error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value"));
    }

    ProblemDetail body = ex.getBody();
    body.setDetail("Request validation failed");
    body.setProperty("errors", errors);
    return handleExceptionInternal(ex, body, headers, status, request);
  }
}
