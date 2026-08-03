ALTER TABLE produits ADD COLUMN code_barres TEXT;

CREATE UNIQUE INDEX idx_produits_code_barres ON produits(code_barres);
