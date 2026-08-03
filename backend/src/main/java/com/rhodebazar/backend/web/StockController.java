package com.rhodebazar.backend.web;

import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.service.StockService;
import com.rhodebazar.backend.web.dto.AlerteStockDto;
import com.rhodebazar.backend.web.dto.MouvementDto;
import com.rhodebazar.backend.web.dto.MouvementRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasAnyRole('ADMIN','VENDEUR')")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/mouvements")
    @ResponseStatus(HttpStatus.CREATED)
    public MouvementDto enregistrerMouvement(
            @Valid @RequestBody MouvementRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return stockService.enregistrerMouvement(requete, principal.getUtilisateur());
    }

    @GetMapping("/mouvements/produit/{produitId}")
    public List<MouvementDto> historiqueProduit(@PathVariable Long produitId) {
        return stockService.historiqueProduit(produitId);
    }

    @GetMapping("/alertes")
    public List<AlerteStockDto> alertes() {
        return stockService.alertes();
    }
}
