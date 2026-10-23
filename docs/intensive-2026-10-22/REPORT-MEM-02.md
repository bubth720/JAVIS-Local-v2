# MEM-02 — rappel traçable et export volontaire

Module CORE/MEMORY, dépôt `bubth720/JAVIS-Local-v2`, branche
`feat/mem-02-intensive`. Parent réel : ID-02 corrigée
`283423cb8b9d567539f33cf7f2f1fafa602cf974` ; commit ID-02 initial
`7a54f85adc5143bf8c7710de81942d49f3b901d0` conservé avec sa preuve de400tests.
Le correctif de revue ajoute six cas,406réexécutés, sans réécriture d'historique.
Dépendances : MEM-01 `e46ba56163e54bc03d7a2321b0fcb51449e68973`, NEX-03
`777e6afa75ecb99b85e52d68837cb0fefcf161cd` (lecture Git, bibliothèque JVM17/JDBC,
pas de connexion Android présumée). Aucune fusion de branche.

## Code livré

- `memory/MemoryRecallIndex.kt` : scan lexical NFC du catalogue MEM-01 confirmé,
  autorisations fermées par défaut, contexte/pertinence/date et explications
  source/révision/confirmation. Pas de collecte de messages ni d'embeddings.
  SENSITIVE toujours exclu. Égalité lexicale => sélection utilisateur, jamais
  affirmation automatique de contradiction sémantique. Suspension de session,
  exclusion sélective, suppression et expiration observées sans cache.
- `memory/MemoryContextAssembler.kt` : bloc JSON cité de données non fiables,
  séparé du documentaire, budget UTF-8 de256à8192octets ; omission d'entrées
  entières, aucun texte consenti tronqué. Ce budget n'est pas un tokenizer et
  n'autorise aucune inférence. Aucun branchement au PromptBuilder/LLM.
- `memory/MemoryExportService.kt` : JSON `jarvis.confirmed-memory-export/1`,
  sélection explicite de1à10souvenirs,32Kio maximum, aperçu exact RAM90s,
  empreintes de contenu et d'archive, révisions et autorisations revérifiées.
  Pas de session/nonce ni de permissions exportées. Un aperçu d'une ancienne
  incarnation de session ne peut pas être réutilisé après suspension/reprise.
- Export privé dans `noBackupFilesDir/memory_exports_lab/`, fichiers neufs sans
  écrasement, synchronisation du descripteur, plafond20fichiers. Aucun effacement
  de fichier utilisateur pour contourner ce quota. JSON en clair annoncé ; le
  port de chiffrement est injectable mais aucun adaptateur Keystore n'est fourni
  ou qualifié : demander le chiffrement sans adaptateur refuse sans repli.
- Port de destination SAF optionnel : sélection locale explicite, **nouvel aperçu
  et nouvelle confirmation après retour du sélecteur**, refus de taille non nulle
  ou inconnue, copie bornée et relecture SHA-256. Aucune nouvelle permission Android,
  aucun FileProvider/service exporté. En cas d'erreur, résultat UNKNOWN et copie
  privée conservée ; aucun effacement du document du fournisseur ni rollback promis.
- Import uniquement en aperçu isolé RAM : JSON strict/UTF-8, version, empreintes,
  champs, doublons, dates et conflits vérifiés. Un `source=USER_EXPLICIT` dans un
  fichier est une assertion non fiable, jamais un consentement valable. Aucune
  API d'importation automatique ou d'écrasement, aucune écriture mémoire par import.
- `MemoryToolsLabUi.kt`, palette MVP2/MVP3 et graph : recherche avec justification,
  résolution d'ambiguïté par clic, sélection/export et aperçu d'import. Opt-in
  existant conservé ; arrière-plan suspend rappel et propositions. Les révocations
  ne prennent pas le moniteur d'une copie bloquée chez un fournisseur.

## Démonstration et vérifications

`MemoryRecallExportTest` utilise de vraies bases SQLite isolées sous Robolectric
API24/25/35 et de vrais fichiers synthétiques : confirmation, rappel cité NFC,
ambiguïté, budget UTF-8/emoji, texte manipulateur gardé comme donnée,
suppression/suspension/expiration, export puis relecture SHA, import sans écriture,
contenu corrompu, version inconnue, doublons et champs d'autorité refusés,
révocation/ABA, confirmation périmée/rejouée et chiffrement indisponible.
`MemoryLabUiTest` clique le parcours de recherche et affiche un vrai souvenir
confirmé ; historique et chargement LLM restent inchangés. Les tests de destination
SAF emploient des doubles de port explicites, dont erreur de relecture et blocage
pendant copie ; ce ne sont pas des qualifications de fournisseur Android physique.

Commandes et résultats finaux : `VALIDATION-MEM-02.json`.
Première passe ciblée :33tests réussis. Première passe complète :498tests
(445debug+53MVP2), zéro échec/erreur/ignoré, builds debug/MVP2/MVP3 et Lint réussis.
Une régression supplémentaire de révocation pendant blocage fournisseur est
ensuite ajoutée ; la validation finale est enregistrée dans le reçu et ci-dessous.
Ces comptes successifs se recouvrent, ne pas les additionner.

## Protections et limites

`LocalDatabase`, son schéma et ses données historiques, `SettingsStore`, toutes les
classes IDENTITY, JNI/AAR/admission/seuils RAM/batterie/thermique, VOICE, manifeste,
Gradle et fixtures `/2` sont inchangés. Aucun profil personnel, audio, poids ou
conversation n'est ajouté au dépôt. Athéna désactivée, aucun second moteur, aucune
installation ou génération native effectuée.

Le répertoire privé est un stockage hôte de confiance, pas un filesystem hostile
qualifié contre toutes les courses. SAF : EXTRA_LOCAL_ONLY est une demande au
sélecteur ; le comportement d'un fournisseur doit encore être qualifié. Une cible
qui change après contrôle ou un fournisseur qui bloque peuvent laisser un résultat
incomplet/inconnu. La révocation reste immédiate, mais ne peut pas retirer des
bytes déjà exportés ni annuler de force un appel bloqué. Une copie autorisée
commencée avant suppression ne devient pas rétroactivement une absence d'export.
Les consommateurs futurs devront revalider le rappel avant utilisation ; aucun
prompt/LLM n'en consomme actuellement. Suppression ne signifie pas effacement
cryptographique, et la SQLite historique n'est pas migrée.

Suite autorisée : CORE-03 dans les seules bibliothèques réellement disponibles,
sans installer d'APK, activer Athéna, changer JNI ou connecter des applications.

## Résultat final de reprise

**501 tests réellement réexécutés :448debug+53MVP2, zéro échec/erreur/ignoré.**
Les36résultats de fixture `/2` restent UP-TO-DATE et ne sont pas ajoutés aux501.
BUILD SUCCESSFUL en2m36s,167tâches dont33exécutées. Builds debug/MVP2/MVP3
réussis ; Lint debug/MVP2 : zéro erreur,30/40avertissements historiques.
La régression de fournisseur bloqué confirme une révocation non bloquante,
aucun octet copié ensuite et aucune fausse promesse de rollback.
Garde MEM-02 PASS :345fichiers amont protégés ; aucun fichier IDENTITY,
stockage historique, fixture, AAR/JNI, admission, Gradle ou manifeste modifié.
