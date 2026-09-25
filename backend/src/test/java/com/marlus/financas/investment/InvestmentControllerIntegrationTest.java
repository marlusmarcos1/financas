package com.marlus.financas.investment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class InvestmentControllerIntegrationTest extends AbstractIntegrationTest {

    private String createAsset(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/investment-assets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ticker", "MXRF11",
                                "name", "Maxi Renda",
                                "assetClass", "FII",
                                "purpose", "RETIREMENT"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void buyingTwiceAtDifferentPricesUpdatesPortfolioWeightedAveragePrice() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-investimentos", "senha123");
        String assetId = createAsset(session);

        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUY", "date", "2026-01-10", "quantity", "100", "unitPrice", "10.00",
                                "amount", "1000.00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUY", "date", "2026-02-10", "quantity", "100", "unitPrice", "20.00",
                                "amount", "2000.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/investment-assets/" + assetId + "/price")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("price", "18.00"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/portfolio").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].quantity").value(200))
                .andExpect(jsonPath("$.positions[0].averagePrice").value(15.0))
                .andExpect(jsonPath("$.positions[0].currentValue").value(3600.00))
                .andExpect(jsonPath("$.totalCurrentValue").value(3600.00));
    }

    @Test
    void dividendsContributeToYieldOnCost() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-proventos", "senha123");
        String assetId = createAsset(session);

        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUY", "date", "2026-01-10", "quantity", "100", "unitPrice", "10.00",
                                "amount", "1000.00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "DIVIDEND", "date", "2026-02-05", "amount", "50.00"))))
                .andExpect(status().isCreated());

        // yield on cost = 50 / 1000 * 100 = 5%
        mockMvc.perform(get("/api/v1/portfolio").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].yieldOnCost").value(5.00));
    }
}
