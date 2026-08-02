package com.rhodebazar.backend.domain.audit;

import com.rhodebazar.backend.domain.utilisateur.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Journalisation des activites sensibles (exigence securite du cahier des charges). */
@Entity
@Table(name = "journal_audit")
@Getter
@Setter
@NoArgsConstructor
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String entite;

    @Column(name = "entite_id")
    private Long entiteId;

    @Column(length = 1000)
    private String details;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    public JournalAudit(Utilisateur utilisateur, String action, String entite, Long entiteId, String details) {
        this.utilisateur = utilisateur;
        this.action = action;
        this.entite = entite;
        this.entiteId = entiteId;
        this.details = details;
    }
}
