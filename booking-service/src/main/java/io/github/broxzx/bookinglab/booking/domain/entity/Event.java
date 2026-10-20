package io.github.broxzx.bookinglab.booking.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Setter
@Getter
@Entity
@Table(name = "events")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
  private String name;

  @NotNull
  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @NotNull
  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

  @NotNull
  @Column(name = "event_date", nullable = false)
  private OffsetDateTime eventDate;

  @NotNull
  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @NotNull
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
