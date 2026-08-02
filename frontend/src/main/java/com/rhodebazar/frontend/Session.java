package com.rhodebazar.frontend;

import com.rhodebazar.frontend.client.LoginResult;

/** Contexte de la session utilisateur courante (en memoire, cote client). */
public final class Session {

    private static LoginResult utilisateurConnecte;

    private Session() {
    }

    public static void demarrer(LoginResult resultat) {
        utilisateurConnecte = resultat;
    }

    public static void terminer() {
        utilisateurConnecte = null;
    }

    public static LoginResult getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public static boolean estConnecte() {
        return utilisateurConnecte != null;
    }
}
