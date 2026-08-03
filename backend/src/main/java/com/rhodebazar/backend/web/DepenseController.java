package com.rhodebazar.backend.web;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.service.DepenseService;
import com.rhodebazar.backend.web.dto.DepenseDto;
import com.rhodebazar.backend.web.dto.DepenseRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les depenses (charges de l'entreprise) sont reservees au role ADMIN,
 * a la difference du stock et des ventes qui restent visibles aux vendeurs.
 */
@RestController
@RequestMapping("/api/depenses")
@PreAuthorize("hasRole('ADMIN')")
public class DepenseController {

    private final DepenseService depenseService;

    public DepenseController(DepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @GetMapping
    public List<DepenseDto> lister(@RequestParam(required = false) CategorieDepense categorie) {
        return depenseService.lister(categorie);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepenseDto creer(
            @Valid @RequestBody DepenseRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return depenseService.creer(requete, principal.getUtilisateur());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        depenseService.supprimer(id);
    }
}
