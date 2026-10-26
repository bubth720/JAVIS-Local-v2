Source : https://github.com/bubth720/JAVIS-Local-v2/issues/14
Révision : 2026-10-23T22:42:48Z

# Mission CORE-03A — Brancher en processus NEXUS + SERVICES au vrai SessionCoordinator Android API24

> **Préparation uniquement. Ne pas exécuter cette mission tant que les ports dépendants ne sont pas réellement livrés.**

## Faits vérifiés
- Base CORE : `feat/core-03-intensive` au commit `aea5ac859ac551d54fe16187c1883a487dbee9d6` ; vrai laboratoire `integration-lab/` Linux/JVM 10 tests, citations NEXUS et actions SERVICES (CSV/DOCX) réussis.
- Parent CORE MEM-02 : `49ad428e94edd75c2a1d115c8c2504f26609eace`, 501 tests Robolectric dans sa livraison, **non rejoués par CORE-03**.
- Dépendance **bloquante** NEXUS : [NEX-06A](https://github.com/bubth720/JARVIS-NEXUS/issues/1) (port API24 SQLite/index/citations).
- Dépendance **bloquante** SERVICES : [SERV-04A](https://github.com/bubth720/JARVIS-SERVICES/issues/1) (port outils/API24).
- CORE minSdk24 ; pas d'intégration de `java.sql`/sqlite-jdbc desktop ou `java.nio.file`/minSdk26 non compatible.

## Objectif code
Remplacer le statut laboratoire-only de CORE-03 par une vraie composition opt-in Android **in-process**, qui réutilise le `SessionCoordinator` historique, les ports existants et les refus de permissions. Livrer de vraies recherches citées et des outils sûrs depuis l'application diagnostique, sans créer une seconde autorité ou charger un second LLM.

## Programme d'implémentation
1. Lire les commits réels de NEX-06A et SERV-04A. Si port absent ou incompatible : **BLOQUÉ** ; ne pas produire de fausse passerelle Android, ne pas copier JDBC/JVM dans APK.
2. Créer un adaptateur Android `NexusSearchPort` vers la bibliothèque Android NEXUS, et `ServicesToolPort` vers la bibliothèque Android SERVICES. Utiliser exactement les sémantiques vérifiées par `integration-lab/` (citation résolue, UUID/session, revision, cancellation, UNKNOWN, approbation).
3. Injecter les ports via AppGraph/DI sans ajouter de processus, service exporté, permission INTERNET ou Binder ; les connecteurs restent DISABLED/UNAVAILABLE jusqu'à activation délibérée par l'utilisateur dans la **variante de laboratoire**.
4. Exiger `SessionCoordinator` comme **unique propriétaire** des opérations et du gate. Toute réponse arrivée après annulation/délai/session révoquée est rejetée ; ne pas prouver l'unload natif à partir du retour des tools.
5. Faire une UI diagnostique simple : sélectionner une source locale synthétique (SAF), importer/indexer, requêter, voir citation/page/URI/provenance puis lancer CSV lecture seule ; action DOCX exige consentement exact. Les fonctions indisponibles sur API24 montrent refus explicite.
6. **Ne pas consommer** encore ContextBundle dans une génération JNI tant que MVP-005 et le compteur/tokenizer natif ne sont pas qualifiés ; afficher `NATIVE_INFERENCE_UNQUALIFIED`. IDENTITY et mémoire restent sous leurs règles de confirmation.
7. Préserver schéma SQLite historique, réglages, anciens APK packages/signatures, admission, budgets, identité, souvenirs, protections thermique/batterie ; aucune donnée privée importée automatiquement.
8. Lancer minimum tests JVM/Robolectric sur API24/25/26/35, compilation ARM64 APK, tests instrumentés en laboratoire si possible. Réexécuter régression ancienne sur base nouvelle, ne pas comptabiliser les tests parent comme réexécutés.
9. Rapports de provenance (SHA des librairies NEXUS/SERVICES embarquées et fichiers de sources), états réellement fonctionnels, tests mesurés et points bloquants. Les tests non exécutés doivent rester explicites.

## Architecture et fichiers à examiner
- Existants : `integration-lab/src/main/kotlin/com/stark/jarvislocal/integrations/{CoreIntegrationLab,NexusSearchAdapter,ServicesToolAdapter}.kt`, `app/src/main/java/com/stark/jarvislocal/{AppGraph,JarvisCore,SessionCoordinator}.kt`, `app/src/main/java/com/stark/jarvislocal/events/{ModuleConnector,CoreEventBus}.kt`, `app/src/mvp2/`.
- Cibles proposées à créer : `app/src/main/java/com/stark/jarvislocal/integrations/{AndroidNexusSearchAdapter,AndroidServicesToolAdapter,CoreIntegrationCoordinator}.kt`, tests JVM/Android, `docs/core-03a/`.
- Vérifier les chemins avant écriture, ne pas réécrire l'historique.

## Acceptation minimale
- Sur un runtime Android API24 compatible : importer une source synthétique, retrouver une citation NEXUS réellement résoluble et traiter une transformation CSV SERVICES via la **véritable session CORE** avec permissions/révocation et état d'erreur corrects.
- Pas de production Binder/LLM/JNI implicite, pas de second moteur, refus normal si ports absents, API non supportée ou état inconnu.
- Aucune fusion `main` ; pas de déploiement sur POCO/Redmi sans accord distinct, aucun changement natif, de sécurité ou de signature non approuvé.

## Restitution Codex
Module, mission, branche, commit source/final, code, scénarios Android vraiment exécutés, tests, source ABI et minSdk, blocages et prochaine mission.

**Ne pas lancer l'exécution simplement parce que cette issue est ouverte.**
