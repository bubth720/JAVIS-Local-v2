# CORE-03 — client de bibliothèques réellement disponibles

Module CORE, dépôt `bubth720/JAVIS-Local-v2`, branche `feat/core-03-intensive`,
parent MEM-02 `49ad428e94edd75c2a1d115c8c2504f26609eace`.
**Statut : PARTIAL.** Client JVM fonctionnel livré, aucun raccordement Android.
La suite Android MEM-02 de501tests et toute l'implémentation ID-02 sont préservées.

## Références vérifiées

| Dépendance | Commit complet | Fichiers/API examinés |
|---|---|---|
| CORE-01/02 | e86107b8213b0aeaf9d22025cf6bef91a8e544a5 | `orchestration/CoreCapabilities.kt`, `events/ModuleConnector.kt` : lectures historiques et connecteurs externes refusés |
| NEX-03 | 777e6afa75ecb99b85e52d68837cb0fefcf161cd | `nexus-core/.../rag/{RagTypes,RagService}.kt`, `store/NexusDatabase.kt`, `api/KnowledgeCatalogService.kt` |
| SERVICES | 3decdc47d82f9b43713c48cab244762941770a81 | `shared-runtime/.../tools/{ToolRegistry,ToolDispatcher,ToolInvocation,ApprovalGate,ServicesTools}.java`, Guard, Data, Documents et `desktop/.../platform/JdbcDatabase.java` |
| UNI-01 | bd283dad3a57effa276f7f4441901839d2834d5c | Contrats proposés v1 et outillage Java/Kotlin, aucun transport ni amendement de schéma |

SHA NEXUS/SERVICES et branches publiées revérifiés par `git ls-remote`. Les sources
sont extraites des objets Git exacts vers des **copies séparées** hors dépôts.
`CORE03-SOURCES.json` lie commit, arbre Git, nombre de fichiers et SHA-256 de la
liste des empreintes. Le helper refuse les liens et les modifications de sources,
sans fetch, changement de branche ou téléchargement de modèle. NEXUS/SERVICES
conservent leurs branches, commits et modifications locales préexistantes.

## Incrément réel

Projet autonome `integration-lab/`, absent des settings/dependencies Android :

- SourceSearchPort/NexusSearchAdapter appellent le vrai RagContextPort sur SQLite
  et FTS5. Chaque citation est résolue, les groupements simples/identiques/contigus
  sont vérifiés sans perdre URI, SHA, document/version, page et offsets.
- Correlation exige session/request UUID et génération. HostAccessPort refuse par
  défaut et revalide recherche, assemblage et retour d'outil ; une réponse d'une
  session révoquée ne devient pas un résultat utilisable.
- ContextAssembler enveloppe le documentaire comme donnée non fiable, avec budget
  UTF-8 réel et aucun repli SYSTEM/user. `confirmed_memory=null` signale l'absence
  de raccordement, sans relire un store personnel. Les estimations NEXUS restent
  explicitement des estimations, jamais utilisées pour autoriser le LLM.
- ServicesToolAdapter réutilise le véritable registre/Guard/ApprovalGate et son
  journal SERVICES, pas un second catalogue ou une autorité persistante. Aperçu
  d'arguments exact/digest, refus des mutations en lecture seule, appel de
  confirmation propriétaire distinct, expiration, consommation unique, quota RAM
  et refus des IDs rejoués. Aucune décision lue dans un document ou un résultat IA.
- ToolOperation propage une demande d'annulation et conserve UNKNOWN au timeout.
  Ni annulation ni retour ne sont une attestation JNI/unload. Aucun moteur natif
  LLM, seuil matériel, protocole Binder ou service exporté ajouté.

La démonstration utilise une vraie source textuelle synthétique dans NEXUS,
retourne une citation résolue, normalise/déduplique réellement un CSV par SERVICES,
refuse un DOCX sans approbation puis l'écrit après décision de fixture explicitement
synthétique et le relit. Aucun appareil, modèle ou fichier personnel n'est utilisé.

## Résultats réellement exécutés

JDK17 Linux : **10 tests JVM, zéro échec/erreur/ignoré** ; compilation et CLI PASS.
Commande exacte et compte dans `VALIDATION-CORE-03.json`. Sortie réelle :

```text
CORE03 JVM: real_nexus_citations=1 csv_tool=SUCCESS mutation_without_approval=REFUSED docx_readback=VERIFIED android_connected=false native_inference=false
```

Dix scénarios couvrent source/citations réelles et tous leurs champs, autorité
absente, périmètre interdit, révocation/résultat tardif, mauvaise corrélation,
budget UTF-8, injection comme donnée, citation falsifiée refusée par le résolveur,
CSV réel, mutation/digest/refus/rejeu, DOCX vérifié et aperçu expiré.
Un snapshot valide a été copié en fixture temporaire puis modifié : vérification
normale PASS, version modifiée REFUSÉE. Aucun dépôt source original modifié.

Le premier essai de démo a échoué sur une attente de CSV non quoté, avant exécution
JUnit : SERVICES quote légitimement chaque cellule. Correction du client de
vérification pour relire le CSV avec son vrai parseur et comparer les lignes
normalisées ; aucun test historique ni code SERVICES affaibli. Neuf tests et la
démo passent ensuite ; après revalidation d'autorité au moment d'assembler, la
suite finale de dix passe. Avertissements existants : Store Java unchecked et
SLF4J sans binder (logger NOP), sans échec ni télémétrie nouvelle.

Garde CORE-03 :358fichiers amont protégés. Diff `app/`, fixtures et fichiers de
build Android vide contre MEM-02. Les501tests Android/Robolectric viennent du
parent validé, ne sont **pas** annoncés comme réexécutés par CORE-03. Pas besoin
de remplacer leur preuve par une exécution UP-TO-DATE. Aucun SDK/permission/AAR/
signature, profil IDENTITY, schéma SQLite historique ou réglage modifié.

## Blocages et suite

NEXUS est Kotlin/JVM17 avec java.sql/SQLite JDBC, SERVICES utilise java.nio.file
et son Android min26 n'est pas qualifié CORE min24. Aucun JDBC ou API26 n'est
ajouté dans l'APK ; aucune hausse de minSdk. L'affichage MVP des vraies capacités
externes et l'utilisation dans PromptBuilder restent **non raccordés**. Les
appels hôtes de démonstration ne prouvent pas SessionCoordinator/permissions
Android en production, ni une linéarisation complète avec une mutation concurrente.

Prochaine tranche non sensible : fournir/qualifier des adaptateurs de bibliothèques
Android API24 conservant la provenance et les permissions, puis réutiliser les
ports de ce laboratoire sous le vrai SessionCoordinator, sans doublon. Les
changements JNI/admission, signatures/permissions critiques, IPC et installations
physiques nécessitent toujours une validation distincte. F3/CORE004D restent
bloqués ; Athéna reste inactive. Aucun merge dans main, installation, migration,
second LLM ou activation de la voix/avatar par cette mission.
