package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import com.rhodebazar.backend.domain.depense.Depense;
import com.rhodebazar.backend.domain.depense.DepenseRepository;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.DepenseDto;
import com.rhodebazar.backend.web.dto.DepenseRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final AuditService auditService;

    public DepenseService(DepenseRepository depenseRepository, AuditService auditService) {
        this.depenseRepository = depenseRepository;
        this.auditService = auditService;
    }

    public DepenseDto creer(DepenseRequest requete, Utilisateur utilisateur) {
        Depense depense = new Depense();
        depense.setCategorie(requete.categorie());
        depense.setLibelle(requete.libelle());
        depense.setMontant(requete.montant());
        depense.setUtilisateur(utilisateur);
        depense = depenseRepository.save(depense);

        auditService.enregistrer(utilisateur, "DEPENSE_CREEE", "Depense", depense.getId(),
                "categorie=%s, montant=%s".formatted(requete.categorie(), requete.montant()));

        return versDto(depense);
    }

    @Transactional(readOnly = true)
    public List<DepenseDto> lister(CategorieDepense categorie) {
        List<Depense> depenses = categorie != null
                ? depenseRepository.findByCategorieOrderByDateDepenseDesc(categorie)
                : depenseRepository.findAllByOrderByDateDepenseDesc();
        return depenses.stream().map(this::versDto).toList();
    }

    public void supprimer(Long id) {
        Depense depense = depenseRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Depense introuvable : " + id));
        depenseRepository.delete(depense);
    }

    private DepenseDto versDto(Depense depense) {
        return new DepenseDto(
                depense.getId(),
                depense.getCategorie(),
                depense.getLibelle(),
                depense.getMontant(),
                depense.getDateDepense(),
                depense.getUtilisateur() != null ? depense.getUtilisateur().getNom() : null);
    }
}
