package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.CategorieDepense;
import com.rhodebazar.frontend.client.DepenseDto;
import com.rhodebazar.frontend.client.DepenseRequest;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class DepenseController {

    @FXML
    private ComboBox<CategorieDepense> champCategorie;

    @FXML
    private TextField champLibelle;

    @FXML
    private TextField champMontant;

    @FXML
    private ComboBox<CategorieDepense> champFiltre;

    @FXML
    private TableView<DepenseDto> tableDepenses;

    @FXML
    private TableColumn<DepenseDto, String> colonneCategorie;

    @FXML
    private TableColumn<DepenseDto, String> colonneLibelle;

    @FXML
    private TableColumn<DepenseDto, String> colonneMontant;

    @FXML
    private TableColumn<DepenseDto, String> colonneDate;

    @FXML
    private TableColumn<DepenseDto, String> colonneUtilisateur;

    @FXML
    private BarreNavigationController barreNavigationController;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        ResourceBundle messages = MessagesService.bundle();
        StringConverter<CategorieDepense> convertisseur = new StringConverter<>() {
            @Override
            public String toString(CategorieDepense categorie) {
                return categorie == null ? "" : messages.getString("depense.categorie." + categorie.name());
            }

            @Override
            public CategorieDepense fromString(String texte) {
                return null;
            }
        };

        champCategorie.setItems(FXCollections.observableArrayList(CategorieDepense.values()));
        champCategorie.setConverter(convertisseur);
        champCategorie.getSelectionModel().selectFirst();

        List<CategorieDepense> optionsFiltre = new ArrayList<>();
        optionsFiltre.add(null);
        optionsFiltre.addAll(List.of(CategorieDepense.values()));
        champFiltre.setItems(FXCollections.observableArrayList(optionsFiltre));
        champFiltre.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategorieDepense categorie) {
                return categorie == null ? messages.getString("depense.toutes") : convertisseur.toString(categorie);
            }

            @Override
            public CategorieDepense fromString(String texte) {
                return null;
            }
        });
        champFiltre.getSelectionModel().selectFirst();
        champFiltre.valueProperty().addListener((obs, ancien, nouveau) -> chargerDepenses());

        colonneCategorie.setCellValueFactory(d -> new SimpleStringProperty(convertisseur.toString(d.getValue().categorie())));
        colonneLibelle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().libelle()));
        colonneMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montant().toPlainString()));
        colonneDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().dateDepense()));
        colonneUtilisateur.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().utilisateurNom() != null ? d.getValue().utilisateurNom() : ""));

        if (barreNavigationController != null) {
            barreNavigationController.marquerActif("DEPENSES");
        }

        chargerDepenses();
    }

    private void chargerDepenses() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        CategorieDepense filtre = champFiltre.getValue();

        new Thread(() -> {
            try {
                List<DepenseDto> depenses = apiClient.listerDepenses(token, filtre);
                Platform.runLater(() -> tableDepenses.setItems(FXCollections.observableArrayList(depenses)));
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-depenses").start();
    }

    @FXML
    private void enregistrerDepense() {
        var messages = MessagesService.bundle();
        String libelle = champLibelle.getText();
        if (libelle == null || libelle.isBlank()) {
            afficherErreur(messages.getString("depense.libelleObligatoire"));
            return;
        }
        BigDecimal montant;
        try {
            montant = new BigDecimal(champMontant.getText().trim());
            if (montant.signum() <= 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            afficherErreur(messages.getString("depense.montantInvalide"));
            return;
        }

        DepenseRequest requete = new DepenseRequest(champCategorie.getValue(), libelle.trim(), montant);
        String token = Session.getUtilisateurConnecte().token();

        new Thread(() -> {
            try {
                apiClient.creerDepense(requete, token);
                Platform.runLater(() -> {
                    champLibelle.clear();
                    champMontant.clear();
                    afficherInfo(messages.getString("depense.succes"));
                    chargerDepenses();
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "enregistrement-depense").start();
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
