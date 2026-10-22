package io.github.broxzx.bookinglab.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBookingDto(
    @NotNull(message = "seat id can't be null")
        @Positive(message = "seat id must be positive number")
        Integer seatId,
    @NotNull(message = "email can't be null")
        @Email(message = "email is not valid")
        @NotBlank(message = "email can't be blank")
        String email) {}
