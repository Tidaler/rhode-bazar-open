package com.rhodebazar.frontend;

/**
 * Point d'entree separe de MainApp (qui etend javafx.application.Application). Lancer
 * directement une classe Application comme Main-Class d'un jar "fat" (via jpackage/java -jar)
 * declenche l'erreur "JavaFX runtime components are missing", meme si tous les modules
 * JavaFX sont bien presents dans le jar - c'est une verification stricte de la JVM
 * independante de la presence reelle des classes. Cette classe intermediaire contourne
 * cette verification ; c'est le correctif standard documente par le projet OpenJFX.
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
