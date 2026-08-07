package com.rhodebazar.backend.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Cree le dossier parent du fichier SQLite avant que Flyway/HikariCP n'essaient d'y
 * ouvrir une connexion. Le pilote SQLite cree le fichier lui-meme s'il n'existe pas,
 * mais pas les dossiers parents : au tout premier lancement sur une machine neuve
 * (ex. ~/.rhodebazar inexistant), la connexion echouait sinon au demarrage.
 */
public class DossierSqliteEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PREFIXE_SQLITE = "jdbc:sqlite:";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith(PREFIXE_SQLITE)) {
            return;
        }

        String chemin = url.substring(PREFIXE_SQLITE.length());
        int indexParametres = chemin.indexOf('?');
        if (indexParametres >= 0) {
            chemin = chemin.substring(0, indexParametres);
        }

        Path dossierParent = Path.of(chemin).toAbsolutePath().getParent();
        if (dossierParent == null) {
            return;
        }

        try {
            Files.createDirectories(dossierParent);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de creer le dossier pour la base SQLite : " + dossierParent, e);
        }
    }
}
