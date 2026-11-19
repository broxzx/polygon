package io.github.broxzx.bookinglab.booking.config.properties;

import io.github.broxzx.bookinglab.booking.domain.BookingConcurrency;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "booking")
public class BookingProperties {

  private BookingConcurrency concurrency;
}
