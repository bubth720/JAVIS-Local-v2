# MEM-02 — Rappel contextuel borné et export volontaire des souvenirs

## 1. Identification, source et priorité
**Mission** MEM-02 · **Domaine** MEMORY (hébergé dans CORE, pas de dépôt autonome) · **Dépôt** `bubth720/JAVIS-Local-v2`.
**Base code vérifiée** : `test/mvp-004-native-qualification` à `a6f13dd101990c6b5e3992be506c9a5b99873621` ; le modèle GGUF Qwen est vérifié sur octets depuis MVP-004C mais **JNI Android non qualifié**.
**Branche plan** `plan/intensive-2026-10-22` ; **branche implémentation proposée** `feat/mem-02-intensive`.
**Priorité** P2 · **Dépendances** MEM-01,NEX-03 · **Statut** préparation, aucune exécution autorisée.

## 2. État initial et code déjà présent
Existant dans CORE : `JarvisCore.kt`, `SessionCoordinator.kt`, `InferenceController.kt`, `LocalDatabase.kt`, `PromptBuilder.kt`, `RagEngine.kt`, `LocalTts.kt`, contrats `identity/*`, UI diagnostique MVP001-003. Tests logiciels MVP004 rapportés 439/439, sans exécution JNI Android. Les couches IDENTITY et MEMORY ici proposées ne constituent pas de nouveaux moteurs ou de nouvelles APK : préserver les chemins et leurs migrations.

## 3. Objectif d'ingénierie
Donner à CORE un vrai rappel des préférences confirmées sans collecte cachée de conversations, avec sélection adaptée à la question et export local contrôlé.

## 4. Fonctionnalités et scénarios concrets à développer
1. Implémenter MemoryRecallIndex textuel local séparé du RAG documentaire, classant préférences confirmées par contexte, date et pertinence lexicale.
2. Créer MemoryContextAssembler respectant un budget propre, source citée et ligne de séparation avec les documents NEXUS.
3. Ajouter expiration, oubli, suspension par session et filtre de catégories sensibles non autorisées.
4. Créer export JSON versionné explicitement demandé, aperçu des champs et chiffrement possible via port ; import sandbox refusant écrasement silencieux.
5. Créer vue utilisateur « ce que JARVIS sait de moi » avec corrections et justification de rappel.
6. Implémenter demande de confirmation quand rappel ambigu ou contradictoire.

## 5. Design interne, interfaces et ownership
1. MemoryRecallIndex + MemoryQuery
2. MemoryContextAssembler + MemoryExplanation
3. MemoryExportService + ImportPreview
4. ExpiryPolicy + UserMemoryControl
CORE orchestre sessions/permissions, LLM reste unique moteur ; les ports constituent des interfaces locales optionnelles, pas des accès directs au stockage des autres modules.

## 6. Fichiers à créer ou éditer
1. À créer : app/src/main/java/com/stark/jarvislocal/memory/{MemoryRecallIndex,MemoryContextAssembler,MemoryExportService}.kt
2. Existants : LocalDatabase.kt, PromptBuilder.kt, identity/IdentityComposer.kt
3. À créer : tests de budget/suppression/export
Les chemins app/src/main sont vérifiés. Les dossiers nouveaux sont des cibles **proposées** : établir le chemin exact des sourceSets `mvp`, `mvp2`, `mvp3` et la compatibilité Gradle avant toute écriture.

## 7. Plan de programmation
1. Définir corpus mémoire validé et droit de consultation explicite.
2. Coder index lexical minimal, pas de seconde inférence/embedding.
3. Assembler contexte avec limite de taille et justifications par identifiant mémoire.
4. Développer export et aperçu de restauration isolée, sans mutation des données actuelles.
5. Tester suppression, expiration, conflits et refus de rappel sensible.

## 8. Technologies et dépendances
SQLite local, Kotlin, SAF; chiffrement via Android Keystore seulement si vérifiable.
Conserver versions Gradle/Android, JNI historique et chargeur GGUF. Aucun deuxième moteur natif, serveur, télémétrie ou dépendance cloud.

## 9. Connexions vers autres modules
IDENTITY peut lire des préférences confirmées, NEXUS fournit faits documentaires à part, services n'ont pas accès au store brut.
Respecter ADR-001, contrat UNIFIED v1, et les décisions D4-02D (fixtures uniquement) : pas de connexion Binder production implicite.

## 10. Compatibilité et sécurité
Aucune extraction de messages historiques pour constituer des souvenirs sans consentement.
Préserver admissions, quarantaine UNKNOWN/CLEANUP_FAILED, minSdk, budgets mémoire, batterie et thermique. Ne pas migrer, supprimer, désinstaller, resign/installer une APK historique ou enregistrer automatiquement des informations privées.

## 11. Livrables obligatoires
Nouveaux composants compilables et intégrés dans le module CORE existant, démonstration fonctionnelle de chemin utilisateur, tests minimaux réalistes (les mocks seulement pour frontière non disponible), rapport bref des changements, commits sur branche dédiée. Pas de livraison purement documentaire.

## 12. Critères d'acceptation
1. Le rappel est traçable à un souvenir confirmé et respecte le budget.
2. Suppression/expiration empêchent rappel et export ultérieurs.
3. Un export choisi produit un fichier valide sans publication cloud.
La réussite des tests hôtes ne constitue pas une validation JNI Android.

## 13. Ordre et parallélisme
MEM-01,NEX-03. Toute mission modifiant `JarvisCore`, `LocalDatabase` ou `PromptBuilder` doit travailler sur branche séquencée après intégration consciente des précédentes ; IDENTITY et MEMORY peuvent développer interfaces internes séparées lorsque leurs contrats sont figés.

## 14. Autorisations et Git
La présente fiche est déposée **sans autorisation d'exécution**. Aucune fusion vers main, aucun accès à un appareil personnel, aucune utilisation de voix privée, aucune mise en production de nouveau Binder. Exiger revue distincte pour JNI, signatures, permissions, migration de données ou contrat inter-applications.

## 15. Rapport obligatoire
Module ; mission ; branche ; commit base/final ; code/fichiers livrés ; tests réellement exécutés vs historiques ; erreurs ; fonctionnalités inactives ; régressions possibles ; blocages ; prochaine action.
