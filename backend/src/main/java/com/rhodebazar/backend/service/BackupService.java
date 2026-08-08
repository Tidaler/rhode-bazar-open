package com.rhodebazar.backend.service;

import com.rhodebazar.backend.web.dto.SauvegardeDto;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Sauvegarde la base SQLite locale (copie de fichier) : automatiquement chaque nuit, et a la
 * demande via l'endpoint d'administration. Ne s'applique qu'au profil "sqlite" (mono-poste) ;
 * le profil "mysql" (deploiement Docker multi-poste) n'a pas d'equivalent dans cette version.
 */
@Service
@Profile("sqlite")
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FORMAT_NOM = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int RETENTION_JOURS = 30;

    private final Path fichierBase;

    public BackupService(@Value("${RHODEBAZAR_DB_PATH:${user.home}/.rhodebazar/rhodebazar.db}") String cheminBase) {
        this.fichierBase = Path.of(cheminBase).toAbsolutePath();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void sauvegardeAutomatique() {
        try {
            declencherSauvegarde();
        } catch (Exception e) {
            log.error("Echec de la sauvegarde automatique de la base de donnees", e);
        }
    }

    public SauvegardeDto declencherSauvegarde() {
        try {
            Path dossierSauvegardes = dossierSauvegardes();
            Files.createDirectories(dossierSauvegardes);

            String nom = "rhodebazar-" + LocalDateTime.now().format(FORMAT_NOM) + ".db";
            Path cible = dossierSauvegardes.resolve(nom);
            Files.copy(fichierBase, cible, StandardCopyOption.REPLACE_EXISTING);

            purgerAnciennesSauvegardes(dossierSauvegardes);

            log.info("Sauvegarde de la base de donnees creee : {}", cible);
            return versDto(cible);
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de la sauvegarde de la base de donnees", e);
        }
    }

    public List<SauvegardeDto> lister() {
        Path dossierSauvegardes = dossierSauvegardes();
        if (!Files.isDirectory(dossierSauvegardes)) {
            return List.of();
        }
        try (var flux = Files.list(dossierSauvegardes)) {
            return flux.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::dateSauvegarde).reversed())
                    .map(this::versDto)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister les sauvegardes", e);
        }
    }

    private void purgerAnciennesSauvegardes(Path dossierSauvegardes) throws IOException {
        LocalDateTime limite = LocalDateTime.now().minusDays(RETENTION_JOURS);
        try (var flux = Files.list(dossierSauvegardes)) {
            for (Path fichier : flux.filter(Files::isRegularFile).toList()) {
                if (dateSauvegarde(fichier).isBefore(limite)) {
                    Files.deleteIfExists(fichier);
                }
            }
        }
    }

    /**
     * Extrait la date de creation depuis le nom du fichier (rhodebazar-yyyyMMdd-HHmmss.db)
     * plutot que depuis les attributs du systeme de fichiers : une copie de fichier ne
     * garantit pas que la date de derniere modification reflete l'instant de la copie
     * (elle peut heriter de celle du fichier source selon le systeme d'exploitation).
     */
    private LocalDateTime dateSauvegarde(Path fichier) {
        String nom = fichier.getFileName().toString();
        try {
            String horodatage = nom.substring("rhodebazar-".length(), nom.length() - ".db".length());
            return LocalDateTime.parse(horodatage, FORMAT_NOM);
        } catch (Exception e) {
            try {
                return LocalDateTime.ofInstant(Files.getLastModifiedTime(fichier).toInstant(), ZoneId.systemDefault());
            } catch (IOException ioe) {
                return LocalDateTime.MIN;
            }
        }
    }

    private Path dossierSauvegardes() {
        Path dossierParent = fichierBase.getParent();
        return (dossierParent != null ? dossierParent : Path.of(".")).resolve("backups");
    }

    private SauvegardeDto versDto(Path fichier) {
        try {
            long taille = Files.size(fichier);
            return new SauvegardeDto(fichier.getFileName().toString(), taille, dateSauvegarde(fichier).format(FORMAT_DATE));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire les informations de la sauvegarde : " + fichier, e);
        }
    }
}
