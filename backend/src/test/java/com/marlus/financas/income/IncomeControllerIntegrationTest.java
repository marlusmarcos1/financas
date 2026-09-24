package com.marlus.financas.income;

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

class IncomeControllerIntegrationTest extends AbstractIntegrationTest {

    private String createSource(
            MockHttpSession session, String name, String type, boolean countsInBaseBudget, boolean titheApplies)
            throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("type", type);
        payload.put("recurrence", "MONTHLY");
        payload.put("expectedAmount", "5000.00");
        payload.put("titheApplies", titheApplies);
        payload.put("countsInBaseBudget", countsInBaseBudget);

        MvcResult result = mockMvc.perform(post("/api/v1/income-sources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createEntry(MockHttpSession session, String sourceId, String month, String amount, String status)
            throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", sourceId);
        payload.put("referenceMonth", month);
        payload.put("amount", amount);
        payload.put("status", status);
        payload.put("receivedOn", status.equals("RECEIVED") ? "2026-09-10" : null);

        mockMvc.perform(post("/api/v1/income-entries")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    void scholarshipAndThirteenthStayOutOfBaseIncomeAndTitheAppliesToEverything() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-renda", "senha123");

        String salaryId = createSource(session, "Salário", "SALARY", true, true);
        String scholarshipId = createSource(session, "Bolsa", "SCHOLARSHIP", false, true);
        String thirteenthId = createSource(session, "13º", "THIRTEENTH", false, true);

        createEntry(session, salaryId, "2026-09", "5031.74", "RECEIVED");
        createEntry(session, scholarshipId, "2026-09", "3800.00", "RECEIVED");
        createEntry(session, thirteenthId, "2026-09", "1200.00", "RECEIVED");

        MvcResult result = mockMvc.perform(get("/api/v1/dashboard").session(session).param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseIncomeReceived").value(5031.74))
                .andExpect(jsonPath("$.extrasReceived").value(5000.00))
                .andReturn();

        // Dízimo de 10% sobre TUDO que entrou: (5031.74 + 3800 + 1200) * 10% = 1003.174 -> 1003.17
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.get("titheDue").decimalValue())
                .isEqualByComparingTo("1003.17");
    }

    @Test
    void titheAppliesFalseIsExcludedFromTitheBase() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-dizimo-isento", "senha123");
        String giftId = createSource(session, "Presente isento", "OTHER", false, false);

        createEntry(session, giftId, "2026-09", "500.00", "RECEIVED");

        MvcResult result = mockMvc.perform(get("/api/v1/dashboard").session(session).param("month", "2026-09"))
                .andExpect(status().isOk())
                .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.get("titheDue").decimalValue())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void cannotDeleteSourceWithEntries() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-fonte-exclusao", "senha123");
        String sourceId = createSource(session, "Extra", "EXTRA", false, true);
        createEntry(session, sourceId, "2026-09", "100.00", "RECEIVED");

        mockMvc.perform(delete("/api/v1/income-sources/" + sourceId).session(session).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void payingTitheUpdatesStatus() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-pagar-dizimo", "senha123");
        String salaryId = createSource(session, "Salário", "SALARY", true, true);
        createEntry(session, salaryId, "2026-09", "1000.00", "RECEIVED");

        MvcResult listResult = mockMvc.perform(get("/api/v1/tithe-ledger").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String ledgerId = objectMapper
                .readTree(listResult.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/tithe-ledger/" + ledgerId + "/pay")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", "100.00", "paidOn", "2026-09-15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
