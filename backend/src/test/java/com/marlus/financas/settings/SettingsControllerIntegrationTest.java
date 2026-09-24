package com.marlus.financas.settings;

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

class SettingsControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void defaultsAreReturnedWhenNothingWasCustomized() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-settings-padrao", "senha123");

        mockMvc.perform(get("/api/v1/settings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tithePercent").value(10))
                .andExpect(jsonPath("$.installmentLimitPercent").value(30))
                .andExpect(jsonPath("$.emergencyMonthsTarget").value(6))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void updatingSettingsPersistsNewValues() throws Exception {
        MockHttpSession session = createUserAndLogin("usuario-settings-custom", "senha123");

        mockMvc.perform(put("/api/v1/settings")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tithePercent", "10",
                                "installmentLimitPercent", "25",
                                "emergencyMonthsTarget", "8",
                                "currency", "BRL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentLimitPercent").value(25))
                .andExpect(jsonPath("$.emergencyMonthsTarget").value(8));

        mockMvc.perform(get("/api/v1/settings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentLimitPercent").value(25));
    }
}
