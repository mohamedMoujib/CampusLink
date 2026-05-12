# Configuration n8n pour CampusLink

Les workflows n8n permettent de créer des **publications** et des **services** via l'IA (Claude via OpenRouter).

## 1. Démarrer n8n

```bash
npx n8n
```

Ou avec Docker :
```bash
docker run -it --rm -p 5678:5678 n8nio/n8n
```

Interface : http://localhost:5678

---

## 2. Importer les workflows

1. Ouvrir n8n → **Workflows** → **Import**
2. Importer les fichiers :
   - `src/main/java/org/example/campusLink/IA/N8n_workflow1_publications.JSON`
   - `src/main/java/org/example/campusLink/IA/N8n_workflow2_services.JSON`
3. Chaque workflow doit être **activé** (toggle ON en haut à droite) pour que les webhooks fonctionnent.

---

## 3. Configurer les credentials

### OpenRouter API Key (requis)

1. Créer une clé sur [openrouter.ai](https://openrouter.ai/keys)
2. Dans n8n : **Settings** → **Credentials** → **Add Credential**
3. Type : **Header Auth**
4. Nom : `OpenRouter API Key`
5. Nom de l'en-tête : `Authorization`
6. Valeur : `Bearer sk-or-v1-VOTRE_CLE_OPENROUTER`
7. Sauvegarder
8. Dans chaque workflow : cliquer sur le nœud **OpenRouter** → sélectionner ce credential

### MySQL CampusLink (requis)

**⚠️ IMPORTANT** : n8n et l'app Java doivent utiliser **exactement la même base de données** (campusLink).

Si **n8n tourne en Docker** : `localhost` dans MySQL pointe vers le conteneur, pas votre PC. Utilisez plutôt :
- **Host** : `host.docker.internal` (Windows/Mac) ou l’IP de votre machine (ex. `192.168.1.x`)

Si **n8n tourne en local** (`npx n8n`) : **Host** : `localhost`

1. Dans n8n : **Settings** → **Credentials** → **Add Credential**
2. Type : **MySQL**
3. Nom : `CampusLink MySQL`
4. Paramètres :
   - **Host** : `localhost` (ou `host.docker.internal` si n8n en Docker)
   - **Database** : `campusLink` (même nom que MyDatabase.java)
   - **User** : `root`
   - **Password** : vide (comme dans MyDatabase.java)
   - **Port** : `3306`
5. Tester la connexion (bouton Test)
6. Sauvegarder
7. Dans chaque workflow : cliquer sur le nœud **MySQL** → sélectionner ce credential

---

## 4. URL des webhooks (production)

Une fois les workflows **activés**, les URLs sont :

- **Publication** : `http://localhost:5678/webhook/creer-publication`
- **Service** : `http://localhost:5678/webhook/creer-service`

L'application Java appelle ces URLs. Si n8n tourne sur un autre hôte/port, définir :

```
CAMPUSLINK_N8N_URL=http://votre-host:port
```

---

## 5. Test rapide (curl)

**Publication :**
```bash
curl -X POST http://localhost:5678/webhook/creer-publication \
  -H "Content-Type: application/json" \
  -d "{\"student_id\":1,\"prompt\":\"Cherche tuteur maths L1\",\"type_publication\":\"DEMANDE_SERVICE\",\"prix_vente\":20}"
```

**Service :**
```bash
curl -X POST http://localhost:5678/webhook/creer-service \
  -H "Content-Type: application/json" \
  -d "{\"prestataire_id\":1,\"prompt\":\"Cours de maths niveau lycée\",\"prix\":25,\"category_id\":1}"
```

Si tout est OK, vous recevez une réponse JSON avec `success: true` et `publication_id` ou `service_id`.

---

## 6. Dépannage

| Problème | Cause probable | Solution |
|----------|----------------|----------|
| 404 Not Found | Webhook non enregistré | Activer le workflow (toggle ON) |
| Timeout / Connexion refusée | n8n pas démarré | Lancer `npx n8n` |
| Erreur OpenRouter | Clé API manquante/invalide | Créer credential Header Auth |
| Erreur MySQL | Base inaccessible | Vérifier host, user, password |
| Réponse vide | Workflow échoue avant Respond | Vérifier les exécutions dans n8n (onglet Executions) |
| **Données non visibles dans l'app** | **n8n et Java n'utilisent pas la même base** | **Host MySQL : `localhost` si n8n local, `host.docker.internal` si n8n Docker** |
| **HTTP 200 avec 0 chars / corps vide** | **Workflow n'atteint pas le nœud Respond** | **Onglet Executions dans n8n → vérifier où le workflow échoue (OpenRouter, MySQL).** |
| **curl fonctionne mais l'app Java reçoit 0 chars** | **URL différente ou timeout** | **Utilisez EXACTEMENT `http://localhost:5678/webhook/creer-publication` (pas webhook-test). Vérifiez que le workflow est ACTIF. Augmentez readTimeout (déjà 60s dans l'app).** |

---

## 7. Vérifier que les données sont bien enregistrées

1. Dans n8n : **Executions** → ouvrir la dernière exécution → vérifier que le nœud **MySQL** est vert (succès)
2. Dans MySQL (Workbench, phpMyAdmin, etc.) :
   ```sql
   USE campusLink;
   SELECT * FROM publications ORDER BY id DESC LIMIT 5;
   SELECT * FROM services ORDER BY id DESC LIMIT 5;
   ```
3. Les lignes créées via n8n doivent apparaître ici et dans l’application Java si les deux pointent vers la même base.

---

## 8. Structure des tables MySQL

Les workflows insèrent dans les tables `publications` et `services` de la base `campusLink`. Assurez-vous que ces tables existent et correspondent au schéma attendu (voir `Gestion_publication` et `Gestion_Service`).
