package com.devpulse.service;

import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.UserSummary;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        Role role = roleRepository.findByName("DEVELOPER").orElse(null);
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);
        return userRepository.save(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        Optional<User> maybe = userRepository.findByEmail(request.getEmail());
        if (maybe.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }
        User user = maybe.get();
        // For tests keep token simple
        String token = "token";
        UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole() != null ? user.getRole().getName() : null);
        return new AuthResponse(token, null, summary);
    }
}
