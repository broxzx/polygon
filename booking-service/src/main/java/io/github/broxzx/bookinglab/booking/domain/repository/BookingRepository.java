package io.github.broxzx.bookinglab.booking.domain.repository;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
  boolean existsBySeatIdAndDeletedFalseAndStateIn(Integer seatId, Collection<Booking.State> states);

  Optional<Booking> findByIdAndDeletedFalse(Integer id);
}
