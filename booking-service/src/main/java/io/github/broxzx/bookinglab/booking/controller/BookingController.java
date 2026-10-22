package io.github.broxzx.bookinglab.booking.controller;

import io.github.broxzx.bookinglab.booking.dto.BookingDto;
import io.github.broxzx.bookinglab.booking.dto.CreateBookingDto;
import io.github.broxzx.bookinglab.booking.service.BookingService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @PostMapping
  public ResponseEntity<BookingDto> createBooking(
      @RequestBody @Valid CreateBookingDto createBookingDto) {
    BookingDto booking = bookingService.createBooking(createBookingDto);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(booking.id())
            .toUri();
    return ResponseEntity.created(location).body(booking);
  }

  @GetMapping("/{id:\\d+}")
  public ResponseEntity<BookingDto> getBookingById(@PathVariable("id") Integer bookingId) {
    return ResponseEntity.ok(bookingService.getById(bookingId));
  }
}
