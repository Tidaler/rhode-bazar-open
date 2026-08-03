package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import com.rhodebazar.backend.domain.depense.Depense;
import com.rhodebazar.backend.domain.depense.DepenseRepository;
import com.rhodebazar.backend.domain.stock.Produit;
import com.rhodebazar.backend.domain.stock.ProduitRepository;
import com.rhodebazar.backend.domain.stock.StockProduitDepot;
import com.rhodebazar.backend.domain.stock.StockProduitDepotRepository;
import com.rhodebazar.backend.domain.vente.LigneVente;
import com.rhodebazar.backend.domain.vente.Vente;
import com.rhodebazar.backend.domain.vente.VenteRepository;
import com.rhodebazar.backend.web.dto.AnomalieDto;
import com.rhodebazar.backend.web.dto.PrevisionVentesDto;
import com.rhodebazar.backend.web.dto.SuggestionReapprovisionnementDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Analyses "intelligentes" calculees localement (regression lineaire simple, vitesse de
 * vente, ecart-type) : aucune dependance externe, fonctionne meme hors ligne.
 */
@Service
@Transactional(readOnly = true)
public class AnalyseLocaleService {

    private static final int JOURS_HISTORIQUE_PREVISION = 30;
    private static final int JOURS_PREVISION = 7;
    private static final int JOURS_COUVERTURE_CIBLE = 14;
    private static final int SEUIL_JOURS_COUVERTURE_ALERTE = 7;

    private final VenteRepository venteRepository;
    private final DepenseRepository depenseRepository;
    private final ProduitRepository produitRepository;
    private final StockProduitDepotRepository stockProduitDepotRepository;

    public AnalyseLocaleService(
            VenteRepository venteRepository,
            DepenseRepository depenseRepository,
            ProduitRepository produitRepository,
            StockProduitDepotRepository stockProduitDepotRepository) {
        this.venteRepository = venteRepository;
        this.depenseRepository = depenseRepository;
        this.produitRepository = produitRepository;
        this.stockProduitDepotRepository = stockProduitDepotRepository;
    }

    public PrevisionVentesDto prevoirVentes() {
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(JOURS_HISTORIQUE_PREVISION - 1);

        List<Vente> ventes = venteRepository.findByDateVenteBetweenOrderByDateVenteAsc(
                debut.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        TreeMap<LocalDate, BigDecimal> totauxParJour = new TreeMap<>();
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            totauxParJour.put(jour, BigDecimal.ZERO);
        }
        for (Vente vente : ventes) {
            totauxParJour.merge(vente.getDateVente().toLocalDate(), vente.getMontantTotal(), BigDecimal::add);
        }

        List<Double> valeurs = totauxParJour.values().stream().map(BigDecimal::doubleValue).toList();
        int n = valeurs.size();
        double moyenne = valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double pente = 0;
        double ordonnee = moyenne;
        if (n >= 2) {
            double sommeX = 0;
            double sommeY = 0;
            double sommeXY = 0;
            double sommeX2 = 0;
            for (int i = 0; i < n; i++) {
                sommeX += i;
                sommeY += valeurs.get(i);
                sommeXY += i * valeurs.get(i);
                sommeX2 += i * (double) i;
            }
            double denominateur = n * sommeX2 - sommeX * sommeX;
            if (denominateur != 0) {
                pente = (n * sommeXY - sommeX * sommeY) / denominateur;
                ordonnee = (sommeY - pente * sommeX) / n;
            }
        }

        double previsionTotale = 0;
        for (int i = n; i < n + JOURS_PREVISION; i++) {
            previsionTotale += Math.max(0, pente * i + ordonnee);
        }

        double seuilTendance = Math.max(moyenne * 0.02, 1.0);
        String tendance = pente > seuilTendance ? "HAUSSE" : pente < -seuilTendance ? "BAISSE" : "STABLE";

        return new PrevisionVentesDto(
                JOURS_HISTORIQUE_PREVISION,
                arrondir(moyenne),
                arrondir(previsionTotale),
                tendance);
    }

