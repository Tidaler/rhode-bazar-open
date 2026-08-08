package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.Role;
import com.rhodebazar.frontend.client.SauvegardeDto;
import com.rhodebazar.frontend.client.UtilisateurDto;
import com.rhodebazar.frontend.client.UtilisateurModificationRequest;
import com.rhodebazar.frontend.client.UtilisateurRequest;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class AdministrationController {

    @FXML
    private TableView<UtilisateurDto> tableUtilisateurs;

    @FXML
    private TableColumn<UtilisateurDto, String> colonneUtilisateurNom;

    @FXML
    private TableColumn<UtilisateurDto, String> colonneUtilisateurEmail;

    @FXML
    private TableColumn<UtilisateurDto, String> colonneUtilisateurRole;

    @FXML
    private TableColumn<UtilisateurDto, String> colonneUtilisateurActif;

    @FXML
    private Button boutonModifierUtilisateur;

    @FXML
    private Button boutonActiverDesactiverUtilisateur;

    @FXML
    private TableView<SauvegardeDto> tableSauvegardes;

    @FXML
    private TableColumn<SauvegardeDto, String> colonneSauvegardeNom;

    @FXML
    private TableColumn<SauvegardeDto, String> colonneSauvegardeTaille;

    @FXML
    private TableColumn<SauvegardeDto, String> colonneSauvegardeDate;

    @FXML
    private BarreNavigationController barreNavigationController;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        ResourceBundle messages = MessagesService.bundle();

        colonneUtilisateurNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colonneUtilisateurEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email()));
        colonneUtilisateurRole.setCellValueFactory(d -> new SimpleStringProperty(
                messages.getString("utilisateurs.role." + d.getValue().role())));
        colonneUtilisateurActif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().actif() ? "✓" : "✗"));

        tableUtilisateurs.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            boutonModifierUtilisateur.setDisable(nouveau == null);
            boutonActiverDesactiverUtilisateur.setDisable(nouveau == null);
            if (nouveau != null) {
                boutonActiverDesactiverUtilisateur.setText(
                        nouveau.actif() ? messages.getString("utilisateurs.desactiver") : messages.getString("utilisateurs.activer"));
            }
        });

        colonneSauvegardeNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colonneSauvegardeTaille.setCellValueFactory(d -> new SimpleStringProperty(formaterTaille(d.getValue().tailleOctets())));
        colonneSauvegardeDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().dateCreation()));

        if (barreNavigationController != null) {
            barreNavigationController.marquerActif("ADMINISTRATION");
        }

        actualiserUtilisateurs();
        actualiserSauvegardes();
    }

    private void actualiserUtilisateurs() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = jeton();
        new Thread(() -> {
            try {
                List<UtilisateurDto> utilisateurs = apiClient.listerUtilisateurs(token);
                Platform.runLater(() -> tableUtilisateurs.setItems(FXCollections.observableArrayList(utilisateurs)));
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-utilisateurs").start();
    }

    private void actualiserSauvegardes() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = jeton();
        new Thread(() -> {
            try {
                List<SauvegardeDto> sauvegardes = apiClient.listerSauvegardes(token);
                Platform.runLater(() -> tableSauvegardes.setItems(FXCollections.observableArrayList(sauvegardes)));
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-sauvegardes").start();
    }

    @FXML
    private void nouvelUtilisateur() {
        ouvrirDialogueNouvelUtilisateur().ifPresent(requete -> executerAction(
                () -> apiClient.creerUtilisateur(requete, jeton()), this::actualiserUtilisateurs));
    }

    @FXML
    private void modifierUtilisateur() {
        UtilisateurDto selection = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }
        ouvrirDialogueModificationUtilisateur(selection).ifPresent(requete -> executerAction(
                () -> apiClient.modifierUtilisateur(selection.id(), requete, jeton()), this::actualiserUtilisateurs));
    }

    @FXML
    private void activerOuDesactiverUtilisateur() {
        UtilisateurDto selection = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }
        UtilisateurModificationRequest requete = new UtilisateurModificationRequest(
                selection.nom(), Role.valueOf(selection.role()), !selection.actif());
        executerAction(() -> apiClient.modifierUtilisateur(selection.id(), requete, jeton()), this::actualiserUtilisateurs);
    }

    @FXML
    private void declencherSauvegarde() {
        var messages = MessagesService.bundle();
        executerAction(() -> apiClient.declencherSauvegarde(jeton()), () -> {
            actualiserSauvegardes();
            afficherInfo(messages.getString("sauvegarde.succes"));
        });
    }

    private String jeton() {
        return Session.getUtilisateurConnecte().token();
    }

    private interface AppelApi<T> {
        T executer() throws ApiException;
    }

    private <T> void executerAction(AppelApi<T> appel, Runnable apresSucces) {
        new Thread(() -> {
            try {
                appel.executer();
                Platform.runLater(apresSucces);
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "action-administration").start();
    }

    private Optional<UtilisateurRequest> ouvrirDialogueNouvelUtilisateur() {
        var messages = MessagesService.bundle();
        Dialog<UtilisateurRequest> dialogue = new Dialog<>();
        dialogue.setTitle(messages.getString("utilisateurs.titreAjout"));
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        TextField champNom = new TextField();
        TextField champEmail = new TextField();
        PasswordField champMotDePasse = new PasswordField();
        ComboBox<Role> champRole = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        champRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : messages.getString("utilisateurs.role." + role.name());
            }

            @Override
            public Role fromString(String texte) {
                return null;
            }
        });
        champRole.getSelectionModel().select(Role.VENDEUR);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label(messages.getString("utilisateurs.nom")), champNom);
        grille.addRow(1, new Label(messages.getString("utilisateurs.email")), champEmail);
        grille.addRow(2, new Label(messages.getString("utilisateurs.motDePasse")), champMotDePasse);
        grille.addRow(3, new Label(messages.getString("utilisateurs.role")), champRole);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                afficherErreur(messages.getString("utilisateurs.nomObligatoire"));
                evenement.consume();
            } else if (champEmail.getText() == null || champEmail.getText().isBlank()) {
                afficherErreur(messages.getString("utilisateurs.emailObligatoire"));
                evenement.consume();
            } else if (champMotDePasse.getText() == null || champMotDePasse.getText().length() < 8) {
                afficherErreur(messages.getString("utilisateurs.motDePasseCourt"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> bouton != ButtonType.OK ? null
                : new UtilisateurRequest(champNom.getText().trim(), champEmail.getText().trim(),
                        champMotDePasse.getText(), champRole.getValue()));

        return dialogue.showAndWait();
    }

    private Optional<UtilisateurModificationRequest> ouvrirDialogueModificationUtilisateur(UtilisateurDto existant) {
        var messages = MessagesService.bundle();
        Dialog<UtilisateurModificationRequest> dialogue = new Dialog<>();
        dialogue.setTitle(messages.getString("utilisateurs.titreModification"));
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        TextField champNom = new TextField(existant.nom());
        ComboBox<Role> champRole = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        champRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : messages.getString("utilisateurs.role." + role.name());
            }

            @Override
            public Role fromString(String texte) {
                return null;
            }
        });
        champRole.getSelectionModel().select(Role.valueOf(existant.role()));
        CheckBox champActif = new CheckBox(messages.getString("utilisateurs.actif"));
        champActif.setSelected(existant.actif());

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label(messages.getString("utilisateurs.nom")), champNom);
        grille.addRow(1, new Label(messages.getString("utilisateurs.role")), champRole);
        grille.addRow(2, champActif);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                afficherErreur(messages.getString("utilisateurs.nomObligatoire"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> bouton != ButtonType.OK ? null
                : new UtilisateurModificationRequest(champNom.getText().trim(), champRole.getValue(), champActif.isSelected()));

        return dialogue.showAndWait();
    }

    private String formaterTaille(long octets) {
        if (octets < 1024) {
            return octets + " o";
        }
        double ko = octets / 1024.0;
        if (ko < 1024) {
            return String.format("%.1f Ko", ko);
        }
        return String.format("%.1f Mo", ko / 1024.0);
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
