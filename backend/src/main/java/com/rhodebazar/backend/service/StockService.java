package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.stock.Depot;
import com.rhodebazar.backend.domain.stock.DepotRepository;
import com.rhodebazar.backend.domain.stock.MouvementStock;
import com.rhodebazar.backend.domain.stock.MouvementStockRepository;
import com.rhodebazar.backend.domain.stock.Produit;
import com.rhodebazar.backend.domain.stock.ProduitRepository;
import com.rhodebazar.backend.domain.stock.StockProduitDepot;
import com.rhodebazar.backend.domain.stock.StockProduitDepotId;
import com.rhodebazar.backend.domain.stock.StockProduitDepotRepository;
import com.rhodebazar.backend.domain.stock.TypeMouvement;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.exception.StockInsuffisantException;
import com.rhodebazar.backend.web.dto.AlerteStockDto;
import com.rhodebazar.backend.web.dto.MouvementDto;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;
    private final StockProduitDepotRepository stockProduitDepotRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final AuditService auditService;

    public StockService(
            ProduitRepository produitRepository,
            DepotRepository depotRepository,
            StockProduitDepotRepository stockProduitDepotRepository,
            MouvementStockRepository mouvementStockRepository,
            AuditService auditService) {
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.stockProduitDepotRepository = stockProduitDepotRepository;
        this.mouvementStockRepository = mouvementStockRepository;
        this.auditService = auditService;
    }

    public MouvementDto enregistrerMouvement(MouvementRequest requete, Utilisateur utilisateur) {
        Produit produit = produitRepository.findById(requete.produitId())
                .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + requete.produitId()));
        Depot depot = depotRepository.findById(requete.depotId())
                .orElseThrow(() -> new RessourceIntrouvableException("Depot introuvable : " + requete.depotId()));

        StockProduitDepot stock = stockProduitDepotRepository
                .findById(new StockProduitDepotId(produit.getId(), depot.getId()))
                .orElseGet(() -> new StockProduitDepot(produit, depot, 0));

        int nouvelleQuantite = appliquerVariation(stock.getQuantite(), requete.typeMouvement(), requete.quantite());
        stock.setQuantite(nouvelleQuantite);
        stockProduitDepotRepository.save(stock);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setDepot(depot);
        mouvement.setTypeMouvement(requete.typeMouvement());
        mouvement.setQuantite(requete.quantite());
        mouvement.setMotif(requete.motif());
        mouvement.setUtilisateur(utilisateur);
        mouvement = mouvementStockRepository.save(mouvement);

        auditService.enregistrer(
                utilisateur,
                "MOUVEMENT_STOCK_" + requete.typeMouvement(),
                "Produit",
                produit.getId(),
                "Depot=%s, quantite=%d, nouveau stock=%d".formatted(depot.getNom(), requete.quantite(), nouvelleQuantite));

        return versDto(mouvement);
    }

    @Transactional(readOnly = true)
    public List<MouvementDto> historiqueProduit(Long produitId) {
        return mouvementStockRepository.findByProduitIdOrderByDateMouvementDesc(produitId).stream()
                .map(this::versDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlerteStockDto> alertes() {
        return stockProduitDepotRepository.findAll().stream()
                .filter(s -> s.getQuantite() <= s.getProduit().getStockMinimal())
                .map(s -> new AlerteStockDto(
                        s.getProduit().getId(),
                        s.getProduit().getNom(),
                        s.getDepot().getId(),
                        s.getDepot().getNom(),
                        s.getQuantite(),
                        s.getProduit().getStockMinimal()))
                .toList();
    }

    private int appliquerVariation(int quantiteActuelle, TypeMouvement type, int quantite) {
        return switch (type) {
            case ENTREE, RETOUR -> quantiteActuelle + quantite;
            case SORTIE, PERTE -> {
                int resultat = quantiteActuelle - quantite;
                if (resultat < 0) {
                    throw new StockInsuffisantException(
                            "Stock insuffisant : disponible=%d, demande=%d".formatted(quantiteActuelle, quantite));
                }
                yield resultat;
            }
        };
    }

    private MouvementDto versDto(MouvementStock m) {
        return new MouvementDto(
                m.getId(),
                m.getProduit().getId(),
                m.getProduit().getNom(),
                m.getDepot().getId(),
                m.getDepot().getNom(),
                m.getTypeMouvement(),
                m.getQuantite(),
                m.getMotif(),
                m.getUtilisateur() != null ? m.getUtilisateur().getNom() : null,
                m.getDateMouvement());
    }
}
