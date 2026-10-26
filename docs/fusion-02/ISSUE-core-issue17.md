Source : https://github.com/bubth720/JAVIS-Local-v2/issues/17
Révision : 2026-10-25T17:32:59Z

# Mission FUSION-02 / CORE — Unifier réellement NEXUS+SERVICES dans l'application Android

**Objectif :** porter la preuve multi-modules JVM de FUSION-01 dans le **vrai CORE Android** sur API24, avec autorité, SAF et interface de test honnête. C'est la mission consommatrice finale ; **ne pas la forcer tant que les ports NEXUS/SERVICES ne sont pas admissibles**.

### Point de départ et actifs protégés
- Base préférée à revalider : `feat/fusion-01-core-integration` HEAD `36761cea101b`, parent `feat/core-03-intensive` `aea5ac859ac5`. PR brouillon #16, 567 cas JVM/Robolectric testés (501 historiques +36 fixtures +30 laboratoires). Corrections : sticky cancel, grant revalidé, reçus de workflow persistants. C1 PARTIAL à cause de fence révocation/effet.
- CORE Android `minSdk24`, bases historiques, profils Identity/Athéna, MEMORY/confirmations, API24/25, TTS/Whisper/Llama historiques, budgets/ressources : **préserver**. Ne pas cherry-pick `feat/core-voice-pilot` ou CORE004D dans la base sans revue de filiation et décision.
- Dépendances actuellement consommées par lab : NEXUS777/JVM JDBC, SERVICES3de/JVM. Les portages Android candidats **NEX374/SERVf000** ne sont PAS encore consommés. Voir UNIFIED F0 `3caa57d`.
- Ancienne issue CORE-03A #14 existe : se coordonner, pas créer une deuxième implémentation sous nom différent.

### Lots ordonnés
**C0 — Audit et contrat intégrateur (dès maintenant)** : choisir parent immuable, inspecter `AppGraph`, `SessionCoordinator`, `PromptBuilder`, `integration-lab/` et véritables ports; vérifier minSdk/build/schemas/mémoires, registre d'identité et version de l'autorité. Inventaire des tests et de la nouvelle CI CORE run40215604539 ; recontrôler résultat et résoudre si échec sans contourner. Adapter tests de refus `UNAVAILABLE`/capability et scripts de build, sans activer producteurs absents.

**C1 — Composer uniquement après GO producteurs** : prendre les SHAs **réellement qualifiés** NEXUS Android24 et SERVICES Android24 ; recompiler les API (ABI peut avoir changé), épingler artefacts/sources SHA et licenses, empêcher injection de JDBC/java.sql/natif inconnu. Aucune copie des algorithmes NEXUS/SERVICES dans CORE. Un seul `AppGraph` source de vérité, bibliothèque in-process uniquement, pas de Binder nouveau/service exporté pour ce lot. Feature flag OFF par défaut (pas d'activation de données perso).

**C2 — Orchestrer** : proposer ports `SourceSearchPort / KnowledgeReadPort / ServicesToolPort` réellement liés à modules producteurs, vérifier aux frontières `session/request/generation`, scope, grant, privacy, deadline, idempotence et demandes d'arrêt. Protocole d'autorité/fence de SERVICES nécessaire **avant mutation réelle**, avec décision de CORE au point d'engagement. Les reçus persistants existants ne sont ni effacés ni convertis en permissions. La relecture de document = donnée non fiable, jamais prompt système/instruction.

**C3 — Pilote UI in-process** : écran/commande lab distinct(e), activé(e) volontairement en environnement synthétique ; SAF choix de fichier TXT/PDF puis index/recherche/citation réelle (si NEXUS ready), CSV réel -> preview -> confirmation exacte -> nouvelle destination -> lecture. Si une capability est UNSUPPORTED (FTS5/SAF), l'afficher, tester le refus ; ne pas remplacer par dataset mock en prétendant vrai. Ne pas utiliser `INTERFACE` ni une voix pour faire croire à une fusion E2E.

**C4 — Scénarios émulateur** : construire APK debug pilot, install et tests sur **émulateur propre API24 puis API25**, ensuite26/35 si disponible. Mode avion, processus tué, rotation, permission source révoquée, URI altérée, reprise/dispatched UNKNOWN, erreur SQLite, export suspendu, grant double usage, race annulation/revocation/commit. Tests ADB et Android instrumentation réels, logs/screen/capability/sha. Aucune installation POCO/Redmi sans accord. Mesurer RAM/PSS, durée, coût de parse si environnement réel, sans extrapoler aux Xiaomi.

**C5 — Non-régression et livraison** : 501 historiques +36 fixtures +30 laboratoires (revalider le nouveau compte distinct sans addition des replays), tests NEX/SERV aux SHA exacts; app debug/mvp2/mvp3 builds, Lint, signature, minSdk24, pas de réseau, invariants SQLite/Identity/Memory et TTS historique. Tester lecture des anciennes bases sur copies, rollback non destructif; ne pas changer budget thermique ou charger plus d'un moteur lourd.

**C6 — Préparer le handoff UI et LLM sans activer** : schéma de statuts CORE honnête, `SubmitText/ACK/ToolState/Citation/Failure` proposition, pas de transport exposé ni de vrais tokens LLM tant que F3B/native bloqué. Préserver UI autonome précédente.

### Critères de déblocage
- GO C1/C2 uniquement si le producteur NEXUS possède FTS5/Android24 réel (ou variante explicitement approuvée) ET SERVICES possède fence/exécution SAF testés ou capacité limitée documentée. En absence, livrer C0 et tests de refus, s'arrêter.
- GO Android seulement si **même processus CORE** tourne réellement sur émulateur API24/25, vraie recherche/citation et transformation CSV avec autorité, scénario négatif, hash source/target. Les tests JVM/Robolectric ne valent pas verdict Android.
- Si DOCX bloqué, limiter pilote à recherche+CSV réellement qualifiés, afficher DOCX indisponible et poursuivre ce qui est sûr.

### Chemins proposés (vérifier arborescence)
`integration-lab/`, `app/src/main/java/com/stark/jarvislocal/{AppGraph,orchestration,events,memory,identity}/` selon chemins réels, `docs/fusion-01/`, `fixtures/p2-client/` en régression uniquement, tests Android; `.github/workflows/fusion-01-core.yml`.

## Consignes générales et rapport
Mission mère : https://github.com/bubth720/JARVIS-UNIFIED/issues/5. **Branche dédiée neuve**, base SHA et filiation Git explicites, pas de merge/auto-merge main, force-push ou écrasement des anciennes branches ; vérifier HEAD distants et AGENTS.md avant travail. Chaque dépôt garde la propriété de son code. Ne pas installer sur appareil personnel, toucher à une base réelle, transférer des données privées, importer du natif sensible ou changer signatures/permissions sans accord spécifique. Une mission bloquée livre un incrément autonome testé avec `PARTIAL/BLOCKED`, jamais un GO fictif.

Livrables `docs/fusion-02/REPORT.md`, `VALIDATION.json`, `PROVENANCE.json`, hashes des preuves et tableau contrat/code/JVM/Robolectric/instrumentation/physique/production. Renseigner module, lot, branche/base/HEAD, commits, fichiers, tests **exécutés**, CI à SHA exact, tests non exécutés, risques, limites, dernière action, prochain gate. Fournir PR **brouillon** si possible, sans merge.
