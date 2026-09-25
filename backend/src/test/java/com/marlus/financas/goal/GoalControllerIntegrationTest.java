package com.marlus.financas.goal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class GoalControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void requiredMonthlyContributionAccountsForExistingContributions() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-metas", "senha123");
        LocalDate targetDate = LocalDate.now().withDayOfMonth(1).plusMonths(10);

        Map<String, Object> goalPayload = new HashMap<>();
        goalPayload.put("name", "Entrada da casa");
        goalPayload.put("type", "HOUSE");
        goalPayload.put("targetAmount", "50000.00");
        goalPayload.put("targetDate", targetDate.toString());
        goalPayload.put("priority", 1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/goals")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/contributions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("date", "2026-09-01", "amount", "10000.00", "source", "MANUAL"))))
                .andExpect(status().isCreated());

        // Faltam 40000 em 10 meses -> 4000/mês.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/goals/" + goalId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(10000.00))
                .andExpect(jsonPath("$.amountRemaining").value(40000.00))
                .andExpect(jsonPath("$.monthsRemaining").value(10))
                .andExpect(jsonPath("$.requiredMonthlyContribution").value(4000.00));
    }

    @Test
    void houseGoalWithShortHorizonAndVariableIncomeAssetsGetsRiskWarning() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-metas-risco", "senha123");

        mockMvc.perform(post("/api/v1/investment-assets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ticker", "MXRF11", "name", "Maxi Renda", "assetClass", "FII", "purpose", "HOUSE"))))
                .andExpect(status().isCreated());

        Map<String, Object> goalPayload = new HashMap<>();
        goalPayload.put("name", "Entrada da casa");
        goalPayload.put("type", "HOUSE");
        goalPayload.put("targetAmount", "50000.00");
        goalPayload.put("targetDate", LocalDate.now().plusYears(1).toString());
        goalPayload.put("priority", 1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/goals")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(body).get("warning").asText())
                .contains("renda variável");
    }
}
