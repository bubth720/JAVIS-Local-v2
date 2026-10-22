# MEM-01 — Registre de souvenirs explicitement confirmés et consentements

## 1. Identification, source et priorité
**Mission** MEM-01 · **Domaine** MEMORY (hébergé dans CORE, pas de dépôt autonome) · **Dépôt** `bubth720/JAVIS-Local-v2`.
**Base code vérifiée** : `test/mvp-004-native-qualification` à `a6f13dd101990c6b5e3992be506c9a5b99873621` ; le modèle GGUF Qwen est vérifié sur octets depuis MVP-004C mais **JNI Android non qualifié**.
**Branche plan** `plan/intensive-2026-10-22` ; **branche implémentation proposée** `feat/mem-01-intensive`.
**Priorité** P1 · **Dépendances** CORE-01 · **Statut** préparation, aucune exécution autorisée.

## 2. État initial et code déjà présent
Existant dans CORE : `JarvisCore.kt`, `SessionCoordinator.kt`, `InferenceController.kt`, `LocalDatabase.kt`, `PromptBuilder.kt`, `RagEngine.kt`, `LocalTts.kt`, contrats `identity/*`, UI diagnostique MVP001-003. Tests logiciels MVP004 rapportés 439/439, sans exécution JNI Android. Les couches IDENTITY et MEMORY ici proposées ne constituent pas de nouveaux moteurs ou de nouvelles APK : préserver les chemins et leurs migrations.

## 3. Objectif d'ingénierie
Structurer et enrichir la mémoire personnelle actuellement élémentaire de CORE, sans la déplacer implicitement vers NEXUS ou SERVICES.

## 4. Fonctionnalités et scénarios concrets à développer
1. Réutiliser SQLite et l'API de souvenirs confirmés des MVP, ajouter MemoryRecord versionné, scope, source, échéance, consentReceipt et état.
2. Créer PendingMemoryCandidate que l'utilisateur peut approuver, corriger ou refuser ; aucune écriture sans événement positif.
3. Implémenter opérations liste/édition/suppression, effet immédiat sur la recherche, cache invalidation et journal local de modifications.
4. Définir priorité de source et politique de conflit quand deux préférences s'opposent, toujours demander clarification plutôt qu'élire arbitrairement.
5. Exposer MemoryLookupPort avec projection de données non sensibles pour CORE/IDENTITY, aucun accès au catalogue complet par outil.
6. Ajouter écran minimal opt-in de confirmation/édition, et parcours tests de reprise/retour arrière.

## 5. Design interne, interfaces et ownership
1. MemoryRecord/MemoryCandidate/ConsentReceipt
2. ConfirmedMemoryStore adaptant SQLite existant
3. MemoryApprovalWorkflow, ConflictDetector
4. MemoryLookupPort, ProjectionPolicy
CORE orchestre sessions/permissions, LLM reste unique moteur ; les ports constituent des interfaces locales optionnelles, pas des accès directs au stockage des autres modules.

## 6. Fichiers à créer ou éditer
1. Existants : app/src/main/java/com/stark/jarvislocal/{LocalDatabase,Models,JarvisCore}.kt et sources MVP2 de mémoire
2. À créer : app/src/main/java/com/stark/jarvislocal/memory/{ConfirmedMemoryStore,MemoryApprovalWorkflow,MemoryLookupPort}.kt
3. À créer : app/src/test/.../ConfirmedMemoryWorkflowTest.kt
Les chemins app/src/main sont vérifiés. Les dossiers nouveaux sont des cibles **proposées** : établir le chemin exact des sourceSets `mvp`, `mvp2`, `mvp3` et la compatibilité Gradle avant toute écriture.

## 7. Plan de programmation
1. Cartographier tables et API existantes ; écrire migration additive sans supprimer les données.
2. Implémenter distinction candidat/confirmé, preuve de consentement et invalidation.
3. Coder consultation par session et affichage de source, version et oubli.
4. Ajouter mécanisme de conflit et projection strictement limitée.
5. Prouver sur base réelle de test ajout/édition/refus/suppression/réouverture.

## 8. Technologies et dépendances
Kotlin/SQLite existants, tests Robolectric/JVM ; ne pas ajouter un autre SQLite par module.
Conserver versions Gradle/Android, JNI historique et chargeur GGUF. Aucun deuxième moteur natif, serveur, télémétrie ou dépendance cloud.

## 9. Connexions vers autres modules
CORE reste autorité des souvenirs personnels ; NEXUS stocke documents/archives, SERVICES conserve ses propres données diagnostiques sans duplication.
Respecter ADR-001, contrat UNIFIED v1, et les décisions D4-02D (fixtures uniquement) : pas de connexion Binder production implicite.

## 10. Compatibilité et sécurité
Toute migration de données personnelles existantes doit être proposée/revue séparément.
Préserver admissions, quarantaine UNKNOWN/CLEANUP_FAILED, minSdk, budgets mémoire, batterie et thermique. Ne pas migrer, supprimer, désinstaller, resign/installer une APK historique ou enregistrer automatiquement des informations privées.

## 11. Livrables obligatoires
Nouveaux composants compilables et intégrés dans le module CORE existant, démonstration fonctionnelle de chemin utilisateur, tests minimaux réalistes (les mocks seulement pour frontière non disponible), rapport bref des changements, commits sur branche dédiée. Pas de livraison purement documentaire.

## 12. Critères d'acceptation
1. Seuls les candidats acceptés apparaissent en mémoire persistante.
2. Effacement d'un souvenir est effectif après réouverture sans effacer l'historique de chat.
3. Aucune fuite de mémoire dans les événements CORE ou les tools sans droit.
La réussite des tests hôtes ne constitue pas une validation JNI Android.

## 13. Ordre et parallélisme
CORE-01. Toute mission modifiant `JarvisCore`, `LocalDatabase` ou `PromptBuilder` doit travailler sur branche séquencée après intégration consciente des précédentes ; IDENTITY et MEMORY peuvent développer interfaces internes séparées lorsque leurs contrats sont figés.

## 14. Autorisations et Git
La présente fiche est déposée **sans autorisation d'exécution**. Aucune fusion vers main, aucun accès à un appareil personnel, aucune utilisation de voix privée, aucune mise en production de nouveau Binder. Exiger revue distincte pour JNI, signatures, permissions, migration de données ou contrat inter-applications.

## 15. Rapport obligatoire
Module ; mission ; branche ; commit base/final ; code/fichiers livrés ; tests réellement exécutés vs historiques ; erreurs ; fonctionnalités inactives ; régressions possibles ; blocages ; prochaine action.
