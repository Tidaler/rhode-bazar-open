package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.SceneManager;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField champEmail;

    @FXML
    private PasswordField champMotDePasse;

    @FXML
    private Button boutonConnexion;

    @FXML
    private Label labelErreur;

    @FXML
    private Button boutonFr;

    @FXML
    private Button boutonEn;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        mettreAJourBoutonsLangue();
    }

    @FXML
    private void seConnecter() {
        masquerErreur();
        boutonConnexion.setDisable(true);

        String email = champEmail.getText();
        String motDePasse = champMotDePasse.getText();

        new Thread(() -> {
            try {
                var resultat = apiClient.login(email, motDePasse);
                javafx.application.Platform.runLater(() -> {
                    Session.demarrer(resultat);
                    SceneManager.afficherEcranTableauDeBord();
                });
            } catch (ApiException e) {
                javafx.application.Platform.runLater(() -> {
                    boutonConnexion.setDisable(false);
                    afficherErreur(traduireErreur(e.getMessage()));
                });
            }
        }, "connexion-rhode-bazar").start();
    }

    @FXML
    private void choisirFrancais() {
        MessagesService.setLocale(Locale.FRENCH);
        SceneManager.afficherEcranConnexion();
    }

    @FXML
    private void choisirAnglais() {
        MessagesService.setLocale(Locale.ENGLISH);
        SceneManager.afficherEcranConnexion();
    }

    private void mettreAJourBoutonsLangue() {
        boolean francais = MessagesService.getLocale().equals(Locale.FRENCH);
        boutonFr.getStyleClass().removeAll("bouton-lien-actif");
        boutonEn.getStyleClass().removeAll("bouton-lien-actif");
        (francais ? boutonFr : boutonEn).getStyleClass().add("bouton-lien-actif");
    }

    private String traduireErreur(String code) {
        var messages = MessagesService.bundle();
        return "SERVEUR_INACCESSIBLE".equals(code)
                ? messages.getString("login.error.serveur")
                : messages.getString("login.error.invalid");
    }

    private void afficherErreur(String texte) {
        labelErreur.setText(texte);
        labelErreur.setVisible(true);
        labelErreur.setManaged(true);
    }

    private void masquerErreur() {
        labelErreur.setVisible(false);
        labelErreur.setManaged(false);
    }
}
