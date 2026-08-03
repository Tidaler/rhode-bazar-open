package com.rhodebazar.frontend;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifie que chaque ecran FXML se charge sans erreur (fx:id coherents avec le
 * controleur, cles i18n presentes), sans avoir a piloter une vraie fenetre.
 */
class FxmlLoadingTest {

    @BeforeAll
    static void demarrerLeToolkitJavaFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException dejaDemarre) {
            latch.countDown();
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Le toolkit JavaFX n'a pas demarre a temps");
        }
    }

    @Test
    void ecranConnexionSeCharge() throws Exception {
        chargerSurThreadFx("/fxml/login.fxml", Locale.FRENCH);
    }

    @Test
    void ecranTableauDeBordSeCharge() throws Exception {
        chargerSurThreadFx("/fxml/dashboard.fxml", Locale.FRENCH);
    }

    @Test
    void ecranStockSeCharge() throws Exception {
        chargerSurThreadFx("/fxml/stock.fxml", Locale.FRENCH);
    }

    @Test
    void ecranVentesSeCharge() throws Exception {
        chargerSurThreadFx("/fxml/ventes.fxml", Locale.FRENCH);
    }

    @Test
    void ecransSeChargentEnAnglais() throws Exception {
        chargerSurThreadFx("/fxml/login.fxml", Locale.ENGLISH);
        chargerSurThreadFx("/fxml/stock.fxml", Locale.ENGLISH);
        chargerSurThreadFx("/fxml/ventes.fxml", Locale.ENGLISH);
    }

    private void chargerSurThreadFx(String cheminFxml, Locale locale) throws Exception {
        AtomicReference<Exception> erreur = new AtomicReference<>();
        AtomicReference<Parent> resultat = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(cheminFxml), bundle);
                resultat.set(loader.load());
            } catch (Exception e) {
                erreur.set(e);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Chargement de " + cheminFxml + " trop long");
        }
        if (erreur.get() != null) {
            throw erreur.get();
        }
        assertNotNull(resultat.get(), cheminFxml + " n'a pas produit de racine");
    }
}
