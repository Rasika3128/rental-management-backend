package com.rental.system.controller;

import com.rental.system.dto.AuthResponse;
import com.rental.system.dto.LoginRequest;
import com.rental.system.entity.User;
import com.rental.system.repository.UserRepository;
import com.rental.system.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam("startDate") String startDate,
            @RequestParam("idProof") MultipartFile idProof,
            @RequestParam("photo") MultipartFile photo
    ) {
        Map<String, String> error = new HashMap<>();

        // Basic validations
        if (!password.equals(confirmPassword)) {
            error.put("message", "Password and Confirm Password do not match");
            return ResponseEntity.badRequest().body(error);
        }

        if (userRepository.existsByEmail(email)) {
            error.put("message", "Email already registered");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Files save karणे
            String idProofFileName = saveFile(idProof, "idproof");
            String photoFileName = saveFile(photo, "photo");

            // User object तयार करणे
            User user = new User();
            user.setName(name.trim());
            user.setEmail(email.trim());
            user.setPhone(phone.trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setStartDate(LocalDate.parse(startDate));
            user.setIdProofPath(idProofFileName);
            user.setPhotoPath(photoFileName);
            user.setRole(User.Role.USER);

            userRepository.save(user);

            // Token generate करून लगेच login करून देणे
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name()));

        } catch (IOException e) {
            error.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Map<String, String> error = new HashMap<>();

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            error.put("message", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name()));
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