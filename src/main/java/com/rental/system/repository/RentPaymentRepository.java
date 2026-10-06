package com.rental.system.repository;

import com.rental.system.entity.RentPayment;
import com.rental.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {
    List<RentPayment> findByUser(User user);
    List<RentPayment> findByUserId(Long userId);
    List<RentPayment> findByStatus(RentPayment.PaymentStatus status);
}