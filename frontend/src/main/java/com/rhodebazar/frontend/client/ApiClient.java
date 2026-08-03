package com.rhodebazar.frontend.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    public List<DepotDto> listerDepots(String token) throws ApiException {
        return versListe(get("/api/depots", token, DepotDto[].class));
    }

    public DepotDto creerDepot(DepotRequest requete, String token) throws ApiException {
        return envoyerAvecCorps("POST", "/api/depots", requete, token, DepotDto.class, 201);
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
