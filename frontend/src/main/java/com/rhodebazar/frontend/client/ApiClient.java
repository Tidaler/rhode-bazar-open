package com.rhodebazar.frontend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Client REST minimal vers le backend Rhode Bazar. */
public class ApiClient {

    private static final String URL_BASE_PAR_DEFAUT = "http://localhost:8085";

    private final String urlBase;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            String corps = objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "motDePasse", motDePasse));

            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corps))
                    .build();

            HttpResponse<String> reponse = httpClient.send(requete, HttpResponse.BodyHandlers.ofString());

            if (reponse.statusCode() == 200) {
                return objectMapper.readValue(reponse.body(), LoginResult.class);
            } else if (reponse.statusCode() == 401 || reponse.statusCode() == 403) {
                throw new ApiException("IDENTIFIANTS_INVALIDES");
            } else {
                throw new ApiException("Erreur serveur (" + reponse.statusCode() + ")");
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("SERVEUR_INACCESSIBLE", e);
        }
    }
}
