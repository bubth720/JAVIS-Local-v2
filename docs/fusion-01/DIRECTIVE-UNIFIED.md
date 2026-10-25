Source: https://github.com/bubth720/JARVIS-UNIFIED/issues/4
Relevé: 2026-10-25T10:27:50Z

# FUSION-01 — Programme directeur de convergence JARVIS (25 octobre 2026)

> **MISSION D'INGÉNIERIE À EXÉCUTER PAR ÉTAPES, PAS ORDRE DE FUSION IMMÉDIATE.** Cette issue autorise l'étude, les tests isolés, les changements sur de nouvelles branches et la préparation de PR brouillon. Elle **n'autorise pas** de merge vers `main`, d'activation du JNI/worker natif bloqué, d'installation personnelle, de migration des données réelles, d'IPC sensible non revu, de contournement de sandbox ni d'action autonome sur un appareil. En cas de blocage : livrer une tranche testée + BLOCKED documenté, jamais une simulation maquillée en intégration.

## 0. Finalité produit — critère de vérité
Livrer progressivement **un service JARVIS local, hors ligne et cohérent**, visible dans une interface Android unique et évolutif vers mini-PC, **sans fusionner indistinctement les dépôts**. Un unique CORE détient les sessions, l'orchestration, l'autorisation, la mémoire confirmée et le prompt approuvé. Un unique LLM local interchangeable exécute l'inférence lorsqu'il est effectivement qualifié. VOICE parle/écoute sous permissions et politique native. INTERFACE porte le dialogue et Athéna 2.5D. NEXUS détient le catalogue documentaire et les citations. SERVICES détient les outils/exécutions. EVOLUTION observe, diagnostique, propose en isolation sans s'auto-patcher. UNIFIED est le **registre des contrats, du BOM de versions et des preuves**, **pas** une nouvelle application ou un troisième moteur.

**Le succès produit n'est pas le nombre de branches.** Un scénario doit parcourir de véritables implémentations avec preuves de contenu/citations/fichiers/permissions, en mode avion, sans faux message « prêt ». Les situations non qualifiées s'affichent comme telles.

## 1. Sources vérifiées et filiation à reconfirmer au démarrage
| Dépôt | Référence / état connu | Point de vigilance |
| --- | --- | --- |
| `bubth720/JAVIS-Local-v2` (CORE) | `feat/core-03-intensive` `aea5ac859ac5` ; ID-02 `283423cb8b9d`, MEM-02 `49ad428e94ed` ; `feat/core-voice-pilot` `7426665f2794` | CORE-03 raccorde réellement NEXUS/SERVICES **sur JVM seulement** ; minSdk24 et mémoire SQLite doivent rester intacts. |
| `bubth720/JARVIS-LLM` | `feat/llm-03-runtime` `2efb3f7aa620` ; `feat/llm-02-runtime` `895a067ffc86` | Tokeniseur exact `UNSUPPORTED`; JNI/F3B/fin de résidence non attestés ; aucun GO natif. |
| `bubth720/JARVIS-VOICE` | `feat/voice-final-01` `fe57452fcb33`, `feat/voice-bridge-pilot` `04c4ca1c050c`, `feat/voice-07-voice-delivery` `40cb27b1e275` | Studio indépendant, voix longue expérimentale ; émotions acoustiques non validées ; production inter-APK non activée. |
| `bubth720/JARVIS-INTERFACE` | `feat/interface-final-01` `db459ba388a3`, `feat/ava-04-delivery` `283ed2f3cd36` | Interface autonome améliorée ; vrai rig Athéna premium et raccordement réel manquants. |
| `bubth720/JARVIS-NEXUS` | `feat/nexus-final-01` `d903bf04f497`; API24 `feat/nex-06a-android-api24` `374ea2e2fd64` | Bibliothèque/atlas v4 codés ; Java/Kotlin final non qualifié dans dernier rapport ; portage Android à vérifier ; ZIM Android non qualifié. |
| `bubth720/JARVIS-SERVICES` | `feat/services-final-01` `d16dd84f2788`; port API24 `feat/serv-04a-android-tool-port` `f00091a63961` | Atelier autonome 17 outils ; BRIDGE file authentifiée sans transport activé ; pas d'autorité CORE connectée. |
| `bubth720/JARVIS-VOLUTION-` | `feat/evol-03-atelier-de-correctifs-isoles-e` `c01801818043` | Diagnostic réel sur JVM ; sandbox OS échouée ; patch execution INTERDITE. |
| `bubth720/JARVIS-UNIFIED` | `feat/uni-02-tooling` `329f304115c9` ; `main` `e4435f4bd60c` | Source de vérité/contrats/outillage, aucun runtime. |

