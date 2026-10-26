# FUSION-02 CORE C0 — contrat de préparation

Base CORE FUSION01 36761cea101b5f88b87cd4136dc075d28a1adb96.
**Conception et refus uniquement.** Aucun changement des contrats UNIFIED v1,
CORE004C /1 ou /2, du protocole d'autorité, d'un journal ou des signatures.
Les propositions ci-dessous nécessitent revue CORE/SERVICES/UNIFIED avant code réel.

## Interface livrée et limites

`DocumentToolReadinessPort` est une interface locale Kotlin **diagnostique**,
composée par AppGraph avec son ConnectorRegistry existant. Ce n'est pas une
implémentation de KnowledgeReadPort ou ServicesToolPort Android, un catalogue
exécutable, une découverte de services ou une autorité de consentement.
Elle n'accepte ni contenu, URI, jeton, droit, preuve native ni callback producteur.

| Observation locale du connecteur | Projection livrée | Admission |
| --- | --- | --- |
| DISABLED | BLOCKED / FEATURE_DISABLED | Refus |
| UNAVAILABLE | UNAVAILABLE / PRODUCER_ANDROID_UNQUALIFIED | Refus |
| CONNECTED | UNKNOWN / HEALTH_NOT_QUALIFICATION | Refus |
| DEGRADED | UNKNOWN / RECOVERY_REQUIRED | Refus |
| Absence du module | UNAVAILABLE / MODULE_ABSENT | Refus |
| Erreur de lecture du statut | UNKNOWN / HEALTH_UNAVAILABLE | Refus, message interne non exposé |

`enabled=false`, `canExecute=false` sont constants. La méthode requestLabOptIn
retourne uniquement LabAdmissionRefused, sans modifier connecteurs/préférences
ni invoquer discover/connect/disconnect. Aucune sortie READY n'est constructible
par cette interface. Aucun UNSUPPORTED FTS5 n'est inventé : nous n'avons exécuté
aucune sonde SQLite dans un processus Android réel. Les objets ne sont pas un
format wire approuvé ; pas de sérialiseur ou d'extension de contrat partagé.

L'UI future peut lire cette projection. Aucun écran SAF ou export n'est ajouté
avant qualification ; les fonctions CORE historiques et le laboratoire JVM restent
accessibles par leurs parcours antérieurs. SessionCoordinator n'est pas contourné :
aucune nouvelle opération n'est admise. La requête d'opt-in n'est pas une permission.

## Producteurs observés et mapping futur

| Propriétaire | Interface source réelle / sémantique | Adaptation future CORE |
| --- | --- | --- |
| NEXUS NEX374 | NexusStoragePort, AndroidNexusStorage.open -> Ready/Rejected, sonde temp FTS5/vocab/MATCH/BM25 avant migration | App-private backend après qualification, pas LocalDatabase ni JDBC |
| NEXUS FUSION01 9d6928c | Schéma portable v4, gateway documentaire, callbacks de l'hôte, citations conservées | Recompiler consommateur ; conserver URI/hash/version/page/offset, revalider session au retour |
| SERVICES SERVf000 | ServicesAndroidToolPort : selectSource, dispatch, confirm, closeAsync ; Guard/ApprovalGate/Store propres au producteur | Réutiliser ces interfaces, pas recopier les outils ; CORE délivre autorité et corrélation |
| SERVICES export actuel | exportDocx -> SAF_EXPORT_UNQUALIFIED ; aucun authorizeAtCommit livré | Aucun branchement mutateur, même avec un grant statique |
| CORE existant | SessionCoordinator gate applicatif ; AppGraph unique ; PromptBuilder historique | Toute future opération réelle passe par la session, pas un coordinateur parallèle |

Le lab compile toujours NEX777 + SERV3de sur JVM ; les références Android restent
**en lecture seule**. Il n'y a aucune promesse d'ABI binaire entre ces branches.
Ne pas remplacer une coordonnée JAR dans l'APK : reprendre le port, compiler et
prouver le runtime à son SHA exact. FTS5 compile_options seul ne suffit pas.

