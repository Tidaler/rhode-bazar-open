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
| Packaging | Maven, Docker (pour la base de données / services annexes), jlink + jpackage (installateur Windows) |

## Structure du dépôt (à venir)

```
rhode-bazar/
├── backend/    # Spring Boot : API, logique métier, accès données
├── frontend/   # JavaFX : interface desktop
├── docs/       # Cahier des charges, guides, documentation technique
└── scripts/    # Scripts d'installation, sauvegarde, packaging
```

## Packaging & distribution Windows

ManagEx peut être empaqueté en un installateur Windows (`.msi`) autonome, qui n'exige
**aucune installation de Java** sur le poste cible : le `.msi` embarque une image JRE
minimale (générée par `jlink`) contenant uniquement les modules nécessaires (backend
Spring Boot + client JavaFX), ainsi que les deux jars de l'application. Au lancement,
le client JavaFX démarre automatiquement le backend en arrière-plan (sans fenêtre de
console), attend qu'il soit prêt, puis affiche l'interface ; il l'arrête proprement à
la fermeture. Ce mécanisme (classe `BackendLauncher`) ne s'active qu'en version
empaquetée - le workflow de développement habituel (`mvn spring-boot:run` +
`mvn javafx:run` dans deux terminaux) reste inchangé.

### Prérequis (uniquement pour construire l'installateur)

1. **JDK 21** (Eclipse Temurin recommandé) - déjà requis pour le reste du projet.
2. **WiX Toolset v3.11** (`candle.exe`/`light.exe`) - requis par `jpackage` du JDK 21
   pour produire un `.msi`. **Attention : il s'agit bien de la branche WiX 3.x**, pas
   de WiX 4/5 (`wix.exe`), incompatible avec `jpackage` sur JDK 21.
   ```powershell
   winget install --id WiXToolset.WiXToolset --exact
   ```
3. **JavaFX jmods** (paquet binaire officiel, séparé du JDK - nécessaire à `jlink`
   pour inclure les modules JavaFX dans l'image runtime). Télécharger
   `openjfx-21.0.7_windows-x64_bin-jmods.zip` depuis
   [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/) (ou directement
   `https://download2.gluonhq.com/openjfx/21.0.7/openjfx-21.0.7_windows-x64_bin-jmods.zip`),
   puis extraire le dossier `javafx-jmods-21.0.7` obtenu vers
   `%USERPROFILE%\tools\javafx-jmods-21.0.7` (chemin par défaut dans
   `installer/pom.xml`, surchageable via `-Djavafx.jmods.dir=<chemin>`).

### Construire l'installateur

Depuis la racine du dépôt :

```bash
mvn -Pdistribution package
```

Cette commande construit backend + frontend (jar "fat" du frontend avec toutes ses
dépendances via `maven-shade-plugin`), génère l'image runtime via `jlink`, puis
produit l'installateur avec `jpackage` (plugin `org.panteleyev:jpackage-maven-plugin`).
Le résultat final est déposé dans `target/dist/ManagEx-<version>.msi` (à la racine du
dépôt). `mvn package` sans `-Pdistribution` reconstruit backend + frontend comme
d'habitude, sans toucher à l'installateur.

### Publier une mise à jour

Les boutiques déjà équipées reçoivent les mises à jour en exécutant simplement le
nouvel installateur - il remplace l'ancienne version en place, sans doublon et sans
toucher aux données existantes.

1. Incrémenter la version (met à jour `pom.xml` racine, `backend/`, `frontend/` et
   `installer/` en une seule commande) :
   ```bash
   mvn versions:set -DnewVersion=1.1.0 -DprocessAllModules=true
   mvn versions:commit
   ```
2. Régénérer l'installateur avec la même commande : `mvn -Pdistribution package`.
3. Distribuer `target/dist/ManagEx-1.1.0.msi` aux boutiques déjà équipées. Elles
   exécutent ce nouvel installateur : l'ancienne version est désinstallée puis la
   nouvelle installée au même emplacement, automatiquement (mécanisme WiX
   "major upgrade" piloté par l'UpgradeCode - voir ci-dessous). La base de données
   SQLite (`%USERPROFILE%\.rhodebazar\rhodebazar.db`), hors du dossier
   d'installation, n'est jamais touchée ; les migrations Flyway éventuellement
   nécessaires s'appliquent automatiquement au premier démarrage de la nouvelle
   version, sans intervention manuelle.

### ⚠️ UpgradeCode WiX - NE JAMAIS MODIFIER

`installer/pom.xml` définit une propriété `managex.win.upgrade.uuid` (un GUID fixe,
généré une seule fois). C'est le **seul** identifiant qui permet à WiX de reconnaître
qu'un nouvel installateur est une mise à jour d'une version déjà installée (plutôt
qu'une application différente) : il déclenche la désinstallation silencieuse de
l'ancienne version puis l'installation de la nouvelle au même endroit. Le
`ProductCode`, lui, change automatiquement à chaque version (géré par jpackage/WiX) -
c'est normal et attendu.

**Si ce GUID est modifié entre deux versions, la mise à jour automatique casse** :
les boutiques déjà équipées se retrouvent avec deux installations de ManagEx en
parallèle au lieu d'un remplacement propre. Ne jamais régénérer ce GUID pour une
nouvelle version - uniquement le numéro de version (`--app-version`, dérivé
automatiquement de `${project.version}` sans le suffixe `-SNAPSHOT`).

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
- [x] Packaging & installateur Windows

## Langue

L'interface est en français par défaut, avec une option de bascule vers l'anglais (internationalisation prévue via `ResourceBundle`).

---

*ManagEx - projet privé.*
