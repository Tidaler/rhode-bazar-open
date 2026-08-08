package com.rhodebazar.frontend.controller;

import com.rhodebazar.frontend.Session;
import com.rhodebazar.frontend.client.ApiClient;
import com.rhodebazar.frontend.client.ApiException;
import com.rhodebazar.frontend.client.CategorieDto;
import com.rhodebazar.frontend.client.CategorieRequest;
import com.rhodebazar.frontend.client.DepotDto;
import com.rhodebazar.frontend.client.DepotRequest;
import com.rhodebazar.frontend.client.MouvementRequest;
import com.rhodebazar.frontend.client.ProduitDto;
import com.rhodebazar.frontend.client.ProduitRequest;
import com.rhodebazar.frontend.client.TypeMouvement;
import com.rhodebazar.frontend.i18n.MessagesService;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StockController {

    @FXML
    private TableView<ProduitDto> tableProduits;

    @FXML
    private TableColumn<ProduitDto, String> colonneNom;

    @FXML
    private TableColumn<ProduitDto, String> colonneCategorie;

    @FXML
    private TableColumn<ProduitDto, String> colonnePrixVente;

    @FXML
    private TableColumn<ProduitDto, String> colonneStock;

    @FXML
    private TableColumn<ProduitDto, String> colonneSeuil;

    @FXML
    private Button boutonNouveauProduit;

    @FXML
    private Button boutonModifier;

    @FXML
    private Button boutonMouvement;

    @FXML
    private Label labelAlertes;

    @FXML
    private TextField champScanner;

    @FXML
    private Button boutonNouvelleCategorie;

    @FXML
    private Button boutonNouveauDepot;

    @FXML
    private BarreNavigationController barreNavigationController;

    private final ApiClient apiClient = new ApiClient();
    private final ObservableList<ProduitDto> produits = FXCollections.observableArrayList();
    private List<CategorieDto> categories = List.of();
    private List<DepotDto> depots = List.of();
    private boolean estAdmin;

    @FXML
    private void initialize() {
        colonneNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colonneCategorie.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().categorieNom() != null ? d.getValue().categorieNom() : ""));
        colonnePrixVente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().prixVente().toPlainString()));
        colonneStock.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().stockTotal())));
        colonneSeuil.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().stockMinimal())));

        tableProduits.setItems(produits);
        tableProduits.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ProduitDto produit, boolean vide) {
                super.updateItem(produit, vide);
                getStyleClass().remove("ligne-alerte");
                if (!vide && produit != null && produit.stockCritique()) {
                    getStyleClass().add("ligne-alerte");
                }
            }
        });

        tableProduits.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            boolean selectionne = nouveau != null;
            boutonModifier.setDisable(!selectionne || !estAdmin);
            boutonMouvement.setDisable(!selectionne);
        });

        if (barreNavigationController != null) {
            barreNavigationController.marquerActif("STOCK");
        }

        var utilisateur = Session.getUtilisateurConnecte();
        estAdmin = utilisateur != null && "ADMIN".equals(utilisateur.role());
        boutonNouveauProduit.setVisible(estAdmin);
        boutonNouveauProduit.setManaged(estAdmin);
        boutonModifier.setVisible(estAdmin);
        boutonModifier.setManaged(estAdmin);
        boutonNouvelleCategorie.setVisible(estAdmin);
        boutonNouvelleCategorie.setManaged(estAdmin);
        boutonNouveauDepot.setVisible(estAdmin);
        boutonNouveauDepot.setManaged(estAdmin);

        actualiser();
    }

    @FXML
    private void actualiser() {
        if (!Session.estConnecte()) {
            return;
        }
        String token = Session.getUtilisateurConnecte().token();
        new Thread(() -> {
            try {
                List<ProduitDto> listeProduits = apiClient.listerProduits(token);
                List<CategorieDto> listeCategories = apiClient.listerCategories(token);
                List<DepotDto> listeDepots = apiClient.listerDepots(token);
                long nbAlertes = listeProduits.stream().filter(ProduitDto::stockCritique).count();

                Platform.runLater(() -> {
                    produits.setAll(listeProduits);
                    categories = listeCategories;
                    depots = listeDepots;
                    labelAlertes.setText(nbAlertes > 0 ? nbAlertes + " " + MessagesService.bundle().getString("stock.alertes") : "");
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "chargement-stock").start();
    }

    @FXML
    private void nouveauProduit() {
        ouvrirDialogueProduit(null).ifPresent(requete -> executerEtActualiser(
                () -> apiClient.creerProduit(requete, jeton())));
    }

    @FXML
    private void modifierProduit() {
        ProduitDto selection = tableProduits.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }
        ouvrirDialogueProduit(selection).ifPresent(requete -> executerEtActualiser(
                () -> apiClient.modifierProduit(selection.id(), requete, jeton())));
    }

    @FXML
    private void nouvelleCategorie() {
        ouvrirDialogueGestionCategories();
    }

    @FXML
    private void nouveauDepot() {
        ouvrirDialogueGestionDepots();
    }

    @FXML
    private void enregistrerMouvement() {
        ProduitDto selection = tableProduits.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }
        ouvrirDialogueMouvement(selection).ifPresent(requete -> executerEtActualiser(() -> {
            apiClient.enregistrerMouvement(requete, jeton());
            return null;
        }));
    }

    @FXML
    private void scannerCodeBarres() {
        String code = champScanner.getText();
        if (code == null || code.isBlank()) {
            return;
        }
        String token = jeton();
        new Thread(() -> {
            try {
                ProduitDto produit = apiClient.rechercherProduitParCodeBarres(code.trim(), token);
                Platform.runLater(() -> {
                    champScanner.clear();
                    produits.stream().filter(p -> p.id().equals(produit.id())).findFirst()
                            .ifPresent(p -> {
                                tableProduits.getSelectionModel().select(p);
                                tableProduits.scrollTo(p);
                            });
                    ouvrirDialogueMouvement(produit).ifPresent(requete -> executerEtActualiser(() -> {
                        apiClient.enregistrerMouvement(requete, jeton());
                        return null;
                    }));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "scan-code-barres").start();
    }

    private String jeton() {
        return Session.getUtilisateurConnecte().token();
    }

    private interface AppelApi<T> {
        T executer() throws ApiException;
    }

    private <T> void executerEtActualiser(AppelApi<T> appel) {
        executerAction(appel, this::actualiser);
    }

    private <T> void executerAction(AppelApi<T> appel, Runnable apresSucces) {
        new Thread(() -> {
            try {
                appel.executer();
                Platform.runLater(apresSucces);
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "action-stock").start();
    }

    private boolean confirmerSuppression(String message) {
        Alert alerte = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alerte.setTitle(MessagesService.bundle().getString("confirmation.titre"));
        alerte.setHeaderText(null);
        Optional<ButtonType> resultat = alerte.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.YES;
    }

    private void ouvrirDialogueGestionCategories() {
        var messages = MessagesService.bundle();
        Dialog<Void> dialogue = new Dialog<>();
        dialogue.setTitle(messages.getString("categorie.gerer.titre"));
        dialogue.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogue.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        TableView<CategorieDto> table = new TableView<>(FXCollections.observableArrayList(categories));
        TableColumn<CategorieDto, String> colonneNomTable = new TableColumn<>(messages.getString("stock.nom"));
        colonneNomTable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colonneNomTable.setPrefWidth(180);
        TableColumn<CategorieDto, String> colonneDescriptionTable = new TableColumn<>(messages.getString("produit.description"));
        colonneDescriptionTable.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().description() != null ? d.getValue().description() : ""));
        colonneDescriptionTable.setPrefWidth(220);
        table.getColumns().setAll(List.of(colonneNomTable, colonneDescriptionTable));
        table.setPrefHeight(240);

        Button boutonAjouter = new Button(messages.getString("categorie.gerer.ajouter"));
        Button boutonModifierCategorie = new Button(messages.getString("categorie.gerer.modifier"));
        Button boutonSupprimerCategorie = new Button(messages.getString("categorie.gerer.supprimer"));
        boutonAjouter.getStyleClass().add("bouton-lien");
        boutonModifierCategorie.getStyleClass().add("bouton-lien");
        boutonSupprimerCategorie.getStyleClass().add("bouton-lien");
        boutonModifierCategorie.setDisable(true);
        boutonSupprimerCategorie.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            boutonModifierCategorie.setDisable(nouveau == null);
            boutonSupprimerCategorie.setDisable(nouveau == null);
        });

        Runnable rafraichir = () -> new Thread(() -> {
            try {
                List<CategorieDto> fraiches = apiClient.listerCategories(jeton());
                Platform.runLater(() -> {
                    categories = fraiches;
                    table.setItems(FXCollections.observableArrayList(fraiches));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "rafraichissement-categories").start();

        boutonAjouter.setOnAction(e -> ouvrirDialogueCategorie(null).ifPresent(requete ->
                executerAction(() -> apiClient.creerCategorie(requete, jeton()), rafraichir)));
        boutonModifierCategorie.setOnAction(e -> {
            CategorieDto selection = table.getSelectionModel().getSelectedItem();
            if (selection != null) {
                ouvrirDialogueCategorie(selection).ifPresent(requete -> executerAction(
                        () -> apiClient.modifierCategorie(selection.id(), requete, jeton()), rafraichir));
            }
        });
        boutonSupprimerCategorie.setOnAction(e -> {
            CategorieDto selection = table.getSelectionModel().getSelectedItem();
            if (selection != null && confirmerSuppression(
                    MessageFormat.format(messages.getString("categorie.gerer.confirmationSuppression"), selection.nom()))) {
                executerAction(() -> {
                    apiClient.supprimerCategorie(selection.id(), jeton());
                    return null;
                }, rafraichir);
            }
        });

        VBox contenu = new VBox(10, table, new HBox(10, boutonAjouter, boutonModifierCategorie, boutonSupprimerCategorie));
        contenu.setPrefWidth(430);
        dialogue.getDialogPane().setContent(contenu);
        dialogue.showAndWait();
        actualiser();
    }

    private void ouvrirDialogueGestionDepots() {
        var messages = MessagesService.bundle();
        Dialog<Void> dialogue = new Dialog<>();
        dialogue.setTitle(messages.getString("depot.gerer.titre"));
        dialogue.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogue.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        TableView<DepotDto> table = new TableView<>(FXCollections.observableArrayList(depots));
        TableColumn<DepotDto, String> colonneNomTable = new TableColumn<>(messages.getString("stock.nom"));
        colonneNomTable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colonneNomTable.setPrefWidth(180);
        TableColumn<DepotDto, String> colonneAdresseTable = new TableColumn<>(messages.getString("depot.adresse"));
        colonneAdresseTable.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().adresse() != null ? d.getValue().adresse() : ""));
        colonneAdresseTable.setPrefWidth(220);
        table.getColumns().setAll(List.of(colonneNomTable, colonneAdresseTable));
        table.setPrefHeight(240);

        Button boutonAjouter = new Button(messages.getString("depot.gerer.ajouter"));
        Button boutonModifierDepot = new Button(messages.getString("depot.gerer.modifier"));
        Button boutonSupprimerDepot = new Button(messages.getString("depot.gerer.supprimer"));
        boutonAjouter.getStyleClass().add("bouton-lien");
        boutonModifierDepot.getStyleClass().add("bouton-lien");
        boutonSupprimerDepot.getStyleClass().add("bouton-lien");
        boutonModifierDepot.setDisable(true);
        boutonSupprimerDepot.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            boutonModifierDepot.setDisable(nouveau == null);
            boutonSupprimerDepot.setDisable(nouveau == null);
        });

        Runnable rafraichir = () -> new Thread(() -> {
            try {
                List<DepotDto> fraiches = apiClient.listerDepots(jeton());
                Platform.runLater(() -> {
                    depots = fraiches;
                    table.setItems(FXCollections.observableArrayList(fraiches));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> afficherErreur(e.getMessage()));
            }
        }, "rafraichissement-depots").start();

        boutonAjouter.setOnAction(e -> ouvrirDialogueDepot(null).ifPresent(requete ->
                executerAction(() -> apiClient.creerDepot(requete, jeton()), rafraichir)));
        boutonModifierDepot.setOnAction(e -> {
            DepotDto selection = table.getSelectionModel().getSelectedItem();
            if (selection != null) {
                ouvrirDialogueDepot(selection).ifPresent(requete -> executerAction(
                        () -> apiClient.modifierDepot(selection.id(), requete, jeton()), rafraichir));
            }
        });
        boutonSupprimerDepot.setOnAction(e -> {
            DepotDto selection = table.getSelectionModel().getSelectedItem();
            if (selection != null && confirmerSuppression(
                    MessageFormat.format(messages.getString("depot.gerer.confirmationSuppression"), selection.nom()))) {
                executerAction(() -> {
                    apiClient.supprimerDepot(selection.id(), jeton());
                    return null;
                }, rafraichir);
            }
        });

        VBox contenu = new VBox(10, table, new HBox(10, boutonAjouter, boutonModifierDepot, boutonSupprimerDepot));
        contenu.setPrefWidth(430);
        dialogue.getDialogPane().setContent(contenu);
        dialogue.showAndWait();
        actualiser();
    }

    private Optional<ProduitRequest> ouvrirDialogueProduit(ProduitDto existant) {
        var messages = MessagesService.bundle();
        Dialog<ProduitRequest> dialogue = new Dialog<>();
        dialogue.setTitle(existant == null ? messages.getString("produit.titreAjout") : messages.getString("produit.titreModification"));
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm());

        TextField champNom = new TextField(existant != null ? existant.nom() : "");
        TextArea champDescription = new TextArea(existant != null ? existant.description() : "");
        champDescription.setPrefRowCount(2);
        ComboBox<CategorieDto> champCategorie = new ComboBox<>(FXCollections.observableArrayList(categories));
        champCategorie.setPromptText(messages.getString("produit.aucuneCategorie"));
        if (existant != null && existant.categorieId() != null) {
            categories.stream().filter(c -> c.id().equals(existant.categorieId())).findFirst()
                    .ifPresent(champCategorie::setValue);
        }
        TextField champPrixAchat = new TextField(existant != null ? existant.prixAchat().toPlainString() : "0");
        TextField champPrixVente = new TextField(existant != null ? existant.prixVente().toPlainString() : "0");
        TextField champStockMinimal = new TextField(existant != null ? String.valueOf(existant.stockMinimal()) : "0");
        TextField champUnite = new TextField(existant != null ? existant.unite() : "unite");
        TextField champCodeBarres = new TextField(existant != null && existant.codeBarres() != null ? existant.codeBarres() : "");
        champCodeBarres.setPromptText(messages.getString("stock.scanner.prompt"));
        champCodeBarres.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, evenement -> {
            if (evenement.getCode() == javafx.scene.input.KeyCode.ENTER) {
                evenement.consume();
            }
        });

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        int ligne = 0;
        grille.addRow(ligne++, new Label(messages.getString("produit.nom")), champNom);
        grille.addRow(ligne++, new Label(messages.getString("produit.description")), champDescription);
        grille.addRow(ligne++, new Label(messages.getString("produit.categorie")), champCategorie);
        grille.addRow(ligne++, new Label(messages.getString("produit.prixAchat")), champPrixAchat);
        grille.addRow(ligne++, new Label(messages.getString("produit.prixVente")), champPrixVente);
        grille.addRow(ligne++, new Label(messages.getString("produit.stockMinimal")), champStockMinimal);
        grille.addRow(ligne++, new Label(messages.getString("produit.unite")), champUnite);
        grille.addRow(ligne, new Label(messages.getString("produit.codeBarres")), champCodeBarres);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                afficherErreur(messages.getString("produit.nomObligatoire"));
                evenement.consume();
            } else if (!estNombreValide(champPrixAchat.getText()) || !estNombreValide(champPrixVente.getText())) {
                afficherErreur(messages.getString("produit.prixInvalide"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> {
            if (bouton != ButtonType.OK) {
                return null;
            }
            String codeBarresSaisi = champCodeBarres.getText();
            return new ProduitRequest(
                    champNom.getText().trim(),
                    champDescription.getText(),
                    champCategorie.getValue() != null ? champCategorie.getValue().id() : null,
                    new BigDecimal(champPrixAchat.getText().trim()),
                    new BigDecimal(champPrixVente.getText().trim()),
                    parseEntierOuZero(champStockMinimal.getText()),
                    champUnite.getText() == null || champUnite.getText().isBlank() ? "unite" : champUnite.getText().trim(),
                    codeBarresSaisi == null || codeBarresSaisi.isBlank() ? null : codeBarresSaisi.trim());
        });

        return dialogue.showAndWait();
    }

    private Optional<MouvementRequest> ouvrirDialogueMouvement(ProduitDto produit) {
        var messages = MessagesService.bundle();
        Dialog<MouvementRequest> dialogue = new Dialog<>();
        dialogue.setTitle(messages.getString("mouvement.titre") + " - " + produit.nom());
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm());

        ComboBox<DepotDto> champDepot = new ComboBox<>(FXCollections.observableArrayList(depots));
        if (!depots.isEmpty()) {
            champDepot.getSelectionModel().selectFirst();
        }
        ComboBox<TypeMouvement> champType = new ComboBox<>(FXCollections.observableArrayList(TypeMouvement.values()));
        champType.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TypeMouvement type) {
                return type == null ? "" : messages.getString("mouvement.type." + type.name());
            }

            @Override
            public TypeMouvement fromString(String texte) {
                return null;
            }
        });
        champType.getSelectionModel().selectFirst();
        TextField champQuantite = new TextField("1");
        TextField champMotif = new TextField();

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label(messages.getString("mouvement.depot")), champDepot);
        grille.addRow(1, new Label(messages.getString("mouvement.type")), champType);
        grille.addRow(2, new Label(messages.getString("mouvement.quantite")), champQuantite);
        grille.addRow(3, new Label(messages.getString("mouvement.motif")), champMotif);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champDepot.getValue() == null) {
                afficherErreur(messages.getString("mouvement.selectionObligatoire"));
                evenement.consume();
            } else if (!estEntierPositif(champQuantite.getText())) {
                afficherErreur(messages.getString("mouvement.quantiteInvalide"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> {
            if (bouton != ButtonType.OK) {
                return null;
            }
            return new MouvementRequest(
                    produit.id(),
                    champDepot.getValue().id(),
                    champType.getValue(),
                    Integer.parseInt(champQuantite.getText().trim()),
                    champMotif.getText());
        });

        return dialogue.showAndWait();
    }

    private Optional<CategorieRequest> ouvrirDialogueCategorie(CategorieDto existant) {
        var messages = MessagesService.bundle();
        Dialog<CategorieRequest> dialogue = new Dialog<>();
        dialogue.setTitle(existant == null ? messages.getString("categorie.titreAjout") : messages.getString("categorie.titreModification"));
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm());

        TextField champNom = new TextField(existant != null ? existant.nom() : "");
        TextArea champDescription = new TextArea(existant != null && existant.description() != null ? existant.description() : "");
        champDescription.setPrefRowCount(2);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label(messages.getString("produit.nom")), champNom);
        grille.addRow(1, new Label(messages.getString("produit.description")), champDescription);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                afficherErreur(messages.getString("categorie.nomObligatoire"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> bouton != ButtonType.OK ? null
                : new CategorieRequest(champNom.getText().trim(), champDescription.getText()));

        return dialogue.showAndWait();
    }

    private Optional<DepotRequest> ouvrirDialogueDepot(DepotDto existant) {
        var messages = MessagesService.bundle();
        Dialog<DepotRequest> dialogue = new Dialog<>();
        dialogue.setTitle(existant == null ? messages.getString("depot.titreAjout") : messages.getString("depot.titreModification"));
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm());

        TextField champNom = new TextField(existant != null ? existant.nom() : "");
        TextField champAdresse = new TextField(existant != null && existant.adresse() != null ? existant.adresse() : "");

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label(messages.getString("produit.nom")), champNom);
        grille.addRow(1, new Label(messages.getString("depot.adresse")), champAdresse);
        dialogue.getDialogPane().setContent(grille);

        Button boutonOk = (Button) dialogue.getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.addEventFilter(javafx.event.ActionEvent.ACTION, evenement -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                afficherErreur(messages.getString("depot.nomObligatoire"));
                evenement.consume();
            }
        });

        dialogue.setResultConverter(bouton -> bouton != ButtonType.OK ? null
                : new DepotRequest(champNom.getText().trim(), champAdresse.getText()));

        return dialogue.showAndWait();
    }

    private boolean estNombreValide(String texte) {
        try {
            new BigDecimal(texte.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean estEntierPositif(String texte) {
        try {
            return Integer.parseInt(texte.trim()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int parseEntierOuZero(String texte) {
        try {
            return Integer.parseInt(texte.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void afficherErreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alerte.setTitle(MessagesService.bundle().getString("erreur.titre"));
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
