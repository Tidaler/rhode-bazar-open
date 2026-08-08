package com.rhodebazar.backend.web;

import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.service.AuditService;
import com.rhodebazar.backend.web.dto.ChangerMotDePasseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compte")
public class CompteController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public CompteController(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PutMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(
            @Valid @RequestBody ChangerMotDePasseRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        Utilisateur utilisateur = principal.getUtilisateur();
        if (!passwordEncoder.matches(requete.motDePasseActuel(), utilisateur.getMotDePasse())) {
            throw new ConflitDonneesException("Mot de passe actuel incorrect.");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(requete.nouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
        auditService.enregistrer(utilisateur, "MOT_DE_PASSE_CHANGE", "Utilisateur", utilisateur.getId(), null);
    }
}
