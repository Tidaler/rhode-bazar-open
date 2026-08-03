package com.rhodebazar.backend.service;

import com.rhodebazar.backend.domain.audit.JournalAudit;
import com.rhodebazar.backend.domain.audit.JournalAuditRepository;
import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final JournalAuditRepository journalAuditRepository;

    public AuditService(JournalAuditRepository journalAuditRepository) {
        this.journalAuditRepository = journalAuditRepository;
    }

    @Transactional
    public void enregistrer(Utilisateur utilisateur, String action, String entite, Long entiteId, String details) {
        journalAuditRepository.save(new JournalAudit(utilisateur, action, entite, entiteId, details));
    }
}
