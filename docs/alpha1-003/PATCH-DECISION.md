# ALPHA1-003 — décision de gate avant phase B

**Phase A uniquement. Décision recommandée : NO_GO pour application du patch initial
en l'état. Phase B NON AUTORISÉE et NON COMMENCÉE.**

## Patch exact en attente

Fichier conservé : `docs/alpha1-002/PATCH-A-VALIDER.patch`.
SHA-256 : `94f06d0179b95c332f5842b73b69d103e09ca319befc319a0aeca341735c6df7`.
Les douze fichiers avant/après sont vérifiés et intégralement repris dans RESULTS.json.
Aucun octet du patch n'a été changé. Aucun autre patch candidat n'est implicitement
approuvé par ce rapport.

| Préfixe | Fichier sensible | Retour arrière futur |
|---|---|---|
| upstream | library/src/main/jni/llama_jni.cpp | Source exacte823d1de2, processus réellement terminé |
| upstream | library/src/main/jni/measured.inc | Retrait de ce seul ajout candidat, jamais reset de registre à chaud |
| upstream | library/src/main/kotlin/dev/ffmpegkit/llama/LlamaJNI.kt | Anciennes déclarations natives et AAR correspondante |
| upstream | library/src/main/kotlin/dev/ffmpegkit/llama/Llama.kt | Worker et API amont, après fin effective du propriétaire |
| upstream | library/src/main/kotlin/dev/ffmpegkit/llama/LlamaModel.kt | Constructeur/état amont cohérents avec l'AAR |
| upstream | library/src/main/kotlin/dev/ffmpegkit/llama/LlamaMeasured.kt | Retrait du seul wrapper candidat hors runtime |
| upstream | library/build.gradle.kts | Coordonnées amont dans la copie source ; aucune publication automatique |
| core | app/src/main/java/com/stark/jarvislocal/ExactLocalInference.kt | Retrait de l'ajout seulement dans copie de développement |
| core | app/src/main/java/com/stark/jarvislocal/ExactAndroidBackend.kt | Retrait de l'ajout, aucun changement de données |
| core | app/src/main/java/com/stark/jarvislocal/LlamaLocalEngine.kt | Base exacte, bail jamais remis à zéro à chaud |
| core | app/src/main/java/com/stark/jarvislocal/InferenceController.kt | Base exacte, drainage/quarantaine conservés |
| core | app/build.gradle.kts | Ancienne dépendance0.1.1 et hash contrôlés |

## Ce qui est établi

Sources/licences/empreintes vérifiées ; trois builds amont propres réussis.
Reproductibilité byte-identique sur même hôte au même chemin, **non obtenue entre
répertoires**. Classes.jar identique au Maven, binaires natifs compilés différents.
Pas de qualification du candidat ni d'équivalence native avec l'AAR publiée.
Le propriétaire proposé de l'expérimentation est le mainteneur CORE sous validation
utilisateur, pas un nouvel APK autonome autorisé à contourner l'admission.

## Modifications à proposer avant toute autorisation d'application

1. Revoir R01–R03 : destination privée explicitement détenue, copie bornée,
   transfert du handle résistant à l'annulation et nettoyage sûr/quarantaine.
2. Séparer un patch bibliothèque seule (sept fichiers upstream) du raccordement CORE
   (cinq fichiers core). Une première construction ne doit pas changer la production.
   Cette séparation produira une **nouvelle empreinte à valider**, pas une autorisation
   de filtrer opportunément le composite avec `git apply --include`.
3. Définir le traitement des chemins/build-id pour une fabrication reproductible ;
   ne pas modifier silencieusement les flags de compilation de la référence.
4. Préciser release/nettoyage du snapshot, appels UI et interdiction de réentrée,
   puis qualifier template/encodage/IDs sur un modèle séparément autorisé.
5. Préserver anciennes méthodes load/complete/release ; rejouer leur comportement
   réel avec le nouvel artefact, pas seulement les299 tests de l'ancien.

Les corrections ci-dessus sont des exigences de revue, **pas des sources natives
modifiées**. L'utilisateur peut demander une révision documentaire du diff, puis
valider son hash et périmètre exacts dans une autorisation supplémentaire.

## Conditions et tests de phase B

Accord explicite sur patch/version/périmètre, responsabilité de l'AAR, construction
isolée sans dépendance de production remplacée. Tests de TEST-PLAN.md : vrais IDs
prepare/prefill, unicité modèle/worker/bail, absence decode en prepare, annulation,
concurrence, dépassement de contexte et toutes erreurs de nettoyage.
Sans GGUF autorisé : compilation/ABI seulement, aucune réussite de génération déclarée.
Sans laboratoire Android autorisé : pas d'installation ou validation matérielle.
F3/CORE004D/Binder/2 restent des gates séparés, toujours fermés.

## Retour arrière

Aujourd'hui : rien à restaurer dans l'application ; les167 fichiers de base sont
intacts. Les AAR amont générées sont conservées hors Git et jamais installées.
Les preuves textuelles peuvent rester dans leur branche sans changer le runtime.

Après une future expérimentation autorisée : arrêter et drainer le vrai propriétaire.
Une inconnue/erreur de release conserve le bail/quarantaine ; aucun reset à chaud.
Revenir aux sources/AAR antérieures dans un processus de développement neuf après
fin réellement établie, sans prétendre que ce redémarrage réconcilie un autre APK.
Supprimer seulement les snapshots dont la propriété et la fin d'usage sont connues.
Ne jamais supprimer le GGUF original, SQLite, préférences, profils, journaux /2 ou
répertoires modèles globaux. Aucune migration, installation personnelle ou fusion.

**Décision utilisateur attendue : autoriser/refuser une révision du diff et son
périmètre avant une nouvelle revue. Aucune demande ALPHA1-003 ne vaut approbation JNI.**
