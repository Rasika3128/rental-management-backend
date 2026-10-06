package com.rental.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "rent_payments")
@Data
public class RentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String month;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String screenshotPath;

    private LocalDateTime paymentDate = LocalDateTime.now();

    private String adminRemark;

    private LocalDateTime reviewedAt;

    public enum PaymentStatus {
        PENDING, ACCEPTED, REJECTED
    }
}