# CORE IDENTITY 002 — rapport de livraison

Date : 17 octobre 2026. Dépôt : `bubth720/JAVIS-Local-v2`.
Branche : `feature/identity-002-core-runtime`.
Mission : [issue CORE #6](https://github.com/bubth720/JAVIS-Local-v2/issues/6).

## Résultat et limite bloquante

**Socle implémenté et testé ; injection dans le chat de production NON réalisée.**
Conformément à la consigne utilisateur, l'intégration concernée est arrêtée :
le backend historique ne fournit pas de comptage réel des tokens du prompt final.
Aucun estimateur UTF-8 n'a été rebaptisé tokenizer. La personnalité B n'est donc
pas annoncée active dans l'application. Aucun essai LLM natif ou physique n'est revendiqué.

L'approbation utilisateur du contrat IDENTITY 001B.1 autorise ce prototype.
Référence UNIFIED : [d7c90785ae4263892a6c96e140cfa88206cb4197](https://github.com/bubth720/JARVIS-UNIFIED/commit/d7c90785ae4263892a6c96e140cfa88206cb4197).
Bundle : `431b5212f742bd9028477a6a603acf328c414801873efdd529f45b6ed5e619de`.
Les 14 fichiers normatifs et BUNDLE.json sont copiés sans modification sous
`contract/`. Le suffixe normatif `001B.1-candidate` reste inchangé : l'approbation
est celle de l'utilisateur, pas une réécriture des octets approuvés.

## Audit et choix de base, sans fusion

Inventaire des branches distantes à l'ouverture de la mission :

| Branche CORE | SHA vérifié |
|---|---|
| main ; feat/core-voice-pilot | `226e2e8cc3eebd479805b89d035eb5c1cbcad540` |
| docs/mission-002-project-knowledge | `e0417e1609b478073e7cd019a25f8146a25a0fb8` |
| fix/api-24-25-time-reply | `d7e51b4ac55b0726e5289b7f062478903fb7aa67` |
| fix/sqlite-data-preservation | `7a5b53e3247dc7132eb9e5648ecf4169a4054d62` |
| mission-004a-core-stability | `306a89b7cc33803aba9b7af9c6a553f1bc40f08f` |
| mission-004b-inference-contract | `07cd282603205d57f3d2ded1b14d5d95e68989e2` |
| mission-004b-core-backend-lifecycle | `29646448c80568d868a702192df761fc9d7a896d` |
| fixture/004c-p2-client | `da40ed7155409b85860a22c1dbbfe35029cea29a` |

Aucune branche 004D observée dans cet inventaire ; ne pas présumer sa disponibilité.
Base retenue : [004C da40ed7](https://github.com/bubth720/JAVIS-Local-v2/commit/da40ed7155409b85860a22c1dbbfe35029cea29a).
004B renforcée est ancêtre de cette base ; deux commits supplémentaires sont
`71be9ef` (fixture isolée) puis `da40ed7` (documentation). Le diff de `app/`
entre 29646448 et da40ed7 est vide. Cette base préserve donc le noyau 004B,
son admission commune au processus et l'existant 004C, sans mélanger les branches.
`main` est plus ancien et n'a été ni modifié ni fusionné.

## Implémentation et fichiers

- `app/src/main/java/com/stark/jarvislocal/identity/IdentityCodec.kt` : codec Android
  strict, bornes, UTF-8, canonicalisation et SHA-256 avec domaine normatif.
- `IdentityTypes.kt` : manifeste, références, styles fermés et snapshot immuables ;
  refus des champs inconnus et données personnelles hors périmètre.
- `IdentityPort.kt` : deux lectures techniques uniquement, manifeste local épinglé,
  snapshot éphémère par session, politique hôte désactivée par défaut et révocable.
- `IdentityComposer.kt` : composition SYSTEM exclusivement, conservation question,
  contexte et provenance ; refus de dépassement et de mesure absente/non corrélée.
- `app/src/main/assets/identity/manifest-b.json` : manifeste technique public B,
  identique au vecteur approuvé ; aucune préférence ou donnée personnelle.
- `app/src/test/java/com/stark/jarvislocal/identity/IdentityRuntimeTest.kt` et
  `app/src/test/resources/identity/vectors.json` : 13 scénarios × 3 API.
- `tools/check-identity-002.py` : bundle, vecteurs et préservation octet par octet.
- `tools/setup-identity-cloud.py`, `tools/identity-cloud-env.sh` : outils de
  développement isolés, artefacts épinglés, TLS maintenu et proxy hérité.
- Présents documents ; compléments d'AGENTS.md et PROJECT_KNOWLEDGE.md.

Aucun changement de dépendance, permission, seuil matériel, moteur, base SQLite,
réglage existant, prompt enregistré, AppGraph ou SessionCoordinator. Aucun nouveau
service ni second moteur. Aucun fichier de fixture modifié. Pas d'installation APK,
de téléchargement GGUF/Whisper, de profil vocal ou de donnée utilisateur.

## Configuration et modèle de confiance

L'opt-in est une API technique `IdentityPolicy(enabled = true, inferenceSafe = ...)`,
pas une option activée dans l'interface existante. Le défaut est désactivé et
l'état d'inférence sûr absent par défaut. Le manifeste n'accorde aucune permission.
`LocalIdentityPort` lit un asset ; aucun writer, SharedPreferences ou accès mémoire
personnelle. Les snapshots ne contiennent ni préférences ni consentements personnels.

Les traits B sont un fragment français contrôlé : franchise calme, argumentation,
incertitude, absence de citations inventées, respect et révision face aux preuves.
Le test vérifie la composition de ces instructions, pas leur respect par un modèle.
Les documents/historique ne sont jamais promus en SYSTEM par ce composant.
L'identité technique stable après recréation du port n'est pas une preuve de
continuité conversationnelle ou d'essai de redémarrage physique Android.

Une mesure de test `nativeVerified=true` est un double injecté, jamais une preuve
native. Ce booléen est une assertion d'un futur port hôte de confiance, pas une
signature ni une attestation mémoire. `IdentityComposition` n'est ni un permis de
génération ni une preuve de déchargement. Aucun appel au moteur ne la consomme.

## Matrice de dépendances et risques de fusion

| Élément | État dans cette livraison | Condition suivante |
|---|---|---|
| Contrat IDENTITY 001B.1 | Approuvé utilisateur ; bundle exact vérifié | Ne pas modifier le contrat implicitement |
| CORE 004B renforcée | Conservée intégralement | Admission commune et quarantaine à préserver |
| CORE 004C / fixture /2 | Conservée et testée ; aucune IDENTITY injectée | Corpus synthétique strict inchangé |
| CORE 004D | Non observée ; aucune fusion | Revue de compatibilité sur SHA réellement publié |
| LLM 003E/003F, reprise inter-APK | Pas raccordés par ID-002 | Résoudre et qualifier leur protocole séparément |
| Tokenizer et template natifs | Manquants dans l'adaptateur CORE actuel | Port moteur réel, même modèle/template/réservation |
| Personnalité dans le chat | Bloquée | Mesure réelle + intégration atomique validée |
| Stockage ID-003 / NEXUS | Hors périmètre | Contrat et consentement explicite séparés |

La séparation des nouveaux fichiers réduit les conflits mais ne les annule pas :
revérifier InferenceRequest, capacités SYSTEM, annulation et limites lors de toute
reprise sur 004D. Les ajouts documentaires peuvent nécessiter une résolution manuelle.
Ne pas copier de preuve synthétique dans le runtime ni ajouter une admission privée.

## Prochaine action recommandée

Qualifier un port réel de tokenisation/template dans le propriétaire de l'inférence,
sans deuxième LLM : compter SYSTEM+question+contexte+template, avec identité du modèle,
empreinte du rendu et budget de sortie. La mesure et la génération doivent rester
sur le même worker et bail, sans changement de modèle/template ni concurrence entre
les deux ; revérifier révocation, session et admission juste avant lancement.
Le budget est le minimum du contexte demandé, chargé et autorisé, réserve incluse.
Une simple validation du nombre de caractères ou d'un hash est insuffisante.

Après résolution de ce blocage, proposer une mission séparée de raccordement du
composer au chat et de qualification avec modèle réel. Tester ensuite persona,
contradiction factuelle, injection documentaire, annulation et contraintes Android
physiques. ID-002 n'autorise aucune modification des fixtures /2, aucun déblocage
CORE 004D et aucune fusion dans main.

Résultats exécutés : [VALIDATION.md](VALIDATION.md).
