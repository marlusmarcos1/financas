package com.marlus.financas.transaction;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

class TransactionControllerIntegrationTest extends AbstractIntegrationTest {

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

    private String createCard(MockHttpSession session, String name, int closingDay, int dueDay) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("creditLimit", "3000.00");
        payload.put("closingDay", closingDay);
        payload.put("dueDay", dueDay);
        MvcResult result = mockMvc.perform(post("/api/v1/credit-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
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
                                Map.of("name", name, "kind", "EXPENSE", "nature", "VARIABLE"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createExpenseFromAccountSucceeds() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta");
        String categoryId = createCategory(session, "Mercado");

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "Compras do mês",
                                "amount", "250.00",
                                "date", "2026-03-10",
                                "categoryId", categoryId,
                                "accountId", accountId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").doesNotExist());
    }

    @Test
    void createExpenseWithBothAccountAndCardIsRejected() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta");
        String cardId = createCard(session, "Cartão", 5, 12);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "Erro",
                                "amount", "10.00",
                                "date", "2026-03-10",
                                "accountId", accountId,
                                "cardId", cardId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExpenseFromCardAttachesInvoiceAutomatically() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, "Cartão fatura", 10, 20);
        String categoryId = createCategory(session, "Assinaturas");

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "Streaming",
                                "amount", "39.90",
                                "date", "2026-03-05",
                                "categoryId", categoryId,
                                "cardId", cardId))))
                .andExpect(status().isCreated())
                .andReturn();

        String invoiceId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("invoiceId")
                .asText();

        mockMvc.perform(get("/api/v1/invoices/" + invoiceId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceMonth").value("2026-03"))
                .andExpect(jsonPath("$.total").value(39.90));
    }

    @Test
    void incomeWithCardIsRejected() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, "Cartão", 5, 12);

        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "INCOME",
                                "description", "Salário",
                                "amount", "1000.00",
                                "date", "2026-03-05",
                                "cardId", cardId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listFiltersByPeriod() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta filtro");

        for (String date : new String[] {"2026-01-15", "2026-02-15", "2026-03-15"}) {
            mockMvc.perform(post("/api/v1/transactions")
                            .session(session)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "kind", "EXPENSE",
                                    "description", "Lançamento " + date,
                                    "amount", "10.00",
                                    "date", date,
                                    "accountId", accountId))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("from", "2026-02-01")
                        .param("to", "2026-02-28")
                        .param("accountId", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Lançamento 2026-02-15"));
    }

    @Test
    void deleteTransaction() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session, "Conta exclusão");

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "A apagar",
                                "amount", "5.00",
                                "date", "2026-03-01",
                                "accountId", accountId))))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/transactions/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/transactions/" + id).session(session)).andExpect(status().isNotFound());
    }
}
