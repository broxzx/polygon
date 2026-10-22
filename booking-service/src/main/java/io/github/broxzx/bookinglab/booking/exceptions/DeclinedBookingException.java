package io.github.broxzx.bookinglab.booking.exceptions;

public class DeclinedBookingException extends RuntimeException {
  public DeclinedBookingException(String number) {
    super("Booking is declined as seat with number '%s' already taken".formatted(number));
  }
}
