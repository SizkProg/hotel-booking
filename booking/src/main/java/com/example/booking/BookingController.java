package com.example.booking;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookingController {
  private final BookingService service;
  private final BookingRepository bookings;

  public BookingController(BookingService s, BookingRepository b) {
    service = s;
    bookings = b;
  }

  @PostMapping("/booking")
  public Booking create(@AuthenticationPrincipal Jwt jwt, @RequestBody BookingService.Input in) {
    return service.create(Long.valueOf(jwt.getSubject()), in, jwt.getTokenValue());
  }

  @GetMapping({"/bookings", "/booking"})
  public Page<Booking> history(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return bookings.findByUserId(
        Long.valueOf(jwt.getSubject()),
        PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
  }

  @GetMapping("/booking/{id}")
  public Booking get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.own(id, Long.valueOf(jwt.getSubject()));
  }

  @DeleteMapping("/booking/{id}")
  public Booking cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return service.cancel(id, Long.valueOf(jwt.getSubject()));
  }
}
