package com.marlus.financas.installment;

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

class InstallmentPlanControllerIntegrationTest extends AbstractIntegrationTest {

    private String createCard(MockHttpSession session, String name, int closingDay, int dueDay) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("creditLimit", "10000.00");
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

    @Test
    void newPlanWithoutInterestGeneratesAllInstallmentsFromPurchaseMonth() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, "Cartão colchão", 5, 15);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", cardId);
        payload.put("description", "Colchão");
        payload.put("purchaseDate", "2026-08-10");
        payload.put("totalAmount", "4140.00");
        payload.put("installmentCount", 12);

        MvcResult result = mockMvc.perform(post("/api/v1/installment-plans")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installmentAmount").value(345.00))
                .andExpect(jsonPath("$.firstInvoiceMonth").value("2026-09"))
                .andExpect(jsonPath("$.lastInstallmentMonth").value("2027-08"))
                .andReturn();

        String planId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("cardId", cardId)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(12));

        mockMvc.perform(delete("/api/v1/installment-plans/" + planId).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("cardId", cardId)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void planAlreadyInProgressOnlyGeneratesRemainingInstallments() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, "Cartão celular", 10, 20);

        // "Celular 10x de R$ 325, estou na 3ª parcela, que já caiu na fatura de 2026-09."
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", cardId);
        payload.put("description", "Celular");
        payload.put("purchaseDate", "2026-07-01");
        payload.put("totalAmount", "3250.00");
        payload.put("installmentCount", 10);
        payload.put("firstInstallmentNumber", 3);
        payload.put("firstInvoiceMonth", "2026-09");

        mockMvc.perform(post("/api/v1/installment-plans")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastInstallmentMonth").value("2027-04"));

        // 10 parcelas - 2 já feitas fora do sistema = 8 lançamentos gerados.
        mockMvc.perform(get("/api/v1/transactions")
                        .session(session)
                        .param("cardId", cardId)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(8));
    }

    @Test
    void planWithInterestUsesPriceTableAndReportsTotalCost() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String cardId = createCard(session, "Cartão juros", 5, 15);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", cardId);
        payload.put("description", "Compra parcelada com juros");
        payload.put("purchaseDate", "2026-08-01");
        payload.put("totalAmount", "1000.00");
        payload.put("installmentCount", 10);
        payload.put("interestRateMonthly", "0.02");

        mockMvc.perform(post("/api/v1/installment-plans")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installmentAmount").value(111.33))
                .andExpect(jsonPath("$.totalToPay").value(1113.30))
                .andExpect(jsonPath("$.totalInterest").value(113.30));
    }
}
