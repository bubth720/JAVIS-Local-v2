# MVP-003 — audit de la candidate Android

Base distante vérifiée : MVP-002 `c7fec293791dc0b30c83d712e0fae0bbf59736a0`,
branche `feature/mvp-002-unified-interaction`. Livraison :
`release/mvp-003-android-candidate`. Les résultats MVP-002 restent historiques ;
les nouvelles exécutions sont dans VALIDATION.md et TEST-RESULTS.json.

## Périmètre vérifié

- `MvpApplication` compose un contrôleur historique unique, avec
  `ProcessInferenceAdmission.process`. Ni AppGraph historique ni second backend
  ne sont construits par `Mvp2Application`.
- `MvpRuntime` sérialise import, chargement, conversation et déchargement ;
  garde busy jusqu'au drainage du contrôleur. L'annulation invalide la réponse,
  sans attester un arrêt JNI. Modèle résident après génération/annulation.
- `InferenceController`, `LlamaLocalEngine`, `SessionCoordinator` : inchangés.
  Release inconnue/échouée implique quarantaine. Aucun reset ajouté.
- `MvpModelStore` : stockage privé noBackupFilesDir, taille exacte,
  SHA attendu, en-tête, réserve disque, copie bornée, renommage et AtomicFile.
  GGUF >100 Mio et <=512 Mio ; ancienne sélection préservée si échec.
  Un fichier validé peut rester orphelin après interruption avant sélection.
  L'en-tête ne prouve ni compatibilité native ni innocuité d'un GGUF malveillant.
- `CoreConversationAdapter` passe réellement par JarvisCore, SQLite et les
  mécanismes de prompt existants. Pas de réponse de remplacement fabriquée.
  SYSTEM distinct, budget conservateur ; aucun tokenizer exact ajouté.
- `LocalDatabase` : version2, migration1→2 sans mutation, autres versions
  refusées, insertions vérifiées et transaction documents/passages. Inchangé.
- `SettingsStore` : inchangé. SharedPreferences.apply reste asynchrone ;
  réouverture simulée ne démontre pas la durabilité face à coupure électrique.
- `Mvp2Speech` : voix déclarée française, locale et installée, lecture manuelle
  <=300 caractères après test/confirmation. Service Android externe au UID
  JARVIS : absence INTERNET de JARVIS ne suffit pas à qualifier ce service.
- Whisper : import GGML privé >50 Mio <=160 Mio, aucune activation micro,
  aucun recorder et aucune permission RECORD_AUDIO dans la candidate.
- Avatar statique ; NEXUS/SERVICES/EVOLUTION désactivés ; IDENTITY/Athéna,
  contrats Binder expérimentaux et profils vocaux non activés.

## Défaut confirmé et correction limitée

`Mvp2Speech.verifiedByUser` masquait temporairement la confirmation lorsque le
mode avion était faux, mais ne la révoquait pas. Si l'UI observait cet état puis
le mode avion redevenait vrai, l'ancienne confirmation et son ancienne phrase
pouvaient autoriser une lecture. Reproduit avant correction par
`observedAirplaneOffRevokesProbeAndProofPermanently` (1 test exécuté, 1 échec).
Désormais toute observation d'un prérequis faux ou illisible révoque les deux
indicateurs, y compris lors de la consultation du getter. Une nouvelle phrase
et une nouvelle confirmation sont requises. Le test et la suite sont rejoués.

Gravité : moyenne, validité d'une confirmation de confidentialité vocale.
Limite résiduelle : aucune surveillance réseau continue ni attestation du
service TTS ; un aller-retour du réglage jamais observé peut rester invisible.
Le Wi-Fi peut rester actif en mode avion. L'UI exige sa désactivation manuelle ;
ne pas présenter cela comme une preuve technique d'isolement réseau du TTS.

## Isolation de livraison

Nouvelle variante `mvp3` : package `com.stark.jarvislocal.mvp003`, label
« JARVIS MVP 003 diagnostic », version `3.1.0-mvp003-diagnostic`, minSdk24,
target35, ARM64 uniquement. Reprend les mêmes sources MVP/MVP002, affiche
explicitement les limites. Aucune migration de données entre packages,
aucun warmup ni import automatique. Signature de développement, debuggable :
candidate diagnostique, pas version de production durcie.

`tools/check-mvp-003.py` compare les fichiers suivis à la base exacte et
contrôle les six fichiers préexistants autorisés. Les sources main, JNI,
SQLite, fixtures, IDENTITY, anciens scripts/rapports et dépendances sont
préservés ; les connaissances sont seulement complétées.

## Risques non clos

1. **Bloquant qualification** : aucune génération JNI/GGUF Android réelle,
   aucun environnement Android exécutable fourni avec modèle qualifié.
2. **Bloquant qualification vocale** : audition TTS et Whisper non testés.
3. **Élevé, historique** : stop JNI non attesté ; un calcul peut continuer
   après annulation/événement thermique. Ne pas lever l'exclusivité prématurément.
4. **Moyen** : signature debug non stable entre environnements ; aucune mise à
   jour d'une APK existante ne doit être tentée avec certificat différent.
5. **Moyen** : absence d'export/restauration Android applicatif. Ne pas effacer
   stockage/package ; aucun retour arrière avec migration promis.
6. **À qualifier** : mémoire/latence, arrière-plan système, mort de processus,
   ABI réelle, taille de pages, API24/25 natives, service TTS et fournisseur SAF.
7. **Non audité exhaustivement** : vulnérabilités transitives des AAR natives.
   Hash inchangé = préservation, pas certification de sécurité du moteur.
