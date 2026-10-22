package io.github.broxzx.bookinglab.booking.controller;

import io.github.broxzx.bookinglab.booking.dto.SeatDto;
import io.github.broxzx.bookinglab.booking.service.EventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EventController {

  private final EventService eventService;

  @GetMapping("/events/{id}/seats")
  public ResponseEntity<List<SeatDto>> getEventSeats(@PathVariable(value = "id") Integer eventId) {
    return ResponseEntity.ok(eventService.getSeatsByEventId(eventId));
  }
}
