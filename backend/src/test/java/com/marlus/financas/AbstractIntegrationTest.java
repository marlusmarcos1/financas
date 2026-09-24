package com.marlus.financas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.user.AppUser;
import com.marlus.financas.user.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    /**
     * Container único compartilhado por toda a suíte (padrão "singleton container"): iniciado uma vez
     * e nunca parado explicitamente — o Ryuk do Testcontainers derruba ao final da JVM de testes. Não
     * usamos {@code @Testcontainers}/{@code @Container} aqui porque eles parariam este container
     * estático ao final da PRIMEIRA classe de teste, quebrando as classes seguintes.
     */
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("financas")
                    .withUsername("financas")
                    .withPassword("financas");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AppUserRepository appUserRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Faz login como o usuário administrador padrão ("marlus") e devolve a sessão autenticada. */
    protected MockHttpSession loginAsDefaultUser() throws Exception {
        return login("marlus", "marlus");
    }

    /** Cria um segundo usuário isolado (para testes de isolamento por usuário) e faz login com ele. */
    protected MockHttpSession createUserAndLogin(String username, String password) throws Exception {
        AppUser user = new AppUser(
                UuidV7Generator.generate(), username, passwordEncoder.encode(password), username, null);
        appUserRepository.save(user);
        return login(username, password);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        HttpSession session = result.getRequest().getSession(false);
        return (MockHttpSession) session;
    }
}
