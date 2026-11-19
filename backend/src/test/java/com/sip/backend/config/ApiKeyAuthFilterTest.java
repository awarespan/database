package com.sip.backend.config;

import com.sip.backend.service.PiAuthenticationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthFilterTest {
    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void globalDeviceApiKeyIsAcceptedOnlyForEnrollment() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("ci-device-key", mock(PiAuthenticationService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/nodes/enroll");
        request.addHeader("X-API-Key", "ci-device-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DEVICE")));
    }

    @Test
    void globalApiKeyCannotAuthenticateRoutineIncidentTraffic() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("ci-device-key", mock(PiAuthenticationService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/incidents");
        request.addHeader("X-API-Key", "ci-device-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("enrollment-only"));
    }

    @Test
    void perNodeSecretAuthenticatesRoutineIncidentTraffic() throws Exception {
        PiAuthenticationService authService = mock(PiAuthenticationService.class);
        when(authService.validateDeviceSecret("node-issued", "device-secret")).thenReturn(true);
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("ci-device-key", authService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/incidents");
        request.addHeader("X-Node-Id", "node-issued");
        request.addHeader("X-Device-Secret", "device-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
        verify(authService).validateDeviceSecret("node-issued", "device-secret");
    }
}
