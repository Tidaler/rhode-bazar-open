package com.rhodebazar.backend.web;

import com.rhodebazar.backend.service.ProduitService;
import com.rhodebazar.backend.web.dto.ProduitDto;
import com.rhodebazar.backend.web.dto.ProduitRequest;
import com.rhodebazar.backend.web.dto.StockDepotDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produits")
@PreAuthorize("hasAnyRole('ADMIN','VENDEUR')")
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(ProduitService produitService) {
        this.produitService = produitService;
    }

    @GetMapping
    public List<ProduitDto> lister() {
        return produitService.lister();
    }

    @GetMapping("/{id}")
    public ProduitDto trouver(@PathVariable Long id) {
        return produitService.trouverDto(id);
    }

    @GetMapping("/{id}/stock")
    public List<StockDepotDto> stockParDepot(@PathVariable Long id) {
        return produitService.stockParDepot(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto creer(@Valid @RequestBody ProduitRequest requete) {
        return produitService.creer(requete);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProduitDto modifier(@PathVariable Long id, @Valid @RequestBody ProduitRequest requete) {
        return produitService.modifier(id, requete);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@PathVariable Long id) {
        produitService.desactiver(id);
    }

    @PostMapping("/{id}/reactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public void reactiver(@PathVariable Long id) {
        produitService.reactiver(id);
    }
}
