package com.example.booking;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "users")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true, nullable = false)
  public String username;

  @com.fasterxml.jackson.annotation.JsonIgnore public String password;
  public String role;

  public Account() {}
}
