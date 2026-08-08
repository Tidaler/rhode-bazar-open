package com.rhodebazar.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.utilisateur.Role;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.SauvegardeDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-administration-test.db?foreign_keys=on",
        "RHODEBAZAR_DB_PATH=target/rhodebazar-administration-test.db",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdministrationModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-administration-test.db"));
    }

    private String jetonAdmin() throws Exception {
        String corps = objectMapper.writeValueAsString(new HashMap<>() {{
            put("email", "admin@rhodebazar.local");
            put("motDePasse", "AdminTest123!");
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
    void declencherUneSauvegardeEtLaRetrouverDansLaListe() throws Exception {
        String token = jetonAdmin();

        String reponse = mockMvc.perform(post("/api/administration/sauvegardes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        SauvegardeDto sauvegarde = objectMapper.readValue(reponse, SauvegardeDto.class);
        assertThat(sauvegarde.tailleOctets()).isGreaterThan(0);

        String reponseListe = mockMvc.perform(get("/api/administration/sauvegardes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<SauvegardeDto> sauvegardes = List.of(objectMapper.readValue(reponseListe, SauvegardeDto[].class));
        assertThat(sauvegardes).extracting(SauvegardeDto::nom).contains(sauvegarde.nom());
    }

    @Test
    @Order(2)
    void unVendeurNePeutPasAccederALAdministration() throws Exception {
        if (utilisateurRepository.findByEmailIgnoreCase("vendeur.admin.test@rhodebazar.local").isEmpty()) {
            Utilisateur vendeur = new Utilisateur();
            vendeur.setNom("Vendeur Test");
            vendeur.setEmail("vendeur.admin.test@rhodebazar.local");
            vendeur.setMotDePasse(passwordEncoder.encode("VendeurTest123!"));
            vendeur.setRole(Role.VENDEUR);
            utilisateurRepository.save(vendeur);
        }
        String corps = objectMapper.writeValueAsString(new HashMap<>() {{
            put("email", "vendeur.admin.test@rhodebazar.local");
            put("motDePasse", "VendeurTest123!");
        }});
        String reponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readValue(reponse, LoginResponse.class).token();

        mockMvc.perform(get("/api/administration/sauvegardes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