Avant de modifier quoi que ce soit : lire `AGENTS.md` et les rapports/CI de chaque dépôt, comparer SHA distants, branches et permis. Les branches sont **parallèles** ; ne pas supposer qu'une branche finale contient tous les autres travaux. Reconstruire un graphe de dépendances/cherry-picks depuis les diffs, jamais les noms. Prendre comme états `VERIFIED`, `PARTIAL`, `BLOCKED`, `NOT_RUN`, `UNKNOWN`.

Sources de campagne : `docs/intensive-2026-10-22/VERIFIED_STATUS.md` et `CAMPAIGN_REPORT.md` dans UNIFIED. La photographie du 23 octobre répertorie 20/31 missions livrées sur preuve logicielle et six incréments partiels ; **cela ne mesure pas le produit intégré**.

## 2. Architecture obligatoire, frontières et menaces
- **CORE** reste seul arbitre des permissions et consentements persistants ; un document, un prompt, l'UI et un modèle ne créent jamais d'autorisation. L'éditeur de personnalité n'active pas Athéna sans accord explicite.
- **LLM** reste une unique autorité de résidence native et de génération ; pas deux backends simultanés, pas de TTS/Whisper chargé pendant un autre travail lourd sans attestation d'unload, pas de seuil abaissé pour « faire passer » des tests. `UNKNOWN` interdit la réadmission.
- **NEXUS** réutilise ses objets sources, révisions et citations (URI, hash, page, offsets, origine). Le contexte récupéré est **non fiable** et borné ; aucun texte documentaliste ne devient un message SYSTEM.
- **SERVICES** centralise registre, schémas, Guard, ApprovalGate, journaux et exécutions ; distinguer lecture et mutation ; export/suppression = confirmation exacte distincte, consommable une fois.
- **VOICE** conserve les coffres/identités, droits d'utilisation et son profil B. Aucune voix/clonage personnel envoyé vers un autre module sans choix utilisateur ; aucun « playback successful » inventé à partir d'un simple buffering.
- **INTERFACE** demeure client, n'invente ni progression LLM ni citation ; « NON CONNECTÉ », « AUDIO INDISPONIBLE » et « AVATAR TECHNIQUE » sont visibles quand c'est réel.
- **EVOLUTION** expose mesures techniques expurgées. Jamais de patch auto-appliqué, auto-mergé, ou de contournement de sandbox.
- Pas d'internet runtime, télémétrie, lecture de fichiers privés d'un autre package, migration destructrice, changement silencieux de signature, minSdk ou AAR.
- Modèles/art assets téléchargés volontairement, provenance/licence/sha vérifiés, stockage privé et local ; jamais dans Git public.
- Distinction contrats / transport / intégration Android / appareils. DTO ou mock « PASS » n'autorise pas des assertions de communication réelle.

