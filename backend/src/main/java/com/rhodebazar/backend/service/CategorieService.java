package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.stock.Categorie;
import com.rhodebazar.backend.domain.stock.CategorieRepository;
import com.rhodebazar.backend.domain.stock.ProduitRepository;
import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.CategorieDto;
import com.rhodebazar.backend.web.dto.CategorieRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;

    public CategorieService(CategorieRepository categorieRepository, ProduitRepository produitRepository) {
        this.categorieRepository = categorieRepository;
        this.produitRepository = produitRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorieDto> lister() {
        return categorieRepository.findAll().stream().map(this::versDto).toList();
    }

    public CategorieDto creer(CategorieRequest requete) {
        if (categorieRepository.existsByNomIgnoreCase(requete.nom())) {
            throw new ConflitDonneesException("Une categorie avec ce nom existe deja : " + requete.nom());
        }
        Categorie categorie = new Categorie();
        categorie.setNom(requete.nom());
        categorie.setDescription(requete.description());
        return versDto(categorieRepository.save(categorie));
    }

    public CategorieDto modifier(Long id, CategorieRequest requete) {
        Categorie categorie = trouver(id);
        if (!categorie.getNom().equalsIgnoreCase(requete.nom())
                && categorieRepository.existsByNomIgnoreCase(requete.nom())) {
            throw new ConflitDonneesException("Une categorie avec ce nom existe deja : " + requete.nom());
        }
        categorie.setNom(requete.nom());
        categorie.setDescription(requete.description());
        return versDto(categorieRepository.save(categorie));
    }

    public void supprimer(Long id) {
        Categorie categorie = trouver(id);
        if (produitRepository.existsByCategorieId(id)) {
            throw new ConflitDonneesException("Impossible de supprimer : des produits sont rattaches a cette categorie.");
        }
        categorieRepository.delete(categorie);
    }

    private Categorie trouver(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Categorie introuvable : " + id));
    }

    private CategorieDto versDto(Categorie categorie) {
        return new CategorieDto(categorie.getId(), categorie.getNom(), categorie.getDescription());
    }
}
