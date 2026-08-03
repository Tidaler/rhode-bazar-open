package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.SceneManager;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.text.MessageFormat;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label labelBienvenue;

    @FXML
    private Label labelRole;

    @FXML
    private void initialize() {
        var utilisateur = Session.getUtilisateurConnecte();
        var messages = MessagesService.bundle();
        if (utilisateur != null) {
            labelBienvenue.setText(MessageFormat.format(messages.getString("dashboard.bienvenue"), utilisateur.nom()));
            labelRole.setText(MessageFormat.format(messages.getString("dashboard.role"), utilisateur.role()));
        }
    }

    @FXML
    private void seDeconnecter() {
        Session.terminer();
        SceneManager.afficherEcranConnexion();
    }

    @FXML
    private void ouvrirStock() {
        SceneManager.afficherEcranStock();
    }
}
