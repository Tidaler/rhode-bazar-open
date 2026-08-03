package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.stock.Depot;
import com.rhodebazar.backend.domain.stock.DepotRepository;
import com.rhodebazar.backend.domain.stock.Produit;
import com.rhodebazar.backend.domain.stock.ProduitRepository;
import com.rhodebazar.backend.domain.stock.TypeMouvement;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import com.rhodebazar.backend.domain.vente.LigneVente;
import com.rhodebazar.backend.domain.vente.Vente;
import com.rhodebazar.backend.domain.vente.VenteRepository;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.LigneVenteDto;
import com.rhodebazar.backend.web.dto.LigneVenteRequest;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import com.rhodebazar.backend.web.dto.VenteDto;
import com.rhodebazar.backend.web.dto.VenteRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VenteService {

    private final VenteRepository venteRepository;
    private final ProduitRepository produitRepository;
    private final DepotRepository depotRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public VenteService(
            VenteRepository venteRepository,
            ProduitRepository produitRepository,
            DepotRepository depotRepository,
            StockService stockService,
            AuditService auditService) {
        this.venteRepository = venteRepository;
        this.produitRepository = produitRepository;
        this.depotRepository = depotRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    public VenteDto creerVente(VenteRequest requete, Utilisateur utilisateur) {
        Depot depot = depotRepository.findById(requete.depotId())
                .orElseThrow(() -> new RessourceIntrouvableException("Depot introuvable : " + requete.depotId()));

        String numero = genererNumero();

        Vente vente = new Vente();
        vente.setNumero(numero);
        vente.setUtilisateur(utilisateur);
        vente.setDepot(depot);
        vente.setModePaiement(requete.modePaiement());
        vente.setClientNom(requete.clientNom());

        BigDecimal montantTotal = BigDecimal.ZERO;
        for (LigneVenteRequest ligneRequete : requete.lignes()) {
            Produit produit = produitRepository.findById(ligneRequete.produitId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable : " + ligneRequete.produitId()));

            // Decremente le stock (leve StockInsuffisantException si indisponible, annulant toute la vente).
            stockService.enregistrerMouvement(
                    new MouvementRequest(produit.getId(), depot.getId(), TypeMouvement.SORTIE,
                            ligneRequete.quantite(), "Vente " + numero),
                    utilisateur);

            BigDecimal prixUnitaire = produit.getPrixVente();
            BigDecimal montantLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligneRequete.quantite()));
            montantTotal = montantTotal.add(montantLigne);

            LigneVente ligne = new LigneVente();
            ligne.setVente(vente);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneRequete.quantite());
            ligne.setPrixUnitaire(prixUnitaire);
            ligne.setMontantLigne(montantLigne);
            vente.getLignes().add(ligne);
        }
        vente.setMontantTotal(montantTotal);

        vente = venteRepository.save(vente);
        auditService.enregistrer(utilisateur, "VENTE_CREEE", "Vente", vente.getId(),
                "numero=%s, montant=%s".formatted(numero, montantTotal));

        return versDto(vente);
    }

    @Transactional(readOnly = true)
    public List<VenteDto> lister() {
        return venteRepository.findAllByOrderByDateVenteDesc().stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public VenteDto trouver(Long id) {
        return venteRepository.findById(id)
                .map(this::versDto)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente introuvable : " + id));
    }

    private String genererNumero() {
        return "V" + String.format("%06d", venteRepository.count() + 1);
    }

    private VenteDto versDto(Vente vente) {
        List<LigneVenteDto> lignes = vente.getLignes().stream()
                .map(l -> new LigneVenteDto(
                        l.getProduit().getId(),
                        l.getProduit().getNom(),
                        l.getQuantite(),
                        l.getPrixUnitaire(),
                        l.getMontantLigne()))
                .toList();

        return new VenteDto(
                vente.getId(),
                vente.getNumero(),
                vente.getDateVente(),
                vente.getUtilisateur() != null ? vente.getUtilisateur().getNom() : null,
                vente.getDepot().getId(),
                vente.getDepot().getNom(),
                vente.getModePaiement(),
                vente.getClientNom(),
                vente.getMontantTotal(),
                lignes);
    }
}
