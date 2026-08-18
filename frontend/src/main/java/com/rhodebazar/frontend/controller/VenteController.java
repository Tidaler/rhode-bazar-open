package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.DepotDto;
import com.rhodebazar.frontend.client.LigneVenteRequest;
import com.rhodebazar.frontend.client.ModePaiement;
import com.rhodebazar.frontend.client.ProduitDto;
import com.rhodebazar.frontend.client.VenteDto;
import com.rhodebazar.frontend.client.VenteRequest;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class VenteController {

    @FXML
    private ComboBox<DepotDto> champDepot;

    @FXML
    private ComboBox<ModePaiement> champModePaiement;

    @FXML
    private TextField champClient;

    @FXML
    private TextField champScanner;

    @FXML
    private ComboBox<ProduitDto> champProduit;

    @FXML
    private Spinner<Integer> champQuantite;

    @FXML
    private TableView<LignePanier> tablePanier;

    @FXML
    private TableColumn<LignePanier, String> colonnePanierProduit;

    @FXML
    private TableColumn<LignePanier, String> colonnePanierQuantite;

    @FXML
    private TableColumn<LignePanier, String> colonnePanierPrix;

    @FXML
    private TableColumn<LignePanier, String> colonnePanierMontant;

    @FXML
    private Label labelTotal;

    @FXML
    private TableView<VenteDto> tableHistorique;

    @FXML
    private TableColumn<VenteDto, String> colonneHistoriqueNumero;

    @FXML
    private TableColumn<VenteDto, String> colonneHistoriqueDate;

    @FXML
    private TableColumn<VenteDto, String> colonneHistoriqueClient;

    @FXML
    private TableColumn<VenteDto, String> colonneHistoriqueMode;

    @FXML
    private TableColumn<VenteDto, String> colonneHistoriqueMontant;

    @FXML
    private Button boutonTelechargerFacture;

    @FXML
    private BarreNavigationController barreNavigationController;

    private final ApiClient apiClient = new ApiClient();
    private final ObservableList<LignePanier> panier = FXCollections.observableArrayList();

    private record LignePanier(ProduitDto produit, int quantite) {
        BigDecimal montant() {
            return produit.prixVente().multiply(BigDecimal.valueOf(quantite));
        }
    }

    @FXML
    private void initialize() {
        ResourceBundle messages = MessagesService.bundle();

        champQuantite.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100_000, 1));

        champModePaiement.setItems(FXCollections.observableArrayList(ModePaiement.values()));
        champModePaiement.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModePaiement mode) {
                return mode == null ? "" : messages.getString("vente.mode." + mode.name());
            }

            @Override
            public ModePaiement fromString(String texte) {
                return null;
            }
        });
        champModePaiement.getSelectionModel().selectFirst();

        champProduit.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProduitDto produit) {
                return produit == null ? "" : produit.nom();
            }

            @Override
            public ProduitDto fromString(String texte) {
                return null;
            }
        });

        colonnePanierProduit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().produit().nom()));
        colonnePanierQuantite.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantite())));
        colonnePanierPrix.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().produit().prixVente().toPlainString()));
        colonnePanierMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montant().toPlainString()));
        tablePanier.setItems(panier);
        panier.addListener((javafx.collections.ListChangeListener<LignePanier>) c -> mettreAJourTotal());

        colonneHistoriqueNumero.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().numero()));
        colonneHistoriqueDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().dateVente()));
        colonneHistoriqueClient.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().clientNom() != null ? d.getValue().clientNom() : ""));
        colonneHistoriqueMode.setCellValueFactory(d -> new SimpleStringProperty(
                messages.getString("vente.mode." + d.getValue().modePaiement().name())));
        colonneHistoriqueMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montantTotal().toPlainString()));
        tableHistorique.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> boutonTelechargerFacture.setDisable(nouveau == null));

        mettreAJourTotal();
        if (barreNavigationController != null) {
            barreNavigationController.marquerActif("VENTES");
        }
        chargerDonnees();
    }

    private void chargerDonnees() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        new Thread(() -> {
            try {
                List<DepotDto> depots = apiClient.listerDepots(token);
                List<ProduitDto> produits = apiClient.listerProduits(token);
                List<VenteDto> ventes = apiClient.listerVentes(token);

                Platform.runLater(() -> {
                    champDepot.setItems(FXCollections.observableArrayList(depots));
                    if (!depots.isEmpty()) {
                        champDepot.getSelectionModel().selectFirst();
                    }
                    champProduit.setItems(FXCollections.observableArrayList(produits));
                    tableHistorique.setItems(FXCollections.observableArrayList(ventes));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-ventes").start();
    }

    @FXML
    private void ajouterAuPanier() {
        var messages = MessagesService.bundle();
        ProduitDto produit = champProduit.getValue();
        Integer quantite = champQuantite.getValue();

        if (produit == null) {
            afficherErreur(messages.getString("vente.produitObligatoire"));
            return;
        }
        if (quantite == null || quantite <= 0) {
            afficherErreur(messages.getString("vente.quantiteInvalide"));
            return;
        }

        ajouterLigneAuPanier(produit, quantite);
    }

    @FXML
    private void scannerCodeBarres() {
        String code = champScanner.getText();
        if (code == null || code.isBlank() || !Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        new Thread(() -> {
            try {
                ProduitDto produit = apiClient.rechercherProduitParCodeBarres(code.trim(), token);
                Platform.runLater(() -> {
                    champScanner.clear();
                    ajouterLigneAuPanier(produit, 1);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "scan-code-barres-vente").start();
    }

    private void ajouterLigneAuPanier(ProduitDto produit, int quantite) {
        for (int i = 0; i < panier.size(); i++) {
            LignePanier ligne = panier.get(i);
            if (ligne.produit().id().equals(produit.id())) {
                panier.set(i, new LignePanier(produit, ligne.quantite() + quantite));
                return;
            }
        }
        panier.add(new LignePanier(produit, quantite));
    }

    @FXML
    private void retirerLigneSelectionnee() {
        LignePanier selection = tablePanier.getSelectionModel().getSelectedItem();
        if (selection != null) {
            panier.remove(selection);
        }
    }

    @FXML
    private void enregistrerVente() {
        var messages = MessagesService.bundle();
        if (panier.isEmpty()) {
            afficherErreur(messages.getString("vente.panierVide"));
            return;
        }
        DepotDto depot = champDepot.getValue();
        if (depot == null) {
            afficherErreur(messages.getString("mouvement.selectionObligatoire"));
            return;
        }

        List<LigneVenteRequest> lignes = panier.stream()
                .map(l -> new LigneVenteRequest(l.produit().id(), l.quantite()))
                .toList();
        VenteRequest requete = new VenteRequest(depot.id(), champModePaiement.getValue(), champClient.getText(), lignes);
        String token = Session.getUtilisateurConnecte().token();

        new Thread(() -> {
            try {
                VenteDto venteCreee = apiClient.creerVente(requete, token);
                ouvrirTicketPourImpression(venteCreee, token);
                Platform.runLater(() -> {
                    panier.clear();
                    champClient.clear();
                    afficherInfo(messages.getString("vente.succes"));
                    chargerDonnees();
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "enregistrement-vente").start();
    }

    private void ouvrirTicketPourImpression(VenteDto vente, String token) {
        try {
            byte[] pdf = apiClient.telechargerFacture(vente.id(), token);
            File fichierTemporaire = File.createTempFile("facture-" + vente.numero() + "-", ".pdf");
            fichierTemporaire.deleteOnExit();
            Files.write(fichierTemporaire.toPath(), pdf);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(fichierTemporaire);
            }
        } catch (Exception e) {
            Platform.runLater(() -> afficherErreur(MessagesService.bundle().getString("vente.facture.ouvertureEchouee")));
        }
    }

    @FXML
    private void telechargerFacture() {
        VenteDto vente = tableHistorique.getSelectionModel().getSelectedItem();
        if (vente == null) {
            return;
        }
        var messages = MessagesService.bundle();

        FileChooser selecteur = new FileChooser();
        selecteur.setTitle(messages.getString("vente.facture.titreDialogue"));
        selecteur.setInitialFileName("facture-" + vente.numero() + ".pdf");
        selecteur.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        Window fenetre = boutonTelechargerFacture.getScene().getWindow();
        java.io.File fichier = selecteur.showSaveDialog(fenetre);
        if (fichier == null) {
            return;
        }

        String token = Session.getUtilisateurConnecte().token();
        new Thread(() -> {
            try {
                byte[] pdf = apiClient.telechargerFacture(vente.id(), token);
                Files.write(fichier.toPath(), pdf);
                Platform.runLater(() -> afficherInfo(
                        MessagesService.bundle().getString("vente.facture.succes").replace("{0}", fichier.getName())));
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            } catch (IOException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "telechargement-facture").start();
    }

    private void mettreAJourTotal() {
        BigDecimal total = panier.stream().map(LignePanier::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        labelTotal.setText(MessagesService.bundle().getString("vente.total") + " " + total.toPlainString());
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
