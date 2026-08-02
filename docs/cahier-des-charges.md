# Cahier des Charges — Application de Gestion Rhode Bazar

> Transcription du cahier des charges validé avec le client (version de référence pour le développement).

## 1. Introduction

Rhode Bazar souhaite doter son entreprise d'une application desktop native et sécurisée pour améliorer la gestion de ses activités. Cette application doit intégrer des fonctionnalités de gestion de stock, de suivi des dépenses et des ventes, ainsi que des outils analytiques pour la prise de décision.

## 2. Objectifs du Projet

- Optimiser la gestion des stocks en réduisant les pertes et en évitant les ruptures.
- Suivre efficacement les dépenses et les ventes.
- Fournir des rapports et analyses pour une meilleure prise de décision.
- Assurer un haut niveau de sécurité et une accessibilité facile en local.

## 3. Périmètre Fonctionnel

### 3.1. Gestion de Stock

- Gestion des articles : ajout, suppression, modification, classification.
- Suivi des mouvements de stock : entrées, sorties, retours, pertes.
- Notifications de seuil de stock critique.
- Gestion multi-dépôts et emplacements internes.
- Inventaires : génération de rapports et réconciliation des données.
- Facturation et impression de factures.

### 3.2. Suivi des Dépenses et Ventes

- Enregistrement des ventes : type de produit, quantité, prix, mode de paiement.
- Suivi des dépenses : entrées par catégories (fournisseurs, charges fixes, divers).
- Historique des transactions : filtres par date, type, produit ou client.

### 3.3. Analyse et Reporting

- Tableaux de bord interactifs pour les ventes, dépenses, et stocks.
- Rapports financiers : bénéfices, pertes, marges.
- Rapports de performance produit : articles les plus et moins vendus.
- Visualisations graphiques et comparaisons temporelles.

### 3.4. Sécurité et Accessibilité

- Authentifications multi-niveaux : admin, vendeur.
- Sécurité des données : chiffrement et journalisation des activités.
- Sauvegardes locales automatisées avec option de restauration.

### 3.5. Interface Utilisateur

- Interface intuitive et réactive.
- Navigation rapide entre les modules.
- Design moderne adapté à un usage desktop.
- Interface en français par défaut, avec option de bascule vers l'anglais.
- Charte graphique : violet, blanc, jaune or.

## 4. Contraintes Techniques

- Application native : développement en Java, installable sur les systèmes d'exploitation Windows.
- Sécurité avancée : intégration de normes de sécurité pour éviter les intrusions ou pertes de données.
- Technologies adaptées : utilisation d'outils modernes et maintenables pour le développement.

## 5. Technologies Retenues

### 5.1. Back-end

- Langage : Java
- Framework : Spring Boot (gestion des services et de la logique métier)
- Base de données : MySQL ou SQLite (léger et adapté à une utilisation locale)
- Installation via Docker

### 5.2. Front-end

- Interface utilisateur : JavaFX
- CSS : personnalisation de l'apparence de l'application

### 5.3. Hébergement et Sécurité

- Application desktop installée localement sur chaque poste.
- Chiffrement des données sensibles.
- Authentification sécurisée par rôle.

## 6. Livrables

1. Application desktop fonctionnelle avec les modules mentionnés.
2. Manuel utilisateur et guide d'installation.
3. Documentation technique pour maintenance future.

---

*Nom de l'application : Rhode Bazar.*
*Note : un document antérieur (« Guide de développement ») proposait une stack web PHP/CodeIgniter/MySQL. Cette piste a été abandonnée au profit de la stack Java/Spring Boot/JavaFX décrite ci-dessus, conformément à la décision prise avec le client.*
