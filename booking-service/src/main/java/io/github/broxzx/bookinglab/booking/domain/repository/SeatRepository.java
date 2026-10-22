package io.github.broxzx.bookinglab.booking.domain.repository;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;
import io.github.broxzx.bookinglab.booking.domain.entity.Seat;
import io.github.broxzx.bookinglab.booking.dto.SeatDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Integer> {

  List<Seat> findByEventIdAndDeletedIsFalseOrderById(Integer eventId);

  @Query(
      """
      SELECT new io.github.broxzx.bookinglab.booking.dto.SeatDto(
            s.id,
            s.number,
            s.price,
            CASE WHEN EXISTS (SELECT 1 FROM Booking b
                                        WHERE b.seat = s
                                            AND b.deleted = false
                                            AND b.state IN :activeStates)
                THEN true ELSE false END)
        FROM Seat s
      WHERE s.deleted = false
        AND s.event.id = :eventId
      ORDER BY s.id
      """)
  List<SeatDto> findSeatDtosByEventId(Integer eventId, List<Booking.State> activeStates);

  @Query(
      """
      SELECT s
        FROM Seat s
      WHERE s.id = :id
        AND s.deleted = false
        AND s.event.deleted = false
      """)
  Optional<Seat> findByIdAndDeletedFalseAndEventDeletedFalse(Integer id);
}
