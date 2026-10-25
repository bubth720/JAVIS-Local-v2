# FUSION-01 — CORE C0/C1

Module : CORE, dépôt `bubth720/JAVIS-Local-v2`. Issues directrice UNIFIED#4 et CORE#15.
Branche : **feat/fusion-01-core-integration**.
Base immuable : **aea5ac859ac551d54fe16187c1883a487dbee9d6** (CORE03).
Le commit de livraison est celui contenant ce rapport ; son SHA exact est fourni
avec le lien de publication. Ne pas écrire un faux SHA autoréférentiel.

**Bilan : C0 vérifié localement ; C1 PARTIAL ; C2 BLOCKED ; C3 DESIGN_ONLY ; C4 BLOCKED.**
FUSION-01 n'est pas terminée et aucune conversation Android intégrée n'est revendiquée.

## Fonctionnement réellement démontré

Le laboratoire autonome appelle les vraies bibliothèques NEXUS et SERVICES
épinglées, sur Linux x86_64/JDK17. Les sources, consentements et identités d'hôte
sont **synthétiques (FIXTURE)** ; les moteurs de recherche/SQLite/CSV/DOCX sont réels.

1. Un vrai TXT isolé est importé/indexé par NEXUS. Recherche, résolution et relecture
   vérifient URI, SHA256, page et offsets. Le contenu reste un contexte JSON non
   fiable, jamais une instruction SYSTEM ni une permission. Source inchangée.
2. SERVICES normalise/dédoublonne un CSV. Son Data.Table.stats calcule exactement
   `0.10 + 0.20 = 0.30`, moyenne `0.15` (DECIMAL128 annoncé pour les moyennes).
3. Aucun export sans confirmation exacte. Après consentement **de fixture**, un
   vrai CSV ou DOCX est créé par Documents.export puis relu et vérifié. Le source
   CSV reste intact, les cibles sont nouvelles dans le dossier temporaire du labo.
4. Session/request/generation suivent le parcours ; deux IDs d'étapes sont dérivés
   de l'ID parent, sans changer la session/génération. Contrôles d'autorité aux
   frontières, délai monotone borné, confirmation consommable et refus du rejeu.
5. Les reçus techniques utilisent le **Store SERVICES existant**, namespace
   core_workflow_receipts, sans texte, CSV, chemin ou autorisation. PREPARED et
   DISPATCHED interrompus deviennent UNKNOWN après reprise par un propriétaire
   arrêté ; aucun retry/reset. Corruption refusée ; autres namespaces préservés.

Aucun nouveau registre d'outils, mémoire, modèle ou runtime Android créé. Le seul
nouveau binding CSV est dans le registre SERVICES existant du **host de labo**,
vers son writer CSV déjà livré ; il n'ajoute pas un catalogue parallèle.

## Corrections démontrées et périmètre

- **Résultat annulé accepté** : reproduit sur l'adaptateur CORE03 original,
  `regression-before-fix.xml` échoue avec CANCELLED_RESULT_ACCEPTED. L'invalidation
  est maintenant sticky : cancel/timeout/interruption/révocation/mauvaise corrélation
  ne peuvent devenir un succès lors d'une seconde attente. Aucun unload déduit.
- **Grant de confirmation périmé** : le Guard.Context SERVICES est un snapshot.
  L'adaptateur revalide désormais aussi `HostAccessPort(..., confirm)` au moment
  de confirmer ; le test refuse malgré le grant statique du host SERVICES.
- Workflow JVM ajouté, pas de réécriture du noyau : état durable avant dispatch,
  attente sans verrou de cancellation, acceptation finale sérialisée avec le stop
  logique, un export actif/inconnu bloque le suivant. Reçus et previews bornés.

Fichiers fonctionnels modifiés : CoreIntegrationLab.kt, ServicesToolAdapter.kt et
NexusSearchAdapter.kt (ajout de la frontière KnowledgeReadPort, logique de recherche
conservée). Le workflow dépend des interfaces KnowledgeReadPort/ServicesToolPort ;
les anciens SourceSearchPort/ToolExecutionPort restent disponibles.
Ajouts : DocumentToolWorkflow.kt, WorkflowReceipts.kt, FusionWorkflowTest.kt.
Autres changements justifiés : README du labo, documentation de connaissances,
verrou/provenance/preuves, validateur offline et workflow CI dédié sans modèles.
**Aucun fichier app/, fixture, build Android, contrat historique, JNI ou dépendance
n'a été changé.** Le garde protège les fichiers historiques hors ces trois adaptations
JVM et les documents autorisés ; voir scope-check.json et le diff Git.

