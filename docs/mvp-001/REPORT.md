# JARVIS CORE — MVP-001

Branche : `feature/mvp-001-local-conversation`.
Base Preview distante vérifiée : `86ea8bfa94deea0bab94914cd7214bc395a83f9b`.
Commit applicatif livré : voir APK-INFO.json du kit ; dernier commit de livraison
par `git log -1 --format=%H -- docs/mvp-001/REPORT.md` et compte rendu utilisateur.

Le MVP est une application expérimentale séparée, compilée, avec import manuel
et appel au moteur natif historique. **Une conversation GGUF réelle n'est pas
validée dans cet environnement.** Aucun résultat de test simulé ne sert de preuve JNI.

## Modifications

- app/build.gradle.kts : build type mvp, package `.mvp001`, signature debug et identifiant
  d'application distinct, minSdk24/ARM64 inchangés. Pas de dépendance modifiée.
- LlamaLocalEngine.kt : constructeur additionnel de chemin/validateur ; mêmes
  backend0.1.1 et ProcessInferenceAdmission, sans modifier les chemins historiques.
- src/mvp : application, Activity, runtime app-owned, store GGUF et politique
  ressources. Launcher historique désactivé uniquement dans cette nouvelle variante.
- testMvp :72 nouveaux cas JVM/Robolectric, avec fixtures synthétiques.
- Instructions/connaissances : compléments seulement. Nouveau garde de préservation,
  outil de kit APK et workflow Actions sans poids ; rapports dans ce dossier.

ModelManager, AppGraph, InferenceController, SessionCoordinator, PreviewActivity,
SQLite, prompts historiques, contrats/fixtures Binder, IDENTITY et voix sont
inchangés. Leurs responsabilités ont été auditées ; les modifier aurait élargi
inutilement ce parcours expérimental et mélangé import/asset ou voix.

## Parcours implémenté

Sélection SAF locale → copie privée bornée → en-tête/taille/SHA attendu → fichier
content-addressé atomique → manifeste AtomicFile → chargement explicite via
InferenceController/LlamaLocalEngine → SYSTEM distinct + question/historique
bornés → Llama.complete natif → réponse corrélée affichée en RAM.
Pas de réponses préenregistrées, commande rapide ni téléchargement dans ce parcours.
Les provenance/licence restent déclaratives ; le GGUF doit être fiable et compatible.

Le runtime refuse une seconde opération, un import tant que le modèle est résident,
et tout changement après release inconnue/échouée. Annulation, timeout et signaux
ressources invalident la sortie et attendent le retour natif ; ils ne libèrent
pas la réservation par anticipation. Le moteur historique ne sait pas garantir
un arrêt natif immédiat. Les autres applications ne sont pas arbitrées par ce
verrou du processus MVP ; l'essai physique devra fermer leurs moteurs au préalable.

La conversation est volatile, le SYSTEM est dans les seules préférences MVP,
les modèles validés sont conservés dans son noBackupFilesDir. Aucune migration,
suppression d'ancienne génération ou modification de données personnelles.

## Vérifications et livraison

**383 tests réussis**,0 échec/erreur/ignoré :311 historiques/Preview/fixture et72 MVP.
Builds Android debug/Preview/MVP/fixture et Lint réussis, avertissements conservés.
Les4 anciens gardes refusent leur périmètre historique ; le garde MVP passe.
Voir VALIDATION.md, TEST-RESULTS.json, PRESERVATION.json et LEGACY-GUARDS.json.

Le kit contient APK, empreintes/certificat, permissions/manifeste et documentation.
Le binaire n'est pas ajouté à Git ni installé ; la branche publie le code/preuves,
le workflow dédié prépare un artefact téléchargeable si la CI réussit. Le lien
local du kit et l'état réellement observé sont fournis dans le bilan utilisateur.

## Blocages et prochaine action

Aucun environnement Android ARM64 ni GGUF qualifié fourni : JNI/GGUF réel **non
exécuté/non validé**. Pas de preuve thermique/mémoire native, d'arbitrage inter-APK,
de durabilité sous coupure électrique ou de compatibilité exhaustive des GGUF.
Voir AUDIT.md pour les risques, GUIDE.md pour la qualification future.

Prochaine action recommandée : revue du code et du kit, puis accord explicite sur
un environnement Android de laboratoire et un GGUF public vérifié, sans installation
personnelle implicite. Aucun raccordement JNI expérimental, Binder004D, Athéna ou
fusion main n'est engagé par cette livraison.
