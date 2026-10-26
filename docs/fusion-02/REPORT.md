# FUSION-02 CORE — livraison C0, intégration Android bloquée

Module : CORE / `bubth720/JAVIS-Local-v2`. Mission directrice UNIFIED#5,
lot CORE#17, préparation CORE03A#14 conservée (pas de deuxième intégration).
Branche : **feat/fusion-02-core-readiness**.
Base exacte : **36761cea101b5f88b87cd4136dc075d28a1adb96**, FUSION01, PR brouillon#16.
HEAD livré : commit contenant ce rapport, SHA exact communiqué après publication.
Aucun merge, cherry-pick, force-push, installation ni modification d'un autre dépôt.

**Statut : C0 SOFTWARE_PASS ; C1–C4 BLOCKED ; FUSION-02 globale PARTIAL/BLOCKED.**
Les dépendances ne permettent pas de livrer le pilote NEXUS/SERVICES Android réel.
Conformément à CORE#17, seule l'interface désactivée et ses refus sont développés.

## 1. État réel vérifié et filiation

Le HEAD distant FUSION01 et le parent local concordent à36761cea. Ce commit descend
de CORE03 aea5ac8, MEM02 49ad428 et ID02 corrigé283423c ; aucune importation de
CORE–VOICE divergent ou CORE004D n'a été faite. Main reste à226e2e8cc3eebd479805b89d035eb5c1cbcad540.
Les sources, AGENTS, PROJECT_KNOWLEDGE, AppGraph, SessionCoordinator, PromptBuilder,
les ports, les tests, les rapports et workflows ont été examinés. Les trois issues
applicables sont conservées sous ISSUE-*.md. Le WIP non suivi docs/identity-002n
est conservé sans modification ni inclusion au commit.

La CI FUSION01 CORE40215604539 est **SUCCESS à36761cea**, recontrôlée via l'API.
Les567 cas de référence ont été **rejoués ici avant modification**, pas hérités
du rapport :448 debug +53 MVP2 +36 fixture +30 vrai laboratoire JVM.
Les objets et snapshots NEX777/SERV3de de FUSION01 sont toujours vérifiés par le
helper existant ; aucun remplacement de dépendance ni algorithme recopié.

### Dépendances et CI observées, sans les requalifier

| Référence exacte | Fait vérifié | Conséquence |
| --- | --- | --- |
| NEX374 `374ea2e2fd640e78f2f5e12e60e75b493420e504` | CI40115049468 échoue dans setup-android ; pas de preuve RAG instrumentée24/25 | Pas de composition Android |
| NEXUS FUSION01 `9d6928cd8651f83fa5091bdb474b33761d366412` | v4/API24 réconciliés, rapport95 JVM/37 Robolectric, **0 instrumenté** ; aucun run exact observé | Progrès producteur reconnu, pas GO Android |
| SERVf000 `f00091a639612b91eca1a301580c8f531249f1e3` | CI40115305244 : JVM vert, SDK Android24 en échec ; exportDocx refuse SAF_EXPORT_UNQUALIFIED | Mutation non admissible |
| Même CI SERVICES, job android-instrumented vert | Workflow exact : api-level26, tâche **:android:connectedDebugAndroidTest** | Ancienne application26, pas SDK24/25 ; aucune preuve héritée |
| UNIFIED F0 `3caa57d91ce0dba31043e7cb6373a5e2df8b9fa4` | CI40214722427 en échec, pas de job détaillé exposé ; rapport local147 historique | Pas de faux GO global |

Les têtes NEXUS/SERVICES/UNIFIED ont été revérifiées à la reprise : aucune livraison
FUSION02 admissible n'est observée. Les parents/arbres/SHA/CI et hashes des sources
lues figurent dans PROVENANCE.json ; evidence/heads-rechecked.json date le relevé.
Les succès annoncés par les producteurs **ne sont pas additionnés** à nos tests.
Les erreurs des workflows tiers sont la responsabilité de leurs propriétaires ;
aucun fichier tiers n'a été modifié pour les contourner.

## 2. Code livré et comportement

