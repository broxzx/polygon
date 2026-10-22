package io.github.broxzx.bookinglab.booking.domain.repository;

import io.github.broxzx.bookinglab.booking.domain.entity.Event;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Integer> {
  Optional<Event> findByIdAndDeletedFalse(Integer id);
}
