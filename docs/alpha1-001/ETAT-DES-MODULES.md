# ALPHA1-001 — état consolidé des modules

Audit du 18 octobre 2026. État vérifié dans GitHub et le workspace avant modification.
Dépôt de livraison : bubth720/JAVIS-Local-v2 ; base71831913a772bcc51b20885b52b965f5d3e9f61b.
Aucun dépôt extérieur modifié. Les valeurs ci-dessous sont des preuves historiques
consultées, distinctes des tests ALPHA1-001 réexécutés dans VALIDATION.md.

## Références distantes retrouvées et SHA vérifiés

| Module / branche | Commit | Rapport et constat |
|---|---|---|
| CORE feature/identity-002-core-runtime |8adec3e0e8dfdce685a33ef796fc3150facc7f0a|[ID-002](https://github.com/bubth720/JAVIS-Local-v2/blob/8adec3e0e8dfdce685a33ef796fc3150facc7f0a/docs/identity-002/REPORT.md) : socle, codec/six vecteurs, persona inactive |
| CORE feature/identity-002t-tokenizer-port |71831913a772bcc51b20885b52b965f5d3e9f61b|[ID-002T](https://github.com/bubth720/JAVIS-Local-v2/blob/71831913a772bcc51b20885b52b965f5d3e9f61b/docs/identity-002t/REPORT.md) :268 tests rapportés ; mesure JNI préalable absente ; diff sensible non appliqué |
| CORE fixture/004c-p2-client |da40ed7155409b85860a22c1dbbfe35029cea29a|[004C](https://github.com/bubth720/JAVIS-Local-v2/blob/da40ed7155409b85860a22c1dbbfe35029cea29a/docs/mission-004c/REPORT.md) : fixture séparée, récupération bloquante, pas E2E natif |
| CORE feat/core-voice-pilot |7426665f2794cba83646008fa0f342d102141201|[CORE–VOICE](https://github.com/bubth720/JAVIS-Local-v2/blob/7426665f2794cba83646008fa0f342d102141201/docs/mission-core-voice-pilot/RESULTATS.md) :57 tests rapportés, C1 désactivée, C2 native bloquée, C3 absente |
| LLM feat/003f-f3-native-observation |aa85ad4b506bb655dbe5b50ec4b125d19b48e81e|[F3A](https://github.com/bubth720/JARVIS-LLM/blob/aa85ad4b506bb655dbe5b50ec4b125d19b48e81e/docs/mission-003f-f3a/RAPPORT.md) :215 JUnit rapportés, composants isolés, aucun reçu terminal natif |
| LLM design/003f-f3b-worker-connection |8f1c2f5a936d6c01874d7be6bb987f4459b83374|[F3B](https://github.com/bubth720/JARVIS-LLM/blob/8f1c2f5a936d6c01874d7be6bb987f4459b83374/docs/mission-003f-f3b/RAPPORT.md) :232 JUnit et CTest1/1 rapportés ; patch exact livré NON APPLIQUÉ |
| UNIFIED design/identity-001b-contract-freeze |d7c90785ae4263892a6c96e140cfa88206cb4197|[Contrat IDENTITY](https://github.com/bubth720/JARVIS-UNIFIED/tree/d7c90785ae4263892a6c96e140cfa88206cb4197/docs/IDENTITY/001B) : bundle431b5212…e619de approuvé ; style B actuel VOUS/OFF |
| UNIFIED design/d4-02e-f1-f2-recovery-clarification |49dd4027d2ccb54bf330f591d3ee17f5dc577132|[D4-02E](https://github.com/bubth720/JARVIS-UNIFIED/tree/49dd4027d2ccb54bf330f591d3ee17f5dc577132/docs/D4-02E) : F1/F2, deux preuves propres, COMMIT/outboxes ; F3 et revue /2 restent gates |
| INTERFACE test/interface-device-006 |ebd6c5fc873054d3f04bb2388d53e407dc73b7b7|[006](https://github.com/bubth720/JARVIS-INTERFACE/blob/ebd6c5fc873054d3f04bb2388d53e407dc73b7b7/docs/mission-006.md) :59 JVM,20 instrumentés API26,10 reprises processus rapportés ; appareils personnels non qualifiés |
| INTERFACE feature/interface-avatar-001 |139bfdc4aac5d2fc9b841c5b804c3b89bfc57ec6|[Avatar](https://github.com/bubth720/JARVIS-INTERFACE/blob/139bfdc4aac5d2fc9b841c5b804c3b89bfc57ec6/docs/avatar/architecture-et-validation.md) :76 JVM,29 instrumentés API26 rapportés ; prototype vectoriel, pas rendu artistique final ni connexion CORE |
| VOICE feat/voice-bridge-pilot |04c4ca1c050c16e097d27b08831544b66121128c|[Pilote](https://github.com/bubth720/JARVIS-VOICE/blob/04c4ca1c050c16e097d27b08831544b66121128c/docs/mission-voice-bridge-pilot/RESULTATS.md) : authentification Binder API26/35,13 succès/14 sauts par API ; génération toujours refusée |

Les branches CORE ID-002T et CORE–VOICE divergent depuis226e2e8. La base retenue
contient les protections004B/004C et n'est pas remplacée par la branche vocale plus
ancienne. Aucune fusion/cherry-pick. UNIFIED est un registre documentaire, pas un runtime.

## Missions explicitement recherchées

| Nom demandé | Résultat de la recherche | Conclusion |
|---|---|---|
| CORE ID-002N | Aucun rapport/branche portant ce nom retrouvé dans les emplacements inspectés | NON RETROUVÉ / NON VÉRIFIABLE ; pas déclaré échoué, aucune modification JNI présumée |
| IDENTITY ATHENA-001 | Proposition documentaire présente dans la conversation ; aucun rapport publié correspondant retrouvé | NON VÉRIFIABLE indépendamment sur GitHub ; aucune approbation du futur contrat présumée ; portrait personnel non recopié dans Git |
| INTERFACE ID-UI-001 | Aucun rapport sous ce nom ; missions002–006 et avatar retrouvées | Nom non vérifié ; ne pas renommer rétroactivement une autre mission |
| COORD-001 | Audit documentaire dans la conversation ; pas de rapport publié retrouvé dans les branches examinées | Non publié / non vérifiable indépendamment ; décisions sensibles non approuvées par cet audit |
| CORE 004D-PREP | [Issue CORE5](https://github.com/bubth720/JAVIS-Local-v2/issues/5) et spécification UNIFIED003F/004D retrouvées | Préparation retrouvée ; issue OPEN et BLOQUÉE, aucun vérificateur004D implémenté présumé |
| LLM F3B | Rapport, mapping, patch, manifeste et CI retrouvés au SHA ci-dessus | Livraison documentaire/test candidats prouvée ; modification native toujours suspendue |

F3B publie un patch SHA256 be4bcce5ec7066851a09e2e372edca562a45dad3a3a88e35b6a7d5792c07b374.
Cette mission ne l'applique pas. Backend instrumenté proposé non compilé, F3
native non prouvée, admission /2 fermée et CORE004D toujours bloquée.

## Étendue réelle de la collecte

- git ls-remote de tous les heads des cinq dépôts autorisés ; SHA fournis comparés.
- Inventaire des refs CORE disponibles et recherche des noms dans leurs documents.
- Archives immuables publiques recherchées sur les24 branches UNIFIED/INTERFACE,
 fichiers Markdown/JSON/texte<1Mo ; snapshots INTERFACE006/avatar et LLMF3B lus.
- Recherche ciblée dans les checkouts autorisés, docs antérieurs et fichiers publics
 disponibles dans le workspace ; aucune base personnelle ou clé inspectée.
- Listes publiques issues/PR des cinq dépôts consultées. CORE issues5/8 lues ;
 PR9 brouillon CORE–VOICE et PR4 stabilité constatées ; aucun merge ni nouvelle issue.
- [CI F3B39077934892](https://github.com/bubth720/JARVIS-LLM/actions/runs/39077934892/job/117528861858)
 consultée : succès au code99b946e ; le patch sensible n'est pas appliqué par cette CI.
- [CI VOICE39060156787](https://github.com/bubth720/JARVIS-VOICE/actions/runs/39060156787/job/117476280339)
 consultée : succès du pilote Binder au codeea37d5a, pas preuve de synthèse.
- Les XML/JSON INTERFACE sont des résultats historiques versionnés consultés,
 pas des tests reproduits par ALPHA1-001. Les tests physiques restent inconnus.

La recherche bornée ne prouve pas une absence dans toutes les conversations,
commentaires, branches supprimées ou contenus privés. Les listes publiques ouvertes
ne constituent pas un inventaire exhaustif de toute issue fermée. Aucune information
introuvable n'est remplacée par une hypothèse ni demandée à l'utilisateur pour
continuer les travaux indépendants.