## 3. Phases et portes — une seule grande mission, sous-lots indépendamment vérifiables
### F0 — Inventaire, matrice de compatibilité et BOM verrouillé (UNIFIED, obligatoire avant intégration)
1. Lire les 8 repos, lister toutes les branches candidates et leurs commits, rapport, CI, sourceSets, toolchains, minSdk, ABI, licences, data schema, SHA.
2. Définir une **composition candidate unique** (BOM) dont chaque élément pointe vers un SHA immuable ; signaler les divergences de parents plutôt que les écraser.
3. Cartographier les types/contrats inter-modules : session, génération, idempotence, cancellation, statut natif, identité, source, citation, outil, approbation, voix/temps, animation, diagnostic. Versionner; fournir des exemples valides, invalides et tailles maximales.
4. Tableau de disponibilité par appareil/API24, API25, API26, API29/ARM64, Linux mini-PC : **SUPPORTED / BLOCKED / NOT_TESTED**. Surtout, ne pas remonter CORE minSdk24.
5. Élaborer matrice ownership/données et menaces (replay, TOCTOU, prompt injection, credential confusion, unlink/revoke, arrêt processus, batterie, thermique).
6. Produire un `integration-lock.json` (schéma, refs, SHA, licences, état de CI), script de validation hors ligne, dépendances DAG et commandes reproductibles.

**F0 GO** : outil indépendant sur branches test, diffs et SHA vérifiés, pas de runtime modifié, rapports cohérents. Si 2 branches incompatibles : publier un conflit explicite et proposer stratégie de portage.

### F1 — D'abord une preuve complète in-process JVM (CORE + NEXUS + SERVICES)
1. Partir du vrai `integration-lab` CORE-03 (ne pas remplacer les bibliothèques par des fixtures). Rendre le cycle `intent -> authorization -> NEXUS query -> citations resolvable -> SERVICES preview -> consent -> output` reproductible.
2. Unifier les ID session/request/generation, annulation, deadline, révocation, idempotence et reprise après crash. Réponses tardives rejetées, UNKNOWN ne devient jamais SUCCESS.
3. Démonstrations : recherche documentaire source vérifiable, citation relue ; CSV calcul exact et export isolé ; DOCX refusé sans consentement, puis créé et relu après consentement de fixture ; perte de permission et coupure entre étapes.
4. Pas de LLM fictif présenté comme vrai. Le contexte RAG peut être assemblé/testé hors génération.

**F1 GO** : tests d'intégration multi-modules réels sur JVM, corpus synthétique, provenance sans perte, fichiers sources inchangés, contrôle négatif « consent absent = aucun effet ». Rapport complet + CI.

### F2 — Portage Android API24 et connexion CORE production (CORE/NEXUS/SERVICES)
1. Lire les missions existantes NEXUS `NEX-06A` issue #1 et SERVICES `SERV-04A` issue #1 ; réutiliser les ports et branches déjà publiés, auditer s'ils satisfont réellement Android24, ne pas dupliquer la couche métier.
2. Port SQLite NEXUS non-JDBC sur Android ; vérifier FTS5 réellement disponible, migrations v3/v4 en copie, tests citations/révisions. `UNSUPPORTED` honnête si moteur/format manquant.
3. Port outils SERVICES `InputStream/OutputStream` et SAF, séparation I/O JVM/Android, grilles API24/25/26/35, prompts/permissions hors LLM ; ne pas imposer min26 à CORE.
4. Sous le vrai `SessionCoordinator` CORE, raccorder ports API24, contrôles au départ et au retour, circuit-breaker et capability flags désactivés avant preuve. Préserver 501 tests MEM-02/ID-02, leurs bases et l'ancien fonctionnement.
5. Test instrumenté API24 et 25 d'abord ; API26 et 35 ensuite ; test redémarrage/rotation/mode avion et parcours SAF du fournisseur réel.

**F2 GO** : APK du pilote expérimental contient un chemin in-process réel, sans régression ni élévation minSdk. S'il ne passe pas, ne pas activer — conserver preuve F1.

