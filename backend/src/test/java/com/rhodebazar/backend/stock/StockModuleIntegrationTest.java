package com.rhodebazar.backend.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.utilisateur.Role;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import com.rhodebazar.backend.web.dto.CategorieDto;
import com.rhodebazar.backend.web.dto.CategorieRequest;
import com.rhodebazar.backend.web.dto.DepotDto;
import com.rhodebazar.backend.web.dto.DepotRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-stock-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Long categorieId;
    private static Long depotId;
    private static Long produitId;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-stock-test.db"));
    }

    private String jetonAdmin() throws Exception {
        return seConnecter("admin@rhodebazar.local", "AdminTest123!");
    }

    private String seConnecter(String email, String motDePasse) throws Exception {
        String corps = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
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
    void creerCategorieDepotEtProduit() throws Exception {
        String token = jetonAdmin();

        String reponseCategorie = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategorieRequest("Bougies", "Bougies parfumees"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        categorieId = objectMapper.readValue(reponseCategorie, CategorieDto.class).id();

        String reponseDepot = mockMvc.perform(post("/api/depots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepotRequest("Entrepot principal", "Douala"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        depotId = objectMapper.readValue(reponseDepot, DepotDto.class).id();

        ProduitRequest requeteProduit = new ProduitRequest(
                "Bougie parfumee vanille", "Bougie artisanale", categorieId,
                new BigDecimal("1500"), new BigDecimal("2500"), 5, "unite");
        String reponseProduit = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteProduit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ProduitDto produit = objectMapper.readValue(reponseProduit, ProduitDto.class);
        produitId = produit.id();

        assertThat(produit.stockTotal()).isZero();
        assertThat(produit.stockCritique()).isTrue();
    }

    @Test
    @Order(2)
    void enregistrerEntreeMetAJourLeStock() throws Exception {
        String token = jetonAdmin();

        MouvementRequest entree = new MouvementRequest(produitId, depotId,
                com.rhodebazar.backend.domain.stock.TypeMouvement.ENTREE, 10, "Reception fournisseur");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entree)))
                .andExpect(status().isCreated());

        String reponse = mockMvc.perform(get("/api/produits/" + produitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ProduitDto produit = objectMapper.readValue(reponse, ProduitDto.class);

        assertThat(produit.stockTotal()).isEqualTo(10);
        assertThat(produit.stockCritique()).isFalse();
    }

    @Test
    @Order(3)
    void sortieSuperieureAuStockEstRefusee() throws Exception {
        String token = jetonAdmin();

        MouvementRequest sortieExcessive = new MouvementRequest(produitId, depotId,
                com.rhodebazar.backend.domain.stock.TypeMouvement.SORTIE, 999, "Vente");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sortieExcessive)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void sortiePasseSousLeSeuilDeclencheUneAlerte() throws Exception {
        String token = jetonAdmin();

        MouvementRequest sortie = new MouvementRequest(produitId, depotId,
                com.rhodebazar.backend.domain.stock.TypeMouvement.SORTIE, 8, "Vente comptoir");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sortie)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock/alertes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void unVendeurNePeutPasCreerDeCategorie() throws Exception {
        if (utilisateurRepository.findByEmailIgnoreCase("vendeur.test@rhodebazar.local").isEmpty()) {
            Utilisateur vendeur = new Utilisateur();
            vendeur.setNom("Vendeur Test");
            vendeur.setEmail("vendeur.test@rhodebazar.local");
            vendeur.setMotDePasse(passwordEncoder.encode("VendeurTest123!"));
            vendeur.setRole(Role.VENDEUR);
            utilisateurRepository.save(vendeur);
        }
        String token = seConnecter("vendeur.test@rhodebazar.local", "VendeurTest123!");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategorieRequest("Interdit", null))))
                .andExpect(status().isForbidden());
    }
}
