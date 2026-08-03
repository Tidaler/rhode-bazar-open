package com.rhodebazar.backend.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.web.dto.CategorieDto;
import com.rhodebazar.backend.web.dto.CategorieRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La grande majorite des produits n'ont pas de code-barres : ce module verifie que
 * l'unicite du code-barres (quand il est fourni) n'empeche pas plusieurs produits
 * sans code de coexister, que la recherche par scan fonctionne, et que les doublons
 * sont bien refuses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-codebarres-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeBarresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long categorieId;
    private static Long produitAvecCodeId;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-codebarres-test.db"));
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
    void plusieursProduitsSansCodeBarresPeuventCoexister() throws Exception {
        String token = jetonAdmin();

        String reponseCategorie = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategorieRequest("Deco", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        categorieId = objectMapper.readValue(reponseCategorie, CategorieDto.class).id();

        ProduitRequest sansCode1 = new ProduitRequest(
                "Produit artisanal 1", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", null);
        ProduitRequest sansCode2 = new ProduitRequest(
                "Produit artisanal 2", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", "");

        mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sansCode1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sansCode2)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    void creerUnProduitAvecCodeBarresEtLeRetrouverParScan() throws Exception {
        String token = jetonAdmin();

        ProduitRequest requete = new ProduitRequest(
                "Bougie scannable", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", "1234567890123");

        String reponse = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ProduitDto produit = objectMapper.readValue(reponse, ProduitDto.class);
        produitAvecCodeId = produit.id();
        assertThat(produit.codeBarres()).isEqualTo("1234567890123");

        String reponseScan = mockMvc.perform(get("/api/produits/code-barres/1234567890123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ProduitDto trouve = objectMapper.readValue(reponseScan, ProduitDto.class);
        assertThat(trouve.id()).isEqualTo(produitAvecCodeId);
    }

    @Test
    @Order(3)
    void scannerUnCodeInconnuRenvoie404() throws Exception {
        String token = jetonAdmin();
        mockMvc.perform(get("/api/produits/code-barres/0000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(4)
    void unCodeBarresDejaUtiliseEstRefuseALaCreation() throws Exception {
        String token = jetonAdmin();
        ProduitRequest requeteDoublon = new ProduitRequest(
                "Autre bougie", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", "1234567890123");

        mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteDoublon)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    void unCodeBarresDejaUtiliseEstRefuseALaModification() throws Exception {
        String token = jetonAdmin();

        ProduitRequest autreProduit = new ProduitRequest(
                "Produit a modifier", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", null);
        String reponse = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autreProduit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long autreId = objectMapper.readValue(reponse, ProduitDto.class).id();

        ProduitRequest modification = new ProduitRequest(
                "Produit a modifier", null, categorieId, new BigDecimal("500"), new BigDecimal("1000"), 2, "unite", "1234567890123");
        mockMvc.perform(put("/api/produits/" + autreId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isConflict());
    }
}
