package com.rhodebazar.frontend;

import com.rhodebazar.frontend.i18n.MessagesService;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Point central pour charger les ecrans FXML dans la fenetre principale, avec le bon theme et la bonne langue. */
public final class SceneManager {

    private static final String VERSION = chargerVersion();

    private static Stage stagePrincipal;

    private SceneManager() {
    }

    public static void initialiser(Stage stage) {
        stagePrincipal = stage;
    }

    public static void afficherEcranConnexion() {
        charger("/fxml/login.fxml", MessagesService.bundle().getString("login.title"));
    }

    public static void afficherEcranTableauDeBord() {
        charger("/fxml/dashboard.fxml", MessagesService.bundle().getString("dashboard.title"));
    }

    public static void afficherEcranStock() {
        charger("/fxml/stock.fxml", MessagesService.bundle().getString("stock.title"));
    }

    public static void afficherEcranVentes() {
        charger("/fxml/ventes.fxml", MessagesService.bundle().getString("vente.title"));
    }

    public static void afficherEcranDepenses() {
        charger("/fxml/depenses.fxml", MessagesService.bundle().getString("depense.title"));
    }

    public static void afficherEcranReporting() {
        charger("/fxml/reporting.fxml", MessagesService.bundle().getString("reporting.title"));
    }

    public static void afficherEcranAdministration() {
        charger("/fxml/administration.fxml", MessagesService.bundle().getString("administration.title"));
    }

    public static void afficherEcranMonCompte() {
        charger("/fxml/mon-compte.fxml", MessagesService.bundle().getString("compte.title"));
    }

    private static void charger(String cheminFxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(SceneManager.class.getResource(cheminFxml)),
                    MessagesService.bundle());
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(SceneManager.class.getResource("/css/theme.css")).toExternalForm());

            stagePrincipal.setTitle("ManagEx - " + titre + VERSION);
            stagePrincipal.setScene(scene);
            stagePrincipal.show();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'ecran : " + cheminFxml, e);
        }
    }

    private static String chargerVersion() {
        try (var flux = SceneManager.class.getResourceAsStream("/version.properties")) {
            if (flux == null) {
                return "";
            }
            Properties proprietes = new Properties();
            proprietes.load(flux);
            String version = proprietes.getProperty("version");
            return (version != null && !version.isBlank() && !version.startsWith("$"))
                    ? " (v" + version + ")" : "";
        } catch (IOException e) {
            return "";
        }
    }
}
