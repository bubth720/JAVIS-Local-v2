# FUSION-01 — composition et portes CORE

Base retenue : CORE03 `aea5ac859ac551d54fe16187c1883a487dbee9d6`.
La photographie distante est dans `integration-lock.json` et `evidence/remote-ci.json`.
Ce verrou **local CORE** n'est pas le GO F0 du registre UNIFIED. Les références
candidates ne sont pas toutes des dépendances compilées ni des versions intégrées.

## Filiation vérifiée par Git

`CORE02 e86107b -> ID01 -> MEM01 -> ID02 corrigé 283423c -> MEM02 49ad428 -> CORE03 aea5ac8`.
IDENTITY002 `8adec3e` est également un ancêtre. Les SHA complets et merge-bases
sont dans `evidence/lineage.json`. Pas de cherry-pick ni de fusion.
CORE–VOICE `7426665f2794cba83646008fa0f342d102141201` diverge depuis main
`226e2e8cc3eebd479805b89d035eb5c1cbcad540` : non retenu comme parent,
aucun de ses fichiers injecté. La fixture CORE004C /1 et ses AIDL restent inchangés.
Les travaux /2 et F3B ne sont ni remplacés ni débloqués.

## Dépendances effectivement compilées

- NEXUS `777e6afa75ecb99b85e52d68837cb0fefcf161cd`, arbre
  `b534f8a7f7e441cf264097773afb042103aefd36`, 46 sources vérifiées.
- SERVICES `3decdc47d82f9b43713c48cab244762941770a81`, arbre
  `56f5214de9a0d1d164638e9e128bb3335ee23499`, 189 sources vérifiées.
- Snapshots immuables hors dépôt, contrôlés par `prepare-core03-sources.py` et
  CORE03-SOURCES.json à chaque configuration Gradle. Ne pas les remplacer par
  une branche finale ou un checkout utilisateur modifié.
- JDK17, Gradle8.10.2, Kotlin2.0.21 ; SQLite JDBC/FTS5 sur Linux x86_64 seulement.
  Aucune dépendance Android ni version de production changée. Les autres
  versions transitives restent celles des builds épinglés ; leurs hashes de
  fichiers de build sont relevés, pas une attestation universelle de supply-chain.

## Références non consommées et écarts

| Référence observée | Fait / limite | Décision CORE |
| --- | --- | --- |
| NEXUS final d903bf0 | Bibliothèque v4 ; dernier rapport ne rejoue pas Kotlin/JVM final | Ne pas substituer au NEXUS CORE03 |
| NEX-06A 374ea2e | Port min24, compilation stubs et Robolectric historiques ; FTS5 hôte absent, 0 instrumenté | C2 BLOCKED |
| SERVICES final d16dd84 | Catalogue 17 outils ; SDK historique min26 | Pas de nouvelle dépendance APK |
| SERV-04A f00091a | Port min24, Robolectric40 historiques ; export SAF explicitement indisponible, instrumentation non exécutée | C2 BLOCKED |
| LLM 2efb3f7 | CI distante en échec ; exact tokenizer UNSUPPORTED, F3B non autorisé/attesté | C4 BLOCKED |
| VOICE fe57452 | Studio indépendant min26, CI mixte ; aucun raccordement CORE qualifié | TTS historique conservé ; aucune voix activée |
| INTERFACE db459ba | UI autonome, CI réussie à ce SHA | C3 conception seulement |
| EVOLUTION c018018 | Diagnostic JVM ; sandbox OS non qualifiée | Aucun runner, aucune réparation |
| UNIFIED 329f304, livraison F0 3caa57d | F0 publiée ; rapport147 tests lu, non rejoué ici ; composition encore candidate | Verrou comparé, pas de GO Android déduit |

Les réussites distantes sont des observations GitHub, **pas des tests rejoués ici**.
Les validations des producteurs API24 ont été lues à leur SHA exact, sans les
requalifier. L'absence de licence racine explicite dans les huit arbres examinés
reste une question de redistribution : aucune licence générale n'est déduite
de la licence Qwen ou de celle d'une dépendance. Aucun binaire producteur publié.

## Matrice d'exécution actuelle