- **Ajout** `app/.../integrations/DocumentToolReadiness.kt` : interface locale passive,
  dépendances NEXUS/SERVICES et refus typés. Les sorties effectives sont BLOCKED,
  UNAVAILABLE ou UNKNOWN avec raison finie, sans texte privé. `enabled=false` et
  `canExecute=false` restent constants, même après requestLabOptIn.
- **AppGraph.kt** : exactement deux lignes supplémentaires composent la projection
  avec **son ConnectorRegistry existant**. Aucun second registre/Guard/mémoire,
  aucune ouverture de DB ou démarrage de worker par la projection. Pas d'appel
  discover/connect/disconnect, de sélection SAF ou de chargement de modèle.
- **Ajout** DocumentToolReadinessTest.kt :32 nouveaux cas Robolectric (8 scénarios
  sur24/25/26/35), avec métadonnées de connecteurs simulées explicitement ; pas de
  faux NEXUS/CSV/DOCX présenté comme une intégration Android.
- **Ajout** check-fusion-02.py : garde hors ligne contre la base exacte ; AppGraph
  doit être égal au blob historique plus l'ajout passif exact, instructions append-only,
 459 autres fichiers historiques protégés. Le garde vérifie les inventaires XML
  actuels, les567 cas de référence et les32 nouveaux, et refuse les faux GO.
- **Ajout** workflow fusion-02-core.yml : environnement et SDK épinglés existants,
  dépendances JVM aux SHA historiques, tests avant SDK, builds/Lint, preuves XML.
  Aucun modèle téléchargé, appareil installé ou workflow de publication déclenché.
- Documentation des contrats et prérequis, PROVENANCE/VALIDATION, matrices et preuves.
  Les anciennes instructions restent présentes, avec un appendice FUSION02.

La simple annonce CONNECTED n'est pas une preuve de qualification ou de permission :
la nouvelle projection la classe UNKNOWN. DEGRADED ne réinitialise rien. Les échecs
de lecture de health sont expurgés ; l'erreur privée n'est pas propagée dans le refus.
L'opt-in demandé est toujours refusé : il n'existe aucune voie d'admission ou factory
producteur dans cet incrément. Ceci est une **préparation C0**, pas C1/C2 simulée.

## 3. Corrections et problèmes confirmés / à vérifier

| Constat | Preuve | Statut / action |
| --- | --- | --- |
| Une santé CONNECTED ne prouve pas Android24/25 ni grant CORE | Ancien ModuleHealth ne transporte aucune attestation ; tests négatifs du nouvel observateur | Refus explicite désormais disponible ; pas d'ancien appel métier modifié |
| Ancien Guard.Context SERVICES est un snapshot, sans authorizeAtCommit publié | Source ServicesAndroidToolPort f00091a et limite FUSION01 | BLOCKED ; contrat d'engagement proposé, aucune correction runtime interrepo implicite |
| DOCX SDK Android non qualifié | exportDocx lance Failure(SAF_EXPORT_UNQUALIFIED) | Refus conservé ; pas de faux export |
| Ouverture persistante avant sonde de capacité NEXUS | AndroidNexusStorage.open à9d6928c appelle openOrCreateDatabase(file) avant probeFts5 | Ordre confirmé par lecture ; effets exacts de création/métadonnées et refus sans écriture à qualifier par NEXUS sur Android réel |
| Erreur initiale de typage d'un nouveau test | appel getApplication<Application>() non applicable | Corrigé en getApplication(), compilation/tests finaux réussis |
| Avertissement Kotlin de copy sur un nouveau data class à constructeur internal | Premier log de compilation | Remplacé par classes immuables sans copy ; aucun état d'admission configurable |

L'ordre d'ouverture NEXUS est un risque producteur à instruire, **pas une preuve
instrumentée de corruption ou perte d'une base existante**. Aucun effet Android
réel n'est inventé à partir d'une lecture de code. L'extension commit/fence reste
une proposition à valider par CORE/SERVICES/UNIFIED, pas un mécanisme d'autorité activé.

## 4. Tests et compilation exécutés

Environnement : Linux x86_64, JDK17, Gradle8.10.2, Kotlin2.0.21/AGP8.7.3, SDK35.
Proxy/TLS conservés. Les API24/25/26/35 des nouveaux tests sont des simulations
Robolectric. Aucun émulateur ou /dev/kvm disponible ; aucun téléphone utilisé.

