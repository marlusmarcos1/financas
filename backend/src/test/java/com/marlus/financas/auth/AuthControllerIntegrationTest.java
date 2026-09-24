package com.marlus.financas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminUserIsCreatedOnStartup() {
        assertThat(appUserRepository.findByUsername("marlus")).isPresent();
    }

    @Test
    void meWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentialsCreatesSessionAndAllowsAccessingMe() throws Exception {
        MockHttpSession session = loginAsDefaultUser();

        mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
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

    @Test
    void changePasswordWithWrongCurrentPasswordIsRejected() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-troca-senha-1", "senha-original");

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "senha-errada", "newPassword", "nova-senha-123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordAllowsLoginWithNewPassword() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-troca-senha-2", "senha-original");

        mockMvc.perform(put("/api/v1/auth/password")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "senha-original", "newPassword", "nova-senha-123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "usuario-troca-senha-2", "password", "nova-senha-123"))))
                .andExpect(status().isOk());
    }
}
