package com.example.hotel;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "rooms")
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long hotelId;
  public String number;
  public boolean available = true;
  public long timesBooked;
  @Version public long version;

  public Room() {}
}