| Suite / contrôle | Résultat final réellement obtenu |
| --- | --- |
| app:testDebugUnitTest | **480 PASS**, dont448 historiques et32 nouveaux |
| app:testMvp2UnitTest, filtres Mvp/MemoryLabUi | **53 PASS** |
| p2-client:testDebugUnitTest | **36 PASS**, fixture expérimentale inchangée |
| integration-lab:test et run | **30 PASS**, vrais NEXUS/SERVICES JVM sur données/accords FIXTURE |
| Total distinct | **599 PASS**, zéro échec/erreur/ignoré final |
| Tests baseline avant modifications |567 PASS, inclus dans les599, pas additionnés |
| Scénarios ciblés avant la suite complète |32 PASS, inclus dans les599, pas additionnés |
| Builds debug/mvp2/mvp3 | PASS |
| Lint debug/mvp2/mvp3 |0 erreur chacun ;27 /37 /37 avertissements conservés |
| Garde FUSION02 + six négatifs | PASS ; absence de projection, réécriture, second registre, faux GO/flag/base refusés |
| APK des trois variantes | Signature vérifiée, certificat local identique au contrôle FUSION01, minSdk24, ARM64 seul, pas de permissions réseau, pas de poids GGUF/Whisper |
| Bibliothèques natives des APK | Hashes de tous les .so identiques aux APK locales préexistantes avant rebuild |

Les32 cas couvrent : défaut OFF, opt-ins répétés refusés, métadonnées d'activation
insuffisantes, CONNECTED/DEGRADED sans admission, absence/erreur expurgée, concurrence,
composition dans le graphe réel sans préparation de modèles et réouverture de bases
synthétiques avec historique/souvenir/document/passages et réglages **non par défaut**.
Aucune admission native, génération, audition vocale ou exécution SAF n'est simulée
comme preuve. Les anciennes migrations SQLite sont exercées par leur suite existante
sur données/copies synthétiques ; aucun changement de schéma ni migration personnelle.

Échecs séparés conservés : erreur initiale de compilation du nouveau test corrigée,
anciens gardes check-core-03.py/check-fusion-01.py refusant le nouveau périmètre.
Ils ne sont ni assouplis ni renommés PASS. Le garde spécifique FUSION02 protège la
base plus récente et l'unique adaptation AppGraph ; leurs logs restent disponibles.

### Commandes effectivement employées (outils configurés au préalable)

```sh
gradle -PincludeP2Fixture=true :app:testDebugUnitTest \
  :app:testMvp2UnitTest --tests '*Mvp*' --tests '*MemoryLabUiTest*' \
  :p2-client:testDebugUnitTest :app:assembleDebug :app:assembleMvp2 :app:assembleMvp3 \
  :app:lintDebug :app:lintMvp2 :app:lintMvp3 --rerun-tasks --offline \
  --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process

gradle -p integration-lab -PcoreNexusSource=/chemin/snapshot-nexus777 \
  -PcoreServicesSource=/chemin/snapshot-services3de test run --rerun-tasks \
  --offline --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process

python3 tools/check-fusion-02.py --self-test --verify-results
```