| Chemin | Linux x86_64 JVM | Robolectric24/25/35 | Android24/25/26/29/35 instrumenté | Android ARM64 physique | Production |
| --- | --- | --- | --- | --- | --- |
| CORE/MEM02/ID02 historique | Suites pures exécutées | 501 cas CORE/MVP incluent simulations Android | NOT_RUN | NOT_RUN | Inchangée, non requalifiée matériellement |
| Fixture CORE004C /1 | 36 cas hôtes | Hôte uniquement | NOT_RUN | NOT_RUN | RECOVERY_REQUIRED conservé |
| NEXUS/SERVICES via integration-lab | Vraies implémentations, données et autorisations FIXTURE | Aucun nouveau port raccordé | BLOCKED | BLOCKED | Désactivé / absent de l'APK |
| JNI exact / LLM / VOICE inter-APK | NOT_RUN | Non probant pour JNI | BLOCKED | BLOCKED | Inactif |
| Transport INTERFACE | Conception | NOT_RUN | NOT_RUN | NOT_RUN | Aucun service ajouté |

Aucun émulateur ni /dev/kvm disponible dans cette session. Un build Android ne
prouve pas FTS5, SAF, les ressources ou le comportement sur appareil. Les APK
debug/mvp2/mvp3 construits sont diagnostic, pas une livraison JARVIS intégrée.

## Propriété et menaces

CORE conserve sessions, autorité et mémoire confirmée ; NEXUS catalogue/citations ;
SERVICES son registre, Guard, ApprovalGate, exécutions et Store. Les nouveaux reçus
techniques utilisent **le même Store SERVICES**, namespace distinct borné256,
sans texte ni permission persistée. Aucune deuxième mémoire ou base CORE.
Une seule instance du propriétaire de workflow est admise par convention de ce
laboratoire : pas de verrou interprocessus, pas de garantie de résidence native.

- Injection : TXT/CSV restent des données ; pas de parseur de commande ni de SYSTEM.
- Rejeu : IDs dérivés par étape, revue et confirmation consommables, reçu terminal.
- Corrélation : session/request/generation vérifiés avant dispatch et acceptation.
- Timeout, annulation, perte d'ACK : UNKNOWN persistant, aucune reprise automatique.
- Révocation : refus avant confirmation et au retour ; **pas de fence atomique au
  worker SERVICES livré**, dont Guard.Context est un snapshot. Une écriture déjà
  admise peut finir ; aucun rollback ni suppression du fichier promis. Ce point
  interdit un GO C1 global de sécurité et tout raccordement de production.
- Corruption : refus du journal, aucun reset ; sauvegarder pour diagnostic isolé.
- App/SQLite/profilB/signatures/budgets : aucune modification ; aucune donnée réelle.

La livraison F0 `3caa57d91ce0dba31043e7cb6373a5e2df8b9fa4` a été découverte
et relue avant publication CORE. Son verrou exact et son rapport sont archivés
sous `reference/`, avec empreintes et comparaison des huit primaires. Le rapport
UNIFIED annonce147 tests et une validation de20 références/160 fichiers : ces
tests ne sont pas additionnés à nos résultats. La livraison F0 constate elle-même
les blocages API24 et natifs ; elle ne les lève pas.

## Portes de déblocage

1. F0 autoritatif revu dans UNIFIED, composition/licences/contrats validés.
2. Producteurs NEX-06A/SERV-04A qualifiés **instrumentés** API24 et25 puis26/35,
   FTS5 réellement disponible ou autre solution séparément approuvée ; SAF qualifié.
3. Producer SERVICES : linéarisation vérifiable autorité/révocation/début de mutation,
   sans nouveau registre ni rollback fictif. Recompiler et requalifier les consommateurs.
4. Nouvelle mission C2 : composition opt-in OFF, sessions/rotation/process death,
   fournisseur SAF réel ; conserver 501 cas et données historiques.
5. JNI/F3B, arbitre /2, signatures/UID et mesures matérielles sous validations distinctes.

Le verrou F0 choisit **NEX374/SERVf000** comme primaires Android candidates,
avec les finales v4/17 outils comme alternatives. Notre photographie des huit
branches finales (`observed_candidates`) n'est donc pas identique à cette
composition. `primary_candidate_composition` copie exactement le choix F0 ;
`consumed_jvm_sources` reste explicitement NEX777/SERV3de pour C1. La comparaison
champ à champ est dans `evidence/unified-comparison.json` : six correspondances,
deux différences de références documentées, **aucune substitution au build**.
