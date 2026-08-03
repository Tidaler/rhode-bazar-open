package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.stock.Depot;
import com.rhodebazar.backend.domain.stock.DepotRepository;
import com.rhodebazar.backend.domain.stock.StockProduitDepotRepository;
import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.web.dto.DepotDto;
import com.rhodebazar.backend.web.dto.DepotRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepotService {

    private final DepotRepository depotRepository;
    private final StockProduitDepotRepository stockProduitDepotRepository;

    public DepotService(DepotRepository depotRepository, StockProduitDepotRepository stockProduitDepotRepository) {
        this.depotRepository = depotRepository;
        this.stockProduitDepotRepository = stockProduitDepotRepository;
    }

    @Transactional(readOnly = true)
    public List<DepotDto> lister() {
        return depotRepository.findAll().stream().map(this::versDto).toList();
    }

    public DepotDto creer(DepotRequest requete) {
        Depot depot = new Depot();
        depot.setNom(requete.nom());
        depot.setAdresse(requete.adresse());
        depot.setActif(true);
        return versDto(depotRepository.save(depot));
    }

    public DepotDto modifier(Long id, DepotRequest requete) {
        Depot depot = trouver(id);
        depot.setNom(requete.nom());
        depot.setAdresse(requete.adresse());
        return versDto(depotRepository.save(depot));
    }

    public void desactiver(Long id) {
        Depot depot = trouver(id);
        depot.setActif(false);
        depotRepository.save(depot);
    }

    public void supprimer(Long id) {
        Depot depot = trouver(id);
        if (stockProduitDepotRepository.existsByDepotId(id)) {
            throw new ConflitDonneesException("Impossible de supprimer : ce depot contient des mouvements/stock.");
        }
        depotRepository.delete(depot);
    }

    private Depot trouver(Long id) {
        return depotRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Depot introuvable : " + id));
    }

    private DepotDto versDto(Depot depot) {
        return new DepotDto(depot.getId(), depot.getNom(), depot.getAdresse(), depot.isActif());
    }
}
