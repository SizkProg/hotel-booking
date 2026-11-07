package com.example.booking;

import static org.springframework.http.HttpStatus.*;

import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@EnableScheduling
public class BookingService {
  private static final Logger log = LoggerFactory.getLogger(BookingService.class);
  private final BookingTransactions tx;
  private final BookingRepository bookings;
  private final HotelClient hotel;

  public record Input(
      UUID requestId, Long roomId, boolean autoSelect, LocalDate startDate, LocalDate endDate) {}

  public BookingService(BookingTransactions t, BookingRepository b, HotelClient h) {
    tx = t;
    bookings = b;
    hotel = h;
  }

  // Одинаковые requestId обрабатываются по очереди. Число блокировок ограничено.
  private final Object[] locks =
      java.util.stream.IntStream.range(0, 256).mapToObj(i -> new Object()).toArray();

  private Object lock(UUID id) {
    return locks[Math.floorMod(id.hashCode(), locks.length)];
  }

  public Booking create(Long user, Input in, String jwt) {
    if (in.requestId() == null)
      throw new ResponseStatusException(BAD_REQUEST, "requestId обязателен");
    synchronized (lock(in.requestId())) {
      var b = tx.pending(user, in);
      if (!b.status.equals("PENDING")) return b;
      log.info("bookingId={} state=PENDING", b.id);
      try {
        if (b.roomId == null) {
          Long room = in.roomId();
          if (in.autoSelect()) {
            var choices = hotel.recommend(b, jwt);
            if (choices.isEmpty())
              throw new ResponseStatusException(CONFLICT, "Нет свободных номеров");
            room = choices.get(0).id();
          }
          b = tx.room(b.id, room);
        }
        hotel.confirm(b);
        b = tx.state(b.id, "CONFIRMED", false);
        log.info("bookingId={} state=CONFIRMED", b.id);
        return b;
      } catch (RuntimeException e) {
        b = tx.state(b.id, "CANCELLED", b.roomId != null);
        compensate(b);
        log.warn("bookingId={} state=CANCELLED cause={}", b.id, e.toString());
        if (e instanceof ResponseStatusException r) throw r;
        if (e instanceof org.springframework.web.client.HttpClientErrorException.Conflict)
          throw new ResponseStatusException(CONFLICT, "Номер занят");
        throw new ResponseStatusException(
            SERVICE_UNAVAILABLE, "Hotel недоступен; бронирование отменено");
      }
    }
  }

  public Booking own(UUID id, Long user) {
    var b = bookings.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    if (!b.userId.equals(user)) throw new ResponseStatusException(NOT_FOUND);
    return b;
  }

  public Booking cancel(UUID id, Long user) {
    synchronized (lock(id)) {
      var b = own(id, user);
      b = tx.state(id, "CANCELLED", b.roomId != null);
      compensate(b);
      return bookings.findById(id).orElseThrow();
    }
  }

  private void compensate(Booking b) {
    if (b.roomId == null) return;
    try {
      hotel.release(b);
      tx.state(b.id, "CANCELLED", false);
      log.info("bookingId={} compensation=SUCCESS", b.id);
    } catch (RuntimeException e) {
      log.warn("bookingId={} compensation=RETRY_REQUIRED", b.id);
    }
  }

  @Scheduled(fixedDelay = 10000)
  public void repair() {
    for (var b : bookings.findByCompensationRequiredTrue())
      synchronized (lock(b.id)) {
        compensate(b);
      }
  }
}
