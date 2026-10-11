package com.rental.system.controller;

import com.rental.system.entity.RentPayment;
import com.rental.system.entity.User;
import com.rental.system.repository.RentPaymentRepository;
import com.rental.system.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public UserController(UserRepository userRepository, RentPaymentRepository rentPaymentRepository) {
        this.userRepository = userRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            return ResponseEntity.status(404).body(error);
        }

        List<RentPayment> payments = rentPaymentRepository.findByUser(user);
        double totalPaid = payments.stream()
                .filter(p -> p.getStatus() == RentPayment.PaymentStatus.ACCEPTED)
                .mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount())
                .sum();

        double rent = user.getMonthlyRent() == null ? 0 : user.getMonthlyRent();
        long months = 0;
        if (user.getStartDate() != null) {
            months = ChronoUnit.MONTHS.between(
                    user.getStartDate().withDayOfMonth(1),
                    LocalDate.now().withDayOfMonth(1)) + 1;
            if (months < 0) months = 0;
        }
        double totalDue = rent * months;

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("startDate", user.getStartDate());
        profile.put("idProofPath", user.getIdProofPath());
        profile.put("photoPath", user.getPhotoPath());
        profile.put("role", user.getRole());
        profile.put("monthlyRent", rent);
        profile.put("depositAmount", user.getDepositAmount() == null ? 0 : user.getDepositAmount());
        profile.put("depositPaid", user.getDepositPaid() == null ? 0 : user.getDepositPaid());
        profile.put("occupation", user.getOccupation());
        profile.put("organizationName", user.getOrganizationName());
        profile.put("monthsElapsed", months);
        profile.put("totalDue", totalDue);
        profile.put("totalPaid", totalPaid);
        profile.put("balance", totalDue - totalPaid);

        return ResponseEntity.ok(profile);
    }
}