### F3 — LLM natif unique, seulement après déblocage attesté (LLM + CORE)
1. Réexaminer `LLM-02` `LLM-03`, F3B et CORE004D avec les branches de référence ; audit diff JNI/admission, port de tokenizer exact, worker et recv des cycles.
2. Ne jamais remplacer token count exact par l'estimation NEXUS ou un rejet arbitraire. `UNSUPPORTED` demeure si backend non instrumenté.
3. Fournir preuves **natives** d'allocation, annulation, interruption, déchargement/réservation, reprise après process death et unicité de résidence ; isoler d'abord sur fixtures et appareil d'essai autorisé.
4. LLM sélectionné en catalogue n'est pas un worker actif. Interface signale cette différence.
5. Trois profils mobiles sûrs (tablette ÉCO, ÉCO, ÉQUILIBRÉ/AUTO existants), pleine puissance future dédiée uniquement après qualification matérielle ; modèles interchangeables, même contrat CORE.

**F3 GO** : tests natifs + instrumentés Android réels + mesures sur appareils autorisés. **Blocage attendu tant que F3B/reçus natifs ne sont pas validés.** Ne pas activer un service « intelligent » complet artificiellement.

### F4 — Conversation et écran unifié (INTERFACE + CORE)
1. Construire protocole réel `SubmitText / Ack / TextChunk / Stop / Completed / Error / ToolApproval / Citation` avec ID corrélés, version, quotas, authenticité, anti-rejeu et états de connexion ; lire les issues UI-03 #4 et les dernières branches du module.
2. Isoler la fixture de chat, la vraie réponse et les scénarios de démonstration ; interdiction de réponses préenregistrées présentées comme LLM.
3. Appliquer éditeur de personnalité en **prévisualisation/diff/validation explicite** par CORE sans écrire par défaut dans IDENTITY ni les souvenirs.
4. Surface Android adaptative mobile/tablette, commandes d'arrêt accessibles, affichage des permissions, origines, mémoire, modes énergie, statut téléphone/mini-PC réel.
5. Préserver tableaux de bord/DataStore/profils et la logique de restauration.

**F4 GO** : dialogue de bout en bout avec vrai CORE (contenu et non métadonnées seules), refus IPC testés, rotation et processus tué. Si F3 bloquée, mode conversation sans LLM clairement limité.

### F5 — Voix et Athéna (VOICE + INTERFACE + CORE)
1. Réutiliser le pilote Binder authentifié et ses refus ; exiger certificat/package/grants exacts, activation revocable, compatibilité minSdk ; ne pas ouvrir un service exporté général.
2. Publier événements typés `SPEECH_ACCEPTED/PCM_READY/STARTED/PAUSED/FINISHED/UNKNOWN` avec source de l'horloge explicitée. Ne pas inférer phonèmes/lip-sync d'offsets PCM.
3. Conserver TTS CORE historique en mode sûr. Autoriser la nouvelle voix seulement quand modèle et admission réellement qualifiés ; profil vocal privé inchangé, aucun transfert silencieux. Émotions non attestées = `UNSUPPORTED`.
4. AVATAR : validation du style Athéna convenu (anime stylisé, cheveux bruns mi-longs, uniforme futuriste bleu nuit), différence V1 technique vs rig premium clair; assets privés/licences validés ; rendu 2.5D calques articulés, paupières, bouche, tête, expressions, synchronisation approximative explicitement présentée comme telle avant phonèmes vérifiés.
5. Tests : phrases courtes/longues, annulation, interruption, rotation, audio conflict, latence, battery/thermal ; grilles matériels réelles. Pas de « 50 tokens » ou « moins de 30s » promis sans mesures.

**F5 GO** : avatar et voix se synchronisent sur des signaux réellement disponibles, avec désactivation/retour au TTS historique et indication des limites.

