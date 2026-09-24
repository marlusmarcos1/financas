package com.marlus.financas.creditcard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class CreditCardControllerIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> cardPayload(String name, int closingDay, int dueDay) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("issuer", "Visa");
        payload.put("creditLimit", "3000.00");
        payload.put("closingDay", closingDay);
        payload.put("dueDay", dueDay);
        payload.put("color", "#000000");
        return payload;
    }

    @Test
    void createListAndArchiveCreditCard() throws Exception {
        MockHttpSession session = loginAsDefaultUser();

        MvcResult result = mockMvc.perform(post("/api/v1/credit-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload("Nubank", 5, 12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.closingDay").value(5))
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/credit-cards/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void closingDayOutOfRangeReturnsValidationError() throws Exception {
        MockHttpSession session = loginAsDefaultUser();

        mockMvc.perform(post("/api/v1/credit-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload("Cartão inválido", 32, 12))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.closingDay").exists());
    }

    @Test
    void unknownDefaultPaymentAccountReturnsNotFound() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        Map<String, Object> payload = cardPayload("Cartão com conta inválida", 5, 12);
        payload.put("defaultPaymentAccountId", "00000000-0000-0000-0000-000000000000");

        mockMvc.perform(post("/api/v1/credit-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }
}
