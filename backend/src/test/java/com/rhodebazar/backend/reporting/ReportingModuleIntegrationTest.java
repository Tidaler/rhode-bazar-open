package com.rhodebazar.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.domain.depense.CategorieDepense;
import com.rhodebazar.backend.domain.stock.TypeMouvement;
import com.rhodebazar.backend.domain.vente.ModePaiement;
import com.rhodebazar.backend.web.dto.AnomalieDto;
import com.rhodebazar.backend.web.dto.CategorieDto;
import com.rhodebazar.backend.web.dto.CategorieRequest;
import com.rhodebazar.backend.web.dto.DepenseRequest;
import com.rhodebazar.backend.web.dto.DepotDto;
import com.rhodebazar.backend.web.dto.DepotRequest;
import com.rhodebazar.backend.web.dto.LigneVenteRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
import com.rhodebazar.backend.web.dto.SuggestionReapprovisionnementDto;
import com.rhodebazar.backend.web.dto.TableauDeBordDto;
import com.rhodebazar.backend.web.dto.VenteRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-reporting-test.db?foreign_keys=on",
        "RHODEBAZAR_ADMIN_PASSWORD=AdminTest123!"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportingModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long depotId;
    private static Long produitAId;
    private static Long produitBId;

    @BeforeAll
    static void nettoyerBaseDeTest() throws IOException {
        Files.deleteIfExists(Path.of("target/rhodebazar-reporting-test.db"));
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
    void preparerCatalogueStockVentesEtDepenses() throws Exception {
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

        produitAId = creerProduit(token, categorieId, "Bougie", new BigDecimal("500"), new BigDecimal("1000"), 5);
        produitBId = creerProduit(token, categorieId, "Vase", new BigDecimal("300"), new BigDecimal("800"), 5);

        approvisionner(token, produitAId, 50);
        approvisionner(token, produitBId, 10);

        VenteRequest vente1 = new VenteRequest(depotId, ModePaiement.ESPECES, null,
                List.of(new LigneVenteRequest(produitAId, 10)));
        mockMvc.perform(post("/api/ventes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vente1)))
                .andExpect(status().isCreated());

        VenteRequest vente2 = new VenteRequest(depotId, ModePaiement.CARTE, null,
                List.of(new LigneVenteRequest(produitBId, 8)));
        mockMvc.perform(post("/api/ventes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vente2)))
                .andExpect(status().isCreated());

        creerDepense(token, CategorieDepense.CHARGE_FIXE, "Loyer", new BigDecimal("2000"));
        creerDepense(token, CategorieDepense.FOURNISSEUR, "Achat 1", new BigDecimal("1000"));
        creerDepense(token, CategorieDepense.FOURNISSEUR, "Achat 2", new BigDecimal("1100"));
        creerDepense(token, CategorieDepense.FOURNISSEUR, "Achat 3", new BigDecimal("950"));
        creerDepense(token, CategorieDepense.FOURNISSEUR, "Achat anormal", new BigDecimal("20000"));
    }

    @Test
    @Order(2)
    void leTableauDeBordCalculeCorrectementCaMargeEtBenefice() throws Exception {
        String token = jetonAdmin();
        LocalDate aujourdHui = LocalDate.now();

        String reponse = mockMvc.perform(get("/api/reporting/dashboard")
                        .param("debut", aujourdHui.toString())
                        .param("fin", aujourdHui.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        TableauDeBordDto tableau = objectMapper.readValue(reponse, TableauDeBordDto.class);

        // CA = 10*1000 + 8*800 = 16400 ; COGS = 10*500 + 8*300 = 7400 ; marge = 9000
        assertThat(tableau.chiffreAffaires()).isEqualByComparingTo(new BigDecimal("16400"));
        assertThat(tableau.coutMarchandisesVendues()).isEqualByComparingTo(new BigDecimal("7400"));
        assertThat(tableau.margeBrute()).isEqualByComparingTo(new BigDecimal("9000"));
        // depenses = 2000+1000+1100+950+20000 = 25050
        assertThat(tableau.depensesTotales()).isEqualByComparingTo(new BigDecimal("25050"));
        assertThat(tableau.beneficeNet()).isEqualByComparingTo(new BigDecimal("9000").subtract(new BigDecimal("25050")));
        assertThat(tableau.produitsPlusVendus()).isNotEmpty();
        assertThat(tableau.produitsPlusVendus().get(0).produitNom()).isEqualTo("Bougie");
        assertThat(tableau.resumeLocal()).isNotBlank();
    }

    @Test
    @Order(3)
    void lesPrevisionsRepondentSansErreur() throws Exception {
        String token = jetonAdmin();
        mockMvc.perform(get("/api/reporting/previsions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void leReapprovisionnementSuggereLeProduitEnRupture() throws Exception {
        String token = jetonAdmin();
        // Vase : stock initial 10, vendu 8 => reste 2, seuil minimal 5 => doit apparaitre
        String reponse = mockMvc.perform(get("/api/reporting/reapprovisionnement")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<SuggestionReapprovisionnementDto> suggestions = objectMapper.readValue(
                reponse, objectMapper.getTypeFactory().constructCollectionType(List.class, SuggestionReapprovisionnementDto.class));

        assertThat(suggestions).anyMatch(s -> s.produitId().equals(produitBId));
    }

    @Test
    @Order(5)
    void lesAnomaliesDetectentLaDepenseInhabituelle() throws Exception {
        String token = jetonAdmin();
        String reponse = mockMvc.perform(get("/api/reporting/anomalies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<AnomalieDto> anomalies = objectMapper.readValue(
                reponse, objectMapper.getTypeFactory().constructCollectionType(List.class, AnomalieDto.class));

        assertThat(anomalies).anyMatch(a -> a.description().contains("Achat anormal"));
    }

    private Long creerProduit(String token, Long categorieId, String nom, BigDecimal prixAchat, BigDecimal prixVente, int seuil)
            throws Exception {
        ProduitRequest requete = new ProduitRequest(nom, null, categorieId, prixAchat, prixVente, seuil, "unite");
        String reponse = mockMvc.perform(post("/api/produits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(reponse, ProduitDto.class).id();
    }

    private void approvisionner(String token, Long produitId, int quantite) throws Exception {
        MouvementRequest requete = new MouvementRequest(produitId, depotId, TypeMouvement.ENTREE, quantite, "Stock initial");
        mockMvc.perform(post("/api/stock/mouvements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated());
    }

    private void creerDepense(String token, CategorieDepense categorie, String libelle, BigDecimal montant) throws Exception {
        DepenseRequest requete = new DepenseRequest(categorie, libelle, montant);
        mockMvc.perform(post("/api/depenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated());
    }
}
