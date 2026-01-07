# 📦 GhibiriWorldBackup 

GhibiriWorldBackup è un plugin per **Paper / Spigot** che permette di eseguire **backup sicuri dei mondi Minecraft** in formato ZIP, con supporto a **backup automatici**, **GitHub Releases**, **pulizia automatica** e **comandi avanzati**.

---

## ✅ Compatibilità
- **Paper** 1.20+ (testato su 1.21.11)
- **Spigot** compatibile
- **Java 17+**

---

## ✨ Funzionalità principali

- 📁 Effettua il backup in .zip di **tutti i mondi**
- 🧍 Include **playerdata**, inventari, advancements
- ⏱️ Possibilità di fare backup manuali e automatici
- 🔐 Cooldown sui backup manuali opzionale (ignorabile con ***/backupnow force***)
- ☁️ Upload automatico su **GitHub Releases**
- 🧹 Pulizia automatica (Imposta il limite massimo di backup da tenere):
    - backup locali
    - backup su release GitHub
- 🔄 Ricarica il config.yml direttamente in gioco senza dover fare */reload*
- ⌨️ Autocompletamento comandi con TAB

---

## 📂 Struttura backup

### Backup locali
Percorso: 'plugins/WorldBackup/backups/'
Formato: 'yy-MM-dd_hh-mm.zip'

---

## 🧪 Comandi

| Comando | Descrizione |
|------|-----------|
| `/backupnow` | Esegue un backup manuale |
| `/backupnow force` | Forza il backup ignorando il cooldown |
| `/autobackup on` | Attiva autobackup |
| `/autobackup off` | Disattiva autobackup |
| `/autobackup now` | Mostra stato autobackup |
| `/autobackup hourly` | Backup ogni ora |
| `/autobackup daily` | Backup giornaliero |
| `/autobackup every <min>` | Backup ogni X minuti |
| `/worldbackup reload` | Ricarica il config (safe) |

---

## 🔐 Permessi

| Permesso | Descrizione               |
|-------|---------------------------|
| `worldbackup.backupnow` | Usa `/backupnow`          |
| `worldbackup.force` | Usa `/backupnow force`    |
| `worldbackup.admin` | Usa `/worldbackup reload` |

> Gli **OP** hanno tutto automaticamente.

---

## 🔑 GitHub Personal Access Token (PAT) – Guida Completa

Per permettere a **WorldBackup** di caricare automaticamente i backup su **GitHub Releases**, è necessario creare un **Personal Access Token (PAT)** su GitHub.

Il token serve al plugin per:
- creare release
- caricare file ZIP come asset
- eliminare release vecchie durante la pulizia automatica

### 1️⃣ Aprire le impostazioni GitHub

Accedi a GitHub e vai in questo percorso:

Profile picture → Settings → Developer settings → Personal access tokens

### 2️⃣ Creare un nuovo token

Puoi scegliere uno dei due tipi (entrambi funzionano):

**Fine-grained token (consigliato)**
- Clicca su **Generate new token (fine-grained)**

**Oppure**

**Classic token**
- Clicca su **Generate new token (classic)**

### 3️⃣ Selezionare la repository

Durante la creazione del token imposta:

- Repository access:
    - **Only select repositories**
    - seleziona **solo la repository usata per i backup**

Questo evita di dare accesso inutile ad altre repo.

### 4️⃣ Impostare i permessi corretti

Abilita **ESATTAMENTE** questi permessi:

| Permesso  | Livello          |
|-----------|------------------|
| Contents  | Read and write   |
| Metadata  | Read             |

⚠️ Se anche uno solo di questi permessi manca, il plugin mostrerà errore **401 Bad credentials**.

### 5️⃣ Generare e copiare il token

Dopo aver creato il token:
- copialo immediatamente
- **non potrai visualizzarlo di nuovo**

Il token avrà un formato simile a:

ghp_xxxxxxxxxxxxxxxxxxxxx  
oppure  
github_pat_xxxxxxxxxxxxx

### 6️⃣ Inserire il token nel `config.yml`

Apri il file:

plugins/WorldBackup/config.yml

E inserisci il token in questo modo:

githubToken: "ghp_XXXXXXXXXXXX"

Dopo aver salvato il PAT_TOKEN nel config.yml, puoi fare in-game /worldbackup reload

Adesso prova a fare /backupnow e controlla eventuali errori in console!

---

## ⚙️ Configurazione (`config.yml`)

```yml
# !!! Use "false" only to disable the plugin!!!
enabled: true

# Language
lang: it_IT

# Upload to GitHub Releases
uploadToGithub: true

# Repository GitHub (format: owner/repo)
githubRepo: "TUOUSERNAME/TUAREPO"

# GitHub Personal Access Token
githubToken: "ghp_xxxxxxxxxxxxxxxxxxxxx"

# Autobackup
autoEnabled: false
mode: daily        # daily | hourly | every
everyMinutes: 60       # only if autoMode = every

# Cooldown backup (manual) (minutes)
cooldownMinutes: 10

# Max Local backups
keepLastLocal: 10

# Max GitHub backups
keepLastGithub: 10

