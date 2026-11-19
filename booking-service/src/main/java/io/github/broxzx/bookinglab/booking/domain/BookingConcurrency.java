package io.github.broxzx.bookinglab.booking.domain;

import lombok.Getter;

@Getter
public enum BookingConcurrency {
  PESSIMISTIC,
  OPTIMISTIC,
  UNIQUE_ONLY
}
