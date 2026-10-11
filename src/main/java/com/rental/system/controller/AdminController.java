package com.rental.system.controller;

import com.rental.system.entity.RentPayment;
import com.rental.system.entity.User;
import com.rental.system.repository.RentPaymentRepository;
import com.rental.system.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RentPaymentRepository rentPaymentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
            map.put("monthlyRent", user.getMonthlyRent() == null ? 0 : user.getMonthlyRent());
            map.put("depositAmount", user.getDepositAmount() == null ? 0 : user.getDepositAmount());
            map.put("depositPaid", user.getDepositPaid() == null ? 0 : user.getDepositPaid());
            map.put("pendingCount", pending);
            map.put("completedCount", completed);

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

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

    @GetMapping("/payments/pending")
    public ResponseEntity<?> getPendingPayments() {
        List<RentPayment> pending = rentPaymentRepository.findByStatus(RentPayment.PaymentStatus.PENDING);
        return ResponseEntity.ok(pending);
    }

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

    @PutMapping("/payments/{id}/reject")
    public ResponseEntity<?> rejectPayment(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
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

    @PutMapping("/users/{id}/finance")
    public ResponseEntity<?> updateFinance(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            return ResponseEntity.status(404).body(error);
        }

        if (body.containsKey("monthlyRent"))
            user.setMonthlyRent(body.get("monthlyRent"));
        if (body.containsKey("depositAmount"))
            user.setDepositAmount(body.get("depositAmount"));
        if (body.containsKey("depositPaid"))
            user.setDepositPaid(body.get("depositPaid"));

        userRepository.save(user);

        Map<String, String> ok = new HashMap<>();
        ok.put("message", "Updated");
        return ResponseEntity.ok(ok);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        Map<String, String> res = new HashMap<>();

        if (user == null) {
            res.put("message", "User not found");
            return ResponseEntity.status(404).body(res);
        }

        if (user.getRole() == User.Role.ADMIN) {
            res.put("message", "Admin user cannot be deleted");
            return ResponseEntity.badRequest().body(res);
        }

        rentPaymentRepository.deleteAll(rentPaymentRepository.findByUser(user));
        userRepository.delete(user);

        res.put("message", "User deleted");
        return ResponseEntity.ok(res);
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElse(null);
        Map<String, String> res = new HashMap<>();

        if (user == null) {
            res.put("message", "User not found");
            return ResponseEntity.status(404).body(res);
        }

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().length() < 6) {
            res.put("message", "Password must be at least 6 characters");
            return ResponseEntity.badRequest().body(res);
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        res.put("message", "Password reset");
        return ResponseEntity.ok(res);
    }
}