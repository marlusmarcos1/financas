package com.marlus.financas.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> accountPayload(String name) {
        return Map.of(
                "name", name,
                "type", "CHECKING",
                "institution", "Nubank",
                "initialBalance", "100.50",
                "purpose", "DAILY");
    }

    @Test
    void createListGetUpdateAndArchiveAccount() throws Exception {
        MockHttpSession session = loginAsDefaultUser();

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountPayload("Conta corrente"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Conta corrente"))
                .andExpect(jsonPath("$.archived").value(false))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Conta corrente"));

        mockMvc.perform(get("/api/v1/accounts/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution").value("Nubank"));

        mockMvc.perform(put("/api/v1/accounts/" + id)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountPayload("Conta corrente renomeada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Conta corrente renomeada"));

        mockMvc.perform(delete("/api/v1/accounts/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/accounts/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void createWithoutNameReturnsValidationError() throws Exception {
        MockHttpSession session = loginAsDefaultUser();

        mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CHECKING", "initialBalance", "0", "purpose", "DAILY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void accountsAreIsolatedPerUser() throws Exception {
        MockHttpSession ownerSession = loginAsDefaultUser();
        mockMvc.perform(post("/api/v1/accounts")
                        .session(ownerSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountPayload("Conta do marlus"))))
                .andExpect(status().isCreated());

        MockHttpSession otherSession = createUserAndLogin("outro-usuario", "senha123");
        mockMvc.perform(get("/api/v1/accounts").session(otherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
