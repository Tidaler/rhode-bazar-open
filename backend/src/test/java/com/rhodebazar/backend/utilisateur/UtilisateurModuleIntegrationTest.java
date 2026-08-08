package com.rhodebazar.backend.utilisateur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.utilisateur.Role;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.UtilisateurDto;
import com.rhodebazar.backend.web.dto.UtilisateurModificationRequest;
import com.rhodebazar.backend.web.dto.UtilisateurRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-utilisateur-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UtilisateurModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long adminId;
    private static Long vendeurCreeId;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-utilisateur-test.db"));
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

    private String jetonAdmin() throws Exception {
        return jeton("admin@rhodebazar.local", "AdminTest123!");
    }

    @Test
    @Order(1)
    void adminCreeUnVendeurEtLeRetrouveDansLaListe() throws Exception {
        String token = jetonAdmin();

        String reponse = mockMvc.perform(post("/api/utilisateurs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UtilisateurRequest("Vendeur Un", "vendeur.un@rhodebazar.local", "VendeurUn123!", Role.VENDEUR))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UtilisateurDto cree = objectMapper.readValue(reponse, UtilisateurDto.class);
        vendeurCreeId = cree.id();
        assertThat(cree.role()).isEqualTo("VENDEUR");
        assertThat(cree.actif()).isTrue();

        String reponseListe = mockMvc.perform(get("/api/utilisateurs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<UtilisateurDto> utilisateurs = List.of(objectMapper.readValue(reponseListe, UtilisateurDto[].class));
        assertThat(utilisateurs).extracting(UtilisateurDto::email).contains("vendeur.un@rhodebazar.local");
        adminId = utilisateurs.stream().filter(u -> u.email().equals("admin@rhodebazar.local"))
                .findFirst().orElseThrow().id();
    }

    @Test
    @Order(2)
    void creerAvecEmailDejaUtiliseEstRefuse() throws Exception {
        String token = jetonAdmin();

        mockMvc.perform(post("/api/utilisateurs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UtilisateurRequest("Doublon", "vendeur.un@rhodebazar.local", "Quelconque123!", Role.VENDEUR))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void unVendeurNePeutPasAccederALaGestionDesUtilisateurs() throws Exception {
        String token = jeton("vendeur.un@rhodebazar.local", "VendeurUn123!");

        mockMvc.perform(get("/api/utilisateurs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void modifierUnUtilisateur() throws Exception {
        String token = jetonAdmin();

        String reponse = mockMvc.perform(put("/api/utilisateurs/" + vendeurCreeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UtilisateurModificationRequest("Vendeur Un Renomme", Role.VENDEUR, false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UtilisateurDto modifie = objectMapper.readValue(reponse, UtilisateurDto.class);
        assertThat(modifie.nom()).isEqualTo("Vendeur Un Renomme");
        assertThat(modifie.actif()).isFalse();
    }

    @Test
    @Order(5)
    void unAdminNePeutPasSeDesactiverLuiMeme() throws Exception {
        String token = jetonAdmin();

        mockMvc.perform(put("/api/utilisateurs/" + adminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UtilisateurModificationRequest("Administrateur", Role.ADMIN, false))))
                .andExpect(status().isConflict());
    }
}
