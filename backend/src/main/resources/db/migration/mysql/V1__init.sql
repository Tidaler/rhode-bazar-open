-- Schema initial Rhode Bazar (MySQL) -- deploiement multi-poste

CREATE TABLE utilisateurs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,
    role            ENUM('ADMIN', 'VENDEUR') NOT NULL,
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation   VARCHAR(35) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE depots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    adresse         VARCHAR(255),
    actif           BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL UNIQUE,
    description     VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE produits (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    categorie_id    BIGINT REFERENCES categories(id),
    prix_achat      DECIMAL(12,2) NOT NULL DEFAULT 0,
    prix_vente      DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock_minimal   INT NOT NULL DEFAULT 0,
    unite           VARCHAR(30) NOT NULL DEFAULT 'unite',
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation   VARCHAR(35) NOT NULL,
    CONSTRAINT fk_produits_categorie FOREIGN KEY (categorie_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_produit_depot (
    produit_id      BIGINT NOT NULL,
    depot_id        BIGINT NOT NULL,
    quantite        INT NOT NULL DEFAULT 0,
    PRIMARY KEY (produit_id, depot_id),
    CONSTRAINT fk_spd_produit FOREIGN KEY (produit_id) REFERENCES produits(id),
    CONSTRAINT fk_spd_depot FOREIGN KEY (depot_id) REFERENCES depots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mouvements_stock (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    produit_id      BIGINT NOT NULL,
    depot_id        BIGINT NOT NULL,
    type_mouvement  ENUM('ENTREE', 'SORTIE', 'RETOUR', 'PERTE') NOT NULL,
    quantite        INT NOT NULL,
    motif           VARCHAR(500),
    utilisateur_id  BIGINT,
    date_mouvement  VARCHAR(35) NOT NULL,
    CONSTRAINT fk_mouv_produit FOREIGN KEY (produit_id) REFERENCES produits(id),
    CONSTRAINT fk_mouv_depot FOREIGN KEY (depot_id) REFERENCES depots(id),
    CONSTRAINT fk_mouv_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ventes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero          VARCHAR(50) NOT NULL UNIQUE,
    date_vente      VARCHAR(35) NOT NULL,
    utilisateur_id  BIGINT NOT NULL,
    depot_id        BIGINT NOT NULL,
    mode_paiement   ENUM('ESPECES', 'CARTE', 'MOBILE_MONEY', 'VIREMENT', 'AUTRE') NOT NULL,
    client_nom      VARCHAR(150),
    montant_total   DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_ventes_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
    CONSTRAINT fk_ventes_depot FOREIGN KEY (depot_id) REFERENCES depots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lignes_vente (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    vente_id        BIGINT NOT NULL,
    produit_id      BIGINT NOT NULL,
    quantite        INT NOT NULL,
    prix_unitaire   DECIMAL(12,2) NOT NULL,
    montant_ligne   DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_lignes_vente_vente FOREIGN KEY (vente_id) REFERENCES ventes(id),
    CONSTRAINT fk_lignes_vente_produit FOREIGN KEY (produit_id) REFERENCES produits(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE depenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    categorie       ENUM('FOURNISSEUR', 'CHARGE_FIXE', 'DIVERS') NOT NULL,
    libelle         VARCHAR(255) NOT NULL,
    montant         DECIMAL(12,2) NOT NULL,
    date_depense    VARCHAR(35) NOT NULL,
    utilisateur_id  BIGINT,
    CONSTRAINT fk_depenses_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE journal_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id  BIGINT,
    action          VARCHAR(100) NOT NULL,
    entite          VARCHAR(100) NOT NULL,
    entite_id       BIGINT,
    details         VARCHAR(1000),
    date_action     VARCHAR(35) NOT NULL,
    CONSTRAINT fk_audit_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_produits_categorie ON produits(categorie_id);
CREATE INDEX idx_mouvements_produit ON mouvements_stock(produit_id);
CREATE INDEX idx_ventes_utilisateur ON ventes(utilisateur_id);
CREATE INDEX idx_lignes_vente_vente ON lignes_vente(vente_id);
CREATE INDEX idx_depenses_categorie ON depenses(categorie);
CREATE INDEX idx_journal_audit_utilisateur ON journal_audit(utilisateur_id);

-- Le compte administrateur initial est cree au demarrage par AdminInitializer
-- (com.rhodebazar.backend.security), avec un mot de passe hache via le
-- PasswordEncoder de l'application plutot qu'un hash code en dur ici.
