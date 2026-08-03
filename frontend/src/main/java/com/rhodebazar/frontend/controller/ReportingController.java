package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.SceneManager;
import com.rhodebazar.frontend.client.AnomalieDto;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.DepenseCategorieDto;
import com.rhodebazar.frontend.client.PointVenteJourDto;
import com.rhodebazar.frontend.client.PrevisionVentesDto;
import com.rhodebazar.frontend.client.ProduitVenteDto;
import com.rhodebazar.frontend.client.ResumeIaDto;
import com.rhodebazar.frontend.client.SuggestionReapprovisionnementDto;
import com.rhodebazar.frontend.client.TableauDeBordDto;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ReportingController {

    @FXML
    private DatePicker champDebut;

    @FXML
    private DatePicker champFin;

    @FXML
    private Label labelCa;

    @FXML
    private Label labelMarge;

    @FXML
    private Label labelDepenses;

    @FXML
    private Label labelBenefice;

    @FXML
    private LineChart<String, Number> graphiqueVentes;

    @FXML
    private CategoryAxis axeDates;

    @FXML
    private NumberAxis axeMontants;

    @FXML
    private TableView<ProduitVenteDto> tablePlusVendus;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonnePlusVenduNom;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonnePlusVenduQuantite;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonnePlusVenduMontant;

    @FXML
    private TableView<ProduitVenteDto> tableMoinsVendus;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonneMoinsVenduNom;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonneMoinsVenduQuantite;
    @FXML
    private TableColumn<ProduitVenteDto, String> colonneMoinsVenduMontant;

    @FXML
    private TableView<DepenseCategorieDto> tableDepensesCategorie;
    @FXML
    private TableColumn<DepenseCategorieDto, String> colonneDepenseCategorie;
    @FXML
    private TableColumn<DepenseCategorieDto, String> colonneDepenseTotal;

    @FXML
    private Label labelMoyenneJournaliere;
    @FXML
    private Label labelPrevision7Jours;
    @FXML
    private Label labelTendance;

    @FXML
    private TableView<SuggestionReapprovisionnementDto> tableReappro;
    @FXML
    private TableColumn<SuggestionReapprovisionnementDto, String> colonneReapproProduit;
    @FXML
    private TableColumn<SuggestionReapprovisionnementDto, String> colonneReapproStock;
    @FXML
    private TableColumn<SuggestionReapprovisionnementDto, String> colonneReapproVitesse;
    @FXML
    private TableColumn<SuggestionReapprovisionnementDto, String> colonneReapproCouverture;
    @FXML
    private TableColumn<SuggestionReapprovisionnementDto, String> colonneReapproQuantite;

    @FXML
    private TableView<AnomalieDto> tableAnomalies;
    @FXML
    private TableColumn<AnomalieDto, String> colonneAnomalieType;
    @FXML
    private TableColumn<AnomalieDto, String> colonneAnomalieDescription;
    @FXML
    private TableColumn<AnomalieDto, String> colonneAnomalieValeur;
    @FXML
    private TableColumn<AnomalieDto, String> colonneAnomalieMoyenne;

    @FXML
    private Label labelResumeLocal;
    @FXML
    private Label labelResumeIaTitre;
    @FXML
    private Label labelResumeIa;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        champFin.setValue(LocalDate.now());
        champDebut.setValue(LocalDate.now().minusDays(29));

        colonnePlusVenduNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().produitNom()));
        colonnePlusVenduQuantite.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantiteVendue())));
        colonnePlusVenduMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montantVendu().toPlainString()));

        colonneMoinsVenduNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().produitNom()));
        colonneMoinsVenduQuantite.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantiteVendue())));
        colonneMoinsVenduMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montantVendu().toPlainString()));

        ResourceBundle messages = MessagesService.bundle();
        colonneDepenseCategorie.setCellValueFactory(d -> new SimpleStringProperty(
                messages.getString("depense.categorie." + d.getValue().categorie().name())));
        colonneDepenseTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().total().toPlainString()));

        colonneReapproProduit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().produitNom()));
        colonneReapproStock.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().stockActuel())));
        colonneReapproVitesse.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().venteJournaliereMoyenne().toPlainString()));
        colonneReapproCouverture.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().joursDeCouverture() != null ? d.getValue().joursDeCouverture().toPlainString() : "-"));
        colonneReapproQuantite.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantiteSuggeree())));

        colonneAnomalieType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type()));
        colonneAnomalieDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().description()));
        colonneAnomalieValeur.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().valeur().toPlainString()));
        colonneAnomalieMoyenne.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().valeurMoyenneReference().toPlainString()));

        labelResumeIa.setText("");

        actualiser();
    }

    @FXML
    private void retourTableauDeBord() {
        SceneManager.afficherEcranTableauDeBord();
    }

    @FXML
    private void actualiser() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        LocalDate debut = champDebut.getValue();
        LocalDate fin = champFin.getValue();

        new Thread(() -> {
            try {
                TableauDeBordDto tableau = apiClient.obtenirTableauDeBord(token, debut, fin);
                PrevisionVentesDto prevision = apiClient.obtenirPrevisions(token);
                List<SuggestionReapprovisionnementDto> reappro = apiClient.obtenirReapprovisionnement(token);
                List<AnomalieDto> anomalies = apiClient.obtenirAnomalies(token);

                Platform.runLater(() -> {
                    afficherTableau(tableau);
                    afficherPrevision(prevision);
                    tableReappro.setItems(FXCollections.observableArrayList(reappro));
                    tableAnomalies.setItems(FXCollections.observableArrayList(anomalies));
                    labelResumeIaTitre.setVisible(false);
                    labelResumeIaTitre.setManaged(false);
                    labelResumeIa.setText("");
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-reporting").start();
    }

    private void afficherTableau(TableauDeBordDto tableau) {
        labelCa.setText(tableau.chiffreAffaires().toPlainString());
        labelMarge.setText(tableau.margeBrute().toPlainString());
        labelDepenses.setText(tableau.depensesTotales().toPlainString());
        labelBenefice.setText(tableau.beneficeNet().toPlainString());

        tablePlusVendus.setItems(FXCollections.observableArrayList(tableau.produitsPlusVendus()));
        tableMoinsVendus.setItems(FXCollections.observableArrayList(tableau.produitsMoinsVendus()));
        tableDepensesCategorie.setItems(FXCollections.observableArrayList(tableau.depensesParCategorie()));

        graphiqueVentes.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (PointVenteJourDto point : tableau.ventesParJour()) {
            serie.getData().add(new XYChart.Data<>(point.date(), point.total()));
        }
        graphiqueVentes.getData().add(serie);

        labelResumeLocal.setText(tableau.resumeLocal());
    }

    private void afficherPrevision(PrevisionVentesDto prevision) {
        ResourceBundle messages = MessagesService.bundle();
        labelMoyenneJournaliere.setText(messages.getString("reporting.prevision.moyenneJournaliere") + " "
                + prevision.moyenneJournaliere().toPlainString());
        labelPrevision7Jours.setText(messages.getString("reporting.prevision.prevision7Jours") + " "
                + prevision.previsionProchains7Jours().toPlainString());
        labelTendance.setText(messages.getString("reporting.prevision.tendance") + " "
                + messages.getString("reporting.tendance." + prevision.tendance()));
    }

    @FXML
    private void genererResumeIa() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        LocalDate debut = champDebut.getValue();
        LocalDate fin = champFin.getValue();

        new Thread(() -> {
            try {
                ResumeIaDto resume = apiClient.obtenirResumeIa(token, debut, fin);
                Platform.runLater(() -> {
                    labelResumeIaTitre.setVisible(true);
                    labelResumeIaTitre.setManaged(true);
                    labelResumeIa.setText(resume.texte());
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "resume-ia").start();
    }

    private void afficherErreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alerte.setTitle(MessagesService.bundle().getString("erreur.titre"));
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
