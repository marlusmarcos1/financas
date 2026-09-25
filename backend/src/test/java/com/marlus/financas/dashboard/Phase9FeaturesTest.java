package com.marlus.financas.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marlus.financas.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Fase 9: alertas, calendário, evolução patrimonial, checklist "pague-se primeiro" e seed-demo. */
class Phase9FeaturesTest extends AbstractIntegrationTest {

    @Test
    void alertsCalendarNetWorthHistoryAndExtrasRespondForAFreshUser() throws Exception {
        MockHttpSession session = createUserAndLogin("phase9_dashboard", "senhaSegura123");

        mockMvc.perform(get("/api/v1/dashboard/alerts").session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calendar").session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/net-worth/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        mockMvc.perform(get("/api/v1/dashboard/extras").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payYourselfFirst.length()").value(3))
                .andExpect(jsonPath("$.scholarships").isArray());
    }

    @Test
    void seedDemoIsIdempotent() throws Exception {
        MockHttpSession session = createUserAndLogin("phase9_seed", "senhaSegura123");

        mockMvc.perform(post("/api/v1/seed-demo").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(post("/api/v1/seed-demo").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false));

        mockMvc.perform(get("/api/v1/accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/credit-cards").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mockMvc.perform(get("/api/v1/goals").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
