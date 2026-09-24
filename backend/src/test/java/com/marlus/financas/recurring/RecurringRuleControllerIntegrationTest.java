package com.marlus.financas.recurring;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class RecurringRuleControllerIntegrationTest extends AbstractIntegrationTest {

    private String createAccount(MockHttpSession session, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "type", "CHECKING",
                                "institution", "Banco",
                                "initialBalance", "0",
                                "purpose", "DAILY"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCategory(MockHttpSession session, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name, "kind", "EXPENSE", "nature", "FIXED"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void monthlyRuleMaterializesThirteenOccurrencesAcrossTheTwelveMonthHorizon() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta recorrência mensal");
        String categoryId = createCategory(session, "Assinaturas mensais");
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/v1/recurring-rules")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Meli+",
                                "amount", "19.90",
                                "amountIsVariable", false,
                                "frequency", "MONTHLY",
                                "dayOfMonth", today.getDayOfMonth(),
                                "startDate", today.toString(),
                                "categoryId", categoryId,
                                "accountId", accountId,
                                "active", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("accountId", accountId)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(13));
    }

    @Test
    void yearlyRuleMaterializesTwoOccurrencesAcrossTheTwelveMonthHorizon() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta recorrência anual");
        String categoryId = createCategory(session, "IPVA");
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/v1/recurring-rules")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "IPVA anual",
                                "amount", "217.97",
                                "amountIsVariable", false,
                                "frequency", "YEARLY",
                                "dayOfMonth", today.getDayOfMonth(),
                                "startDate", today.toString(),
                                "categoryId", categoryId,
                                "accountId", accountId,
                                "active", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("accountId", accountId)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void ruleRequiresExactlyOnePaymentMethod() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String categoryId = createCategory(session, "Sem método");
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/v1/recurring-rules")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Sem conta nem cartão",
                                "amount", "10.00",
                                "amountIsVariable", false,
                                "frequency", "MONTHLY",
                                "dayOfMonth", today.getDayOfMonth(),
                                "startDate", today.toString(),
                                "categoryId", categoryId,
                                "active", true))))
                .andExpect(status().isBadRequest());
    }
}
