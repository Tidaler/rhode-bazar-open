package com.rhodebazar.frontend.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/** Gere la langue courante de l'interface (francais par defaut, bascule vers l'anglais). */
public final class MessagesService {

    private static Locale localeCourante = Locale.FRENCH;

    private MessagesService() {
    }

    public static Locale getLocale() {
        return localeCourante;
    }

    public static void setLocale(Locale locale) {
        localeCourante = locale;
    }

    public static ResourceBundle bundle() {
        return ResourceBundle.getBundle("i18n.messages", localeCourante);
    }
}
