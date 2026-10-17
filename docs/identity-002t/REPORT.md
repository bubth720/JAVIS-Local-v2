# CORE IDENTITY ID-002T — rapport final

Mission : [issue #8](https://github.com/bubth720/JAVIS-Local-v2/issues/8).
Dépôt : bubth720/JAVIS-Local-v2. Base exacte :
`8adec3e0e8dfdce685a33ef796fc3150facc7f0a`.
Branche : `feature/identity-002t-tokenizer-port`.
Contrat IDENTITY001B.1 inchangé ; approbation du prototype déjà acquise.
Aucune fusion ni activation implicite autorisée par cette livraison.

## Résultat

**Audit et préparation terminés. Port natif exact NON implémenté : gate JNI.**
Le tokenizer existe dans la bibliothèque C du modèle, mais l'AAR0.1.1 n'expose
pas de mesure préalable via JNI. promptTokens disponible après calcul ne règle
pas ce problème. Lire AUDIT.md et les preuves d'empreinte, API, symboles et sources.
Une implémentation native prouvable exigerait l'extension sensible du propriétaire
JNI et une transaction sous les protections CORE. Conformément à l'issue et à la
consigne utilisateur, cette partie est suspendue AVANT application.

Le diff de signatures `PROPOSED_JNI_API.patch.txt` présente la frontière à valider.
Il n'a pas été appliqué ni compilé et n'est pas un patch complet installable.
EXACT_PORT.md spécifie rendu/tokenisation communs, buffer de tokens retenu,
ticket à usage unique, modèle/template/worker/réservation et revalidation finale.
REGRESSION_REVIEW.md décrit les changements restant à autoriser et leurs risques.
Aucune fonction native, wrapper ou preuve de mesure fictifs ajoutés au runtime.
Le port existant continue de refuser explicitement TOKENIZER_UNAVAILABLE.

## Livraison

- tools/audit-identity-tokenizer.py : inspection statique reproductible de l'AAR
  résolu, empreintes et capacités réelles, comparaison des fichiers à8adec3e.
- app/src/test/java/com/stark/jarvislocal/identity/IdentityTokenizerBoundaryTest.kt :
 6 cas nouveaux API24/25/35 ; sources hostiles non promues et copie de provenance
 conservée pendant refus. Jamais de faux compte natif ou génération dans ces tests.
- docs/identity-002t/ : AUDIT.md, EXACT_PORT.md, REGRESSION_REVIEW.md,
 PROPOSED_JNI_API.patch.txt, SOURCE_REFERENCES.json, NATIVE_AUDIT.json,
 TEST_PLAN.md, TEST_RESULTS.json, RESULTS.md et le présent rapport.
- AGENTS.md et docs/PROJECT_KNOWLEDGE.md : constats et commandes de cette mission.

## Vérifications et limites

**268 tests réussis,0 échec/erreur/ignoré**, builds et Lint réussis.
Les6 vecteurs approuvés sont réexécutés sur API24/25/35 via Robolectric.
Inspection APK : SDK24/ARM64 et absence de permissions réseau ; signature debug
vérifiée, aucune APK installée. Code applicatif, moteur historique, réglages,
SQLite, profils, VOICE et fixtures inchangés. Aucun autre dépôt modifié.

**Un vérificateur historique reste en échec** : check-004c.py considère déjà les
ajouts ID-002 du commit de base comme un diff interdit contre004B. Il n'a pas été
altéré. Son diagnostic, les tests réellement exécutés et la comparaison indépendante
des fichiers figurent dans RESULTS.md. Ne pas annoncer « tous les validateurs passent ».

Native exact, modèle changé entre mesure/génération, identité du worker/bail natif
et qualification physique : non exécutés, bloqués par l'extension non approuvée.
Les scénarios futurs sont explicites dans TEST_PLAN.md ; aucun succès simulé
n'est utilisé comme preuve d'inférence native intégrée.

CORE–VOICE reste une branche parallèle divergente : SHA observé7426665f2794cba83646008fa0f342d102141201,
ancêtre commun226e2e8. Ses ports/pilote n'ont pas été intégrés. Son
NativeHandoffGate ne constitue pas une preuve de libération disponible pour IDENTITY.
La fin d'une mesure ne peut pas autoriser un transfert de ressources vers VOICE.

## Décision spécifique requise et prochaine action

Examiner la frontière JNI et le protocole scoped proposés, puis autoriser ou
corriger explicitement : responsabilité et fabrication de l'AAR expérimental,
API prepare/consume/discard, UTF-8 strict pour le seul parcours nouveau,
template sans fallback, loadGeneration et revalidation atomique côté CORE.
L'accord antérieur sur IDENTITY001B.1 n'est pas une autorisation de ces évolutions JNI.

Après cette décision seulement : implémenter/qualifier le port natif sur corpus
synthétique, avec comparaison exacte des tokens préparés et consommés sous même
modèle/worker/bail ; tester les refus avant decode et la non-régression historique.
La qualification Android physique et le raccordement IDENTITY au chat restent des
gates séparés. Persona toujours désactivée par défaut. Aucun merge main.

Le SHA de livraison peut être obtenu par `git rev-parse HEAD` sur cette branche ;
le compte rendu de publication donne le SHA distant vérifié, sans auto-référence
circulaire dans ce document.

## Publication contrôlée

Commit de livraison : `0c934464e891987e61094b4c2466832231c6029b`, publié sans forçage et SHA distant vérifié.
Un complément documentaire corrige la présentation du diff proposé, sans changer
les signatures ou le runtime. Dernier SHA : `git rev-parse HEAD` sur
[feature/identity-002t-tokenizer-port](https://github.com/bubth720/JAVIS-Local-v2/tree/feature/identity-002t-tokenizer-port).
Aucune PR créée ni fusion effectuée. Main reste226e2e8, la branche ID-002 reste8adec3e
et CORE–VOICE reste7426665 lors du contrôle de clôture. Arbre UNIFIED propre.
