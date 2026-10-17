# ID-001B — contrat candidat jarvis.identity/1

**001B.1-candidate — borné et testable, en attente de revue/acceptation.**
Architecture/constitution ID-001 approuvées par la décision1748cb0, pas ce codec
runtime. Issue3 autorise cette spécification et ses validations offline uniquement.
Aucun format ne devient « approuvé » par calcul d’une empreinte.

## Autorité normative et portée

Les huit fichiers schemas/*.schema.json, STYLE_TABLE.json, limits.json, ce texte,
IdentityPort.md et vectors.json forment le bundle candidat décrit dans BUNDLE.json.
Chaque objet exige version=jarvis.identity/1, schema_revision=001B.1-candidate,
kind fermé, id UUID canonique minuscule et revision entier positif. Champs inconnus
refusés à toute profondeur. Les schémas sont JSON Schema draft2020-12 portable ;
le profil sémantique ci-dessous est **obligatoire en complément**, pas seulement
une validation JSON Schema générique. scripts/identity_contracts.py est un
vérificateur de revue UNIFIED réel, pas un codec Android ni un moteur d’identité.

Les anciens docs ID-001 restent historiques au statut de leur livraison. Ce dossier
précise leur base de conception sans modifier contrats UNIFIED v1, SERVICES v1,
CoreVoiceRequestV1, fixtures /1 ou /2. Requêtes de génération, IPC, écriture privée,
IdentityExpressionIntent et IdentityAssessment ne sont **pas gelés** par ce bundle.
Ils restent à proposer dans leurs missions ; aucun transport n’est choisi.

## Huit types séparés, tous fermés

| kind / schéma | Sens, limites et relations |
|---|---|
| manifest | Configuration technique : name=JARVIS, profile=B, locale=fr-FR, constitution_sha256, composer_revision et six styles typés. Aucun champ permissions/capabilities/tools/system/model_path/policy exécutable, ni texte libre de personnalité ou donnée personnelle |
| snapshot | État immuable de session : session_id, manifest_ref, policy_revision, consent_revision, composer_revision, trois listes de références, styles, created_ms/expires_ms. Profil ID-002 : listes jarvis_preferences/personal_preferences/consents vides, styles égaux au manifeste et même composer_revision ; aucune écriture privée cachée |
| jarvis-preference | Préférence **simulée**, category=SIMULATED, key/value typés, storage=TECHNICAL_CONFIG, provenance=PROGRAMMED ou USER_APPROVED, durée et decision_ref nullable. PROGRAMMED exige null ; USER_APPROVED exige décision exacte valide liée au même objet/base/patch, pas un sentiment ou droit réel |
| personal-preference | category=PERSONAL_STYLE, subject_ref opaque, key/value typés, durée et storage=EPHEMERAL_ONLY. Aucun champ de souvenir/conversation/nom/voix ; stockage personnel durable non autorisé par ID-001B |
| consent | Événement GRANTED ou REVOKED, subject_ref, content_ref exacte, purpose=PERSONAL_STYLE_SESSION, confirmation_ref, consent_revision et dates. Un objet bien formé n’authentifie pas la confirmation ni un utilisateur ; CORE doit vérifier hors bande le chemin UI de confiance |
| change-proposal | Candidat immuable decision_state=PENDING, target_kind/base_ref, proposer USER/CORE/EVOLUTION, sensitivity et storage, patch1..6clés uniques de style, rationale_ref opaque et durée. Personnel implique cible personal-preference et EPHEMERAL_ONLY. Pas d’activation implicite |
| decision | Événement APPROVED ou REJECTED lié au proposal_ref exact, reviewer_ref/confirmation_ref, scope=EXACT_PROPOSAL_ONLY et durée. Ni réécriture du proposal ni permission attribuée par son contenu |
| application | Représentation technique STAGED/APPLIED/FAILED/ROLLED_BACK, proposal_ref/decision_ref, transaction_id, previous/result digests, rollback_ref/date. APPLIED exige décision APPROVED exacte valide et résultat correspondant au patch ; **aucune application exécutée** par le vérificateur |

Les schémas constituent les listes exactes de champs et bornes par champ ; pas de
valeur facultative ignorée. Nullable signifie JSON null, jamais chaîne "null".
Reference={id,revision,sha256} exige objet canonique exact de la bonne sorte ; un
répertoire contenant plusieurs révisions utilise (id,revision), pas id seul.
Référence absente/mauvais hash/type/version = REFERENCE_MISMATCH/INCOMPATIBLE.

### Statuts sans cycle d’empreintes

Choix technique **à accepter en revue** : la proposition reste un payload PENDING
immuable ; la vue décisionnelle PENDING/APPROVED/REJECTED est calculée depuis ses
événements decision. La décision référence le digest de la proposition ; la
proposition ne référence pas le digest de la décision. Application référence les
deux et reste distincte. Cela évite un cycle proposition→décision→proposition.
Modifier le patch/base crée une nouvelle proposition et requiert nouvelle décision.
Un JSON déclarant APPROVED dans change-proposal est invalide, pas un reçu.

Même une proposition PENDING personnelle ne peut être conservée durablement ici.
Les structures consent/decision/application sont des modèles pour revue et tests,
pas une autorisation de persistance ni une méthode d’écriture d’IdentityPort.
Révocation prioritaire : parmi événements du même contenu/sujet, révision de
consentement maximale ; égalité conflictuelle refuse, expiré/inconnu refuse. Ne
jamais sélectionner un vieux GRANTED parce que REVOKED est plus récent ou expiré.
Une suppression de preuve de révocation ne réactive pas l’accord. Le vérificateur
ne peut constater une preuve absente de l’inventaire fourni : CORE doit obtenir
l’inventaire de confiance complet, pas une liste sélectionnée par le LLM.

## Styles B autorisés — pas de prompt arbitraire

STYLE_TABLE.json fixe assertiveness={ASSERTIVE,CALM}, warmth={WARM,NEUTRAL},
verbosity={CONCISE,BALANCED,DETAILED}, humour={OFF,LIGHT},
critical_thinking={EVIDENCE_FIRST}, addressing={VOUS,TU}.
Chaque valeur est contrôlée **par sa clé** ; TU n’est pas une intensité d’humour.
Le snapshot de base proposé est ASSERTIVE/WARM/CONCISE/OFF/EVIDENCE_FIRST/VOUS.
Modifier ce choix nécessite revue de la cible et de la version, pas ingestion d’une
préférence induite par un document. L’utilisateur décide du tutoiement et de l’humour
lorsque ces préférences seront explicitement prises en charge.

## Types, bornes et encodage déterministe

- UTF-8 strict sans BOM ; pas de surrogate isolé, doublons, trailing data, Float,
  exposant JSON, NaN ou Infinity. Entiers0..9007199254740991 ; bool n’est pas un entier.
- Corps<=32768octets, profondeur<=8 (racine0 dans le parcours, comptage lexical des
  conteneurs<=8),1024valeurs au plus, listes<=16. Scan profondeur avant parse ;
  volume bytes borné avant allocation du graphe. Le profil impose la borne la
  plus stricte entre schéma et limites globales.
- maxLength de chaque champ borne **aussi les octets UTF-8**, en plus des caractères
  JSON Schema ; chaîne globale<=8192octets. NUL/DEL/contrôles hors LF/CR/TAB refusés.
  Manifest canonique<=4096octets ; patch1..6 et refs<=16, UUID36, SHA64hex minuscules.
- Clés ASCII [a-z][a-z0-9_]{0,47}. Dates epoch UTC en millisecondes entières ; expiry
  strictement supérieur à création. Pour l’usage, vérifier l’horloge de confiance,
  date/validité/révocation au dernier moment ; un timestamp ne prouve pas l’autorité.
- C(v)=JSON canonique restreint : clés ASCII triées récursivement, séparateurs ','
  et ':', pas espaces ni newline finale ; chaînes UTF-8 directes, échappement JSON
  des guillemets/backslash et contrôles (formes courtes \b\t\n\f\r, autres \u00xx
  minuscules), pas d’échappement '/'. Entiers décimaux canoniques, true/false/null
  minuscules, listes dans l’ordre. Unicode **non normalisé**.
- D(v)=SHA256(UTF8("jarvis.identity/1:001B.1") ||00|| C(v)). Ne jamais ajouter
  le digest d’un objet dans ses propres champs. Deux représentations de l’entrée
  différant seulement par l’ordre des clés donnent C identique ; é et e+accent
  combinant restent différents. Les six vecteurs sont vérifiés Python/JavaScript.

Le JSON Schema seul n’exprime pas les octets UTF-8, références, révocation, limite
wire ou sécurité du canal : vérifications sémantiques impératives. La validation
porte seulement sur les mots-clés utilisés, via le vérificateur existant de schémas
UNIFIED réemployé **sans le modifier**, complété par le nouveau module IDENTITY.

## Budgets et IdentityComposer — spécification, pas code runtime

Identité uniquement en **SYSTEM**, via mapping fermé de styles approuvés ; aucun
texte libre de préférence/document/proposition collé comme instruction. CORE garde
son systemPrompt configurable exact : conflit → refus/aperçu d’un changement autorisé,
pas écrasement. Fragment persona<=2048octets UTF-8 candidat, total SYSTEM soumis
au plafond réel du backend (P2 actuel8192octets) et au budget final de tokens.
2048octets n’est pas un budget tokens ni une garantie que le modèle peut le recevoir.

Le backend demeure propriétaire de sa politique fixe, du template GGUF, BOS et
préfill ; le composer ne les copie pas. Compter le **rendu complet exact** : politique
native+SYSTEM+question+historique+contexte+sources+template+réserve de sortie/garde.
Tokenizer réel du modèle/template disponible et attesté avant chemin de génération
avec persona ; sinon TOKENIZER_UNAVAILABLE, pas compteur de fixture ni octets=tokens.
Budget requis<=min(demande,admission,capacité) ; ne relever aucun seuil/profil.
Dépassement : CONTEXT_EXCEEDED, aucun retrait silencieux de sources/politique/
question ni conversion SYSTEM en user. Aucun runtime Composer implémenté ici.

L’ancien proxy UTF-8 CORE est conservé comme précaution historique, pas annoncé
compteur natif. InferenceContract a SYSTEM séparé ; SessionCoordinator garde
single-flight, stop/drain et filtrage des réponses. Un snapshot ne modifie ni
nativeDone ni libération ; valeurs de preuve native ne viennent jamais d’IDENTITY.
Refus du nouveau mode ne permet pas de charger un secours si l’ancien est UNKNOWN.
Aucune persona ni champ ajouté aux fixtures /2 ; bootstrap bloqué reste indépendant.

## Portabilité et sortie de revue

Contrat logique sur types simples compatibles API24, sans java.nio.file ou API29
imposées à CORE. Epochs/dates primitifs, pas de nouvelle permission Android, moteur,
modèle, capteur ou dépendance cloud/PC. Les fournisseurs SERVICES/NEXUS ne sont pas
requis par le port en lecture seule ID-002. Aucune base CORE/LLM d’intégration choisie
par un schéma ; revue004D/intermédiaires avant développement, pas fusion de branches.

Accepter après revue : enums/style de base, bornes/algorithme, proposition immuable
avec événements séparés, read-only ID-002, refus si tokenizer/capacité absente.
BUNDLE.json fournit le digest **réel du candidat présent** ; il ne remplace ni
l’accord utilisateur ni un codec Kotlin/Android testé. CORE ID-002 attend cette
revue/acceptation ; ID-003/004/005 requièrent leurs contrats et autorisations propres.
