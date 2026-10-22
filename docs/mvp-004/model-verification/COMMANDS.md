# Commandes, protocole de tentative et reprise

Contrôles exécutés avant téléchargement :

```sh
git status --short
git branch --show-current
git log -1 --format='%H %s'
git fetch origin test/mvp-004-native-qualification
git rev-parse HEAD FETCH_HEAD
git ls-remote --heads origin refs/heads/test/mvp-004-native-qualification
```

Le nom de référence locale origin/test/mvp-004-native-qualification n'était pas
disponible après le fetch simple ; HEAD, FETCH_HEAD puis ls-remote ont confirmé
le commit exact. Aucun checkout forcé ni recréation du travail.

Tentative exécutée par un script Python éphémère via `python3` :

- urllib.request.build_opener avec HTTPRedirectHandler standard complété pour
  enregistrer seulement les noms d'hôtes HTTPS, sans requête signée de CDN ;
- URL resolve épinglée complète conservée dans DOWNLOAD.json, timeout40s,
  proxy et CA hérités, aucun header d'authentification ajouté ;
- ouverture HTTPS et suivi de la redirection : CONNECT refusé avant réponse du CDN ;
- la création exclusive du .part était située après l'ouverture de la réponse :
  aucun fichier modèle ni fichier partiel n'a été créé ;
- une seule tentative, erreur et horaires enregistrés hors dépôt puis copie
  de la preuve JSON dans ce sous-dossier. Aucun calcul d'empreinte des poids.

Contrôles indépendants effectués : lecture des quatre fichiers indiqués par la
mission, SHA-256 de la licence et du vérificateur, comparaison de tous les fichiers
suivis de69b65685 au disque, lecture de l'APK diagnostique existante sans extraction
ou installation. PRESERVATION.json conserve les résultats. Les changements
indexés doivent être exclusivement sous docs/mvp-004/model-verification/.

## Après levée du blocage réseau uniquement

La commande suivante est préparée, **non exécutée dans MVP004B** :

```sh
python3 tools/mvp4_model_probe.py \
 --model /workspace/model-laboratory/mvp-004b-1208e45/Qwen3-0.6B-Q4_K_M.gguf \
 --report docs/mvp-004/model-verification/GGUF-REPORT.json
```

Un calcul indépendant à rejouer après obtention complète :

```sh
stat -c '%s' /workspace/model-laboratory/mvp-004b-1208e45/Qwen3-0.6B-Q4_K_M.gguf
sha256sum /workspace/model-laboratory/mvp-004b-1208e45/Qwen3-0.6B-Q4_K_M.gguf
```

Aucun fichier existant n'est actuellement disponible à ces commandes. Ne pas
substituer les métadonnées API ou le SHA d'un fichier vide à ces vérifications.
Ne pas appliquer de modification au vérificateur pour obtenir un succès.
