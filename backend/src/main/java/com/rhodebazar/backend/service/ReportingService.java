package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.depense.Depense;
import com.rhodebazar.backend.domain.depense.DepenseRepository;
import com.rhodebazar.backend.domain.vente.LigneVente;
import com.rhodebazar.backend.domain.vente.Vente;
import com.rhodebazar.backend.domain.vente.VenteRepository;
import com.rhodebazar.backend.web.dto.DepenseCategorieDto;
import com.rhodebazar.backend.web.dto.PointVenteJourDto;
import com.rhodebazar.backend.web.dto.ProduitVenteDto;
import com.rhodebazar.backend.web.dto.TableauDeBordDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final int TAILLE_TOP = 5;

    private final VenteRepository venteRepository;
    private final DepenseRepository depenseRepository;

    public ReportingService(VenteRepository venteRepository, DepenseRepository depenseRepository) {
        this.venteRepository = venteRepository;
        this.depenseRepository = depenseRepository;
    }

    public TableauDeBordDto calculerTableauDeBord(LocalDate debut, LocalDate fin) {
        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt = fin.plusDays(1).atStartOfDay();

        List<Vente> ventes = venteRepository.findByDateVenteBetweenOrderByDateVenteAsc(debutDt, finDt);
        List<Depense> depenses = depenseRepository.findByDateDepenseBetweenOrderByDateDepenseAsc(debutDt, finDt);

        BigDecimal chiffreAffaires = BigDecimal.ZERO;
        BigDecimal coutMarchandisesVendues = BigDecimal.ZERO;
        Map<Long, String> nomsProduits = new LinkedHashMap<>();
        Map<Long, Integer> quantitesParProduit = new LinkedHashMap<>();
        Map<Long, BigDecimal> montantsParProduit = new LinkedHashMap<>();
        TreeMap<LocalDate, BigDecimal> ventesParJour = new TreeMap<>();

        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            ventesParJour.put(jour, BigDecimal.ZERO);
        }

        for (Vente vente : ventes) {
            chiffreAffaires = chiffreAffaires.add(vente.getMontantTotal());
            LocalDate jourVente = vente.getDateVente().toLocalDate();
            ventesParJour.merge(jourVente, vente.getMontantTotal(), BigDecimal::add);

            for (LigneVente ligne : vente.getLignes()) {
                Long produitId = ligne.getProduit().getId();
                nomsProduits.putIfAbsent(produitId, ligne.getProduit().getNom());
                quantitesParProduit.merge(produitId, ligne.getQuantite(), Integer::sum);
                montantsParProduit.merge(produitId, ligne.getMontantLigne(), BigDecimal::add);
                coutMarchandisesVendues = coutMarchandisesVendues.add(
                        ligne.getProduit().getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite())));
            }
        }

        BigDecimal depensesTotales = depenses.stream().map(Depense::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margeBrute = chiffreAffaires.subtract(coutMarchandisesVendues);
        BigDecimal beneficeNet = margeBrute.subtract(depensesTotales);

        List<ProduitVenteDto> classement = quantitesParProduit.keySet().stream()
                .map(id -> new ProduitVenteDto(id, nomsProduits.get(id), quantitesParProduit.get(id), montantsParProduit.get(id)))
                .toList();

        List<ProduitVenteDto> plusVendus = classement.stream()
                .sorted(Comparator.comparingInt(ProduitVenteDto::quantiteVendue).reversed())
                .limit(TAILLE_TOP)
                .toList();
        List<ProduitVenteDto> moinsVendus = classement.stream()
                .sorted(Comparator.comparingInt(ProduitVenteDto::quantiteVendue))
                .limit(TAILLE_TOP)
                .toList();

        Map<com.rhodebazar.backend.domain.depense.CategorieDepense, BigDecimal> totauxParCategorie = new LinkedHashMap<>();
        for (Depense depense : depenses) {
            totauxParCategorie.merge(depense.getCategorie(), depense.getMontant(), BigDecimal::add);
        }
        List<DepenseCategorieDto> depensesParCategorie = totauxParCategorie.entrySet().stream()
                .map(e -> new DepenseCategorieDto(e.getKey(), e.getValue()))
                .toList();

        List<PointVenteJourDto> serieVentes = ventesParJour.entrySet().stream()
                .map(e -> new PointVenteJourDto(e.getKey(), e.getValue()))
                .toList();

        String resumeLocal = genererResumeLocal(debut, fin, chiffreAffaires, margeBrute, depensesTotales, beneficeNet, plusVendus);

        return new TableauDeBordDto(
                debut, fin, chiffreAffaires, coutMarchandisesVendues, margeBrute,
                depensesTotales, beneficeNet, plusVendus, moinsVendus, depensesParCategorie, serieVentes, resumeLocal);
    }

    private String genererResumeLocal(
            LocalDate debut, LocalDate fin, BigDecimal chiffreAffaires, BigDecimal margeBrute,
            BigDecimal depensesTotales, BigDecimal beneficeNet, List<ProduitVenteDto> plusVendus) {

        StringBuilder texte = new StringBuilder();
        texte.append("Du ").append(debut).append(" au ").append(fin).append(" : ")
                .append("chiffre d'affaires de ").append(formater(chiffreAffaires)).append(" FCFA, ")
                .append("marge brute de ").append(formater(margeBrute)).append(" FCFA, ")
                .append("depenses de ").append(formater(depensesTotales)).append(" FCFA, ")
                .append("soit un ").append(beneficeNet.signum() >= 0 ? "benefice net de " : "resultat negatif de ")
                .append(formater(beneficeNet.abs())).append(" FCFA.");

        if (!plusVendus.isEmpty()) {
            ProduitVenteDto meilleur = plusVendus.get(0);
            texte.append(" Produit le plus vendu : ").append(meilleur.produitNom())
                    .append(" (").append(meilleur.quantiteVendue()).append(" unites).");
        }
        return texte.toString();
    }

    private String formater(BigDecimal valeur) {
        return valeur.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
