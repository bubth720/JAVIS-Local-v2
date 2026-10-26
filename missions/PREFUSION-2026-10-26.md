# Mission Codex CORE-PF-01 — CORE unique arbitre : raccordement passif NEXUS/SERVICES

Emplacement : missions/PREFUSION-2026-10-26.md dans bubth720/JAVIS-Local-v2
Branche mission : mission/prefusion-gates-20261026
Base observée (à revérifier) : f6a3b2d0c280c69ef1570544fdbf5bcf5818810c

## Contrat d'exécution
Lire AGENTS.md s'il existe, vérifier les refs et les PR avant tout changement. Créer une nouvelle branche de développement depuis la mission. Travail 100 % local/hors ligne au runtime, protections Android/thermiques, contrats et fonctionnalités historiques préservés. Aucun merge main, migration irréversible, activation d'écriture, changement majeur d'architecture, données utilisateur dans Git, suppression/skip de tests. Toute décision sensible => STOP et demande d'accord.

## Étapes exécutables

### 1. Audit

Lire AGENTS.md, docs/PROJECT_KNOWLEDGE.md et docs/fusion-02/INTEGRATOR-CONTRACT.md. Inspecter SessionCoordinator.kt, AppGraph, DocumentToolReadiness.kt, TaskCoordinator. Confirmer minSdk24, modèles et tests; conserver orchestration, mémoire et fonctionnalités existantes.

Livrables : code et tests ciblés, diff, commit séparé, CI exacte au SHA, rapport d'échec et retour arrière pour cette étape.

### 2. Ports de lecture

Préparer des ports typés OFF par défaut vers NEXUS DocumentaryGateway/RagContextPort et SERVICES ServicesToolPort sur SHA épinglés. UNKNOWN/CONNECTED ne crée jamais READY; FTS5 absent et export SAF désactivé entraînent un refus testable.

Livrables : code et tests ciblés, diff, commit séparé, CI exacte au SHA, rapport d'échec et retour arrière pour cette étape.

### 3. Autorité et cycles de vie

Créer tests négatifs exacts session/request/generation, politique changeante, cancellation, expiration, consentement à usage unique, commit/ACK perdu et redémarrage. Pas de second registre/orchestrateur ou de droit issu du modèle/document. Aucun effet d'écriture avant validation ADR-002.

Livrables : code et tests ciblés, diff, commit séparé, CI exacte au SHA, rapport d'échec et retour arrière pour cette étape.

### 4. Pilote indépendant

Construire un pilote de lecture documentaire et citations utilisant des données synthétiques en laboratoire, avec absence de vrai port signalée UNSUPPORTED. Mesurer timeout/annulation, admission des tâches lourdes, régression conversation hors ligne.

Livrables : code et tests ciblés, diff, commit séparé, CI exacte au SHA, rapport d'échec et retour arrière pour cette étape.

### 5. Gates de livraison

Utiliser les commandes de build/CI attestées du dépôt (ne pas supposer ./gradlew si absent). Rapporter CI/JVM/émulateur/physique séparément; PR brouillon et rollback. Aucun raccordement sensible actif ni fusion main.

Livrables : code et tests ciblés, diff, commit séparé, CI exacte au SHA, rapport d'échec et retour arrière pour cette étape.

## Critères d'acceptation
Une étape est validée seulement par tests réellement exécutés, aucun résultat inventé, conservation des données et comportement OFF/refus sûr quand une capacité n'est pas qualifiée. Les tests JVM, Robolectric, Android émulateur et matériel physique ne sont jamais confondus.

## Restitution Codex
Module | mission | branche | commit | PR brouillon | commandes/tests avec nombres et échecs/skips | blocages | compatibilité | rollback | prochaine action. Ne fermer la mission complète que si les critères réels sont satisfaits. Si blocage ou approbation requise, livrer le rapport et s'arrêter. Ne pas fusionner main.