### F6 — NEXUS enrichi + SERVICES de production (CORE)
- Préserver bibliothèque NEXUS v4, graph, recherche citée et restaurations isolées ; qualification Kotlin/JVM non exécutée à exécuter avant intégration ; vérifier licences, tailles, quotas, entrées XML/ZIP malveillantes.
- ZIM Kiwix est **lecture seule**, accès choisi, sans copie/embedding complet d'archive géante, sans prétendre à la qualification Android.
- SERVICES : vrai catalogue de 17 outils, workflow durable, fichiers PDF/DOCX/CSV réels, confirmation exacte, grants SAF, annulation et journal expurgé. Le BRIDGE authentifié peut rester désactivé en attendant transport local/mini-PC autorisé.
- Pas d'OCR, XLSX universel, embeddings ou recherche sémantique neuronale revendiqués sans code/tests.

### F7 — EVOLUTION observe mais ne répare pas
- Intégration en **lecture technique expurgée** : code erreur, état, métrique bornée, aucun texte, conversation, chemin privé ni profil.
- Diagnostique local et recommandations traçables, rapports au propriétaire ; pas de lecture croisée des bases.
- Préparation de patch : signatures/hash/revue humaine uniquement. Runner `NOT_RUN/SANDBOX_NOT_QUALIFIED` inchangé jusqu'à qualification OS **indépendante** et validation explicite. Aucun auto-merge ni auto-deploy.

### F8 — Livraison expérimentale et qualification
- `unified-integration-matrix.md` : pour chaque intégration, `contract / code / JVM / Android emulator / physical / production / merge`.
- Construire une distribution **candidature** : éventuellement APK CORE + INTERFACE + modules séparés, choix architectural par mesures plutôt que forcer une APK.
- Tester Android API24/25/26/29/35, ARM64, mode avion, SAF, permissions refusées, taille/install, chaleur/batterie/PSS (sans promesse sur matériel non testé). Mini-PC = port/maquette tant que hardware non fourni.
- Scénarios E2E : (A) conversation avec un **vrai** moteur local ; (B) document importé -> citation consultable -> réponse ; (C) nettoyage CSV -> aperçu -> confirmation -> fichier relu ; (D) commande vocale/lecture + animation *uniquement si qualifiées* ; (E) reprise/rollback et refus ; (F) diagnostic EVOLUTION expurgé. Marquer séparément PASS/BLOCKED/NOT_RUN.
- Tests régression ciblés + suites historiques par dépôt ; comparer builds, signatures, minSdk, comptes de tests réellement exécutés, consommateurs et producteurs contrats.
- Dossier final : architecture, comparaison à la base, arbre de SHAs/branches, inventaire fichiers, licences, checksums APK, logs, limitations, risques, stratégie rollback sans toucher aux originaux, décisions demandées, prochaine tranche.
- PR **brouillon** uniquement, jamais merge/auto-merge. Aucun renommage de dépôt historique sans accord.

## 4. Conditions d'arrêt obligatoires
STOP/BLOCKED si branche ou SHA source introuvable, bibliothèque API24 incompatible, FTS5 indisponible, contrats divergents, collision d'autorités, vérification d'origine/signature absente, état natif UNKNOWN, test de consentement échoué, transfert de données personnelles nécessaire, profil Athéna non approuvé, sandbox indisponible, licence/art privé douteux. Conserver alors un incrément sûr et proposer la décision précise à valider.

## 5. Gouvernance et livrables Codex
Chaque sous-mission dans un dépôt travaille **sur branche neuve** issue d'une base explicite, sans réécriture d'historique, force-push, merge, installation personnelle ni publication d'assets. Une seule équipe propriétaire par fichier ; adaptations croisées via PR séparées/ports, pas par commits dans les autres dépôts. Ne pas activer les phases bloquées juste pour terminer la campagne.

