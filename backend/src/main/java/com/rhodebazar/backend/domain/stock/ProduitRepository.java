package com.rhodebazar.backend.domain.stock;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByActifTrue();

    boolean existsByCategorieId(Long categorieId);

    Optional<Produit> findByCodeBarres(String codeBarres);

    boolean existsByCodeBarresAndIdNot(String codeBarres, Long id);

    boolean existsByCodeBarres(String codeBarres);
}
