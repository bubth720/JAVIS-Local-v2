# JARVIS CORE — Mission pilote CORE ↔ VOICE (C-VOICE-001)

**État initial : mission autorisée pour développement et tests isolés, NON réalisée.** Accord utilisateur du 17 octobre 2026 sur les cinq orientations proposées dans VOICE 005K : Binder/AIDL explicite, contrôle réel des identités/signatures, arbitre lourd sous autorité CORE, activation opt-in et repli sûr, contrat expérimental épinglé. Cet accord n'autorise PAS l'installation sur son Xiaomi, le déplacement de son profil B, un endpoint public utilisable en production, ni une fusion.

## Dépôt et bornes

- Dépôt exact : `bubth720/JAVIS-Local-v2` (orthographe JAVIS).
- Branche dédiée : `feat/core-voice-pilot`, base `main` au commit `226e2e8cc3eebd479805b89d035eb5c1cbcad540`, HEAD revérifié le 17/10/2026.
- Mission sœur : `bubth720/JARVIS-VOICE` → `feat/voice-bridge-pilot`, base VOICE 005K `6a86d847ff57fd9f695ee87eb5bff21c0c8862ca`.
- Contrat commun proposé : `005k.1-proposed`, bundle SHA256 `9c018ecfee1b62eb0c3cea36b580edb9edcbda43f3772135b01191018d5eacad`. Source et règles : [contrat 005K](https://github.com/bubth720/JARVIS-VOICE/blob/6a86d847ff57fd9f695ee87eb5bff21c0c8862ca/docs/mission005k/CONTRAT-BRIDGE.md), [verrou](https://github.com/bubth720/JARVIS-VOICE/blob/6a86d847ff57fd9f695ee87eb5bff21c0c8862ca/docs/mission005k/CONTRACT-PIN.json), [décisions approuvées en orientation](https://github.com/bubth720/JARVIS-VOICE/blob/6a86d847ff57fd9f695ee87eb5bff21c0c8862ca/docs/mission005k/DECISIONS-IPC.md), [fiche CORE initiale](https://github.com/bubth720/JARVIS-VOICE/blob/6a86d847ff57fd9f695ee87eb5bff21c0c8862ca/docs/mission005k/MISSION-FUTURE-CORE.md).
- Les schémas UNIFIED existants restent inchangés et non déclarés approuvés rétroactivement. Les lacunes de liaison session/bail/profil/réponse et de résolution locale du texte exigent un **complément versionné négocié avec VOICE avant intégration réelle** ; toute mutation du bundle impose nouveau hash, nouveaux tests et validation croisée. Pas de changement caché.
- **Aucune modification de JARVIS-LLM, JARVIS-UNIFIED, JARVIS-VOICE ni de `main`** dans cette mission CORE. Une intervention dans un autre dépôt exige nouvelle autorisation.

## Problème réel constaté

Dans `app/src/main/java/com/stark/jarvislocal/` : `MainActivity.kt`, `JarvisCore.kt`, `AppGraph.kt`, `LlamaLocalEngine.kt`, `WhisperLocalEngine.kt`, `LocalTts.kt`, `SettingsStore.kt`. CORE minSdk24/target35, package `com.stark.jarvislocal`. `LlamaLocalEngine` garde une instance LlamaModel entre messages ; `releaseInternal()` privé appelé au remplacement/OOM, pas de protocole d'arrêt/déchargement fiable pour relayer la RAM à VOICE. `WhisperLocalEngine` charge/libère mais fin de coroutine ≠ preuve de fin native. Un `Mutex` process-local et un timeout de chat ne prouvent pas qu'aucun calcul ou modèle lourd n'est résident. Ne pas libérer durant inférence, ne pas injecter un « resourceFree=true » auto-déclaré.

## Livrables techniques, phases indépendantes

### C1 — Adaptateur de sortie et contrat

- Lire et tester le chemin historique de réponse. Ajouter `SpeechOutputPort`/client abstrait et état de session/corrélation/annulation, sans changer le traitement des commandes, mémoires, SQLite, PDF/RAG, réponses ni le TTS existant.
- Port ancien `LocalTts` par défaut, inchangé tant que pilote inactif. Pilote désactivé par défaut, activation explicite ultérieure via UI gouvernée, jamais auto-enable par build/flag d'environnement.
- Valider les contraintes texte exact (max 300 UTF-16 et 50 tokens en aval), refus explicite de texte trop long ; aucune troncature silencieuse ni paraphrase automatique. Pas de texte, URI, audio ou contenu de documents dans logs.
- Mapper strictement version/schéma/binding moteur. Le moteur d'identité canonique de VOICE est `pocket-tts-french-6l-fp32-v1`, et non `pocket-french-6l-fp32-lsd` (désignation interne).

**Acceptation C1** : tests unitaires sur LocalTts inchangé, session, refus long texte, client absent/port désactivé, callbacks et dépendances injectées, aucune modification des données SQLite existantes.

### C2 — Arbitre natif sûr (bloquant)

- Implémenter/qualifier l'état réel de travail et résidence de Llama/Whisper : admission unique des travaux lourds sous autorité CORE, arrêt/cancellation, confirmation de fin de calcul native et déchargement effectif sous verrou ; si impossible avec le SDK existant, documenter et **bloquer toute admission VOICE réelle** sans feindre le succès.
- Concevoir les baux avec session/corrélation/génération, expiry, death/restart, preuves non forgeables par l'appelant, états UNKNOWN/REFUSED fail-closed ; jamais deux lourds sans preuve de libération.
- CORE gouverne les baux mais VOICE conserve ses propres gardes RAM libre >=2 Gio, thermique, file/thread. Mort CORE durant le service = arrêt/refus jusque nettoyage connu, pas de fail-open.
- Pas de modification non autorisée du dépôt autonome LLM. Si le wrapper actuel ne peut fournir déchargement sûr, livraison C1 + documentation et tests C2 simulés, puis stop gate explicite.

**Acceptation C2** : tests de concurrence, timeout/cancellation native réellement vérifiée ou limitation indiquée, release une seule fois, génération périmée, crash/reboot, certificat de fin impossible, aucun second travail admis avant arrêt/libération fiable. Ne pas assimiler tests mocks à preuve native.

### C3 — Client Binder/AIDL isolé (d'abord fixture)

- Créer un client Android au transport **explicite** et à destination fixée, gated opt-in ; service de destination uniquement sur la nouvelle application pilote VOICE, jamais sur 005J.
- Contrôler UID/package/certificat de l'autre application au moment du lien, capability et grant local de durée limitée, session et anti-rejeu ; compte tenu des signataires Xiaomi non relevés, pas de confiance de facto dans l'alias ou le nom de package. En cas de clés distinctes, appairage visible et révocable, après validation détaillée. Aucune clé privée exportée ni resignature des apps installées.
- CORE API24/25 conserve LocalTts, API26+ peut seulement tenter le pilote une fois le service confirmé compatible et autorisé ; restrictions Android35 de service visible, focus et durée de vie à qualifier. Pas d'Intent implicite, réseau local, content provider de voix ni .jvi transféré.
- Résultat GENERATED ≠ STARTED/COMPLETED audible. Repli vers LocalTts **seulement** sur décision explicite et preuve d'arrêt VOICE, jamais double voix après timeout ambigu.
- Fixture test inter-apps Android sur messages PUBLICS, client hostile/signature différente, grant révoqué, service mort, vieux résultat, mémoire insuffisante. **Aucun E2E personnel** et aucune activation sur Xiaomi sans approbation séparée.

**Acceptation C3** : JVM + Android API24/25/26+/35, tests Binder réels séparés des mocks ; non-régression de tous les cas CORE (migrations SQLite, mémoire, commandes, RAG/PDF, TTS) ; PR brouillon seulement, aucune fusion.

## Synchronisation avec VOICE

Les deux missions publient leurs commit SHA et le même hash de contrat. CORE expose seulement le texte et des enveloppes bornées vers VOICE, pas l'identité vocale privée. Ne pas revendiquer « liaison complète » avant fixture E2E réelle des deux APK avec UID/grants/leases/provenance, plus validation physique utilisateur ultérieure. Les commits de l'app principale restent indépendants de ceux du pilote VOICE.

## Sorties obligatoires de Codex

- `docs/mission-core-voice-pilot/ARCHITECTURE.md`
- `docs/mission-core-voice-pilot/SECURITE-RESSOURCES.md`
- `docs/mission-core-voice-pilot/CONTRAT-SHA.md`
- `docs/mission-core-voice-pilot/TESTS.md` et `RESULTATS.md` (compteurs réels, sauts, blocages, preuves)
- Rapport : module CORE, branche, commit, phase atteinte C1/C2/C3, test/mock/IPC, blockers et prochaine action.
- PR **brouillon** `feat/core-voice-pilot` → `main` après code/test ou documentation de blocage ; pas de merge.

**Consigne Codex :** vérifier HEAD/dépendances réels, faire C1 puis C2 puis C3 seulement si les gates sont satisfaits. Un stop de sécurité documenté est un résultat acceptable ; aucun contournement. Si une action impliquerait `main`, LLM ou l'installation d'un pilote sur le Xiaomi, s'arrêter et redemander l'accord du propriétaire.
