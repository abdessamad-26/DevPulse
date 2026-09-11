package com.devpulse.service;

import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");
        request.setFirstName("Alice");
        request.setLastName("Martin");

        Role role = new Role();
        role.setName("DEVELOPER");

        when(roleRepository.findByName("DEVELOPER")).thenReturn(Optional.of(role));
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = authService.register(request);

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
        assertThat(saved.getRole().getName()).isEqualTo("DEVELOPER");
    }

    @Test
    void shouldAuthenticateUser() {
        User user = new User();
        user.setEmail("bob@example.com");
        user.setPassword("encoded-secret");
        Role role = new Role();
        role.setName("ADMIN");
        user.setRole(role);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.authenticate(new AuthRequest("bob@example.com", "secret123"));

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("bob@example.com");
    }
}
