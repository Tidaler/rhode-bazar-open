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
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-vente-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VenteModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long depotId;
    private static Long produitId;
    private static final BigDecimal PRIX_VENTE = new BigDecimal("2500");

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-vente-test.db"));
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
    void preparerCatalogueEtStock() throws Exception {
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
        depotId = objectMapper.readValue(reponseDepot, DepotDto.class).id();

        ProduitRequest requeteProduit = new ProduitRequest(
                "Vase decoratif", null, categorieId, new BigDecimal("1000"), PRIX_VENTE, 2, "unite");
        String reponseProduit = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteProduit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        produitId = objectMapper.readValue(reponseProduit, ProduitDto.class).id();

        MouvementRequest entree = new MouvementRequest(produitId, depotId, TypeMouvement.ENTREE, 20, "Stock initial");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entree)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    void enregistrerUneVenteDecrementeLeStockEtCalculeLeTotal() throws Exception {
        String token = jetonAdmin();

        VenteRequest requeteVente = new VenteRequest(
                depotId, ModePaiement.ESPECES, "Client Test", List.of(new LigneVenteRequest(produitId, 5)));

        String reponse = mockMvc.perform(post("/api/ventes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteVente)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        VenteDto vente = objectMapper.readValue(reponse, VenteDto.class);

        assertThat(vente.montantTotal()).isEqualByComparingTo(PRIX_VENTE.multiply(BigDecimal.valueOf(5)));
        assertThat(vente.lignes()).hasSize(1);

        String reponseProduit = mockMvc.perform(get("/api/produits/" + produitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ProduitDto produit = objectMapper.readValue(reponseProduit, ProduitDto.class);
        assertThat(produit.stockTotal()).isEqualTo(15);
    }

    @Test
    @Order(3)
    void venteSansStockSuffisantEstRefuseeEtNeModifiePasLeStock() throws Exception {
        String token = jetonAdmin();

        VenteRequest requeteExcessive = new VenteRequest(
                depotId, ModePaiement.CARTE, null, List.of(new LigneVenteRequest(produitId, 999)));

        mockMvc.perform(post("/api/ventes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteExcessive)))
                .andExpect(status().isConflict());

        String reponseProduit = mockMvc.perform(get("/api/produits/" + produitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ProduitDto produit = objectMapper.readValue(reponseProduit, ProduitDto.class);
        assertThat(produit.stockTotal()).isEqualTo(15);
    }

    @Test
    @Order(4)
    void listerLesVentes() throws Exception {
        String token = jetonAdmin();

        mockMvc.perform(get("/api/ventes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
