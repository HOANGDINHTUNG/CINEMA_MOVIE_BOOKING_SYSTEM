package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
}
