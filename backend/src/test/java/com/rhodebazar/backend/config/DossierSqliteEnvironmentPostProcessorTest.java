package com.rhodebazar.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

/**
 * Test unitaire direct (pas de contexte Spring) : reproduit le bug reel constate au
 * tout premier lancement de l'application sur une machine neuve, ou le pilote SQLite
 * ne cree pas les dossiers parents du fichier de base de donnees (contrairement au
 * fichier lui-meme), faisant echouer Flyway/HikariCP avec "does not exist".
 */
class DossierSqliteEnvironmentPostProcessorTest {

    private static final Path DOSSIER_TEST = Path.of("target/rhodebazar-dossier-inexistant");

    @BeforeEach
    @AfterEach
    void nettoyer() throws IOException {
        if (Files.exists(DOSSIER_TEST)) {
            try (var flux = Files.walk(DOSSIER_TEST)) {
                flux.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {
                        // best effort
                    }
                });
            }
        }
    }

    @Test
    void creeLesDossiersParentsManquantsPourUneUrlSqlite() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url",
                "jdbc:sqlite:target/rhodebazar-dossier-inexistant/imbrique/rhodebazar.db?foreign_keys=on");

        new DossierSqliteEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(DOSSIER_TEST.resolve("imbrique")).exists().isDirectory();
    }

    @Test
    void estEnregistreCommeEnvironmentPostProcessorSpringBoot() {
        // Verifie le mecanisme de declaration (META-INF/spring.factories) et pas
        // seulement la logique de la classe : c'est ce point precis qui a echoue
        // en silence lors d'un premier essai avec le mauvais fichier d'enregistrement
        // (.imports, reserve aux auto-configurations, au lieu de spring.factories).
        // loadFactoryNames (et non loadFactories) : on verifie la declaration sans
        // instancier les autres EnvironmentPostProcessor internes de Spring Boot,
        // dont certains exigent des arguments de construction non fournis ici.
        List<String> nomsClasses = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class, getClass().getClassLoader());
        assertThat(nomsClasses).contains(DossierSqliteEnvironmentPostProcessor.class.getName());
    }

    @Test
    void neFaitRienSiLUrlNestPasSqlite() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/rhodebazar");

        new DossierSqliteEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(DOSSIER_TEST).doesNotExist();
    }
}
