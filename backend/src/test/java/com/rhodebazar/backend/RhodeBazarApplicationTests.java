package com.rhodebazar.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("sqlite")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rhodebazar-test.db?foreign_keys=on"
})
class RhodeBazarApplicationTests {

    @Test
    void contextCharge() {
        // Le contexte Spring doit demarrer, se connecter a la base SQLite de test
        // et appliquer les migrations Flyway sans erreur.
    }
}
