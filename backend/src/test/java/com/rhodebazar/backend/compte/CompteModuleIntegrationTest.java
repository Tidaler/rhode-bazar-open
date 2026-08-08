package com.rhodebazar.backend.compte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.web.dto.ChangerMotDePasseRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-compte-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompteModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-compte-test.db"));
    }

    private String jeton(String email, String motDePasse) throws Exception {
        String corps = objectMapper.writeValueAsString(new HashMap<>() {{
            put("email", email);
            put("motDePasse", motDePasse);
        }});
        String reponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(reponse, LoginResponse.class).token();
    }

    @Test
    @Order(1)
    void mauvaisMotDePasseActuelEstRefuse() throws Exception {
        String token = jeton("admin@rhodebazar.local", "AdminTest123!");

        mockMvc.perform(put("/api/compte/mot-de-passe")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangerMotDePasseRequest("MauvaisMotDePasse!", "NouveauMotDePasse123!"))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    void changerLeMotDePassePuisSeReconnecterAvecLeNouveau() throws Exception {
        String token = jeton("admin@rhodebazar.local", "AdminTest123!");

        mockMvc.perform(put("/api/compte/mot-de-passe")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangerMotDePasseRequest("AdminTest123!", "NouveauMotDePasse123!"))))
                .andExpect(status().isNoContent());

        jeton("admin@rhodebazar.local", "NouveauMotDePasse123!");
    }
}
