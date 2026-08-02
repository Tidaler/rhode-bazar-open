package com.rhodebazar.backend.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);
}
