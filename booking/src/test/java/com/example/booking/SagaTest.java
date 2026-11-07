package com.example.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "eureka.client.enabled=false")
class SagaTest {
  @Autowired BookingService service;
  @Autowired BookingRepository bookings;
  @MockitoBean HotelClient hotel;
  private BookingService.Input input;

  @BeforeEach
  void setup() {
    bookings.deleteAll();
    input =
        new BookingService.Input(
            UUID.randomUUID(), 1L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
  }

  @Test
  void confirmationAndReplay() {
    var b = service.create(1L, input, "jwt");
    assertEquals("CONFIRMED", b.status);
    assertEquals(b.id, service.create(1L, input, "jwt").id);
    verify(hotel, times(1)).confirm(any());
  }

  @Test
  void remoteFailureCompensates() {
    doThrow(new org.springframework.web.client.ResourceAccessException("connection refused"))
        .when(hotel)
        .confirm(any());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(1L, input, "jwt"));
    var b = bookings.findById(input.requestId()).orElseThrow();
    assertEquals("CANCELLED", b.status);
    assertFalse(b.compensationRequired);
    verify(hotel).release(any());
  }

  @Test
  void timeoutAndFailedCompensationAreRetried() {
    doThrow(
            new org.springframework.web.client.ResourceAccessException(
                "read timed out", new java.net.SocketTimeoutException()))
        .when(hotel)
        .confirm(any());
    doThrow(new org.springframework.web.client.ResourceAccessException("offline"))
        .when(hotel)
        .release(any());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(1L, input, "jwt"));
    assertTrue(bookings.findById(input.requestId()).orElseThrow().compensationRequired);
    doNothing().when(hotel).release(any());
    service.repair();
    assertFalse(bookings.findById(input.requestId()).orElseThrow().compensationRequired);
  }

  @Test
  void anotherUserCannotSeeBooking() {
    var b = service.create(1L, input, "jwt");
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class, () -> service.own(b.id, 2L));
  }

  @Test
  void changedPayloadRejected() {
    service.create(1L, input, "jwt");
    var other =
        new BookingService.Input(input.requestId(), 2L, false, input.startDate(), input.endDate());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(1L, other, "jwt"));
  }

  @Test
  void backwardsDatesRejected() {
    var invalid =
        new BookingService.Input(UUID.randomUUID(), 1L, false, input.endDate(), input.startDate());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(1L, invalid, "jwt"));
    verifyNoInteractions(hotel);
  }
}
