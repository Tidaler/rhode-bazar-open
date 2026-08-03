package com.rhodebazar.backend.domain.depense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepenseRepository extends JpaRepository<Depense, Long> {

    List<Depense> findAllByOrderByDateDepenseDesc();

    List<Depense> findByCategorieOrderByDateDepenseDesc(CategorieDepense categorie);
}