## Proposition de point d'engagement (non appliquée)

La limite FUSION01 est maintenue : Guard.Context est un instantané, et la seule
revérification au retour ne peut empêcher une écriture déjà admise. La décision
commune doit préciser :

- CORE propriétaire de session/request/generation, scope, politique et révocation ;
  SERVICES propriétaire du worker, registre/Guard/ApprovalGate et de l'effet fichier.
- Aperçu exact immuable : opération/version, hash du contenu, destination **neuve**,
  source/révision, périmètre, deadline monotone, IDs complets. Pas de texte personnel
  dans les journaux techniques. Ne pas dériver les droits d'un document ou d'un LLM.
- Consentement CORE lié à cet aperçu, nonce/usage unique, révision de politique,
  IDs complets et deadline. Aucun booléen fourni dans les arguments ne vaut accord.
- Appel de l'autorité CORE par le worker au dernier point convenu avant effet :
  `authorizeAtCommit`, consommation du consentement et comparaison de génération
  sous le même mécanisme de sérialisation que la révocation. CORE absent = refus.
  Un retour de callback générique ne prouve pas encore ce fence : tester la frontière
  exacte dans le producteur. Aucun code de ce protocole n'est appliqué ici.
- Révocation avant engagement -> aucun effet. Après engagement -> effet possible,
  avis honnête et résultat protégé par la politique de restitution courante ; pas
  d'effacement/rollback universel promis sur un fournisseur externe.

| État proposé | Ce qu'il atteste | Ce qu'il ne permet pas |
| --- | --- | --- |
| PREPARED | Revue liée à une requête et durable selon contrat à revoir | Permission, admission native ou effet |
| DISPATCHED | Opération acceptée par worker | Droit d'écrire avant fence |
| COMMIT_STARTED | Point d'engagement autorisé/consommé, reçu selon ordre de persistance approuvé | Promettre rollback, unload ou réexécution |
| COMMITTED | Fermeture et relecture exacte/digest confirmés | Réadmission LLM, preuve native ou lecture après révocation |
| UNKNOWN | Crash, timeout, close/ACK ou journal incertain | Reset, retry implicite ou succès tardif |

L'ordre journal/consommation/effet doit être spécifié avec SERVICES et testé pour
chaque coupure. Une erreur d'ACK ne réémet jamais la commande. Aucune migration
PREPARED/DISPATCHED/COMPLETED des anciens reçus FUSION01 vers cette proposition.
La réconciliation future exige preuves du propriétaire ; timeout/coroutine/Binder
mort ne prouve ni fin native ni unload. Aucun second moteur autorisé.

## Qualification nécessaire avant branchement

1. NEXUS : sonde + index/citation/réouverture instrumentés API24/25, ou alternative
   explicitement approuvée ; limites/import et sources originales préservés.
2. SERVICES : fence revu et tests barrières avant/après engagement, source intacte,
   nouvelle URI SAF, fermeture/relecture byte-level, UNKNOWN/crash/perte d'ACK.
3. F0 actualisé UNIFIED, sources/artefacts/licences/ABI/schéma précis, compilation
   des consommateurs. CI tierce verte seule ne remplace pas le runtime demandé.
4. CORE sous SessionCoordinator : opt-in de laboratoire seulement, API24/25 puis
   26/35, vraie DocumentsUI/provider, rotation/force-stop, mode avion, corruption,
   refus d'autorité, ancien état/réponse rejeté et conservation des bases sur copies.
5. Contrat de restitution pour INTERFACE : SubmitText/ACK/ToolState/Citation/Failure
   versionné/corrélé à proposer à UNIFIED. Aucun transport exporté ici, aucune fausse
   réponse LLM. LLM/VOICE/Avatar qualifiés séparément ; TTS historique intact.
