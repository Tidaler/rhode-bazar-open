package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.stock.Categorie;
import com.rhodebazar.backend.domain.stock.CategorieRepository;
import com.rhodebazar.backend.domain.stock.Produit;
import com.rhodebazar.backend.domain.stock.ProduitRepository;
import com.rhodebazar.backend.domain.stock.StockProduitDepot;
import com.rhodebazar.backend.domain.stock.StockProduitDepotRepository;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
import com.rhodebazar.backend.web.dto.StockDepotDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final StockProduitDepotRepository stockProduitDepotRepository;

    public ProduitService(
            ProduitRepository produitRepository,
            CategorieRepository categorieRepository,
            StockProduitDepotRepository stockProduitDepotRepository) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.stockProduitDepotRepository = stockProduitDepotRepository;
    }

    @Transactional(readOnly = true)
    public List<ProduitDto> lister() {
        Map<Long, Integer> stockParProduit = calculerStockParProduit();
        return produitRepository.findAll().stream()
                .map(p -> versDto(p, stockParProduit.getOrDefault(p.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProduitDto trouverDto(Long id) {
        Produit produit = trouver(id);
        return versDto(produit, sommeStock(id));
    }

    @Transactional(readOnly = true)
    public List<StockDepotDto> stockParDepot(Long produitId) {
        trouver(produitId);
        return stockProduitDepotRepository.findByProduitId(produitId).stream()
                .map(s -> new StockDepotDto(s.getDepot().getId(), s.getDepot().getNom(), s.getQuantite()))
                .toList();
    }

    public ProduitDto creer(ProduitRequest requete) {
        Produit produit = new Produit();
        appliquer(produit, requete);
        produit = produitRepository.save(produit);
        return versDto(produit, 0);
    }

    public ProduitDto modifier(Long id, ProduitRequest requete) {
        Produit produit = trouver(id);
        appliquer(produit, requete);
        produit = produitRepository.save(produit);
        return versDto(produit, sommeStock(id));
    }

    public void desactiver(Long id) {
        Produit produit = trouver(id);
        produit.setActif(false);
        produitRepository.save(produit);
    }

    public void reactiver(Long id) {
        Produit produit = trouver(id);
        produit.setActif(true);
        produitRepository.save(produit);
    }

    private void appliquer(Produit produit, ProduitRequest requete) {
        produit.setNom(requete.nom());
        produit.setDescription(requete.description());
        produit.setPrixAchat(requete.prixAchat());
        produit.setPrixVente(requete.prixVente());
        produit.setStockMinimal(requete.stockMinimal());
        produit.setUnite(requete.unite());

        if (requete.categorieId() != null) {
            Categorie categorie = categorieRepository.findById(requete.categorieId())
                    .orElseThrow(() -> new RessourceIntrouvableException(
                            "Categorie introuvable : " + requete.categorieId()));
            produit.setCategorie(categorie);
        } else {
            produit.setCategorie(null);
        }
    }

    private Produit trouver(Long id) {
        return produitRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + id));
    }

    private int sommeStock(Long produitId) {
        return stockProduitDepotRepository.findByProduitId(produitId).stream()
                .mapToInt(StockProduitDepot::getQuantite)
                .sum();
    }

    private Map<Long, Integer> calculerStockParProduit() {
        Map<Long, Integer> resultat = new HashMap<>();
        for (StockProduitDepot s : stockProduitDepotRepository.findAll()) {
            resultat.merge(s.getId().getProduitId(), s.getQuantite(), Integer::sum);
        }
        return resultat;
    }

    private ProduitDto versDto(Produit p, int stockTotal) {
        Categorie categorie = p.getCategorie();
        return new ProduitDto(
                p.getId(),
                p.getNom(),
                p.getDescription(),
                categorie != null ? categorie.getId() : null,
                categorie != null ? categorie.getNom() : null,
                p.getPrixAchat(),
                p.getPrixVente(),
                p.getStockMinimal(),
                p.getUnite(),
                p.isActif(),
                stockTotal,
                stockTotal <= p.getStockMinimal());
    }
}
