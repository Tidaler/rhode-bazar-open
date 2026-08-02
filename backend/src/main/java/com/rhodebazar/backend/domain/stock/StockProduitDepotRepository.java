package com.rhodebazar.backend.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockProduitDepotRepository extends JpaRepository<StockProduitDepot, StockProduitDepotId> {

    List<StockProduitDepot> findByDepotId(Long depotId);

    List<StockProduitDepot> findByProduitId(Long produitId);

    List<StockProduitDepot> findByQuantiteLessThanEqual(int seuil);
}
