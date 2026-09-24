package com.marlus.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlus.financas.AbstractIntegrationTest;
import com.marlus.financas.user.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void adminUserIsCreatedExactlyOnceOnStartup() {
        assertThat(appUserRepository.count()).isEqualTo(1);
        assertThat(appUserRepository.findByUsername("marlus")).isPresent();
    }

    @Test
    void meWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentialsCreatesSessionAndAllowsAccessingMe() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "marlus", "password", "marlus"))))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").session((MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "usuario-inexistente", "password", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyFailedLoginsLocksTheUsername() throws Exception {
        String username = "usuario-bloqueio-teste";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "errada"))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "errada"))))
                .andExpect(status().isTooManyRequests());
    }
}
