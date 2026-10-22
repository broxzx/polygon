package io.github.broxzx.bookinglab.booking.dto;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;

public record BookingDto(Integer id, Integer seatId, String email, Booking.State state) {

  public BookingDto(Booking booking) {
    this(booking.getId(), booking.getSeat().getId(), booking.getEmail(), booking.getState());
  }
}