## Résultats exécutés dans cette session

| Vérification | Résultat |
| --- | --- |
| app:testDebugUnitTest | 448 réussis, 0 échec/erreur/ignoré |
| app:testMvp2UnitTest, filtres Mvp et MemoryLabUi | 53 réussis, 0 échec/erreur/ignoré |
| p2-client:testDebugUnitTest | 36 réussis, 0 échec/erreur/ignoré |
| Laboratoire CORE03 avant changement | 10 réussis |
| Laboratoire FUSION final | 30 réussis, dont les 10 historiques +20 nouveaux |
| Build app debug/mvp2/mvp3 | Réussi |
| Lint debug / mvp2 | 0 erreur ; 27 /37 avertissements conservés |
| APK debug/mvp2/mvp3 : signature, minSdk, réseau, ABI | Signature vérifiée, minSdk24, ARM64 seulement, pas de permissions réseau ni GGUF |
| Verrou/scope/filiation et identité des 501+36 cas XML actuels | Réussis ; 501 cas listés individuellement |
| Six vecteurs négatifs du verrou | Tous refusés (base, module, SHA flottant, hash, cycle, faux GO) |
| CLI de vraie recherche/outils | Réussi ; somme0.30 et export relu ; Android/native=false |
| git diff --check | Réussi avant livraison |

**567 cas distincts réussis** : 501 +36 +30. Les 10 tests baseline, rejoués dans
les30, ne sont pas additionnés une deuxième fois. Ni les six vecteurs du script,
ni les147 tests annoncés par UNIFIED ne gonflent ce total.
Les suites app/fixture sont JVM/Robolectric (dont SQLite natif hôte), pas de
l'instrumentation Android. Les nouveaux tests sont JVM, sans JNI Llama.

Preuves brutes : `evidence/xml/`, journaux baseline/final, inventaires JSON et
contrôles APK/Lint. Les logs texte retirent seulement les espaces finaux ; les
originaux exacts sont conservés en .log.gz, empreintes dans log-normalization.json.
La reproduction d'un défaut sur l'ancien code est un **échec
attendu conservé**, pas un échec actuel de la nouvelle suite.

Échecs séparés : le premier essai JVM --offline a échoué sur le plugin Kotlin
non encore en cache ; le même build online a résolu les versions déclarées,
puis les runs finaux offline ont réussi. Un parenthésage dans un nouveau test a
été corrigé avant les résultats finaux. Les anciens check-core-03.py et
check-mem-02.py restent **en échec de périmètre** sur les nouveaux livrables et
le dossier préexistant docs/identity-002n ; aucune règle n'a été assouplie.
Le garde spécifique FUSION compare la base exacte et tous les fichiers protégés.

Non exécutés : instrumentation Android24/25/26/29/35, FTS5/SAF Android réel,
rotation/process kill de l'application, JNI/GGUF, LLM/VOICE Binder, audition TTS,
mesures RAM/thermique/batterie, appareils personnels. Lint mvp3 non rejoué dans
cette tranche (build mvp3 exécuté ; Lint debug/mvp2 couvre le périmètre inchangé).
Le redémarrage/ACK perdu des nouveaux tests est une injection JVM/reconnexion
SQLite contrôlée, pas une coupure physique ni un crash Android réel.

## Références et portes vérifiées

Le graphe local et les huit références distantes sont documentés dans
[COMPATIBILITY.md](COMPATIBILITY.md), le verrou et lineage.json. CORE03 contient
MEM02/ID02 corrigé ; CORE–VOICE divergent n'est pas cherry-pické. Deux dépendances
réellement compilées restent NEX777/SERV3de, pas les branches finales.

F0 UNIFIED **3caa57d91ce0dba31043e7cb6373a5e2df8b9fa4** a été découvert et lu
avant publication : verrou exact, rapport et SHA conservés sous reference/.
Ses primaires NEX374/SERVf000 sont les futurs candidats Android, les finales
v4/17 outils étant alternatives. Les six autres primaires concordent avec notre
photographie. Cette comparaison n'applique aucun de ces ports à l'APK.
Les147 tests F0 annoncés, les tests producteurs et les CI distantes sont des
preuves historiques/observées, pas des exécutions locales supplémentaires.

