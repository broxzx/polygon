package io.github.broxzx.bookinglab.booking.service;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;
import io.github.broxzx.bookinglab.booking.domain.entity.Event;
import io.github.broxzx.bookinglab.booking.domain.repository.EventRepository;
import io.github.broxzx.bookinglab.booking.domain.repository.SeatRepository;
import io.github.broxzx.bookinglab.booking.dto.SeatDto;
import io.github.broxzx.bookinglab.booking.exceptions.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

  private final SeatRepository seatRepository;
  private final EventRepository eventRepository;

  @Transactional(readOnly = true)
  public List<SeatDto> getSeatsByEventId(Integer eventId) {
    Event event =
        eventRepository
            .findByIdAndDeletedFalse(eventId)
            .orElseThrow(() -> new NotFoundException(Event.class, eventId));

    return seatRepository.findSeatDtosByEventId(event.getId(), Booking.State.getActiveStates());
  }
}
