package com.rental.system.controller;

import com.rental.system.entity.RentPayment;
import com.rental.system.entity.User;
import com.rental.system.repository.RentPaymentRepository;
import com.rental.system.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public AdminController(UserRepository userRepository, RentPaymentRepository rentPaymentRepository) {
        this.userRepository = userRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    // सगळ्या users ची यादी (pending/completed counts सहित)
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> result = users.stream().map(user -> {
            List<RentPayment> payments = rentPaymentRepository.findByUser(user);

            long pending = payments.stream()
                    .filter(p -> p.getStatus() == RentPayment.PaymentStatus.PENDING)
                    .count();
            long completed = payments.stream()
                    .filter(p -> p.getStatus() == RentPayment.PaymentStatus.ACCEPTED)
                    .count();

            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("role", user.getRole());
            map.put("pendingCount", pending);
            map.put("completedCount", completed);

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // एका specific user चे पूर्ण details + history
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            return ResponseEntity.status(404).body(error);
        }

        List<RentPayment> history = rentPaymentRepository.findByUser(user);

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("history", history);

        return ResponseEntity.ok(response);
    }

    // सगळे pending payments
    @GetMapping("/payments/pending")
    public ResponseEntity<?> getPendingPayments() {
        List<RentPayment> pending = rentPaymentRepository.findByStatus(RentPayment.PaymentStatus.PENDING);
        return ResponseEntity.ok(pending);
    }

    // Payment Accept करणे
    @PutMapping("/payments/{id}/accept")
    public ResponseEntity<?> acceptPayment(@PathVariable Long id) {
        RentPayment payment = rentPaymentRepository.findById(id).orElse(null);

        if (payment == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Payment not found");
            return ResponseEntity.status(404).body(error);
        }

        payment.setStatus(RentPayment.PaymentStatus.ACCEPTED);
        payment.setReviewedAt(java.time.LocalDateTime.now());
        rentPaymentRepository.save(payment);

        return ResponseEntity.ok(payment);
    }

    // Payment Reject करणे
    @PutMapping("/payments/{id}/reject")
    public ResponseEntity<?> rejectPayment(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        RentPayment payment = rentPaymentRepository.findById(id).orElse(null);

        if (payment == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Payment not found");
            return ResponseEntity.status(404).body(error);
        }

        payment.setStatus(RentPayment.PaymentStatus.REJECTED);
        payment.setReviewedAt(java.time.LocalDateTime.now());

        if (body != null && body.containsKey("remark")) {
            payment.setAdminRemark(body.get("remark"));
        }

        rentPaymentRepository.save(payment);

        return ResponseEntity.ok(payment);
    }
}