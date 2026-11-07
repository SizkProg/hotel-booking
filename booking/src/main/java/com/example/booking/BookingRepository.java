package com.example.booking;

public interface BookingRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Booking, java.util.UUID> {
  org.springframework.data.domain.Page<Booking> findByUserId(
      Long userId, org.springframework.data.domain.Pageable page);

  java.util.List<Booking> findByCompensationRequiredTrue();
}
