package com.example.hotel;

import static org.springframework.http.HttpStatus.*;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
  private final RoomRepository rooms;
  private final SlotRepository slots;

  public InventoryService(RoomRepository r, SlotRepository s) {
    rooms = r;
    slots = s;
  }

  public record Reservation(UUID requestId, LocalDate startDate, LocalDate endDate) {}

  public static void dates(LocalDate start, LocalDate end) {
    if (start == null || end == null || !start.isBefore(end))
      throw new ResponseStatusException(BAD_REQUEST, "startDate должен быть раньше endDate");
  }

  @Transactional
  public void confirm(Long roomId, Reservation request) {
    dates(request.startDate(), request.endDate());
    if (request.requestId() == null)
      throw new ResponseStatusException(BAD_REQUEST, "requestId обязателен");
    var room = rooms.lockById(roomId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    var prior = slots.findById(request.requestId());
    if (prior.isPresent()) {
      var p = prior.get();
      if (!p.roomId.equals(roomId)
          || !Objects.equals(p.startDate, request.startDate())
          || !Objects.equals(p.endDate, request.endDate()))
        throw new ResponseStatusException(CONFLICT, "requestId уже использован");
      if (p.released) throw new ResponseStatusException(CONFLICT, "Слот отменён");
      return;
    }
    if (!room.available
        || slots.findByRoomIdAndReleasedFalse(roomId).stream()
            .anyMatch(
                s ->
                    s.startDate.isBefore(request.endDate())
                        && request.startDate().isBefore(s.endDate)))
      throw new ResponseStatusException(CONFLICT, "Номер занят");
    var slot = new Slot();
    slot.id = request.requestId();
    slot.roomId = roomId;
    slot.startDate = request.startDate();
    slot.endDate = request.endDate();
    slots.save(slot);
    room.timesBooked++;
  }

  @Transactional
  public void release(Long roomId, Reservation request) {
    rooms.lockById(roomId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    var slot =
        slots
            .findById(request.requestId())
            .orElseGet(
                () -> {
                  var s = new Slot();
                  s.id = request.requestId();
                  s.roomId = roomId;
                  s.startDate = request.startDate();
                  s.endDate = request.endDate();
                  return s;
                });
    if (!slot.roomId.equals(roomId)) throw new ResponseStatusException(CONFLICT);
    slot.released = true;
    slots.save(slot);
  }

  @Transactional(readOnly = true)
  public List<Room> available(LocalDate start, LocalDate end, Long hotelId, boolean recommend) {
    dates(start, end);
    var result =
        rooms.findAll().stream()
            .filter(r -> r.available && (hotelId == null || hotelId.equals(r.hotelId)))
            .filter(
                r ->
                    slots.findByRoomIdAndReleasedFalse(r.id).stream()
                        .noneMatch(s -> s.startDate.isBefore(end) && start.isBefore(s.endDate)))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    if (recommend)
      result.sort(Comparator.comparingLong((Room r) -> r.timesBooked).thenComparing(r -> r.id));
    return result;
  }

  @Transactional
  public void deleteRoom(Long id) {
    rooms.lockById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    if (!slots.findByRoomIdAndReleasedFalse(id).isEmpty())
      throw new ResponseStatusException(CONFLICT, "Есть бронирования");
    rooms.deleteById(id);
  }
}
