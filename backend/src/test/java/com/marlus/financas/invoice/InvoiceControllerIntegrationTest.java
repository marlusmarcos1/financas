package com.marlus.financas.invoice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

class InvoiceControllerIntegrationTest extends AbstractIntegrationTest {

    private String createAccount(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Conta pagamento",
                                "type", "CHECKING",
                                "institution", "Banco",
                                "initialBalance", "0",
                                "purpose", "DAILY"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCard(MockHttpSession session, int closingDay, int dueDay) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Cartão fatura " + closingDay + "-" + dueDay);
        payload.put("creditLimit", "1000.00");
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

    private void createCardExpense(MockHttpSession session, String cardId, String date, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE",
                                "description", "Compra",
                                "amount", amount,
                                "date", date,
                                "cardId", cardId))))
                .andExpect(status().isCreated());
    }

    @Test
    void purchaseAfterClosingDayFallsIntoNextMonthInvoiceWithCorrectDueDate() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        // Fechamento dia 10, vencimento dia 20 (>= fechamento -> vencimento na própria competência).
        String cardId = createCard(session, 10, 20);

        createCardExpense(session, cardId, "2026-03-11", "100.00");

        mockMvc.perform(get("/api/v1/credit-cards/" + cardId + "/invoices").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].referenceMonth").value("2026-04"))
                .andExpect(jsonPath("$[0].closingDate").value("2026-04-10"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-04-20"))
                .andExpect(jsonPath("$[0].total").value(100.00));
    }

    @Test
    void payingFullAmountMarksInvoiceAsPaid() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session);
        String cardId = createCard(session, 5, 15);
        createCardExpense(session, cardId, "2026-03-01", "200.00");

        MvcResult listResult = mockMvc.perform(get("/api/v1/credit-cards/" + cardId + "/invoices").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String invoiceId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/pay")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("accountId", accountId, "amount", "200.00", "paidOn", "2026-03-15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void partialPaymentKeepsInvoiceOpen() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String accountId = createAccount(session);
        String cardId = createCard(session, 5, 15);
        createCardExpense(session, cardId, "2026-03-01", "200.00");

        MvcResult listResult = mockMvc.perform(get("/api/v1/credit-cards/" + cardId + "/invoices").session(session))
                .andReturn();
        String invoiceId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/pay")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("accountId", accountId, "amount", "50.00", "paidOn", "2026-03-15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.paidAmount").value(50.00));
    }

    @Test
    void availableLimitDiscountsOpenInvoices() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, 5, 15);
        createCardExpense(session, cardId, "2026-03-01", "300.00");

        mockMvc.perform(get("/api/v1/credit-cards/" + cardId + "/available-limit").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableLimit").value(700.00));
    }
}
