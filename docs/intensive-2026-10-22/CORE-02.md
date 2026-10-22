# CORE-02 — Bus d'événements utilisateur et façade de modules autonomes

## 1. Identification, source et priorité
**Mission** CORE-02 · **Domaine** CORE (hébergé dans CORE, pas de dépôt autonome) · **Dépôt** `bubth720/JAVIS-Local-v2`.
**Base code vérifiée** : `test/mvp-004-native-qualification` à `a6f13dd101990c6b5e3992be506c9a5b99873621` ; le modèle GGUF Qwen est vérifié sur octets depuis MVP-004C mais **JNI Android non qualifié**.
**Branche plan** `plan/intensive-2026-10-22` ; **branche implémentation proposée** `feat/core-02-intensive`.
**Priorité** P1 · **Dépendances** CORE-01,UNI-01 · **Statut** préparation, aucune exécution autorisée.

## 2. État initial et code déjà présent
Existant dans CORE : `JarvisCore.kt`, `SessionCoordinator.kt`, `InferenceController.kt`, `LocalDatabase.kt`, `PromptBuilder.kt`, `RagEngine.kt`, `LocalTts.kt`, contrats `identity/*`, UI diagnostique MVP001-003. Tests logiciels MVP004 rapportés 439/439, sans exécution JNI Android. Les couches IDENTITY et MEMORY ici proposées ne constituent pas de nouveaux moteurs ou de nouvelles APK : préserver les chemins et leurs migrations.

## 3. Objectif d'ingénierie
Installer un bus d'événements versionné pour rendre l'interface, la voix et l'avatar pilotables sans forcer l'intégration de leurs APK et sans créer de nouvelle source de vérité.

## 4. Fonctionnalités et scénarios concrets à développer
1. Créer CoreEvent sealed model : USER_INPUT, THINKING, RESPONSE_CHUNK/COMPLETED, TOOL_PENDING, SPEECH_REQUESTED, ERROR, CANCELLED.
2. Publier événements corrélés à session et requestId ; garantir ordre, déduplication, replay borné uniquement en mémoire.
3. Implémenter ModuleConnector<T> avec discover/connect/disconnect/health et désactivation par défaut.
4. Créer adaptateur de notification UI interne sur état de conversation réel CORE (pas de fausse génération).
5. Créer connecteurs inertes distinguant UNAVAILABLE vs DISABLED pour VOICE/INTERFACE/NEXUS/SERVICES.
6. Ajouter event journal local optionnel non personnel, métriques temps de réponse et signaux observables par EVOLUTION.

## 5. Design interne, interfaces et ownership
1. CoreEvent, EventEnvelope et EventContractVersion
2. CoreEventBus bounded hot stream
3. ModuleConnector/ConnectorRegistry et LifecycleOwner
4. UiStateProjection et DiagnosticsEventSink
CORE orchestre sessions/permissions, LLM reste unique moteur ; les ports constituent des interfaces locales optionnelles, pas des accès directs au stockage des autres modules.

## 6. Fichiers à créer ou éditer
1. Existants : app/src/main/java/com/stark/jarvislocal/{SessionCoordinator,InferenceContract,JarvisCore}.kt
2. À créer : app/src/main/java/com/stark/jarvislocal/events/{CoreEvent,CoreEventBus,ModuleConnector}.kt
3. À créer : app/src/main/java/com/stark/jarvislocal/events/UiStateProjection.kt et tests JVM
Les chemins app/src/main sont vérifiés. Les dossiers nouveaux sont des cibles **proposées** : établir le chemin exact des sourceSets `mvp`, `mvp2`, `mvp3` et la compatibilité Gradle avant toute écriture.

## 7. Plan de programmation
1. Geler événements utiles sans changer le contrat wire UNIFIED v1.
2. Coder publication de transitions réelles sur session existante, pas de doublon de scheduler.
3. Ajouter projections UI, métriques et connecteurs désactivés.
4. Simuler coupure d'abonné uniquement en test, observer persistance des états CORE réels.
5. Vérifier la non-régression des états d'erreur, cancellation et réserve de ressources.

## 8. Technologies et dépendances
Kotlin sealed classes, Flow/StateFlow, Android lifecycle ; no net permission.
Conserver versions Gradle/Android, JNI historique et chargeur GGUF. Aucun deuxième moteur natif, serveur, télémétrie ou dépendance cloud.

## 9. Connexions vers autres modules
INTERFACE consommera un flux stable ; VOICE un port de texte à parler après accord ; EVOLUTION des indicateurs techniques expurgés.
Respecter ADR-001, contrat UNIFIED v1, et les décisions D4-02D (fixtures uniquement) : pas de connexion Binder production implicite.

## 10. Compatibilité et sécurité
Aucun service exporté, permission Binder, voix privée ou NEXUS en production avant revue intégration.
Préserver admissions, quarantaine UNKNOWN/CLEANUP_FAILED, minSdk, budgets mémoire, batterie et thermique. Ne pas migrer, supprimer, désinstaller, resign/installer une APK historique ou enregistrer automatiquement des informations privées.

## 11. Livrables obligatoires
Nouveaux composants compilables et intégrés dans le module CORE existant, démonstration fonctionnelle de chemin utilisateur, tests minimaux réalistes (les mocks seulement pour frontière non disponible), rapport bref des changements, commits sur branche dédiée. Pas de livraison purement documentaire.

## 12. Critères d'acceptation
1. Événements sur parcours CORE réel présentent IDs et ordre stable.
2. Pause/reprise UI ne crée ni nouvelle génération ni doublons d'état.
3. Un connecteur non prêt conserve explicitement DISABLED/UNAVAILABLE et le chat existant fonctionne.
La réussite des tests hôtes ne constitue pas une validation JNI Android.

## 13. Ordre et parallélisme
CORE-01,UNI-01. Toute mission modifiant `JarvisCore`, `LocalDatabase` ou `PromptBuilder` doit travailler sur branche séquencée après intégration consciente des précédentes ; IDENTITY et MEMORY peuvent développer interfaces internes séparées lorsque leurs contrats sont figés.

## 14. Autorisations et Git
La présente fiche est déposée **sans autorisation d'exécution**. Aucune fusion vers main, aucun accès à un appareil personnel, aucune utilisation de voix privée, aucune mise en production de nouveau Binder. Exiger revue distincte pour JNI, signatures, permissions, migration de données ou contrat inter-applications.

## 15. Rapport obligatoire
Module ; mission ; branche ; commit base/final ; code/fichiers livrés ; tests réellement exécutés vs historiques ; erreurs ; fonctionnalités inactives ; régressions possibles ; blocages ; prochaine action.
