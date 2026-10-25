Source: https://github.com/bubth720/JAVIS-Local-v2/issues/15
Relevé: 2026-10-25T10:24:58Z

# JARVIS — lot FUSION-01 / CORE

## Objet
Transformer CORE de propriétaire de composants internes en **orchestrateur de véritables ports NEXUS/SERVICES**, préparer un chemin de réponse contenu vers INTERFACE, puis une seule inference LLM et VOICE uniquement quand ces producteurs sont réellement qualifiés.

## Faits de départ
`feat/core-03-intensive` `aea5ac859ac5` contient `integration-lab/` (10 tests JVM véritables, citations NEXUS/CSV/DOCX SERVICES et contrôle d'autorité), mais pas Android. `feat/mem-02-intensive` `49ad428e94ed` inclut 501 tests et ID-02 `283423cb8b9d`. `feat/core-voice-pilot` `7426665f2794` maintient le TTS historique, contrat Binder non activé ; manque attestation native. `main` `226e2e8cc3ee`. **minSdk24 invariable**, anciennes bases et fonctionnalités intactes. Lire CORE-03 et issue CORE-03A existante s'il y en a une; pas de branche imposée qui exclurait MEMORY/IDENTITY.

## Lots précis
**C0 / convergence :** calculer une matrice de filiation entre CORE02/MEM02/CORE03/CORE-VOICE/IDENTITY002 et les contrats UNIFIED; vérifier l'état des fixtures et chaque API. Choisir parents sans cherry-pick aveugle. Script de compatibilité reproduisant baseline et listant les 501 tests (ne pas les dire réexécutés si non joués).

**C1 / pipeline métier intégré JVM :** conserver vrai `integration-lab/` et isoler ports injectables `KnowledgeReadPort`/citeur résoluble/`ServicesToolPort` ; faire circuler session/request/generation, cancellation, authority, permission scope, délais/UNKNOWN, revocation et confirmations consommables une fois. Lancer des appels réels, y compris refus d'injection texte dans des documents et refus de réponse tardive. Ne pas créer une seconde mémoire ou un autre registre tools.

**C2 / Android API24 :** consommer seulement des ports **réellement compatibles et testés API24** issus de NEX-06A et SERV-04A, en préservant `AppGraph`, `SessionCoordinator`, `PromptBuilder`, bases SQLite et anciens appels. Pointeurs de capacités réelles `UNAVAILABLE / READY / UNKNOWN` et feature flags OFF par défaut. Documentaires cités et outils sous confirmation; vérifier perte de SAF et arrêt process sur émulateur API24/25/26/35. Sans port API24 validé = blocage C2, pas d'emballage sqlite-jdbc.

**C3 / conversation transport :** formaliser submit/ack/stream/final/error/stop/approval/version/limit, identifiants monotones, Android auth UID/package/signature, rejet de messages tardifs et de refus persistant; ne pas activer de service exporté ou nouvelle permission sans revue explicite. Contrat réutilisable par INTERFACE UI-03, avec moteur LLM absent indiqué honnêtement.

**C4 / LLM+VOICE :** ports indépendants pour génération et speech, mais aucun passage au JNI/F3B ou worker non qualifié. Gestion de baux/résidence/leak observable ; un moteur lourd à la fois sur Android non qualifié, erreurs de libération UNKNOWN. TTS historique conservé; voix neuro et Athéna inactives jusqu'à GO distinct.

## Scénarios d'acceptation
A. Lecture NEXUS d'un vrai PDF/TXT synthétique → citation URI/page/hash/offset relue; aucune source traitée comme SYSTEM.
B. CSV SERVICES avec normalisation et calcul exact, fichier source intact; sortie vérifiée.
C. DOCX échoue sans grant/confirmation, puis succès après confirmation exacte à usage unique, anti-replay/revocation.
D. Annulation, rotation/process kill, mauvais client, réponse hors génération, absence de modèle/FTS5/SAF; états sûrs et traces expurgées.
E. Vérifier invariants historiques des données, `minSdk=24`, permissions et signature. LLM/VOICE bloqués ne sont jamais renommés PASS.

## Cibles initiales
`integration-lab/`, `app/src/main/java/com/stark/jarvislocal/orchestration/`, `events/`, `AppGraph`, `SessionCoordinator`, `PromptBuilder`, tests associés (chemins exacts à confirmer). Aucun nouveau backend parallèle. **Livrer C0/C1 immédiatement si possibles; C2-C4 uniquement après leurs gates.**


## Cadre commun obligatoire
Mission mère : https://github.com/bubth720/JARVIS-UNIFIED/issues/4. Lire les rapports de branche, vérifier commits/parents et `AGENTS.md` **avant** d'écrire. Les branches fonctionnelles de différents lots ne sont pas présumées fusionnées. Travail exclusivement sur branche neuve `feat/fusion-01-<module>-integration` après choix documenté d'une base SHA. Ne toucher à aucun autre dépôt dans cette mission. Zéro merge main, auto-merge, force-push, migration de base personnelle, installation/appairage personnel, activation sensible non approuvée. Les simulations sont étiquetées FIXTURE, jamais « intégré ». Compatibilité et données historiques préservées.

### Protocole de rendu
Rapport versionné `docs/fusion-01/REPORT.md` + manifest de provenance SHA et tests bruts ; module, lot, base/head, fichiers, commits, dépendances effectives, risques, branches non retenues, commandes et résultats **exécutés**, limitations/NOT_RUN, appareils, état PASS/PARTIAL/BLOCKED, décisions sollicitées. Distinguer tests JVM, Robolectric, Android instrumenté et mesures physiques. Une PR brouillon peut être proposée, jamais fusionnée.

**Ordre d'action Codex** : inspection et tests baseline → lot indépendant non sensible → tests négatifs → rapport → stop si gate externe. Ne pas bricoler les règles de sécurité pour obtenir une case verte.
