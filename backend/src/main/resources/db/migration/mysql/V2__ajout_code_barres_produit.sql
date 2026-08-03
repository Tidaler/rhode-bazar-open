ALTER TABLE produits ADD COLUMN code_barres VARCHAR(64) NULL;

ALTER TABLE produits ADD CONSTRAINT uq_produits_code_barres UNIQUE (code_barres);
