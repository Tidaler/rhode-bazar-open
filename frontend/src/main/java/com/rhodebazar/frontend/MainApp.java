package com.rhodebazar.frontend;

import javafx.application.Application;
import javafx.stage.Stage;
import javax.swing.JOptionPane;

public class MainApp extends Application {

    @Override
    public void start(Stage stagePrincipal) {
        SceneManager.initialiser(stagePrincipal);
        SceneManager.afficherEcranConnexion();
    }

    @Override
    public void stop() {
        BackendLauncher.arreter();
    }

    public static void main(String[] args) {
        // Demarre le backend embarque (uniquement en version packagee - voir BackendLauncher)
        // et attend qu'il reponde AVANT d'initialiser le toolkit JavaFX, pour ne jamais
        // afficher de fenetre tant que le backend n'est pas pret.
        if (!BackendLauncher.demarrerEtAttendre()) {
            JOptionPane.showMessageDialog(null,
                    "Le service ManagEx n'a pas demarre a temps. Verifiez qu'aucune autre "
                            + "instance n'est deja en cours d'execution, puis reessayez.",
                    "ManagEx - Erreur de demarrage",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        launch(args);
    }
}
