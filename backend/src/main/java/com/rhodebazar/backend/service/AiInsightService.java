package com.rhodebazar.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhodebazar.backend.web.dto.DepenseCategorieDto;
import com.rhodebazar.backend.web.dto.PrevisionVentesDto;
import com.rhodebazar.backend.web.dto.ProduitVenteDto;
import com.rhodebazar.backend.web.dto.ResumeIaDto;
import com.rhodebazar.backend.web.dto.TableauDeBordDto;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resume en langage naturel du tableau de bord, genere via l'API Claude d'Anthropic.
 *
 * <p>N'envoie que des chiffres agreges (CA, marge, depenses par categorie, produits les
 * plus/moins vendus) : aucune donnee client individuelle, aucun libelle de depense qui
 * pourrait contenir des informations personnelles. Si aucune cle API n'est configuree,
 * ou si l'appel echoue (reseau, quota, etc.), le service degrade proprement : le reste
 * du reporting (analyses locales) continue de fonctionner sans IA generative.
 */
@Service
public class AiInsightService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightService.class);
    private static final String URL_ANTHROPIC = "https://api.anthropic.com/v1/messages";
    private static final String VERSION_API = "2023-06-01";

    private final String apiKey;
    private final String modele;
    private final int maxTokens;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiInsightService(
            @Value("${rhodebazar.ai.anthropic.api-key:}") String apiKey,
            @Value("${rhodebazar.ai.anthropic.model:claude-opus-5}") String modele,
            @Value("${rhodebazar.ai.anthropic.max-tokens:1024}") int maxTokens) {
        this.apiKey = apiKey;
        this.modele = modele;
        this.maxTokens = maxTokens;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean estConfiguree() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ResumeIaDto genererResume(TableauDeBordDto tableau, PrevisionVentesDto prevision) {
        if (!estConfiguree()) {
            return ResumeIaDto.indisponible(
                    "Resume IA non configure : definissez la variable d'environnement ANTHROPIC_API_KEY pour l'activer.");
        }

        try {
            String prompt = construirePrompt(tableau, prevision);
            String corpsRequete = objectMapper.writeValueAsString(Map.of(
                    "model", modele,
                    "max_tokens", maxTokens,
                    "system", "Tu es l'assistant analytique de Rhode Bazar, une boutique de decoration. "
                            + "Redige un resume court (4 a 6 phrases), factuel et actionnable en francais, "
                            + "a partir des chiffres agreges fournis. Ne redige jamais en anglais. "
                            + "N'invente aucune donnee absente du message.",
                    "messages", List.of(Map.of("role", "user", "content", prompt))));

            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(URL_ANTHROPIC))
                    .timeout(Duration.ofSeconds(30))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", VERSION_API)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpsRequete))
                    .build();

            HttpResponse<String> reponse = httpClient.send(requete, HttpResponse.BodyHandlers.ofString());

            if (reponse.statusCode() != 200) {
                log.warn("Appel a l'API Anthropic echoue (statut {}) : {}", reponse.statusCode(), reponse.body());
                return ResumeIaDto.indisponible("Le service IA est momentanement indisponible (statut " + reponse.statusCode() + ").");
            }

            AnthropicResponse corps = objectMapper.readValue(reponse.body(), AnthropicResponse.class);
            String texte = corps.content().stream()
                    .filter(bloc -> "text".equals(bloc.type()))
                    .map(BlocContenu::text)
                    .findFirst()
                    .orElse(null);

            if (texte == null || texte.isBlank()) {
                return ResumeIaDto.indisponible("Le service IA n'a pas retourne de resume exploitable.");
            }
            return ResumeIaDto.de(texte.trim());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Impossible de contacter l'API Anthropic pour le resume IA", e);
            return ResumeIaDto.indisponible("Impossible de contacter le service IA (verifiez la connexion internet).");
        }
    }

    private String construirePrompt(TableauDeBordDto tableau, PrevisionVentesDto prevision) {
        StringBuilder texte = new StringBuilder();
        texte.append("Periode : du ").append(tableau.debut()).append(" au ").append(tableau.fin()).append(".\n");
        texte.append("Chiffre d'affaires : ").append(tableau.chiffreAffaires()).append(" FCFA.\n");
        texte.append("Marge brute : ").append(tableau.margeBrute()).append(" FCFA.\n");
        texte.append("Depenses totales : ").append(tableau.depensesTotales()).append(" FCFA.\n");
        texte.append("Resultat net : ").append(tableau.beneficeNet()).append(" FCFA.\n");

        if (!tableau.depensesParCategorie().isEmpty()) {
            texte.append("Depenses par categorie : ");
            for (DepenseCategorieDto d : tableau.depensesParCategorie()) {
                texte.append(d.categorie()).append("=").append(d.total()).append(" FCFA; ");
            }
            texte.append("\n");
        }

        if (!tableau.produitsPlusVendus().isEmpty()) {
            texte.append("Produits les plus vendus : ");
            for (ProduitVenteDto p : tableau.produitsPlusVendus()) {
                texte.append(p.produitNom()).append(" (").append(p.quantiteVendue()).append(" unites); ");
            }
            texte.append("\n");
        }

        if (prevision != null) {
            texte.append("Tendance des ventes sur les 30 derniers jours : ").append(prevision.tendance())
                    .append(" (moyenne journaliere ").append(prevision.moyenneJournaliere())
                    .append(" FCFA, prevision 7 prochains jours ").append(prevision.previsionProchains7Jours())
                    .append(" FCFA).\n");
        }

        texte.append("Redige un resume utile pour le gerant de la boutique.");
        return texte.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<BlocContenu> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BlocContenu(String type, String text) {
    }
}
