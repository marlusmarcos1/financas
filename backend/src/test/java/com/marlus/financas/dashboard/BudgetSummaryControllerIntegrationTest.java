package com.marlus.financas.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class BudgetSummaryControllerIntegrationTest extends AbstractIntegrationTest {

    private String createAccount(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Conta", "type", "CHECKING", "institution", "Banco",
                                "initialBalance", "0", "purpose", "DAILY"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCategory(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Combustível", "kind", "EXPENSE", "nature", "VARIABLE"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void setBudget(MockHttpSession session, String categoryId, String limit) throws Exception {
        mockMvc.perform(post("/api/v1/categories/" + categoryId + "/budgets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("limitAmount", limit))))
                .andExpect(status().isCreated());
    }

    private void spend(MockHttpSession session, String accountId, String categoryId, String amount, String date)
            throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "Gasto",
                                "amount", amount,
                                "date", date,
                                "categoryId", categoryId,
                                "accountId", accountId))))
                .andExpect(status().isCreated());
    }

    @Test
    void budgetStatusReflectsGreenYellowAndRedThresholds() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-orcamento", "senha123");
        String accountId = createAccount(session);
        String categoryId = createCategory(session);
        setBudget(session, categoryId, "1000.00");

        spend(session, accountId, categoryId, "500.00", "2026-09-05"); // 50% -> GREEN
        mockMvc.perform(get("/api/v1/budget-summary").session(session).param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("GREEN"));

        spend(session, accountId, categoryId, "350.00", "2026-09-06"); // total 850 -> 85% -> YELLOW
        mockMvc.perform(get("/api/v1/budget-summary").session(session).param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("YELLOW"));

        spend(session, accountId, categoryId, "300.00", "2026-09-07"); // total 1150 -> 115% -> RED
        mockMvc.perform(get("/api/v1/budget-summary").session(session).param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RED"))
                .andExpect(jsonPath("$[0].spent").value(1150.00));
    }
}
