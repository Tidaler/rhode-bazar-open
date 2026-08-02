package com.rhodebazar.backend.domain.vente;

import com.rhodebazar.backend.domain.stock.Depot;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ventes")
@Getter
@Setter
@NoArgsConstructor
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "date_vente", nullable = false)
    private LocalDateTime dateVente = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false, length = 20)
    private ModePaiement modePaiement;

    @Column(name = "client_nom", length = 150)
    private String clientNom;

    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneVente> lignes = new ArrayList<>();
}