**Rapport obligatoire par livraison :** module ; mission/lot ; branche ; SHA base et HEAD ; autres SHA consommés ; fichiers ; code écrit ; tests réellement exécutés (nombre, commande, environnement) ; tests non faits ; compatibilité ; risque/régression ; statut GO/PARTIAL/BLOCKED ; prochaine action/décision. Les anciens tests ne sont pas comptés comme fraîchement exécutés. Séparer « client simulé », « vraie bibliothèque JVM », « Android instrumenté », « appareil réel ».

**Ordre de départ :** F0 (UNIFIED) -> F1 CORE/JVM et comparaison ports API24 (NEXUS/SERVICES) -> F2 CORE Android/API24 -> F4 interface sur capacités prouvées -> F3 LLM/ F5 Voice sous revue distincte -> F6/F7/F8. Paralléliser les travaux sans dépendance dure ; arrêter proprement les branches bloquées.

## 6. Définition de terminé
**Intégration expérimentale V1** = réel parcours document/outil et chat CORE Android sûr + preuves appareils/emulateurs pertinentes, matrice intégrations, aucun doublon d'autorité, retour arrière possible. **Vision JARVIS finale** (voix hautement naturelle/émotionnelle, vrai rig Athéna, performance mini-PC, auto-évolution) demeure **une étape ultérieure**. Ne pas étiqueter cette issue DONE si des sections obligatoires sont BLOCKED : livrer un bilan de portée réelle.

### Instruction courte à Codex
> Consulte l'issue `FUSION-01` de ton dépôt et cette issue directrice UNIFIED. Revalide les SHA et exécute uniquement la première tranche techniquement éligible sur une nouvelle branche. Tests/preuves/rapport, PR brouillon si possible, jamais de merge main ni activation sensible.

## 7. Index des sous-missions déposées dans leurs dépôts (25 octobre 2026)
Chaque issue contient les objectifs, sources SHA, fichiers cibles, scénarios, gates et preuves propres au module. Le dépôt CORE est nommé **JAVIS** (orthographe historique) ; EVOLUTION est sur le dépôt **JARVIS-VOLUTION-**. La liste est la référence de lancement ; ne pas redéposer de ticket identique.

| Propriétaire | Issue du dépôt | Dépendance/cible principale |
| --- | --- | --- |
| CORE | [CORE FUSION-01](https://github.com/bubth720/JAVIS-Local-v2/issues/15) | Convergence CORE03/JVM → ports Android24, SessionCoordinator et véritables sorties |
| LLM | [LLM FUSION-01](https://github.com/bubth720/JARVIS-LLM/issues/3) | Tokenisation/JNI native bloqués, modèle unique, matériel |
| VOICE | [VOICE FUSION-01](https://github.com/bubth720/JARVIS-VOICE/issues/21) | Connexion autorisée, voix longue, consentements, événements et STT |
| INTERFACE + AVATAR | [INTERFACE FUSION-01](https://github.com/bubth720/JARVIS-INTERFACE/issues/5) | Vrai chat CORE, consentements, vrai rig Athéna 2.5D |
| NEXUS | [NEXUS FUSION-01](https://github.com/bubth720/JARVIS-NEXUS/issues/2) | NEX-06A, SQLite/FTS5 API24, bibliothèque v4/citations |
| SERVICES | [SERVICES FUSION-01](https://github.com/bubth720/JARVIS-SERVICES/issues/2) | SERV-04A, outils portables API24, exports confirmés |
| EVOLUTION | [EVOLUTION FUSION-01](https://github.com/bubth720/JARVIS-VOLUTION-/issues/1) | Observation expurgée, diagnostic sans patch appliqué |

**Parallélisme initial autorisé par les issues :** UNIFIED F0 ; CORE C0/C1 ; NEXUS N0/N1/N2 en laboratoire ; SERVICES S0/S1 en laboratoire ; INTERFACE I0/I3 design/revue sans activation ; LLM L0/L1 ; VOICE V0/V1 sans raccordement sensible ; EVOLUTION E0/E1 désactivé. Les blocs dépendants Android, JNI, Binder, données privées restent aux portes de validation spécifiques.
