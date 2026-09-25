package com.marlus.financas.allocation;

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

class AllocationTargetControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void comparisonSuggestsMostUnderweightAssetClassForNextContribution() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-alocacao", "senha123");

        mockMvc.perform(post("/api/v1/allocation-targets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("purpose", "RETIREMENT", "assetClass", "FII", "targetPercent", "20"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/allocation-targets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("purpose", "RETIREMENT", "assetClass", "STOCK", "targetPercent", "80"))))
                .andExpect(status().isCreated());

        String assetId = createAsset(session, "STOCK");
        buy(session, assetId, "100", "10.00");

        // 100% do patrimônio está em STOCK (alvo 80%), FII (alvo 20%) está 100% abaixo do alvo -> sugestão FII.
        mockMvc.perform(get("/api/v1/allocation-targets/comparison").session(session).param("purpose", "RETIREMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextContributionSuggestion").value("FII"));
    }

    private String createAsset(MockHttpSession session, String assetClass) throws Exception {
        var result = mockMvc.perform(post("/api/v1/investment-assets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ticker", "AAAA11", "name", "Ativo", "assetClass", assetClass, "purpose", "RETIREMENT"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void buy(MockHttpSession session, String assetId, String quantity, String unitPrice) throws Exception {
        mockMvc.perform(post("/api/v1/investment-assets/" + assetId + "/transactions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUY", "date", "2026-01-10", "quantity", quantity, "unitPrice", unitPrice,
                                "amount", "0"))))
                .andExpect(status().isCreated());
    }
}
