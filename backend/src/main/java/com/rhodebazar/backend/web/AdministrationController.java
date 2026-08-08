package com.rhodebazar.backend.web;

import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.service.BackupService;
import com.rhodebazar.backend.web.dto.SauvegardeDto;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administration")
@PreAuthorize("hasRole('ADMIN')")
public class AdministrationController {

    private final Optional<BackupService> backupService;

    public AdministrationController(Optional<BackupService> backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/sauvegardes")
    public List<SauvegardeDto> listerSauvegardes() {
        return backupService.map(BackupService::lister).orElseGet(List::of);
    }

    @PostMapping("/sauvegardes")
    @ResponseStatus(HttpStatus.CREATED)
    public SauvegardeDto declencherSauvegarde() {
        return backupService.orElseThrow(() -> new ConflitDonneesException(
                        "Sauvegarde indisponible : cette instance utilise le profil MySQL/Docker."))
                .declencherSauvegarde();
    }
}
