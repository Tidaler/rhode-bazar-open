package com.rhodebazar.backend.domain.stock;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class StockProduitDepotId implements Serializable {

    private Long produitId;
    private Long depotId;

    public StockProduitDepotId(Long produitId, Long depotId) {
        this.produitId = produitId;
        this.depotId = depotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockProduitDepotId that)) {
            return false;
        }
        return Objects.equals(produitId, that.produitId) && Objects.equals(depotId, that.depotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produitId, depotId);
    }
}
