# CODEX — JARVIS CORE LOT 4 / CORE-PF-03

**Objet : autorité CORE fail-closed et contrat de laboratoire, sans raccordement réel.**
**Branche mission :** `mission/adr002-principles-lot4-20261028`.
**Base vérifiée :** `aed84860f2ca85713140a687ba7d86efc0203dc8`,
branche `codex/prefusion-lot3-core`. Étudier les décisions validées
dans JARVIS-UNIFIED :
`docs/DECISIONS/ADR-002-PRINCIPLES-APPROVED-2026-10-28.md`,
branche `mission/adr002-principles-lot4-20261028`.

## Ordre de travail — nouvelle branche de développement, pas celle-ci

**P0. Inspecter avant code.** Lire `AGENTS.md`,
`docs/prefusion/lot3/REPORT.md`, `docs/prefusion/lot3/PROTOCOL-REVIEW.md`,
`SessionCoordinator.kt`, `AppGraph.kt`,
`ReadOnlyCoreUiLabPort.kt` et leurs tests ; vérifier SHA, diff, minSdk24,
régressions historiques et CI. Relever propriétaire de session, politique,
métadonnées read-only et protections existantes.

**P1. Contrat candidat local D1 — aucun IPC.** Créer sous le répertoire
de laboratoire existant une proposition de type `CommitDecisionFixture`
inert, alimenté seulement par données synthétiques canoniques. Le
consommateur ne peut jamais déclencher effet ni s'authentifier par
payload. Exiger UUID canoniques, epoch renouvelé, génération monotone
et version, corrélation session/request/intent, aperçu exact lié au hash,
destination et deadline monotone. Rejeter génération0 en wire>=1,
inconnu/stale/replay/expiré/révoqué ou identité non attestée.

**P2. Barrière et annulation.** Tester en concurrence avec horloge injectée
la consommation usage unique du consentement, sa révocation, annulation
sticky, cancel avant et après point de décision, crash avant reçu, refus
si CORE n'est pas présent. Un seul propriétaire de politique. Tester
plusieurs threads/duplicats et la relecture exacte de jetons sans
stocker de token réel. Jamais tenir un verrou de décision à travers une
IO de provider ou une opération lourde LLM.

**P3. Pas de fausse connexion.** Les composants Lab et le nouvel
adaptateur restent NON composés dans `AppGraph`, toujours
`enabled=false`, `canExecute=false`, `authenticated=false`.
Tester impossibilité de passer à READY/COMMITTED sans preuve; ne pas
toucher au transport réel, `AndroidManifest.xml`, permissions,
préférences personnelles, contrats CORE004C gelés ou moteurs JNI.

**P4. Validation.** Écrire tests positifs/négatifs JVM/Robolectric
locaux, vérifier les suites hôte déjà vertes et builds/Lint avec les
commandes du dépôt (voir AGENTS.md). Si CI indisponible, marquer
NOT_RUN et ne pas annoncer un total inventé. PR **brouillon** ciblant
cette branche de mission. Rapport `docs/prefusion/lot4/REPORT-CORE-PF-03.md`
avec SHA, diff, échecs/skips, précédents tests, contrats, risques,
rollback, blocages et décisions restantes.

## Acceptation stricte

- Aucun changement runtime/manifest, aucune liaison inter-APK,
  aucune écriture réelle, aucun mock positif étiqueté UID authentifié.
- Tous les échecs de droit/epoch/consentement empêchent la décision,
  y compris perte d'ACK/erreur et replays; l'état UNKNOWN demeure bloquant.
- Tests entièrement reproductibles, sources historiques conservées et
  CI au HEAD exact, sous réserve des limitations d'environnement.
- **STOP et demander autorisation** avant la moindre intégration CORE↔SERVICES,
  migration, changement de contrats publics ou fusion dans `main`.