Contrôles APK exécutés avec `aapt dump badging`, `aapt dump permissions` et
`apksigner verify --print-certs` sur chaque APK ; SHA256 des octets décompressés
de chaque entrée lib/*.so comparés aux APK locales préexistantes.

Les snapshots se préparent avec tools/prepare-core03-sources.py, comme FUSION01.
--offline exige des caches complets ; la première suite ciblée a utilisé le réseau
pour les seules dépendances de test, aucun modèle. Les logs texte retirent uniquement les espaces finaux ; les originaux exacts
sont conservés en .log.gz avec leurs hashes (log-normalization.json).
Les logs conservent les chemins
réels, les arguments JVM et les commandes exécutées. La CI dédiée exécute également
ces contrôles et publie les XML ; son résultat distant sera vérifié au SHA publié,
jamais déduit des succès locaux ou d'un ancien run.

## 5. Matrice d'acceptation

| Lot | Contrat/code | JVM / Robolectric | Instrumenté24/25/26/35 | Physique | Production / merge |
| --- | --- | --- | --- | --- | --- |
| C0 refus et préservation | Implémenté, interface locale uniquement |32 nouveaux PASS +567 références | NOT_RUN | NOT_RUN | Projection passive seule, aucune admission ; aucun merge |
| C1 composition de producteurs Android | Non implémentée, prérequis rouges | Le vrai lab JVM reste séparé | BLOCKED | BLOCKED | OFF |
| C2 orchestration/fence mutation | Proposition documentaire ; anciens reçus inchangés | Aucune nouvelle preuve de fence | BLOCKED | BLOCKED | OFF |
| C3 UI/SAF réel | Non ajouté | Pas de simulation vendue comme pilote | BLOCKED | BLOCKED | OFF |
| C4 pilote émulateur | Aucun APK pilote fonctionnel revendiqué | Sans objet | NOT_RUN / gates bloquées | NOT_RUN | Aucun appareil installé |
| C5 régression / APK / sécurité | Source et dépendances préservées hors ajout passif |599 PASS, builds/Lint/contrôles APK | Ne qualifie pas le runtime | NOT_RUN | Aucun nouveau moteur ni budget |
| C6 handoff | Proposition dans INTEGRATOR-CONTRACT.md | Pas de transport | NOT_RUN | NOT_RUN | Aucun IPC/LLM/VOICE activé |

Les scénarios directeurs A–J ne sont **pas validés Android**. Démarrage/mode avion,
DocumentsUI/provider SAF, FTS5 réel, PDF/deux pages, export nouveau, annulation au
point d'effet, force-stop et rotation du pilote restent non exécutés. Une réouverture
Robolectric du graphe n'est pas un vrai process kill. Le lancement d'un émulateur
n'aurait pas levé les gates de modules absents ; aucun système image téléchargé.

## 6. Préservation et retour arrière

Aucune modification de LocalDatabase, SettingsStore, SessionCoordinator,
PromptBuilder, moteurs, mémoire/Identity, profils, admission, limites matérielles,
manifestes, permissions, packages, signatures/configs de build ou dépendances.
Aucun producteur Android/JDBC importé dans l'APK. Les .so restent byte-identiques.
AppGraph reçoit seulement une projection passive sans hooks de démarrage/fermeture.
Aucun poids, donnée privée, voix/profilB ou portrait n'a été lu, transféré ou publié.
Les APK locales restent des diagnostics historiques ; aucune distribution pilote
multicomposants ni installation personnelle n'est effectuée.

Retour arrière : ne pas installer ce build ; conserver les applications et données
existantes. Pour le code, repartir de36761cea dans une future branche ou révoquer
les seuls ajouts C0 par revue, sans toucher aux bases. Aucun export/restauration Android
n'est inventé. Les formats et anciens reçus ne sont ni réinitialisés ni migrés.

## 7. Blocages et prochaines décisions

1. **NEXUS#3** : sonde FTS5/vocab/MATCH/BM25 et RAG/citation/réouverture sur runtime
   Android24/25, effets du refus d'ouverture, backend alternatif seulement après
   autorisation spécifique si nécessaire. Revoir licences/ABI et SHA de livraison.
2. **SERVICES#3** : API authorizeAtCommit/fence revue, preuves barrières et consentement
   exact unique ; vraie nouvelle URI SAF et close/readback, UNKNOWN et ACK perdu.
   Le job historique26 ne remplace pas la qualification SDK24/25.
3. **UNIFIED#5** : mise à jour BOM/gates et revue du contrat commun sans changer v1
   silencieusement. Licences de redistribution des projets toujours à clarifier.
4. Ensuite seulement : nouvelle tranche de composition CORE in-process sous la
   session unique, opt-in de laboratoire, instrumentation24/25 puis26/35, sans
   nouveau Binder, moteur, migration ou activation personnelle.

Publication sur la seule branche de mission et PR **brouillon vers
feat/fusion-01-core-integration** pour isoler l'incrément. Aucun main fusionné,
auto-merge ou activation sensible. S'arrêter à C0 tant que ces portes restent rouges.
