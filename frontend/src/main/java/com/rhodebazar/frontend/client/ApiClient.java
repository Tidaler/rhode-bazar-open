package com.rhodebazar.frontend.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Client REST vers le backend Rhode Bazar. */
public class ApiClient {

    private static final String URL_BASE_PAR_DEFAUT = "http://localhost:8085";

    private final String urlBase;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ApiClient() {
        this(System.getProperty("rhodebazar.api.url", URL_BASE_PAR_DEFAUT));
    }

    public ApiClient(String urlBase) {
        this.urlBase = urlBase;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public LoginResult login(String email, String motDePasse) throws ApiException {
        return envoyerAvecCorps("POST", "/api/auth/login",
                Map.of("email", email, "motDePasse", motDePasse), null, LoginResult.class, 200);
    }

    public List<CategorieDto> listerCategories(String token) throws ApiException {
        return versListe(get("/api/categories", token, CategorieDto[].class));
    }

    public CategorieDto creerCategorie(CategorieRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/categories", requete, token, CategorieDto.class, 201);
    }

    public CategorieDto modifierCategorie(Long id, CategorieRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("PUT", "/api/categories/" + id, requete, token, CategorieDto.class, 200);
    }

    public void supprimerCategorie(Long id, String token) throws ApiException {
        supprimer("/api/categories/" + id, token, 204);
    }

    public List<DepotDto> listerDepots(String token) throws ApiException {
        return versListe(get("/api/depots", token, DepotDto[].class));
    }

    public DepotDto creerDepot(DepotRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/depots", requete, token, DepotDto.class, 201);
    }

    public DepotDto modifierDepot(Long id, DepotRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("PUT", "/api/depots/" + id, requete, token, DepotDto.class, 200);
    }

    public void supprimerDepot(Long id, String token) throws ApiException {
        supprimer("/api/depots/" + id, token, 204);
    }

    public List<ProduitDto> listerProduits(String token) throws ApiException {
        return versListe(get("/api/produits", token, ProduitDto[].class));
    }

    public ProduitDto creerProduit(ProduitRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/produits", requete, token, ProduitDto.class, 201);
    }

    public ProduitDto modifierProduit(Long id, ProduitRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("PUT", "/api/produits/" + id, requete, token, ProduitDto.class, 200);
    }

    public ProduitDto rechercherProduitParCodeBarres(String codeBarres, String token) throws ApiException {
        String codeEncode = java.net.URLEncoder.encode(codeBarres, java.nio.charset.StandardCharsets.UTF_8);
        return get("/api/produits/code-barres/" + codeEncode, token, ProduitDto.class);
    }

    public void enregistrerMouvement(MouvementRequest requete, String token) throws ApiException {
        envoyerAvecCorps("POST", "/api/stock/mouvements", requete, token, Void.class, 201);
    }

    public List<AlerteStockDto> listerAlertes(String token) throws ApiException {
        return versListe(get("/api/stock/alertes", token, AlerteStockDto[].class));
    }

    public VenteDto creerVente(VenteRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/ventes", requete, token, VenteDto.class, 201);
    }

    public List<VenteDto> listerVentes(String token) throws ApiException {
        return versListe(get("/api/ventes", token, VenteDto[].class));
    }

    public byte[] telechargerFacture(Long venteId, String token) throws ApiException {
        return getBinaire("/api/ventes/" + venteId + "/facture", token);
    }

    public DepenseDto creerDepense(DepenseRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/depenses", requete, token, DepenseDto.class, 201);
    }

    public List<DepenseDto> listerDepenses(String token, CategorieDepense categorie) throws ApiException {
        String chemin = categorie != null ? "/api/depenses?categorie=" + categorie.name() : "/api/depenses";
        return versListe(get(chemin, token, DepenseDto[].class));
    }

    public TableauDeBordDto obtenirTableauDeBord(String token, LocalDate debut, LocalDate fin) throws ApiException {
        return get("/api/reporting/dashboard" + periode(debut, fin), token, TableauDeBordDto.class);
    }

    public PrevisionVentesDto obtenirPrevisions(String token) throws ApiException {
        return get("/api/reporting/previsions", token, PrevisionVentesDto.class);
    }

    public List<SuggestionReapprovisionnementDto> obtenirReapprovisionnement(String token) throws ApiException {
        return versListe(get("/api/reporting/reapprovisionnement", token, SuggestionReapprovisionnementDto[].class));
    }

    public List<AnomalieDto> obtenirAnomalies(String token) throws ApiException {
        return versListe(get("/api/reporting/anomalies", token, AnomalieDto[].class));
    }

    public ResumeIaDto obtenirResumeIa(String token, LocalDate debut, LocalDate fin) throws ApiException {
        return get("/api/reporting/resume-ia" + periode(debut, fin), token, ResumeIaDto.class);
    }

    public List<UtilisateurDto> listerUtilisateurs(String token) throws ApiException {
        return versListe(get("/api/utilisateurs", token, UtilisateurDto[].class));
    }

    public UtilisateurDto creerUtilisateur(UtilisateurRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/utilisateurs", requete, token, UtilisateurDto.class, 201);
    }

    public UtilisateurDto modifierUtilisateur(Long id, UtilisateurModificationRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("PUT", "/api/utilisateurs/" + id, requete, token, UtilisateurDto.class, 200);
    }

    public void changerMonMotDePasse(ChangerMotDePasseRequest requete, String token) throws ApiException {
        envoyerAvecCorps("PUT", "/api/compte/mot-de-passe", requete, token, Void.class, 204);
    }

    public List<SauvegardeDto> listerSauvegardes(String token) throws ApiException {
        return versListe(get("/api/administration/sauvegardes", token, SauvegardeDto[].class));
    }

    public SauvegardeDto declencherSauvegarde(String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/administration/sauvegardes", null, token, SauvegardeDto.class, 201);
    }

    private String periode(LocalDate debut, LocalDate fin) {
        if (debut == null && fin == null) {
            return "";
        }
        StringBuilder query = new StringBuilder("?");
        if (debut != null) {
            query.append("debut=").append(debut);
        }
        if (fin != null) {
            if (debut != null) {
                query.append("&");
            }
            query.append("fin=").append(fin);
        }
        return query.toString();
    }

    private <T> List<T> versListe(T[] tableau) {
        return new ArrayList<>(Arrays.asList(tableau));
    }

    private <T> T get(String chemin, String token, Class<T> type) throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(urlBase + chemin))
                .timeout(Duration.ofSeconds(10))
                .GET();
        ajouterAutorisation(builder, token);
        HttpResponse<String> reponse = envoyer(builder.build());
        verifierStatut(reponse, 200);
        return lireCorps(reponse, type);
    }

    private void supprimer(String chemin, String token, int statutAttendu) throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(urlBase + chemin))
                .timeout(Duration.ofSeconds(10))
                .DELETE();
        ajouterAutorisation(builder, token);
        HttpResponse<String> reponse = envoyer(builder.build());
        verifierStatut(reponse, statutAttendu);
    }

    private byte[] getBinaire(String chemin, String token) throws ApiException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(urlBase + chemin))
                .timeout(Duration.ofSeconds(10))
                .GET();
        ajouterAutorisation(builder, token);
        try {
            HttpResponse<byte[]> reponse = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (reponse.statusCode() != 200) {
                throw new ApiException("Erreur serveur (" + reponse.statusCode() + ")");
            }
            return reponse.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("SERVEUR_INACCESSIBLE", e);
        }
    }

    private <T> T envoyerAvecCorps(String methode, String chemin, Object corps, String token, Class<T> type, int statutAttendu)
            throws ApiException {
        try {
            String json = objectMapper.writeValueAsString(corps);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(urlBase + chemin))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(methode, HttpRequest.BodyPublishers.ofString(json));
            ajouterAutorisation(builder, token);

            HttpResponse<String> reponse = envoyer(builder.build());
            verifierStatut(reponse, statutAttendu);
            return type == Void.class ? null : lireCorps(reponse, type);
        } catch (IOException e) {
            throw new ApiException("SERVEUR_INACCESSIBLE", e);
        }
    }

    private void ajouterAutorisation(HttpRequest.Builder builder, String token) {
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private HttpResponse<String> envoyer(HttpRequest requete) throws ApiException {
        try {
            return httpClient.send(requete, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("SERVEUR_INACCESSIBLE", e);
        }
    }

    private void verifierStatut(HttpResponse<String> reponse, int statutAttendu) throws ApiException {
        if (reponse.statusCode() == statutAttendu) {
            return;
        }
        if (reponse.statusCode() == 401 || reponse.statusCode() == 403) {
            throw new ApiException("ACCES_REFUSE");
        }
        String message = extraireMessageErreur(reponse.body());
        throw new ApiException(message != null ? message : "Erreur serveur (" + reponse.statusCode() + ")");
    }

    private String extraireMessageErreur(String corps) {
        try {
            return objectMapper.readValue(corps, ErreurServeur.class).message();
        } catch (IOException e) {
            return null;
        }
    }

    private <T> T lireCorps(HttpResponse<String> reponse, Class<T> type) throws ApiException {
        try {
            return objectMapper.readValue(reponse.body(), type);
        } catch (IOException e) {
            throw new ApiException("REPONSE_INVALIDE", e);
        }
    }
}
