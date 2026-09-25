package com.marlus.financas.networth;

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

class NetWorthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void netWorthCombinesAccountBalancesAndInvestmentValue() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-patrimonio", "senha123");

        MvcResult accountResult = mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Conta", "type", "CHECKING", "institution", "Banco",
                                "initialBalance", "1000.00", "purpose", "DAILY"))))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = objectMapper.readTree(accountResult.getResponse().getContentAsString()).get("id").asText();

        // Receita de 500 e despesa de 200 na conta -> saldo = 1000 + 500 - 200 = 1300.
        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "INCOME", "description", "Bico", "amount", "500.00", "date", "2026-09-05",
                                "accountId", accountId))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "kind", "EXPENSE", "description", "Compra", "amount", "200.00", "date", "2026-09-06",
                                "accountId", accountId))))
                .andExpect(status().isCreated());

        // Investimento: 10 cotas a 10 = 100 de valor.
        MvcResult assetResult = mockMvc.perform(post("/api/v1/investment-assets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ticker", "MXRF11", "name", "Maxi Renda", "assetClass", "FII", "purpose", "RETIREMENT"))))
                .andExpect(status().isCreated())
                .andReturn();
        String assetId = objectMapper.readTree(assetResult.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUY", "date", "2026-01-10", "quantity", "10", "unitPrice", "10.00",
                                "amount", "100.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/net-worth").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountsTotal").value(1300.00))
                .andExpect(jsonPath("$.investmentsTotal").value(100.00))
                .andExpect(jsonPath("$.netWorth").value(1400.00));
    }
}
