package io.github.broxzx.bookinglab.booking.service;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;
import io.github.broxzx.bookinglab.booking.domain.entity.Seat;
import io.github.broxzx.bookinglab.booking.domain.repository.BookingRepository;
import io.github.broxzx.bookinglab.booking.domain.repository.SeatRepository;
import io.github.broxzx.bookinglab.booking.dto.BookingDto;
import io.github.broxzx.bookinglab.booking.dto.CreateBookingDto;
import io.github.broxzx.bookinglab.booking.exceptions.DeclinedBookingException;
import io.github.broxzx.bookinglab.booking.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final SeatRepository seatRepository;

  @Transactional
  public BookingDto createBooking(CreateBookingDto createBookingDto) {
    Seat seat =
        seatRepository
            .findByIdAndDeletedFalseAndEventDeletedFalse(createBookingDto.seatId())
            .orElseThrow(() -> new NotFoundException(Seat.class, createBookingDto.seatId()));

    if (bookingRepository.existsBySeatIdAndDeletedFalseAndStateIn(
        createBookingDto.seatId(), Booking.State.getActiveStates())) {
      throw new DeclinedBookingException(seat.getNumber());
    }

    Booking createdBooking =
        bookingRepository.save(
            Booking.builder()
                .state(Booking.State.NEW)
                .seat(seat)
                .email(createBookingDto.email())
                .build());

    return new BookingDto(createdBooking);
  }

  public BookingDto getById(Integer id) {
    return bookingRepository
        .findByIdAndDeletedFalse(id)
        .map(BookingDto::new)
        .orElseThrow(() -> new NotFoundException(Booking.class, id));
  }
}
