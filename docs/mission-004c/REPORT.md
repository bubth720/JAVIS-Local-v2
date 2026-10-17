# CORE 004C — livraison expérimentale avec admission bloquée

## Références vérifiées

- Dépôt modifié : `bubth720/JAVIS-Local-v2` uniquement.
- Base : `mission-004b-core-backend-lifecycle`, `29646448c80568d868a702192df761fc9d7a896d`.
- Livraison : `fixture/004c-p2-client`. Voir l'historique Git pour le commit contenant ce rapport.
- UNIFIED D4-02C lu : `0dbb978994994fa3210124f1e150ca23426d410d`.
- LLM 003E publié, lu sans modifier ses sources : `5751646acd4a7bb13caaea4b96a4ab3a68ec6adb`, branche `fixture/003e-p2-secure-endpoint`.
- Contrat : `jarvis.fixture.p2/1`, SHA-256 `fab6a66a6b99302884849cd1d8aef97cd761082dc20680a4b036d7bc99d5c3c9`.

Les textes historiques et les résultats publiés dans LLM ne sont pas des validations exécutées ici. L'empreinte du bundle canonique et son vecteur de digest ont été vérifiés dans cette session.

## État réel de livraison

La fixture compile et ses tests isolés passent. Elle contient le client Binder explicite, le service arbitre protégé, le mapping des requêtes/reçus et une machine de conservation des baux. L'Activity permet seulement une inspection d'identité et un hello, sans génération. **Aucune génération native interprocessus démontrée ; intégration non opérationnelle.**

Deux blocages indépendants :

1. Aucun émulateur Android ni appareil de laboratoire disponible, aucun modèle réel vérifié provisionné pour cet essai, aucune clé commune durable fournie. Les APK de fixture sont donc compilés non signés ; aucune installation effectuée.
2. Le contrat 003E ne fournit pas de reçu initial/reprise attestant la quiescence de tous les participants avec un défi frais. `hello` confirme des capacités, pas `effective_end`/`model_unloaded`. `status` exige la clé d'une opération active ; après redémarrage LLM, cette opération n'est plus en mémoire. Le serveur conserve son journal UNKNOWN et n'expose aucun reset. Il serait incorrect de fabriquer une preuve locale à partir de hello, d'une mort Binder ou de l'absence d'un ancien fichier. L'arbitre de production démarre donc RECOVERY_REQUIRED, même au premier lancement. Aucun bouton ne le déverrouille. Les preuves injectées des tests ne sont pas empaquetées comme fournisseur de production.

Une décision de protocole supplémentaire est nécessaire pour l'enrôlement initial et la réconciliation authentifiée. Cette mission ne modifie pas LLM ou UNIFIED pour la forcer. Le code d'orchestration `P2Session` est exercé avec un endpoint simulé et un arbitre réconcilié par preuves synthétiques ; il n'est pas branché à un bouton de génération tant que ce blocage demeure.

## Audit et conservation du CORE

`AppGraph` crée toujours `InferenceController(InferenceEngineFactory(historical = { LlamaLocalEngine(models) }))` ; aucun changement à `app/`. `JarvisCore` conserve commandes, mémoire, RAG, prompts et persistance ; `PromptBuilder` conserve son budget et ses sources ; `SessionCoordinator` conserve single-flight, invalidation et drainage ; `ProcessInferenceAdmission` conserve le verrou de processus 004B. Aucun moteur historique, SQLite, préférence, fichier utilisateur, modèle, voix ou permission de l'APK historique n'est modifié.

La fixture n'utilise pas `JarvisApplication`/`AppGraph`, ne contient ni moteur lourd ni accès aux données du CORE historique. Elle ne coordonne donc pas une application historique ouverte séparément, Whisper ou un autre participant non enrôlé. Le pilote de production n'admet que l'identité `llm`, jamais le moteur historique. Les tests de retour historique vérifient une politique d'admission simulée, pas un basculement réel entre APK.

L'opt-in Gradle est la seule modification de configuration historique. Sans `-PincludeP2Fixture=true`, seul `:app` est inclus. Le SDK minimum historique reste 24 ; la fixture est API29+, ARM64 requis avant liaison. Aucune augmentation générale de minSdk, dépendance native ou workflow de publication modifié.

## Fichiers et responsabilités

| Fichier | Responsabilité |
|---|---|
| `settings.gradle.kts` | Inclusion opt-in de la fixture |
| `fixtures/p2-client/build.gradle.kts` | APK indépendant, sources de contrat CORE générées, AIDL, signature dédiée optionnelle |
| `CONTRACT.txt`, `src/main/aidl/…`, `REFERENCE.json` | Copies canoniques exactes et provenance LLM 003E |
| `Wire.kt` | Sous-ensemble portable du codec canonique, sans dépendance au domaine LLM |
| `P2Mapping.kt`, `PilotRequestGate.kt` | Requête P2, refus de contexte réel, budget, identifiants et validation des reçus |
| `PilotArbiter.kt`, `AtomicPilotJournal.kt` | Bail exclusif durable, récupération bloquante et journal technique atomique |
| `PilotRuntime.kt` | Composition de production bloquée, autorisations exactes éphémères liées à une réservation durable |
| `fr/jarvis/fixture/core/P2ArbiterService.kt` | Endpoint d'autorisation Binder authentifié avant désérialisation |
| `BinderIdentityChecks.kt` | UID réel, package unique, certificat unique sans rotation, permission signature et service explicite |
| `P2Session.kt` | Submit unique, demande d'arrêt, polling corrélé, pas de fallback/rejeu |
| `FixtureActivity.kt` | Inspection et hello hors du thread UI ; génération désactivée |
| `src/main/res/…`, manifeste | UI minimale, exclusion de sauvegarde/transfert, aucune permission réseau |
| `src/test/…` | Tests synthétiques JVM et Android simulé Robolectric |
| `tools/check-004c.py` | Contrôle hors ligne des copies canoniques et absence de changement du CORE |
| `AGENTS.md`, `docs/PROJECT_KNOWLEDGE.md`, `docs/mission-004c/…` | Instructions actualisées, limites et guide de qualification |

## Risques et prochaine action

- Priorité bloquante : définir dans un futur contrat une attestation d'état initial/récupération avec identité, epoch et défi frais. Aucun reset ni suppression de journal comme contournement.
- Puis provisionner une paire de fixtures signées par une clé commune dédiée, hors Git et indépendante des applications historiques ; fournir un Android ARM64 API29+ de laboratoire et un modèle public vérifié, sans téléchargement automatique.
- Exécuter la matrice physique et Binder ; compléter la validation JNI, sécurité, mémoire, thermique et batterie avant d'activer la génération dans l'Activity.
- Le journal AtomicFile avec fsync et checksum protège les interruptions et corruptions ordinaires ; ce n'est ni un chiffrement, ni une preuve cryptographique de libération, ni une défense contre un système compromis. Aucune réponse ou prompt n'y figure.
- Une saturation du thread Binder ou un service muet peut maintenir une liaison bloquée. Aucune interruption de thread ne sera présentée comme fin native ; conserver la quarantaine. La résistance à cette panne exige une qualification Android réelle.
- L'arbitre n'est pas global à Android et ne promet pas d'exclusivité face aux applications non participantes. Aucune protection RAM/batterie/température n'est abaissée.

Aucune fusion, aucun déploiement personnel, aucune modification de LLM ou d'UNIFIED. Livraison pour revue et résolution des blocages, pas pour activation sur appareil personnel.
