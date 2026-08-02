package com.rhodebazar.backend.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quantite en stock d'un produit dans un depot donne (support multi-depots). */
@Entity
@Table(name = "stock_produit_depot")
@Getter
@Setter
@NoArgsConstructor
public class StockProduitDepot {

    @EmbeddedId
    private StockProduitDepotId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("produitId")
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("depotId")
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Column(nullable = false)
    private int quantite = 0;

    public StockProduitDepot(Produit produit, Depot depot, int quantite) {
        this.produit = produit;
        this.depot = depot;
        this.quantite = quantite;
        this.id = new StockProduitDepotId(produit.getId(), depot.getId());
    }
}
