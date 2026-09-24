package com.marlus.financas.commitment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Cobre o critério de aceite da Fase 4: "colchão 12x R$ 345 e celular 10x R$ 325 aparecem nos
 * meses certos, com fim e quanto libera".
 */
class CommitmentControllerIntegrationTest extends AbstractIntegrationTest {

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

    private void createPlan(
            MockHttpSession session,
            String cardId,
            String description,
            String purchaseDate,
            String totalAmount,
            int installmentCount) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", cardId);
        payload.put("description", description);
        payload.put("purchaseDate", purchaseDate);
        payload.put("totalAmount", totalAmount);
        payload.put("installmentCount", installmentCount);

        mockMvc.perform(post("/api/v1/installment-plans")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    void commitmentsShowInstallmentsInEveryMonthAndFreeUpWhenPlanEnds() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-compromissos", "senha123");
        String cardId = createCard(session, "Cartão compromissos", 1, 10);

        // Fechamento dia 1: compra dia 1 cai na fatura do próprio mês -> primeira parcela na competência da compra.
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate firstOfMonth = currentMonth.atDay(1);

        createPlan(session, cardId, "Colchão", firstOfMonth.toString(), "4140.00", 12);
        createPlan(session, cardId, "Celular", firstOfMonth.toString(), "3250.00", 10);

        MvcResult result = mockMvc.perform(get("/api/v1/commitments").session(session).param("months", "12"))
                .andExpect(status().isOk())
                .andReturn();
        var months = objectMapper.readTree(result.getResponse().getContentAsString());

        assertMonthCommitted(months, currentMonth, "670.00"); // 345 (colchão) + 325 (celular) nos 10 primeiros meses
        assertMonthCommitted(months, currentMonth.plusMonths(9), "670.00"); // último mês em que os dois coexistem
        assertMonthHasEndingPlan(months, currentMonth.plusMonths(9), "Celular");
        assertMonthCommitted(months, currentMonth.plusMonths(10), "345.00"); // celular já acabou, só colchão
        assertMonthCommitted(months, currentMonth.plusMonths(11), "345.00");
        assertMonthHasEndingPlan(months, currentMonth.plusMonths(11), "Colchão");
    }

    private void assertMonthCommitted(com.fasterxml.jackson.databind.JsonNode months, YearMonth month, String expected) {
        for (var node : months) {
            if (node.get("month").asText().equals(month.toString())) {
                org.assertj.core.api.Assertions.assertThat(node.get("installmentsTotal").decimalValue())
                        .isEqualByComparingTo(expected);
                return;
            }
        }
        throw new AssertionError("Mês não encontrado na resposta: " + month);
    }

    private void assertMonthHasEndingPlan(
            com.fasterxml.jackson.databind.JsonNode months, YearMonth month, String descriptionContains) {
        for (var node : months) {
            if (node.get("month").asText().equals(month.toString())) {
                boolean found = false;
                for (var plan : node.get("endingPlans")) {
                    if (plan.get("description").asText().equals(descriptionContains)) {
                        found = true;
                    }
                }
                org.assertj.core.api.Assertions.assertThat(found)
                        .as("esperava %s terminando em %s", descriptionContains, month)
                        .isTrue();
                return;
            }
        }
        throw new AssertionError("Mês não encontrado na resposta: " + month);
    }
}
