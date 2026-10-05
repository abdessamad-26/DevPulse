package com.devpulse.service;

import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private JwtService jwtService;

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
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectRegistrationWhenDefaultRoleMissing() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("DEVELOPER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldAuthenticateUserAndReturnRealTokens() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bob@example.com");
        user.setPassword("encoded-secret");
        Role role = new Role();
        role.setName("ADMIN");
        user.setRole(role);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("bob@example.com", "secret123"));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("bob@example.com", "ADMIN")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("bob@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.authenticate(new AuthRequest("bob@example.com", "secret123"));

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("bob@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(new AuthRequest("bob@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldRefreshAccessTokenWhenRefreshTokenIsValid() {
        User user = new User();
        user.setId(2L);
        user.setEmail("carol@example.com");
        Role role = new Role();
        role.setName("VIEWER");
        user.setRole(role);

        when(jwtService.isRefreshTokenValid("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("carol@example.com");
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("carol@example.com", "VIEWER")).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("valid-refresh-token");

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        when(jwtService.isRefreshTokenValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(ApiException.class);
    }
}
