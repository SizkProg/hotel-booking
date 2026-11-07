package com.example.hotel;

public interface SlotRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Slot, java.util.UUID> {
  java.util.List<Slot> findByRoomIdAndReleasedFalse(Long roomId);
}
