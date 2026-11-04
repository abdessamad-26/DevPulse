package com.devpulse.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    @Test
    void limitsRegistrationAttempts() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, false);
        int[] accepted = {0};

        for (int attempt = 0; attempt < 11; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
            request.setRemoteAddr("192.0.2.11");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (servletRequest, servletResponse) -> accepted[0]++;

            filter.doFilter(request, response, chain);
            if (attempt < 10) {
                assertThat(response.getStatus()).isEqualTo(200);
            } else {
                assertThat(response.getStatus()).isEqualTo(429);
                assertThat(response.getHeader("Retry-After")).isNotBlank();
            }
        }

        assertThat(accepted[0]).isEqualTo(10);
    }

    @Test
    void limitsLoginAttemptsByClientAddressAndReturnsRetryAfter() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, false);
        int[] accepted = {0};

        for (int attempt = 0; attempt < 11; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.0.2.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (servletRequest, servletResponse) -> accepted[0]++;

            filter.doFilter(request, response, chain);
            if (attempt < 10) {
                assertThat(response.getStatus()).isEqualTo(200);
            } else {
                assertThat(response.getStatus()).isEqualTo(429);
                assertThat(response.getHeader("Retry-After")).isNotBlank();
            }
        }

        assertThat(accepted[0]).isEqualTo(10);
    }
}
