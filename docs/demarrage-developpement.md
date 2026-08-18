# Guide de démarrage - développement

## Prérequis

- **JDK 21** (Eclipse Temurin recommandé)
- **Maven 3.9+**
- Docker (optionnel, uniquement pour le profil MySQL multi-poste)

Si `java`/`mvn` ne sont pas déjà sur le PATH de votre machine :

```bash
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Maven n'est pas distribué sur winget : téléchargez le binaire zip depuis
[maven.apache.org](https://maven.apache.org/download.cgi), extrayez-le, puis ajoutez son
dossier `bin` au PATH (et définissez `JAVA_HOME` vers le JDK 21).

## Lancer le backend (API Spring Boot)

Par défaut, le backend utilise **SQLite** (base de données locale, fichier créé automatiquement
dans `~/.rhodebazar/rhodebazar.db`) - aucune installation de base de données n'est nécessaire.

```bash
cd backend
mvn spring-boot:run
```

Au premier démarrage, un compte administrateur est créé automatiquement si aucun utilisateur
n'existe. Son mot de passe est généré aléatoirement et affiché **une seule fois** dans les logs
(niveau WARN) - notez-le. Vous pouvez aussi imposer un mot de passe via la variable
d'environnement `RHODEBAZAR_ADMIN_PASSWORD` avant le premier démarrage.

L'API écoute par défaut sur `http://localhost:8085`.

### Utiliser MySQL à la place de SQLite

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Variables d'environnement disponibles : `RHODEBAZAR_DB_HOST`, `RHODEBAZAR_DB_PORT`,
`RHODEBAZAR_DB_NAME`, `RHODEBAZAR_DB_USER`, `RHODEBAZAR_DB_PASSWORD`.

### Avec Docker (backend + MySQL)

```bash
cp .env.example .env   # puis renseigner des valeurs reelles
docker compose up -d
```

## Lancer le client desktop (JavaFX)

Le backend doit être démarré au préalable (le client s'y connecte sur `http://localhost:8085`
par défaut ; modifiable via la propriété système `-Drhodebazar.api.url=...`).

```bash
cd frontend
mvn javafx:run
```

## Structure

```
rhode-bazar/
├── backend/     # API Spring Boot (auth, stock, ventes, depenses, reporting)
├── frontend/    # Client desktop JavaFX (FR/EN, theme violet/blanc/jaune or)
├── docker-compose.yml
└── docs/
```
