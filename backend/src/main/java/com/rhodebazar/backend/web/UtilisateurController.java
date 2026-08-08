package com.rhodebazar.backend.web;

import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.service.UtilisateurService;
import com.rhodebazar.backend.web.dto.UtilisateurDto;
import com.rhodebazar.backend.web.dto.UtilisateurModificationRequest;
import com.rhodebazar.backend.web.dto.UtilisateurRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<UtilisateurDto> lister() {
        return utilisateurService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDto creer(
            @Valid @RequestBody UtilisateurRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return utilisateurService.creer(requete, principal.getUtilisateur());
    }

    @PutMapping("/{id}")
    public UtilisateurDto modifier(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurModificationRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return utilisateurService.modifier(id, requete, principal.getUtilisateur());
    }
}
