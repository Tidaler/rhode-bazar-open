-- Schema initial Rhode Bazar (SQLite)

CREATE TABLE utilisateurs (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nom             TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    mot_de_passe    TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN ('ADMIN', 'VENDEUR')),
    actif           INTEGER NOT NULL DEFAULT 1,
    date_creation   TEXT NOT NULL
);

CREATE TABLE depots (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nom             TEXT NOT NULL,
    adresse         TEXT,
    actif           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE categories (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nom             TEXT NOT NULL UNIQUE,
    description     TEXT
);

CREATE TABLE produits (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nom             TEXT NOT NULL,
    description     TEXT,
    categorie_id    INTEGER REFERENCES categories(id),
    prix_achat      NUMERIC NOT NULL DEFAULT 0,
    prix_vente      NUMERIC NOT NULL DEFAULT 0,
    stock_minimal   INTEGER NOT NULL DEFAULT 0,
    unite           TEXT NOT NULL DEFAULT 'unite',
    actif           INTEGER NOT NULL DEFAULT 1,
    date_creation   TEXT NOT NULL
);

CREATE TABLE stock_produit_depot (
    produit_id      INTEGER NOT NULL REFERENCES produits(id),
    depot_id        INTEGER NOT NULL REFERENCES depots(id),
    quantite        INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (produit_id, depot_id)
);

CREATE TABLE mouvements_stock (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    produit_id      INTEGER NOT NULL REFERENCES produits(id),
    depot_id        INTEGER NOT NULL REFERENCES depots(id),
    type_mouvement  TEXT NOT NULL CHECK (type_mouvement IN ('ENTREE', 'SORTIE', 'RETOUR', 'PERTE')),
    quantite        INTEGER NOT NULL,
    motif           TEXT,
    utilisateur_id  INTEGER REFERENCES utilisateurs(id),
    date_mouvement  TEXT NOT NULL
);

CREATE TABLE ventes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    numero          TEXT NOT NULL UNIQUE,
    date_vente      TEXT NOT NULL,
    utilisateur_id  INTEGER NOT NULL REFERENCES utilisateurs(id),
    depot_id        INTEGER NOT NULL REFERENCES depots(id),
    mode_paiement   TEXT NOT NULL CHECK (mode_paiement IN ('ESPECES', 'CARTE', 'MOBILE_MONEY', 'VIREMENT', 'AUTRE')),
    client_nom      TEXT,
    montant_total   NUMERIC NOT NULL DEFAULT 0
);

CREATE TABLE lignes_vente (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    vente_id        INTEGER NOT NULL REFERENCES ventes(id),
    produit_id      INTEGER NOT NULL REFERENCES produits(id),
    quantite        INTEGER NOT NULL,
    prix_unitaire   NUMERIC NOT NULL,
    montant_ligne   NUMERIC NOT NULL
);

CREATE TABLE depenses (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    categorie       TEXT NOT NULL CHECK (categorie IN ('FOURNISSEUR', 'CHARGE_FIXE', 'DIVERS')),
    libelle         TEXT NOT NULL,
    montant         NUMERIC NOT NULL,
    date_depense    TEXT NOT NULL,
    utilisateur_id  INTEGER REFERENCES utilisateurs(id)
);

CREATE TABLE journal_audit (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    utilisateur_id  INTEGER REFERENCES utilisateurs(id),
    action          TEXT NOT NULL,
    entite          TEXT NOT NULL,
    entite_id       INTEGER,
    details         TEXT,
    date_action     TEXT NOT NULL
);

CREATE INDEX idx_produits_categorie ON produits(categorie_id);
CREATE INDEX idx_mouvements_produit ON mouvements_stock(produit_id);
CREATE INDEX idx_ventes_utilisateur ON ventes(utilisateur_id);
CREATE INDEX idx_lignes_vente_vente ON lignes_vente(vente_id);
CREATE INDEX idx_depenses_categorie ON depenses(categorie);
CREATE INDEX idx_journal_audit_utilisateur ON journal_audit(utilisateur_id);

-- Le compte administrateur initial est cree au demarrage par AdminInitializer
-- (com.rhodebazar.backend.security), avec un mot de passe hache via le
-- PasswordEncoder de l'application plutot qu'un hash code en dur ici.
