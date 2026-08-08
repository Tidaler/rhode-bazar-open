package com.rhodebazar.backend.vente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.stock.TypeMouvement;
import com.rhodebazar.backend.domain.vente.ModePaiement;
import com.rhodebazar.backend.web.dto.CategorieDto;
import com.rhodebazar.backend.web.dto.CategorieRequest;
import com.rhodebazar.backend.web.dto.DepotDto;
import com.rhodebazar.backend.web.dto.DepotRequest;
import com.rhodebazar.backend.web.dto.LigneVenteRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
import com.rhodebazar.backend.web.dto.VenteDto;
import com.rhodebazar.backend.web.dto.VenteRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-facture-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FactureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long venteId;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-facture-test.db"));
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
    void preparerUneVente() throws Exception {
        String token = jetonAdmin();

        String reponseCategorie = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategorieRequest("Deco", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categorieId = objectMapper.readValue(reponseCategorie, CategorieDto.class).id();

        String reponseDepot = mockMvc.perform(post("/api/depots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepotRequest("Boutique", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long depotId = objectMapper.readValue(reponseDepot, DepotDto.class).id();

        ProduitRequest requeteProduit = new ProduitRequest(
                "Vase decoratif", null, categorieId, new BigDecimal("1000"), new BigDecimal("2500"), 2, "unite", null);
        String reponseProduit = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteProduit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long produitId = objectMapper.readValue(reponseProduit, ProduitDto.class).id();

        MouvementRequest entree = new MouvementRequest(produitId, depotId, TypeMouvement.ENTREE, 10, "Stock initial");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entree)))
                .andExpect(status().isCreated());

        VenteRequest requeteVente = new VenteRequest(
                depotId, ModePaiement.ESPECES, "Client Facture", List.of(new LigneVenteRequest(produitId, 3)));
        String reponseVente = mockMvc.perform(post("/api/ventes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteVente)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        venteId = objectMapper.readValue(reponseVente, VenteDto.class).id();
    }

    @Test
    @Order(2)
    void telechargerLaFactureRenvoieUnPdfValide() throws Exception {
        String token = jetonAdmin();

        byte[] pdf = mockMvc.perform(get("/api/ventes/" + venteId + "/facture")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf.length).isGreaterThan(0);
        String enTete = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertThat(enTete).isEqualTo("%PDF");
    }
}
