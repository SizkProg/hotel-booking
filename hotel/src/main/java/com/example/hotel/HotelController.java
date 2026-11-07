package com.example.hotel;

import static org.springframework.http.HttpStatus.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class HotelController {
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final InventoryService inventory;
  private final String secret;

  public HotelController(
      HotelRepository h,
      RoomRepository r,
      InventoryService i,
      @Value("${security.internal.secret}") String s) {
    hotels = h;
    rooms = r;
    inventory = i;
    secret = s;
  }

  public record HotelInput(@NotBlank String name, @NotBlank String address) {}

  public record RoomInput(@NotNull Long hotelId, @NotBlank String number, boolean available) {}

  @GetMapping("/api/hotels")
  public List<Hotel> hotels() {
    return hotels.findAll();
  }

  @PostMapping("/api/hotels")
  @PreAuthorize("hasRole('ADMIN')")
  public Hotel create(@Valid @RequestBody HotelInput in) {
    var h = new Hotel();
    h.name = in.name();
    h.address = in.address();
    return hotels.save(h);
  }

  @PutMapping("/api/hotels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Hotel update(@PathVariable Long id, @Valid @RequestBody HotelInput in) {
    var h = hotels.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    h.name = in.name();
    h.address = in.address();
    return hotels.save(h);
  }

  @DeleteMapping("/api/hotels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    if (rooms.findAll().stream().anyMatch(r -> r.hotelId.equals(id)))
      throw new ResponseStatusException(CONFLICT, "Сначала удалите номера");
    hotels.deleteById(id);
  }

  @PostMapping("/api/rooms")
  @PreAuthorize("hasRole('ADMIN')")
  public Room createRoom(@Valid @RequestBody RoomInput in) {
    var r = new Room();
    return saveRoom(r, in);
  }

  @PutMapping("/api/rooms/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Room updateRoom(@PathVariable Long id, @Valid @RequestBody RoomInput in) {
    return saveRoom(
        rooms.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)), in);
  }

  private Room saveRoom(Room r, RoomInput in) {
    if (!hotels.existsById(in.hotelId()))
      throw new ResponseStatusException(NOT_FOUND, "Отель не найден");
    r.hotelId = in.hotelId();
    r.number = in.number();
    r.available = in.available();
    return rooms.save(r);
  }

  @DeleteMapping("/api/rooms/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteRoom(@PathVariable Long id) {
    inventory.deleteRoom(id);
  }

  @GetMapping({"/api/rooms", "/api/rooms/recommend"})
  public List<Room> rooms(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      @RequestParam(required = false) Long hotelId,
      jakarta.servlet.http.HttpServletRequest req) {
    return inventory.available(
        startDate, endDate, hotelId, req.getRequestURI().endsWith("recommend"));
  }

  @GetMapping("/api/rooms/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Room> statistics() {
    return rooms.findAll();
  }

  private void internal(String token) {
    if (!java.security.MessageDigest.isEqual(
        secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        token.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
      throw new ResponseStatusException(FORBIDDEN);
  }

  @PostMapping("/api/rooms/{id}/confirm-availability")
  public void confirm(
      @PathVariable Long id,
      @RequestHeader("X-Internal-Token") String token,
      @RequestBody InventoryService.Reservation r) {
    internal(token);
    inventory.confirm(id, r);
  }

  @PostMapping("/api/rooms/{id}/release")
  public void release(
      @PathVariable Long id,
      @RequestHeader("X-Internal-Token") String token,
      @RequestBody InventoryService.Reservation r) {
    internal(token);
    inventory.release(id, r);
  }
}
