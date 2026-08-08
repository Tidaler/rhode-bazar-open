package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.ChangerMotDePasseRequest;
import com.rhodebazar.frontend.i18n.MessagesService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class MonCompteController {

    @FXML
    private Label labelNom;

    @FXML
    private Label labelEmail;

    @FXML
    private PasswordField champMotDePasseActuel;

    @FXML
    private PasswordField champNouveauMotDePasse;

    @FXML
    private PasswordField champConfirmationMotDePasse;

    @FXML
    private BarreNavigationController barreNavigationController;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        var utilisateur = Session.getUtilisateurConnecte();
        if (utilisateur != null) {
            labelNom.setText(utilisateur.nom());
            labelEmail.setText(utilisateur.email());
        }
        if (barreNavigationController != null) {
            barreNavigationController.marquerActif("MON_COMPTE");
        }
    }

    @FXML
    private void changerMotDePasse() {
        var messages = MessagesService.bundle();
        String actuel = champMotDePasseActuel.getText();
        String nouveau = champNouveauMotDePasse.getText();
        String confirmation = champConfirmationMotDePasse.getText();

        if (nouveau == null || nouveau.length() < 8) {
            afficherErreur(messages.getString("compte.motDePasseCourt"));
            return;
        }
        if (!nouveau.equals(confirmation)) {
            afficherErreur(messages.getString("compte.motDePasseDifferents"));
            return;
        }

        ChangerMotDePasseRequest requete = new ChangerMotDePasseRequest(actuel, nouveau);
        String token = Session.getUtilisateurConnecte().token();

        new Thread(() -> {
            try {
                apiClient.changerMonMotDePasse(requete, token);
                Platform.runLater(() -> {
                    champMotDePasseActuel.clear();
                    champNouveauMotDePasse.clear();
                    champConfirmationMotDePasse.clear();
                    afficherInfo(messages.getString("compte.succes"));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "changement-mot-de-passe").start();
    }

    private void afficherErreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alerte.setTitle(MessagesService.bundle().getString("erreur.titre"));
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }

    private void afficherInfo(String message) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
