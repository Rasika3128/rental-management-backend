package com.rental.system.controller;

import com.rental.system.entity.RentPayment;
import com.rental.system.entity.User;
import com.rental.system.repository.RentPaymentRepository;
import com.rental.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class RentPaymentController {

    private final RentPaymentRepository rentPaymentRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public RentPaymentController(RentPaymentRepository rentPaymentRepository, UserRepository userRepository) {
        this.rentPaymentRepository = rentPaymentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/rent-history")
    public ResponseEntity<?> getRentHistory(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            return ResponseEntity.status(404).body(error);
        }

        List<RentPayment> history = rentPaymentRepository.findByUser(user);

        long pendingCount = history.stream()
                .filter(p -> p.getStatus() == RentPayment.PaymentStatus.PENDING)
                .count();

        long completedCount = history.stream()
                .filter(p -> p.getStatus() == RentPayment.PaymentStatus.ACCEPTED)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("pendingCount", pendingCount);
        response.put("completedCount", completedCount);
        response.put("totalCount", history.size());
        response.put("history", history);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/rent-payment")
    public ResponseEntity<?> submitRentPayment(
            Authentication authentication,
            @RequestParam("month") String month,
            @RequestParam("amount") Double amount,
            @RequestParam("screenshot") MultipartFile screenshot
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "User not found");
            return ResponseEntity.status(404).body(error);
        }

        try {
            String screenshotFileName = saveFile(screenshot, "screenshot");

            RentPayment payment = new RentPayment();
            payment.setUser(user);
            payment.setMonth(month.trim());
            payment.setAmount(amount);
            payment.setScreenshotPath(screenshotFileName);
            payment.setStatus(RentPayment.PaymentStatus.PENDING);

            rentPaymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.getId());
            response.put("month", payment.getMonth());
            response.put("amount", payment.getAmount());
            response.put("status", payment.getStatus());
            response.put("screenshotPath", payment.getScreenshotPath());
            response.put("paymentDate", payment.getPaymentDate());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Screenshot upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = prefix + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return fileName;
    }
}