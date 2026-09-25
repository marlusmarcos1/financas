package com.marlus.financas.simulation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class SimulationControllerIntegrationTest extends AbstractIntegrationTest {

    private String createCard(MockHttpSession session, int closingDay, int dueDay) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Cartão simulação");
        payload.put("creditLimit", "5000.00");
        payload.put("closingDay", closingDay);
        payload.put("dueDay", dueDay);
        var result = mockMvc.perform(post("/api/v1/credit-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createSalarySource(MockHttpSession session, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/income-sources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Salário",
                                "type", "SALARY",
                                "recurrence", "MONTHLY",
                                "expectedAmount", amount,
                                "titheApplies", true,
                                "countsInBaseBudget", true))))
                .andExpect(status().isCreated());
    }

    @Test
    void safePurchaseWithinLimitsReturnsSafeVerdictAndFullProjection() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-simulador", "senha123");
        String cardId = createCard(session, 10, 20);
        createSalarySource(session, "5000.00");

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Notebook novo");
        payload.put("totalAmount", "3000.00");
        payload.put("installmentCount", 10); // parcela de 300 = 6% da renda base
        payload.put("cardId", cardId);
        payload.put("purchaseDate", "2026-09-01");

        mockMvc.perform(post("/api/v1/simulations/installment-purchase")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAFE"))
                .andExpect(jsonPath("$.installmentAmount").value(300.00))
                .andExpect(jsonPath("$.months.length()").value(13)) // 10 parcelas + 3 meses extra
                .andExpect(jsonPath("$.cardLimitImpact.creditLimit").value(5000.00))
                .andExpect(jsonPath("$.cardLimitImpact.availableLimitAfter").value(2000.00))
                .andExpect(jsonPath("$.suggestions.length()").value(0));
    }

    @Test
    void purchaseAboveFortyPercentOfIncomeIsNotRecommendedAndSuggestsFewerInstallments() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-simulador-risco", "senha123");
        String cardId = createCard(session, 10, 20);
        createSalarySource(session, "2000.00");

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "TV grande");
        payload.put("totalAmount", "9000.00");
        payload.put("installmentCount", 10); // parcela de 900 = 45% da renda base
        payload.put("cardId", cardId);
        payload.put("purchaseDate", "2026-09-01");

        mockMvc.perform(post("/api/v1/simulations/installment-purchase")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NOT_RECOMMENDED"))
                .andExpect(jsonPath("$.suggestions.length()").value(2));
    }
}
