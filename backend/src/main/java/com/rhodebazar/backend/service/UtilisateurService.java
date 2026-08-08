package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.UtilisateurDto;
import com.rhodebazar.backend.web.dto.UtilisateurModificationRequest;
import com.rhodebazar.backend.web.dto.UtilisateurRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> lister() {
        return utilisateurRepository.findAll().stream().map(this::versDto).toList();
    }

    public UtilisateurDto creer(UtilisateurRequest requete, Utilisateur admin) {
        if (utilisateurRepository.existsByEmailIgnoreCase(requete.email())) {
            throw new ConflitDonneesException("Un utilisateur avec cet e-mail existe deja : " + requete.email());
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(requete.nom());
        utilisateur.setEmail(requete.email());
        utilisateur.setMotDePasse(passwordEncoder.encode(requete.motDePasse()));
        utilisateur.setRole(requete.role());
        utilisateur.setActif(true);
        utilisateur = utilisateurRepository.save(utilisateur);
        auditService.enregistrer(admin, "UTILISATEUR_CREE", "Utilisateur", utilisateur.getId(),
                "email=%s, role=%s".formatted(utilisateur.getEmail(), utilisateur.getRole()));
        return versDto(utilisateur);
    }

    public UtilisateurDto modifier(Long id, UtilisateurModificationRequest requete, Utilisateur admin) {
        Utilisateur utilisateur = trouver(id);
        if (!requete.actif() && utilisateur.getId().equals(admin.getId())) {
            throw new ConflitDonneesException("Vous ne pouvez pas desactiver votre propre compte.");
        }
        utilisateur.setNom(requete.nom());
        utilisateur.setRole(requete.role());
        utilisateur.setActif(requete.actif());
        utilisateur = utilisateurRepository.save(utilisateur);
        auditService.enregistrer(admin, "UTILISATEUR_MODIFIE", "Utilisateur", utilisateur.getId(),
                "role=%s, actif=%s".formatted(utilisateur.getRole(), utilisateur.isActif()));
        return versDto(utilisateur);
    }

    private Utilisateur trouver(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + id));
    }

    private UtilisateurDto versDto(Utilisateur utilisateur) {
        return new UtilisateurDto(utilisateur.getId(), utilisateur.getNom(), utilisateur.getEmail(),
                utilisateur.getRole().name(), utilisateur.isActif());
    }
}
