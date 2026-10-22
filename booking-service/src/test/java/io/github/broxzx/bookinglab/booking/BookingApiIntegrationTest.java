package io.github.broxzx.bookinglab.booking;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.broxzx.bookinglab.booking.domain.entity.Booking;
import io.github.broxzx.bookinglab.booking.dto.CreateBookingDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql("classpath:db/seed-dev.sql")
class BookingApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void eventSeatsReturnsActiveSeatsWithOccupancy() throws Exception {
    mockMvc
        .perform(get("/events/{id}/seats", 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].number", contains("A-1", "A-2", "A-3", "A-4", "A-5")))
        .andExpect(jsonPath("$[*].occupied", contains(true, true, false, true, false)));
  }

  @Test
  void eventSeatsReturnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/events/{id}/seats", 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*]").isEmpty());
  }

  @Test
  void eventSeatsReturnsNotFound() throws Exception {
    mockMvc.perform(get("/events/{id}/seats", 3)).andExpect(status().isNotFound());
    mockMvc.perform(get("/events/{id}/seats", 999)).andExpect(status().isNotFound());
  }

  @Test
  void createBookingReturns201() throws Exception {
    final Integer eventId = 1;
    final Integer seatId = 5;
    final String email = "test@abc.com";
    final String body = objectMapper.writeValueAsString(new CreateBookingDto(seatId, email));

    String locationHeaderValue =
        mockMvc
            .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.seatId", equalTo(seatId)))
            .andExpect(jsonPath("$.email", equalTo(email)))
            .andExpect(jsonPath("$.state", equalTo(Booking.State.NEW.name())))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(locationHeaderValue);
    mockMvc
        .perform(get(locationHeaderValue))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.seatId", equalTo(seatId)))
        .andExpect(jsonPath("$.email", equalTo(email)))
        .andExpect(jsonPath("$.state", equalTo(Booking.State.NEW.name())));

    mockMvc
        .perform(get("/events/{id}/seats", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == %d)].occupied", seatId).value(contains(true)));
  }

  @Test
  void createBookingReturns409() throws Exception {
    final Integer seatId = 5;
    final String email = "test@abc.com";
    final String body = objectMapper.writeValueAsString(new CreateBookingDto(seatId, email));

    mockMvc
        .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.seatId", equalTo(seatId)))
        .andExpect(jsonPath("$.email", equalTo(email)))
        .andExpect(jsonPath("$.state", equalTo(Booking.State.NEW.name())));

    mockMvc
        .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void createBookingForDeletedSeat() throws Exception {
    final Integer seatId = 6;
    final String email = "test@abc.com";
    final String body = objectMapper.writeValueAsString(new CreateBookingDto(seatId, email));

    mockMvc
        .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void createBookingWithInvalidEmail() throws Exception {
    final Integer seatId = 5;
    final String email = "abc";
    final String body = objectMapper.writeValueAsString(new CreateBookingDto(seatId, email));

    mockMvc
        .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasKey("email")));
  }

  @Test
  void createBookingWithEmptyEmail() throws Exception {
    final Integer seatId = 5;
    final String email = "";
    final String body = objectMapper.writeValueAsString(new CreateBookingDto(seatId, email));

    mockMvc
        .perform(post("/bookings").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasKey("email")));
  }
}
