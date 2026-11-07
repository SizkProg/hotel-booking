package com.example.booking;

import static org.springframework.http.HttpStatus.*;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingTransactions {
  private final BookingRepository bookings;

  public BookingTransactions(BookingRepository b) {
    bookings = b;
  }

  @Transactional
  public Booking pending(Long user, BookingService.Input in) {
    if (in.startDate() == null
        || in.endDate() == null
        || !in.startDate().isBefore(in.endDate())
        || in.startDate().isBefore(LocalDate.now()))
      throw new ResponseStatusException(BAD_REQUEST, "Некорректные даты");
    if (in.requestId() == null || (!in.autoSelect() && in.roomId() == null))
      throw new ResponseStatusException(BAD_REQUEST, "Нужны requestId и roomId");
    var old = bookings.findById(in.requestId());
    if (old.isPresent()) {
      var b = old.get();
      if (!b.userId.equals(user)
          || !b.startDate.equals(in.startDate())
          || !b.endDate.equals(in.endDate())
          || b.autoSelect != in.autoSelect()
          || (!in.autoSelect() && !Objects.equals(b.requestedRoomId, in.roomId())))
        throw new ResponseStatusException(CONFLICT, "requestId уже использован");
      return b;
    }
    var b = new Booking();
    b.id = in.requestId();
    b.userId = user;
    b.startDate = in.startDate();
    b.endDate = in.endDate();
    b.status = "PENDING";
    b.autoSelect = in.autoSelect();
    b.requestedRoomId = in.autoSelect() ? null : in.roomId();
    b.createdAt = Instant.now();
    return bookings.saveAndFlush(b);
  }

  @Transactional
  public Booking room(UUID id, Long room) {
    var b = bookings.findById(id).orElseThrow();
    b.roomId = room;
    return b;
  }

  @Transactional
  public Booking state(UUID id, String state, boolean compensation) {
    var b = bookings.findById(id).orElseThrow();
    b.status = state;
    b.compensationRequired = compensation;
    return b;
  }
}
