# ID-02 — goûts fictifs, variantes bornées et revue explicite

CORE, branche `feat/id-02-intensive`, parent MEM-01
`e46ba56163e54bc03d7a2321b0fcb51449e68973`. Base technique de fiche
`a6f13dd101990c6b5e3992be506c9a5b99873621`, planning
`1b7ac9b35b5e6e133167fcaf1873e58b9db7288e` ; ID-01 et MEM-01 livrées en amont.

## Code et démonstration

- PreferencesCatalog sépare types goûts fictifs, style d'éditeur et préférences
  utilisateur confirmées. MemoryPreferenceReader ne possède aucune méthode
  d'écriture et lit seulement la projection PREFERENCE de MEM-01, sans SENSITIVE,
  message historique ou déduction automatique ; suppression/suspension prennent
  effet sans cache. Le texte retourné reste une donnée non fiable, jamais une
  instruction SYSTEM ou permission.
- LanguageRuleEngine/TemplateRealizer offrent un vocabulaire fini et substitutions
  lexicales en un passage, sans regex/texte libre configurable ; conflits et
  chaînes sont refusés. Trois gabarits internes seulement, avec offsets et
  explication de chaque variante. Aucun résultat LLM/document/citation réécrit.
- ContextStylePolicy borne l'humour en contexte travail et le supprime en urgence,
  suspend les variantes urgentes et laisse le profil demandé intact. Aucun rôle
  d'autorisation ou lancement d'outil ; paramètres purement éditoriaux.
- IdentityChangeJournal prépare une proposition RAM avec comparaison et digest
  d'aperçu lié à une nonce et au profil de départ, valable90s. Approbation
  explicite => révision optimiste du repository ID-01 ; rejet/révocation ne
  persiste pas le contenu. Journal RAM borné et historique durable de révisions
  réutilisé, sans second stockage concurrent de profils ou préférences.

Le format **interne d'éditeur** `jarvis.identity-profile-lab/2` ajoute les intérêts
et règles. Il lit `/1` sans altérer les anciens octets, ni changer le schéma
SQLite. Les révisions nouvelles sont append-only ; restore ajoute une révision.
Ce format n'est **ni jarvis.identity/1 ni le protocole Binder de fixture /2**.
Un ancien binaire ID-01 ne sait pas lire les nouvelles révisions et doit refuser ;
le retour de configuration se fait via restore dans ce repository, pas par
réécriture automatique des données pour l'ancien binaire.

Démo exécutable sans modèle ni installation : `IdentityEditorialTest` réalise
Bonjour → Salut en aperçu, approuve une révision réelle SQLite, ferme/rouvre,
restaure la version initiale et retrouve Bonjour. Un goût fictif ne crée aucune
base de mémoire. Les intérêts/règles sont retirables sélectivement via nouvelle
proposition de ProfileDraft, sans effacer ni inférer de préférence utilisateur.

## Préservation et limites

IdentityComposer, IdentityCodec, manifeste B, IdentityPort et contrats v1
inchangés. Les champs supplémentaires ne sont pas injectés ; l'adaptateur refuse
encore CONTRACT_EXTENSION_REQUIRED. Aucun deuxième moteur, JNI, seuil, permission,
fixture /2, SQLite historique ou voix modifié. Athéna runtime reste inactive.
Les previews sont des gabarits déterministes, pas une qualité LLM mesurée.
Aucun profil personnel stocké dans Git, aucune migration/appareil/merge.
Le port est interne au processus de confiance ; composition UI/contrôle de session
reste à réaliser dans l'hôte avant usage réel, aucun nouveau service exporté.

## Vérifications

Résultat : BUILD SUCCESSFUL in 2m 47s ; 83 tâches,18 exécutées.
**400 tests debug réellement réexécutés, zéro échec/erreur/ignoré**, dont24
nouveaux cas ; fixture /2 :36 résultats UP-TO-DATE, non comptés comme réexécutés.
Build debug réussi, Lint zéro erreur/30 warnings historiques (rapport réutilisé).
Commandes exactes : `VALIDATION-ID-02.json`. Tests Robolectric
SQLite NATIVE API24/25/35 : aperçu/réversion, catégories/provenance, conflits de
règles/instructions hors vocabulaire, consentement/rejet/révocation/expiration,
révision périmée, préférences confirmées avec suppression/suspension immédiate,
ancien payload indépendant /1 inchangé après nouvelle révision /2. Pas de JNI,
conversation native ni appareil physique testé. Garde protège337 fichiers amont.

Suite : MEM-02 (rappel borné/export volontaire du laboratoire), puis portions
CORE-03 compatibles. NEXUS est JVM/JDBC, son raccordement Android API24 demeure
bloqué ; aucun JDBC desktop ne doit être intégré à l'APK.

## Reprise et correctif de revue

Le commit initial `7a54f85adc5143bf8c7710de81942d49f3b901d0` et sa validation
de 400 tests restent préservés. Les modifications locales interrompues ont été
reprises sans recommencer la mission : DiffService expose maintenant les valeurs
par défaut et la présence/retrait des overrides même lorsque les paramètres
résolus des trois contextes sont identiques. Aucun changement caché dans l'aperçu
d'approbation. Test de retrait sélectif des goûts fictifs également conservé.

Suite debug finale forcée par `:app:testDebugUnitTest --rerun` : **406 tests
réexécutés, zéro échec/erreur/ignoré** (les 400 anciens plus six cas API24/25/35).
Build debug, Lint debug et fixture /2 vérifiés en première commande : PASS avec
résultats majoritairement UP-TO-DATE, non présentés comme réexécutés. Garde ID-02
PASS, 337 fichiers protégés ; diff sans erreur. Aucun contrat/JNI/persona activé.
