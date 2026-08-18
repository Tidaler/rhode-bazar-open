# ManagEx - Application de Gestion

*Déployée ici pour la boutique Rhode Bazar.*

Application desktop native pour la gestion d'une boutique de vente d'articles de décoration : stock, ventes, dépenses, facturation et reporting.

> Ce dépôt est au tout début du projet. L'architecture ci-dessous décrit la cible ; le code sera ajouté étape par étape.

## Objectifs

- Optimiser la gestion des stocks (réduction des pertes, alertes de rupture).
- Suivre les dépenses et les ventes de façon centralisée.
- Fournir des rapports et tableaux de bord pour la prise de décision.
- Garantir un haut niveau de sécurité et de fiabilité pour un usage local.

## Périmètre fonctionnel (v1)

- **Gestion de stock** : articles, catégories, mouvements (entrées/sorties/retours/pertes), seuils critiques, multi-dépôts, inventaires, facturation.
- **Ventes & dépenses** : enregistrement des ventes (produit, quantité, prix, mode de paiement), suivi des dépenses par catégorie, historique filtrable.
- **Analyse & reporting** : tableaux de bord, rapports financiers (bénéfices/pertes/marges), produits les plus/moins vendus, graphiques.
- **Sécurité & accessibilité** : authentification multi-niveaux (admin, vendeur), chiffrement des données sensibles, journalisation des activités, sauvegardes locales automatisées.
- **Interface** : FR par défaut avec bascule vers l'anglais, charte graphique violet / blanc / jaune or.

Voir [docs/cahier-des-charges.md](docs/cahier-des-charges.md) pour le détail contractuel, et [docs/fonctionnalites-proposees.md](docs/fonctionnalites-proposees.md) pour les compléments proposés en plus du cahier des charges initial.

## Stack technique

| Couche | Technologie |
|---|---|
| Back-end | Java, Spring Boot |
| Front-end | JavaFX + CSS |
| Base de données | SQLite (mono-poste) / MySQL (multi-poste, optionnel) |
| Packaging | Maven, Docker (pour la base de données / services annexes) |

## Structure du dépôt (à venir)

```
rhode-bazar/
├── backend/    # Spring Boot : API, logique métier, accès données
├── frontend/   # JavaFX : interface desktop
├── docs/       # Cahier des charges, guides, documentation technique
└── scripts/    # Scripts d'installation, sauvegarde, packaging
```

## Statut

- [x] Cahier des charges validé
- [x] Dépôt Git initialisé
- [ ] Structure du projet (backend / frontend)
- [ ] Modèle de données
- [ ] Authentification & rôles
- [ ] Module Gestion de stock
- [ ] Module Ventes & dépenses
- [ ] Module Reporting
- [ ] Internationalisation FR/EN
- [ ] Packaging & installateur Windows

## Langue

L'interface est en français par défaut, avec une option de bascule vers l'anglais (internationalisation prévue via `ResourceBundle`).

---

*ManagEx - projet privé.*
