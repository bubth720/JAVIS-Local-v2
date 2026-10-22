# CORE-03 — Intégrateur local de connaissances et d'outils (ports opt-in)

## 1. Identification, source et priorité
**Mission** CORE-03 · **Domaine** CORE (hébergé dans CORE, pas de dépôt autonome) · **Dépôt** `bubth720/JAVIS-Local-v2`.
**Base code vérifiée** : `test/mvp-004-native-qualification` à `a6f13dd101990c6b5e3992be506c9a5b99873621` ; le modèle GGUF Qwen est vérifié sur octets depuis MVP-004C mais **JNI Android non qualifié**.
**Branche plan** `plan/intensive-2026-10-22` ; **branche implémentation proposée** `feat/core-03-intensive`.
**Priorité** P2 · **Dépendances** CORE-01,CORE-02,NEX-03,SERV-01,UNI-01 · **Statut** préparation, aucune exécution autorisée.

## 2. État initial et code déjà présent
Existant dans CORE : `JarvisCore.kt`, `SessionCoordinator.kt`, `InferenceController.kt`, `LocalDatabase.kt`, `PromptBuilder.kt`, `RagEngine.kt`, `LocalTts.kt`, contrats `identity/*`, UI diagnostique MVP001-003. Tests logiciels MVP004 rapportés 439/439, sans exécution JNI Android. Les couches IDENTITY et MEMORY ici proposées ne constituent pas de nouveaux moteurs ou de nouvelles APK : préserver les chemins et leurs migrations.

## 3. Objectif d'ingénierie
Faire dialoguer CORE avec les vraies capacités NEXUS et SERVICES à travers adaptateurs typés, en laboratoire uniquement tant que modèle et permissions ne sont pas qualifiés.

## 4. Fonctionnalités et scénarios concrets à développer
1. Créer Ports SourceSearchPort et ToolExecutionPort avec métadonnées de capacité, erreurs et identification du demandeur.
2. Construire ContextAssembler consommant les citations NEXUS, les budgets de contexte existants et PromptBuilder sans élever le texte source en instructions système.
3. Coder ToolDecisionGate exigeant une approbation humaine pour chaque action mutante ; journal sans secrets.
4. Implémenter outils lecture-only (recherche, extraction) réellement appelés via adaptateurs de bibliothèques ; laisser IPC APK désactivé.
5. Affichage utilisateur des citations/source/date et des décisions d'outil dans MVP sans casser conversation.
6. Fail closed si port absent, preuve source manquante ou moteur natif non qualifié ; sauvegarder un résultat structuré non trompeur.

## 5. Design interne, interfaces et ownership
1. SourceSearchPort + NexusSearchAdapter
2. ToolExecutionPort + ServicesToolAdapter
3. ContextAssembler et CitationBinding
4. ToolDecisionGate et ModuleCapabilityManager
CORE orchestre sessions/permissions, LLM reste unique moteur ; les ports constituent des interfaces locales optionnelles, pas des accès directs au stockage des autres modules.

## 6. Fichiers à créer ou éditer
1. Existants : app/src/main/java/com/stark/jarvislocal/{PromptBuilder,RagEngine,JarvisCore,InferenceController}.kt
2. À créer : app/src/main/java/com/stark/jarvislocal/integrations/{NexusSearchAdapter,ServicesToolAdapter,ContextAssembler}.kt
3. À créer : app/src/test/.../CoreIntegrationContractsTest.kt
Les chemins app/src/main sont vérifiés. Les dossiers nouveaux sont des cibles **proposées** : établir le chemin exact des sourceSets `mvp`, `mvp2`, `mvp3` et la compatibilité Gradle avant toute écriture.

## 7. Plan de programmation
1. Comparer APIs NEX-03, SERV-01 et contrats UNI-01 réellement livrés ; stopper si incompatibles.
2. Coder adaptateurs compile-only/local sans nouvelle APK propriétaire ni deuxième LLM.
3. Implémenter montage des citations et autorisations indépendamment du moteur.
4. Ajouter affichage opt-in des résultats réellement obtenus et échecs typés.
5. Tests sur vraies bibliothèques et fixtures, documenter précisément les gates laboratoire.

## 8. Technologies et dépendances
Kotlin, modules de bibliothèque locaux si dépendances Gradle alignées ; pas de téléchargement de poids.
Conserver versions Gradle/Android, JNI historique et chargeur GGUF. Aucun deuxième moteur natif, serveur, télémétrie ou dépendance cloud.

## 9. Connexions vers autres modules
NEXUS est propriétaire sources/index, SERVICES outils, CORE sessions/permissions ; aucune base copiée, aucun service Binder production.
Respecter ADR-001, contrat UNIFIED v1, et les décisions D4-02D (fixtures uniquement) : pas de connexion Binder production implicite.

## 10. Compatibilité et sécurité
Mission autorisable pour adapters locaux ; activer l'inférence JNI ou la liaison inter-APK nécessite gates distincts MVP-005/006.
Préserver admissions, quarantaine UNKNOWN/CLEANUP_FAILED, minSdk, budgets mémoire, batterie et thermique. Ne pas migrer, supprimer, désinstaller, resign/installer une APK historique ou enregistrer automatiquement des informations privées.

## 11. Livrables obligatoires
Nouveaux composants compilables et intégrés dans le module CORE existant, démonstration fonctionnelle de chemin utilisateur, tests minimaux réalistes (les mocks seulement pour frontière non disponible), rapport bref des changements, commits sur branche dédiée. Pas de livraison purement documentaire.

## 12. Critères d'acceptation
1. Recherche de corpus réel NEXUS restitue citations via CORE, quand dépendance livrée.
2. Un outil réel SERVICES non mutant s'exécute et l'action mutante attend un accord explicite.
3. Aucune seconde autorité sur LLM, stockage NEXUS ou souvenirs n'est introduite.
La réussite des tests hôtes ne constitue pas une validation JNI Android.

## 13. Ordre et parallélisme
CORE-01,CORE-02,NEX-03,SERV-01,UNI-01. Toute mission modifiant `JarvisCore`, `LocalDatabase` ou `PromptBuilder` doit travailler sur branche séquencée après intégration consciente des précédentes ; IDENTITY et MEMORY peuvent développer interfaces internes séparées lorsque leurs contrats sont figés.

## 14. Autorisations et Git
La présente fiche est déposée **sans autorisation d'exécution**. Aucune fusion vers main, aucun accès à un appareil personnel, aucune utilisation de voix privée, aucune mise en production de nouveau Binder. Exiger revue distincte pour JNI, signatures, permissions, migration de données ou contrat inter-applications.

## 15. Rapport obligatoire
Module ; mission ; branche ; commit base/final ; code/fichiers livrés ; tests réellement exécutés vs historiques ; erreurs ; fonctionnalités inactives ; régressions possibles ; blocages ; prochaine action.
