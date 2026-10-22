package io.github.broxzx.bookinglab.booking.exceptions;

public class NotFoundException extends RuntimeException {

  public NotFoundException(Class<?> clazz, Object id) {
    super("%s with id %s not found".formatted(clazz.getSimpleName(), id));
  }
}
