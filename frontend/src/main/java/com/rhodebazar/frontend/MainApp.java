package com.rhodebazar.frontend;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stagePrincipal) {
        SceneManager.initialiser(stagePrincipal);
        SceneManager.afficherEcranConnexion();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
