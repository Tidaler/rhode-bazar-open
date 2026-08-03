package com.rhodebazar.backend.domain.vente;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    List<Vente> findAllByOrderByDateVenteDesc();
}
