package com.example.hotel;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "slots")
public class Slot {
  @Id public UUID id;
  public Long roomId;
  public LocalDate startDate;
  public LocalDate endDate;
  public boolean released;

  public Slot() {}
}
