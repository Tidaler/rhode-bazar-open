package com.rhodebazar.backend.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByActifTrue();
}
