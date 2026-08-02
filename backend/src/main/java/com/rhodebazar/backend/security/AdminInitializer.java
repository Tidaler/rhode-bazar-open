package com.rhodebazar.backend.security;

import com.rhodebazar.backend.domain.utilisateur.Role;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cree le compte administrateur initial au premier demarrage si aucun utilisateur n'existe.
 * Le mot de passe peut etre impose via RHODEBAZAR_ADMIN_PASSWORD ; a defaut, un mot de passe
 * aleatoire est genere et affiche une seule fois dans les logs (a changer immediatement).
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPasswordConfigure;

    public AdminInitializer(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            @Value("${rhodebazar.admin.email:admin@rhodebazar.local}") String adminEmail,
            @Value("${RHODEBAZAR_ADMIN_PASSWORD:}") String adminPasswordConfigure) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPasswordConfigure = adminPasswordConfigure;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurRepository.count() > 0) {
            return;
        }

        String motDePasse = adminPasswordConfigure.isBlank() ? genererMotDePasseAleatoire() : adminPasswordConfigure;

        Utilisateur admin = new Utilisateur();
        admin.setNom("Administrateur");
        admin.setEmail(adminEmail);
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));
        admin.setRole(Role.ADMIN);
        utilisateurRepository.save(admin);

        if (adminPasswordConfigure.isBlank()) {
            log.warn("=================================================================");
            log.warn(" Compte administrateur initial cree : {}", adminEmail);
            log.warn(" Mot de passe genere (a noter et a changer immediatement) : {}", motDePasse);
            log.warn("=================================================================");
        } else {
            log.info("Compte administrateur initial cree : {}", adminEmail);
        }
    }

    private String genererMotDePasseAleatoire() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }
}
