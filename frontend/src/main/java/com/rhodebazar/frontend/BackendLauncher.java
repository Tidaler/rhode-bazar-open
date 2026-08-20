package com.rhodebazar.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Demarre et arrete le backend Spring Boot en sous-processus lorsque l'application est
 * packagee (jpackage) : le jar backend est alors copie a cote du jar frontend dans le
 * dossier d'installation. En mode developpement (mvn javafx:run), ce jar n'existe pas a
 * cet emplacement : le lancement est silencieusement ignore et le backend doit etre
 * demarre separement (mvn spring-boot:run), comme aujourd'hui.
 */
public final class BackendLauncher {

    private static final String NOM_JAR_BACKEND = "rhode-bazar-backend.jar";
    private static final String URL_SANTE = "http://localhost:8085/actuator/health";
    private static final Duration DELAI_MAXIMAL = Duration.ofSeconds(30);

    private static Process processusBackend;

    private BackendLauncher() {
    }

    /**
     * Lance le backend si son jar est trouve a cote du jar frontend en cours d'execution,
     * puis attend (appel bloquant, hors thread JavaFX) que son endpoint de sante reponde.
     *
     * @return true si le backend est deja pret (ou n'a pas besoin d'etre lance), false si
     *         le delai d'attente est depasse (le lancement doit alors etre annule).
     */
    public static boolean demarrerEtAttendre() {
        File jarBackend = localiserJarBackend();
        if (jarBackend == null) {
            return true;
        }

        try {
            String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
            ProcessBuilder constructeur = new ProcessBuilder(javaExecutable, "-jar", jarBackend.getAbsolutePath());
            constructeur.directory(jarBackend.getParentFile());
            constructeur.redirectErrorStream(true);
            constructeur.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processusBackend = constructeur.start();
        } catch (IOException e) {
            return false;
        }

        return attendreDisponibilite();
    }

    /** Arrete proprement le sous-processus backend, s'il a ete lance. */
    public static void arreter() {
        if (processusBackend == null || !processusBackend.isAlive()) {
            return;
        }
        processusBackend.destroy();
        try {
            if (!processusBackend.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                processusBackend.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processusBackend.destroyForcibly();
        }
    }

    private static File localiserJarBackend() {
        try {
            File jarFrontend = new File(
                    BackendLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dossier = jarFrontend.isFile() ? jarFrontend.getParentFile() : null;
            if (dossier == null) {
                return null;
            }
            File jarBackend = new File(dossier, NOM_JAR_BACKEND);
            return jarBackend.isFile() ? jarBackend : null;
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }

    private static boolean attendreDisponibilite() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest requete = HttpRequest.newBuilder(URI.create(URL_SANTE))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        Instant limite = Instant.now().plus(DELAI_MAXIMAL);
        while (Instant.now().isBefore(limite)) {
            try {
                HttpResponse<Void> reponse = client.send(requete, HttpResponse.BodyHandlers.discarding());
                if (reponse.statusCode() == 200) {
                    return true;
                }
            } catch (IOException | InterruptedException ignore) {
                // Backend pas encore pret : on reessaie jusqu'au timeout.
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
