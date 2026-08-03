package com.rhodebazar.backend.web;

import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.service.VenteService;
import com.rhodebazar.backend.web.dto.VenteDto;
import com.rhodebazar.backend.web.dto.VenteRequest;
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
@RequestMapping("/api/ventes")
@PreAuthorize("hasAnyRole('ADMIN','VENDEUR')")
public class VenteController {

    private final VenteService venteService;

    public VenteController(VenteService venteService) {
        this.venteService = venteService;
    }

    @GetMapping
    public List<VenteDto> lister() {
        return venteService.lister();
    }

    @GetMapping("/{id}")
    public VenteDto trouver(@PathVariable Long id) {
        return venteService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenteDto creer(
            @Valid @RequestBody VenteRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return venteService.creerVente(requete, principal.getUtilisateur());
    }
}
