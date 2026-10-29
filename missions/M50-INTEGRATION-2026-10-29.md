# CORE-M50-01 — Orchestrateur réel minimal, ports versionnés et admission conservatrice

Dépôt: bubth720/JAVIS-Local-v2
Branche de distribution: mission/m50-integrated-beta-20261029
Base figée de travail: 3eafb4acd82255f4578d4a3bc113d418508add86

**MISSION, PAS IMPLÉMENTATION.** Cible M50 = application unique avec coeur conversation, documents/CSV locaux et composants indépendants, seulement après qualifications de confiance. Cette mission est une tranche spécifique, distincte des PR lot3/lot4 existantes.

## Commande pour Codex
Ouvrir AGENTS.md si présent; vérifier les SHA, les PR existantes, provenance/CI et les lignes exactes du code. Créer une NOUVELLE branche `codex/m50-*` depuis cette branche de distribution, puis une PR brouillon vers elle. Ne pas modifier ni merger main, ni toucher aux données personnelles, ni relever de permission/SDK/ABI, ni activer connexion sensible sans validation distincte.

## Fichiers prioritaires à inspecter (confirmer leur présence)
- `AGENTS.md`
- `docs/fusion-02/INTEGRATOR-CONTRACT.md`
- `app/src/main/java/com/stark/jarvislocal/SessionCoordinator.kt`
- `app/src/main/java/com/stark/jarvislocal/AppGraph.kt`
- `app/src/main/java/com/stark/jarvislocal/integrations/PinnedDocumentReadiness.kt`
- `app/src/test/java`

## Étapes détaillées

### 1. A — Lecture des sorties de lots

Comparer PR CORE#23 et #25, leurs SHAs, 629 cas hôte et 12 tests d'autorité de laboratoire ; aucune réussite du labo n'autorise le dispatch productif. Reprendre tous les invariants SessionCoordinator et les budgets Android.

**Preuves d'acceptation de la phase :** diff ciblé et réversible, test nominal + contre-exemple, SHA40, commande exacte, sortie CI liée au HEAD, risques de régression et état PASS/BLOCKED/NOT_RUN.

### 2. B — Pilote conversation hors ligne

Préparer un chemin d'échange simple UI→CORE→résultat strict via port adapter injecté et **désactivé** dans l'APK historique. Utiliser d'abord un fournisseur synthétique clairement identifié, qui ne prétend jamais être une réponse LLM native ; tests ACK/chunks/Stop/UNKNOWN, process death, crash, multi-session, dernier callback.

**Preuves d'acceptation de la phase :** diff ciblé et réversible, test nominal + contre-exemple, SHA40, commande exacte, sortie CI liée au HEAD, risques de régression et état PASS/BLOCKED/NOT_RUN.

### 3. C — Source documentaire et outils en lecture seule

Ajouter interfaces consommateurs de NEXUS (sélection, contexte et citation fiable) et SERVICES (documents.search, data.csv.normalize) sans copier des bases, sans droits mutateurs. Si le producteur Android est UNSUPPORTED, rendre une erreur claire et ne créer ni index ni réponse inventée. CORE maintient autorité et limites mémoire.

**Preuves d'acceptation de la phase :** diff ciblé et réversible, test nominal + contre-exemple, SHA40, commande exacte, sortie CI liée au HEAD, risques de régression et état PASS/BLOCKED/NOT_RUN.

### 4. D — Sécurité Android proposée

Proposer le format exact d'IPC signé CORE↔UI et de traitement d'identité UID/paquet/certificat, epoch et grant; **pas d'activation de Binder/export ni de permission tant que l'approbation du transport n'a pas été accordée**. Même politique pour SERV authorizeAtCommit et deux journaux.

**Preuves d'acceptation de la phase :** diff ciblé et réversible, test nominal + contre-exemple, SHA40, commande exacte, sortie CI liée au HEAD, risques de régression et état PASS/BLOCKED/NOT_RUN.

### 5. E — Qualification

Rejouer 629 cas hôte historiques au HEAD exact, garde du lot4, build/lint debug, négatifs FTS5/SAF absents, mauvaises identités, double charge native et arrêt. Préparer une APK-laboratoire autonome **sans aucun accès privé** uniquement après revue du manifeste Android. Appareil physique NOT_RUN sans consentement.

**Preuves d'acceptation de la phase :** diff ciblé et réversible, test nominal + contre-exemple, SHA40, commande exacte, sortie CI liée au HEAD, risques de régression et état PASS/BLOCKED/NOT_RUN.

## Règles de stop et critères finals
- D'abord prototypes OFF/testOnly. Le consentement utilisateur à l'architecture ou à des principes n'autorise PAS permissions, authentification de production, migration, import natif, export SAF ou installation physique.
- Préserver contrat historique, bases, voix personnelles, minSdk, permissions et anciennes variantes; voir le dossier central M50.
- Aucune réussite hôte ne qualifie Android réel; aucun résultat d'émulateur ne qualifie un appareil physique.
- Ne jamais masquer des échecs, affaiblir des tests, extrapoler READY, prétendre à un outil natif libéré ou créer de citation synthétique présentée comme réelle.
- Rendre : module, mission, branche, HEAD SHA + tree, tests succès/échecs/skips/non exécutés, CI et PR, blocages, dépendances, rollback, prochaine action.
- Ne jamais fusionner main sans confirmation explicite; un échec bloquant = rapport et STOP.
