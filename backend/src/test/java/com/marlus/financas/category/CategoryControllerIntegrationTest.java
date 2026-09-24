package com.marlus.financas.category;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class CategoryControllerIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> categoryPayload(String name, String kind, String nature) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("kind", kind);
        payload.put("nature", nature);
        return payload;
    }

    private String createCategory(MockHttpSession session, String name, String kind, String nature) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryPayload(name, kind, nature))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void parentWithDifferentKindIsRejected() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String expenseCategoryId = createCategory(session, "Moradia", "EXPENSE", "FIXED");

        Map<String, Object> child = categoryPayload("Salário", "INCOME", "FIXED");
        child.put("parentId", expenseCategoryId);

        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotDeleteCategoryWithSubcategory() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String parentId = createCategory(session, "Cartões", "EXPENSE", "VARIABLE");

        Map<String, Object> child = categoryPayload("Fatura Nubank", "EXPENSE", "VARIABLE");
        child.put("parentId", parentId);
        mockMvc.perform(post("/api/v1/categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/categories/" + parentId).session(session).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void createBudgetAndRejectDuplicateDefaultBudget() throws Exception {
        MockHttpSession session = loginAsDefaultUser();
        String categoryId = createCategory(session, "Combustível", "EXPENSE", "VARIABLE");

        mockMvc.perform(post("/api/v1/categories/" + categoryId + "/budgets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("limitAmount", "950.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitAmount").value(950.00));

        mockMvc.perform(post("/api/v1/categories/" + categoryId + "/budgets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("limitAmount", "800.00"))))
                .andExpect(status().isConflict());
    }
}
