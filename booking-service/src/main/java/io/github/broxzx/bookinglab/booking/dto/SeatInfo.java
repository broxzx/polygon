package io.github.broxzx.bookinglab.booking.dto;

import java.math.BigDecimal;

public record SeatInfo(Integer id, String number, BigDecimal price, boolean occupied) {}
