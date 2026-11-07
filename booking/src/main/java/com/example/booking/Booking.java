package com.example.booking;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "bookings")
public class Booking {
  @Id public UUID id;
  public Long userId;
  public Long roomId;
  public LocalDate startDate;
  public LocalDate endDate;
  public String status;
  public Instant createdAt;
  public boolean compensationRequired;
  public boolean autoSelect;
  public Long requestedRoomId;

  public Booking() {}
}
