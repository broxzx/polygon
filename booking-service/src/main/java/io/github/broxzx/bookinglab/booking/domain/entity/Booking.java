package io.github.broxzx.bookinglab.booking.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {
  public enum State {
    NEW,
    PAID,
    CANCELLED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @NotNull
  @Column(name = "state", nullable = false, length = Integer.MAX_VALUE)
  @Enumerated(EnumType.STRING)
  private State state;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seat_id", nullable = false)
  private Seat seat;

  @NotNull
  @Column(name = "email", nullable = false, length = Integer.MAX_VALUE)
  private String email;

  @NotNull
  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

  @NotNull
  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
