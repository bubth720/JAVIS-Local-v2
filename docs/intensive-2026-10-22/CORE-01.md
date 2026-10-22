# CORE-01 — Orchestrateur d'intentions et gestionnaire de tâches locales

## 1. Identification, source et priorité
**Mission** CORE-01 · **Domaine** CORE (hébergé dans CORE, pas de dépôt autonome) · **Dépôt** `bubth720/JAVIS-Local-v2`.
**Base code vérifiée** : `test/mvp-004-native-qualification` à `a6f13dd101990c6b5e3992be506c9a5b99873621` ; le modèle GGUF Qwen est vérifié sur octets depuis MVP-004C mais **JNI Android non qualifié**.
**Branche plan** `plan/intensive-2026-10-22` ; **branche implémentation proposée** `feat/core-01-intensive`.
**Priorité** P0 · **Dépendances** Aucune · **Statut** préparation, aucune exécution autorisée.

## 2. État initial et code déjà présent
Existant dans CORE : `JarvisCore.kt`, `SessionCoordinator.kt`, `InferenceController.kt`, `LocalDatabase.kt`, `PromptBuilder.kt`, `RagEngine.kt`, `LocalTts.kt`, contrats `identity/*`, UI diagnostique MVP001-003. Tests logiciels MVP004 rapportés 439/439, sans exécution JNI Android. Les couches IDENTITY et MEMORY ici proposées ne constituent pas de nouveaux moteurs ou de nouvelles APK : préserver les chemins et leurs migrations.

## 3. Objectif d'ingénierie
Compléter le CORE actuel par un routeur local d'intentions et de tâches qui choisit entre conversation, recherche, outil, diagnostic ou clarification sans inventer une capacité absente.

## 4. Fonctionnalités et scénarios concrets à développer
1. Créer IntentEnvelope versionnée avec requestId, origine, catégorie, contexte explicitement fourni, consentement et deadline.
2. Créer IntentRouter déterministe et StrategyRegistry capable de sélectionner une capacité locale par identifiant, sans heuristique LLM cachée.
3. Ajouter TaskCoordinator exclusif par session : états, annulation logique, quotas, reprises et refus des appels concurrents.
4. Coder CapabilityUnavailable et UserConfirmationRequired comme réponses normales plutôt que prétendre utiliser un service simulé.
5. Brancher les opérations CORE existantes (chat local, consultation historique SQLite, recherche documentaire) sans charger d'autre moteur.
6. Fournir parcours CLI/JVM de 4 commandes réelles et écrans d'états opt-in pour MVP.

## 5. Design interne, interfaces et ownership
1. IntentEnvelope/IntentResult/CapabilityDescriptor
2. IntentRouter, StrategyRegistry et LocalCapabilityAdapter
3. TaskCoordinator, TaskStateStore et RequestGate
4. MvpIntentFacade inactive par défaut
CORE orchestre sessions/permissions, LLM reste unique moteur ; les ports constituent des interfaces locales optionnelles, pas des accès directs au stockage des autres modules.

## 6. Fichiers à créer ou éditer
1. Existants : app/src/main/java/com/stark/jarvislocal/{JarvisCore,AppGraph,SessionCoordinator,InferenceController}.kt
2. Existants MVP : app/src/mvp... et app/src/mvp2... (localiser chemins exacts avant édition)
3. À créer : app/src/main/java/com/stark/jarvislocal/orchestration/{IntentRouter,TaskCoordinator,CapabilityRegistry}.kt
4. À créer : app/src/test/.../CoreIntentRouterTest.kt
Les chemins app/src/main sont vérifiés. Les dossiers nouveaux sont des cibles **proposées** : établir le chemin exact des sourceSets `mvp`, `mvp2`, `mvp3` et la compatibilité Gradle avant toute écriture.

## 7. Plan de programmation
1. Lire gardes MVP et types de session existants ; réutiliser IDs/gates au lieu d'un second admission controller.
2. Définir schémas Kotlin et adapter les commandes locales sans toucher à JNI.
3. Implémenter registre + routeur statique avec priorité et refus fail-closed.
4. Brancher opérations SQLite, historique et document existantes à travers ports.
5. Créer écran/test de démonstration opt-in et exécuter tests d'exclusivité et de refus.

## 8. Technologies et dépendances
Kotlin Android minSdk historique, coroutines/Flow déjà utilisés ; aucun moteur ni permission nouveau.
Conserver versions Gradle/Android, JNI historique et chargeur GGUF. Aucun deuxième moteur natif, serveur, télémétrie ou dépendance cloud.

## 9. Connexions vers autres modules
Préparer ports ToolInvocation de SERVICES et RagContextPort NEXUS ; pas d'IPC ou de Binder production activé.
Respecter ADR-001, contrat UNIFIED v1, et les décisions D4-02D (fixtures uniquement) : pas de connexion Binder production implicite.

## 10. Compatibilité et sécurité
Ne pas modifier la gestion native de charge/unload, budgets, permissions ou contrats IPC sans autorisation séparée.
Préserver admissions, quarantaine UNKNOWN/CLEANUP_FAILED, minSdk, budgets mémoire, batterie et thermique. Ne pas migrer, supprimer, désinstaller, resign/installer une APK historique ou enregistrer automatiquement des informations privées.

## 11. Livrables obligatoires
Nouveaux composants compilables et intégrés dans le module CORE existant, démonstration fonctionnelle de chemin utilisateur, tests minimaux réalistes (les mocks seulement pour frontière non disponible), rapport bref des changements, commits sur branche dédiée. Pas de livraison purement documentaire.

## 12. Critères d'acceptation
1. Au moins trois capacités CORE réelles passent par le routeur et retournent des résultats traçables.
2. Un outil inconnu et une double demande simultanée sont refusés sans charger un second moteur.
3. L'application MVP historique reste compilable et les routes nouvelles demeurent opt-in.
La réussite des tests hôtes ne constitue pas une validation JNI Android.

## 13. Ordre et parallélisme
Aucune. Toute mission modifiant `JarvisCore`, `LocalDatabase` ou `PromptBuilder` doit travailler sur branche séquencée après intégration consciente des précédentes ; IDENTITY et MEMORY peuvent développer interfaces internes séparées lorsque leurs contrats sont figés.

## 14. Autorisations et Git
La présente fiche est déposée **sans autorisation d'exécution**. Aucune fusion vers main, aucun accès à un appareil personnel, aucune utilisation de voix privée, aucune mise en production de nouveau Binder. Exiger revue distincte pour JNI, signatures, permissions, migration de données ou contrat inter-applications.

## 15. Rapport obligatoire
Module ; mission ; branche ; commit base/final ; code/fichiers livrés ; tests réellement exécutés vs historiques ; erreurs ; fonctionnalités inactives ; régressions possibles ; blocages ; prochaine action.
