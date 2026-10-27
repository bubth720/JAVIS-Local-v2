# CORE-PF-02 — Port CORE→INTERFACE authentifiable, OFF par défaut

**État : MISSION CODEX À EXÉCUTER (aucune implémentation revendiquée par ce document).**
Dépôt : bubth720/JAVIS-Local-v2
Branche source : mission/prefusion-lot3-20261027
SHA de base du lot vérifié : a3113797a3c481bc516c5ef1fd4d7a74b919de40

## Règles d'exécution obligatoires
1. Lire AGENTS.md s'il existe, les rapports du lot précédent, vérifier HEAD et CI.
2. Créer une nouvelle branche codex/prefusion-lot3-* issue de la branche de mission. Ne pas développer dans main.
3. Tranches courtes avec tests négatifs, commits, CI exacte, PR brouillon vers la branche mission.
4. Hors ligne au runtime, compatibilité Android et données personnelles préservées. Aucun merge, migration, extension de permissions, import architectural ou activation sensible sans décision explicite.
5. Tout effet d'écriture, nouveau backend SQLite natif ou transport UID/certificat non approuvé reste OFF et limité à tests synthétiques.

## Fichiers à examiner
- `app/src/main/java/com/stark/jarvislocal/SessionCoordinator.kt`
- `app/src/main/java/com/stark/jarvislocal/AppGraph.kt`
- `app/src/main/java/com/stark/jarvislocal/integrations/PinnedDocumentReadiness.kt`
- `docs/fusion-02/INTEGRATOR-CONTRACT.md`

## Étapes

### Phase 1

Réexaminer les clés epoch/session/request/generation/sequence, arrêt borné, permission, autorité de session et projection pinned NEXUS/SERVICES. Ne pas connecter un producteur absent; reconnaître OFF/UNKNOWN.

### Phase 2

Implémenter un **port de laboratoire lecture seule** entre les événements CORE et le consommateur UI. Garder l’interface publique et AppGraph historique inchangés sauf nécessité démontrée. Signatures/UID système non disponibles en JVM => résultat UNTRUSTED, jamais READY.

### Phase 3

Ajouter tests négatifs : fausse origine, UID/paquet revendiqué par payload, cert expiré ou différent, version inconnue, replay, epoch périmé, séquence sautée, annulation avant ACK, process death / UNKNOWN, correction rétroactive des capacités, événements hors session. Les faux succès et citations sont interdits.

### Phase 4

Mesurer l’impact JVM/Android, conserver minSdk24 et ressources/batterie ; fournir adaptateur Android authentifié comme **proposition revue** uniquement, sans Binder exporté, permissions élargies ni connexion activée.

### Phase 5

CI exacte : régresser les 602 tests du lot2 et builds/lint, vérifier le nouveau protocole sur tests synthétiques; rapport PROPOSED/OFF et procédure rollback. Déposer une PR brouillon vers cette branche de mission.

## Critères de réception
- Aucune régression, ancienne fonctionnalité, contrat gelé ou preuve historique supprimés.
- Tests au SHA de la PR et à la granularité JVM / Robolectric / Android émulateur / appareil physique, sans inventer les non exécutés.
- Distinguer fiche d'architecture, prototype et connexion de production; le statut d'intégration reste BLOCKED si manque autorité, backend ou appareil.
- Rapport : module, mission, branche, commit, tests avec totaux/échecs/sauts, blocages, fichier d'impact, rollback, prochaine action.
- PR brouillon uniquement. STOP et demande de décision pour toute opération sensible.
