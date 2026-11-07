package com.example.hotel;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "hotels")
public class Hotel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;
  public String address;

  public Hotel() {}
}
