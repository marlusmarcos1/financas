package com.marlus.financas.retirement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class RetirementPlanControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void projectionReturnsThreeFixedScenariosMatchingSpecAcceptanceCriterion() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-aposentadoria", "senha123");

        mockMvc.perform(put("/api/v1/retirement-plan")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monthlyContribution", "500.00",
                                "contributionAnnualIncreasePercent", "0",
                                "startDate", "2026-09-01",
                                "horizonYears", 30,
                                "expectedReturnNominalAnnual", "0.08",
                                "expectedInflationAnnual", "0.045",
                                "currentBalance", "0"))))
                .andExpect(status().isOk());

        var result = mockMvc.perform(get("/api/v1/retirement-plan/projection").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios.length()").value(3))
                .andExpect(jsonPath("$.scenarios[0].label").value("Pessimista"))
                .andExpect(jsonPath("$.scenarios[1].label").value("Base"))
                .andExpect(jsonPath("$.scenarios[2].label").value("Otimista"))
                .andExpect(jsonPath("$.scenarios[1].annualRate").value(0.08))
                .andReturn();

        var baseScenario = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("scenarios")
                .get(1);
        org.assertj.core.api.Assertions.assertThat(baseScenario.get("finalBalanceNominal").decimalValue())
                .isCloseTo(new java.math.BigDecimal("704000"), org.assertj.core.api.Assertions.within(new java.math.BigDecimal("5000")));
        org.assertj.core.api.Assertions.assertThat(baseScenario.get("yearlyBalances").size()).isEqualTo(30);
    }

    @Test
    void gettingProjectionWithoutAPlanReturnsNotFound() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-sem-plano", "senha123");

        mockMvc.perform(get("/api/v1/retirement-plan/projection").session(session)).andExpect(status().isNotFound());
    }
}