C2 : NEX374 indique FTS5 absent sur hôte, RAG Android non qualifié et zéro
instrumentation ; SERVf000 annonce40 Robolectric mais aucune instrumentation,
export SAF_EXPORT_UNQUALIFIED. F0 observe aussi leurs CI en échec. Ces portes ne
sont donc pas levées. Aucun JDBC dans APK, minSdk inchangé.
C3 : [proposition de contrat](CONVERSATION-DESIGN.md) seulement, soumise à revue
commune ; pas de service exporté/permissions nouvelles ni remplacement /1 ou /2.
C4 : JNI/F3B/garanties de résidence native non prouvés, tokenizer exact UNSUPPORTED,
VOICE non raccordée et profilB intact ; TTS historique conservé.

## Limite de sécurité empêchant un GO global C1

**La révocation n'est pas linéarisée atomiquement avec le début de mutation dans
le worker SERVICES épinglé.** Une tâche déjà admise peut créer un fichier avant
que CORE refuse son résultat devenu périmé. Le workflow ne prétend pas annuler
ce fichier ni garantir un rollback. Les tests vérifient « aucun effet avant
consentement » et « aucune acceptation tardive », pas « aucun effet après toute
révocation concurrente ». Une coopération du producteur, à livrer dans sa propre
mission, est nécessaire pour une garantie plus forte. Aucun autre dépôt modifié.

Le Store et le propriétaire sont in-process JVM, sans fencing de processus ;
recoverInterrupted nécessite que l'ancien propriétaire soit arrêté. Aucune
restauration automatique, autorité persistée ou réadmission native n'en découle.
Les licences racine des projets restent à clarifier avant redistribution globale.

## Reproduction

Depuis le checkout CORE, avec JDK17/Gradle8.10.2 et SDK35 :

```sh
# Dépendances dans des snapshots de build ; utiliser les objets exacts déjà vérifiés.
python3 tools/prepare-core03-sources.py nexus /tmp/fusion-nexus --checkout /chemin/git-contenant-nexus777
python3 tools/prepare-core03-sources.py services /tmp/fusion-services --checkout /chemin/git-contenant-services3de
gradle -p integration-lab -PcoreNexusSource=/tmp/fusion-nexus \
  -PcoreServicesSource=/tmp/fusion-services test run --offline --no-daemon --max-workers=2 \
  -Pkotlin.compiler.execution.strategy=in-process
gradle -PincludeP2Fixture=true :app:testDebugUnitTest \
  :app:testMvp2UnitTest --tests '*Mvp*' --tests '*MemoryLabUiTest*' \
  :p2-client:testDebugUnitTest :app:assembleDebug :app:assembleMvp2 :app:assembleMvp3 \
  :app:lintDebug :app:lintMvp2 --no-daemon --max-workers=2
python3 tools/check-fusion-01.py --verify-results
git diff --check
```

--offline suppose un cache complet ; retirer ce flag pour résoudre les dépendances
publiques de build, jamais pour télécharger des modèles. Le workflow CI fait les
checkouts aux SHA précis, vérifie les snapshots et publie uniquement rapports XML.
Aucun service permanent ou nouvelle configuration cloud nécessaire ; les outils
existants suffisent. Le dossier utilisateur non suivi docs/identity-002n est intact
et exclu de la livraison. Aucun modèle chargé/téléchargé pour FUSION-01.

## Livraison et prochaine action

Publier uniquement cette branche, PR **brouillon vers feat/core-03-intensive** afin
de montrer la tranche sans importer implicitement tout l'historique dans main.
Aucun merge, installation, activation, changement de données personnelles,
signature, budget ou protection. Les APK contrôlées ne sont pas distribuées comme
une version unifiée qualifiée. Le SHA publié et la CI sont vérifiables sur la PR.

Prochaine action : revoir C0/C1 et le fence de révocation avec le propriétaire
SERVICES, recevoir F0, puis qualifier NEXUS FTS5 et les ports API24/25 sur un
laboratoire autorisé. C2 ne commence qu'après ces preuves. Aucun accord sensible
supplémentaire n'est demandé pour masquer les blocages : la tranche s'arrête ici.
