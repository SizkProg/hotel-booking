package com.example.booking;

public interface AccountRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Account, Long> {
  java.util.Optional<Account> findByUsername(String username);
}
