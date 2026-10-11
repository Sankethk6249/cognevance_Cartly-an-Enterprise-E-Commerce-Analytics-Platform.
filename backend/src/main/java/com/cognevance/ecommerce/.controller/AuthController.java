package com.cognevance.ecommerce.controller;

import com.cognevance.ecommerce.dto.Dtos.*;
import com.cognevance.ecommerce.model.User;
import com.cognevance.ecommerce.repository.UserRepository;
import com.cognevance.ecommerce.security.JwtUtil;
import com.cognevance.ecommerce.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final EmailService email;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        String mail = r.email().trim().toLowerCase();
        if (users.existsByEmail(mail))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name().trim()); u.setEmail(mail); u.setPassword(encoder.encode(r.password()));
        users.save(u);
        email.send(mail, "Welcome to Cartly", "Hi " + u.getName() + ",\n\nYour account is ready. Happy shopping!\n\n- Cartly");
        return response(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(u);
    }

    private AuthResponse response(User u) {
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), u.getName(), u.getEmail(), u.getRole().name());
    }
}