    public List<SuggestionReapprovisionnementDto> suggererReapprovisionnement() {
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(JOURS_HISTORIQUE_PREVISION - 1);

        List<Vente> ventes = venteRepository.findByDateVenteBetweenOrderByDateVenteAsc(
                debut.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        Map<Long, Integer> quantitesVenduesParProduit = new HashMap<>();
        for (Vente vente : ventes) {
            for (LigneVente ligne : vente.getLignes()) {
                quantitesVenduesParProduit.merge(ligne.getProduit().getId(), ligne.getQuantite(), Integer::sum);
            }
        }

        Map<Long, Integer> stockParProduit = new HashMap<>();
        for (StockProduitDepot s : stockProduitDepotRepository.findAll()) {
            stockParProduit.merge(s.getId().getProduitId(), s.getQuantite(), Integer::sum);
        }

        List<SuggestionReapprovisionnementDto> suggestions = new ArrayList<>();
        for (Produit produit : produitRepository.findByActifTrue()) {
            int stockActuel = stockParProduit.getOrDefault(produit.getId(), 0);
            int quantiteVendue = quantitesVenduesParProduit.getOrDefault(produit.getId(), 0);
            double vitesseJournaliere = quantiteVendue / (double) JOURS_HISTORIQUE_PREVISION;

            boolean sousLeSeuilMinimal = stockActuel <= produit.getStockMinimal();
            Double joursDeCouverture = vitesseJournaliere > 0 ? stockActuel / vitesseJournaliere : null;
            boolean couvertureFaible = joursDeCouverture != null && joursDeCouverture < SEUIL_JOURS_COUVERTURE_ALERTE;

            if (!sousLeSeuilMinimal && !couvertureFaible) {
                continue;
            }

            int quantiteSuggeree;
            if (vitesseJournaliere > 0) {
                int objectif = (int) Math.ceil(vitesseJournaliere * JOURS_COUVERTURE_CIBLE);
                quantiteSuggeree = Math.max(1, objectif - stockActuel);
            } else {
                quantiteSuggeree = Math.max(1, (produit.getStockMinimal() * 2) - stockActuel);
            }

            suggestions.add(new SuggestionReapprovisionnementDto(
                    produit.getId(),
                    produit.getNom(),
                    stockActuel,
                    arrondir(vitesseJournaliere),
                    joursDeCouverture != null ? arrondir(joursDeCouverture) : null,
                    quantiteSuggeree));
        }

        suggestions.sort((a, b) -> {
            double couvA = a.joursDeCouverture() != null ? a.joursDeCouverture().doubleValue() : -1;
            double couvB = b.joursDeCouverture() != null ? b.joursDeCouverture().doubleValue() : -1;
            return Double.compare(couvA, couvB);
        });

        return suggestions;
    }

    public List<AnomalieDto> detecterAnomaliesDepenses() {
        LocalDateTime debut = LocalDate.now().minusDays(180).atStartOfDay();
        LocalDateTime fin = LocalDate.now().plusDays(1).atStartOfDay();
        List<Depense> depenses = depenseRepository.findByDateDepenseBetweenOrderByDateDepenseAsc(debut, fin);

        Map<CategorieDepense, List<Depense>> parCategorie = new HashMap<>();
        for (Depense depense : depenses) {
            parCategorie.computeIfAbsent(depense.getCategorie(), c -> new ArrayList<>()).add(depense);
        }

        List<AnomalieDto> anomalies = new ArrayList<>();
        for (var entree : parCategorie.entrySet()) {
            List<Depense> liste = entree.getValue();
            if (liste.size() < 4) {
                continue;
            }

            // Mediane + MAD (median absolute deviation) : plus robuste que moyenne/ecart-type,
            // qui sont eux-memes faussees par l'outlier qu'on cherche a detecter sur un petit
            // echantillon (l'ecart-type "absorbe" la depense anormale au lieu de la reveler).
            List<Double> montants = liste.stream().map(d -> d.getMontant().doubleValue()).sorted().toList();
            double mediane = mediane(montants);
            List<Double> deviations = montants.stream().map(m -> Math.abs(m - mediane)).sorted().toList();
            double mad = mediane(deviations);
            if (mad == 0) {
                continue;
            }
            double seuil = mediane + 3 * 1.4826 * mad;

            for (Depense depense : liste) {
                double montant = depense.getMontant().doubleValue();
                if (montant > seuil && montant > mediane * 1.5) {
                    anomalies.add(new AnomalieDto(
                            "DEPENSE",
                            depense.getId(),
                            "Depense \"%s\" (%s) nettement superieure a la mediane de la categorie".formatted(
                                    depense.getLibelle(), entree.getKey()),
                            depense.getMontant(),
                            arrondir(mediane),
                            depense.getDateDepense()));
                }
            }
        }
        return anomalies;
    }

    private double mediane(List<Double> valeursTriees) {
        int taille = valeursTriees.size();
        if (taille % 2 == 1) {
            return valeursTriees.get(taille / 2);
        }
        return (valeursTriees.get(taille / 2 - 1) + valeursTriees.get(taille / 2)) / 2.0;
    }

    private BigDecimal arrondir(double valeur) {
        return BigDecimal.valueOf(valeur).setScale(2, RoundingMode.HALF_UP);
    }
}
