package com.rhodebazar.backend.depense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.depense.CategorieDepense;
import com.rhodebazar.backend.domain.utilisateur.Role;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import com.rhodebazar.backend.web.dto.DepenseDto;
import com.rhodebazar.backend.web.dto.DepenseRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import java.io.IOException;
import java.math.BigDecimal;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-depense-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepenseModuleIntegrationTest {

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
        Files.deleteIfExists(Path.of("target/rhodebazar-depense-test.db"));
    }

    private String seConnecter(String email, String motDePasse) throws Exception {
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
    void unAdminPeutCreerEtListerDesDepenses() throws Exception {
        String token = seConnecter("admin@rhodebazar.local", "AdminTest123!");

        DepenseRequest requete = new DepenseRequest(CategorieDepense.FOURNISSEUR, "Achat de bougies", new BigDecimal("50000"));
        String reponse = mockMvc.perform(post("/api/depenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        DepenseDto depense = objectMapper.readValue(reponse, DepenseDto.class);
        assertThat(depense.montant()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(depense.categorie()).isEqualTo(CategorieDepense.FOURNISSEUR);

        mockMvc.perform(get("/api/depenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/depenses?categorie=FOURNISSEUR").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void unVendeurNePeutPasAccederAuxDepenses() throws Exception {
        if (utilisateurRepository.findByEmailIgnoreCase("vendeur.depense@rhodebazar.local").isEmpty()) {
            Utilisateur vendeur = new Utilisateur();
            vendeur.setNom("Vendeur Depense");
            vendeur.setEmail("vendeur.depense@rhodebazar.local");
            vendeur.setMotDePasse(passwordEncoder.encode("VendeurTest123!"));
            vendeur.setRole(Role.VENDEUR);
            utilisateurRepository.save(vendeur);
        }
        String token = seConnecter("vendeur.depense@rhodebazar.local", "VendeurTest123!");

        mockMvc.perform(get("/api/depenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        DepenseRequest requete = new DepenseRequest(CategorieDepense.DIVERS, "Interdit", new BigDecimal("100"));
        mockMvc.perform(post("/api/depenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());
    }
}
