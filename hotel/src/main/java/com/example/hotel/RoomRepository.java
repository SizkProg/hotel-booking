package com.example.hotel;

public interface RoomRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Room, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select r from Room r where r.id=:id")
  java.util.Optional<Room> lockById(Long id);
}
