package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.SceneManager;
import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.text.MessageFormat;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Barre de navigation incluse (fx:include) en haut de chaque ecran connecte,
 * pour permettre de passer d'un module a l'autre sans repasser par le tableau de bord.
 */
public class BarreNavigationController {

    @FXML
    private Button boutonTableauDeBord;

    @FXML
    private Button boutonStock;

    @FXML
    private Button boutonVentes;

    @FXML
    private Button boutonDepenses;

    @FXML
    private Button boutonReporting;

    @FXML
    private Button boutonAdministration;

    @FXML
    private Button boutonMonCompte;

    @FXML
    private Label labelUtilisateur;

    @FXML
    private void initialize() {
        var utilisateur = Session.getUtilisateurConnecte();
        if (utilisateur == null) {
            return;
        }
        labelUtilisateur.setText(MessageFormat.format(
                MessagesService.bundle().getString("nav.utilisateur"), utilisateur.nom(), utilisateur.role()));

        boolean estAdmin = "ADMIN".equals(utilisateur.role());
        boutonDepenses.setVisible(estAdmin);
        boutonDepenses.setManaged(estAdmin);
        boutonReporting.setVisible(estAdmin);
        boutonReporting.setManaged(estAdmin);
        boutonAdministration.setVisible(estAdmin);
        boutonAdministration.setManaged(estAdmin);
    }

    /** Appele par l'ecran hote apres inclusion, pour mettre en evidence l'onglet courant. */
    public void marquerActif(String cle) {
        Map<String, Button> boutons = Map.of(
                "DASHBOARD", boutonTableauDeBord,
                "STOCK", boutonStock,
                "VENTES", boutonVentes,
                "DEPENSES", boutonDepenses,
                "REPORTING", boutonReporting,
                "ADMINISTRATION", boutonAdministration,
                "MON_COMPTE", boutonMonCompte);
        boutons.values().forEach(b -> b.getStyleClass().remove("bouton-lien-actif"));
        Button actif = boutons.get(cle);
        if (actif != null) {
            actif.getStyleClass().add("bouton-lien-actif");
        }
    }

    @FXML
    private void ouvrirTableauDeBord() {
        SceneManager.afficherEcranTableauDeBord();
    }

    @FXML
    private void ouvrirStock() {
        SceneManager.afficherEcranStock();
    }

    @FXML
    private void ouvrirVentes() {
        SceneManager.afficherEcranVentes();
    }

    @FXML
    private void ouvrirDepenses() {
        SceneManager.afficherEcranDepenses();
    }

    @FXML
    private void ouvrirReporting() {
        SceneManager.afficherEcranReporting();
    }

    @FXML
    private void ouvrirAdministration() {
        SceneManager.afficherEcranAdministration();
    }

    @FXML
    private void ouvrirMonCompte() {
        SceneManager.afficherEcranMonCompte();
    }

    @FXML
    private void seDeconnecter() {
        Session.terminer();
        SceneManager.afficherEcranConnexion();
    }
}
