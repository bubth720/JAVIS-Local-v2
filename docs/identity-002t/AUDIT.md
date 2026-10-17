# ID-002T — audit du propriétaire de l'inférence

Statut : audit réalisé ; implémentation JNI **suspendue avant application**.
Issue lue intégralement : https://github.com/bubth720/JAVIS-Local-v2/issues/8.
Rapport ID-002 et instructions AGENTS/PROJECT_KNOWLEDGE lus. Base exacte vérifiée
sur GitHub et localement : `8adec3e0e8dfdce685a33ef796fc3150facc7f0a`, branche
`feature/identity-002-core-runtime`. Travail : `feature/identity-002t-tokenizer-port`.
Contrat UNIFIED figé : d7c90785ae4263892a6c96e140cfa88206cb4197 ; bundle
431b5212f742bd9028477a6a603acf328c414801873efdd529f45b6ed5e619de.

## Preuves binaires et sources

Artefact réellement résolu par Gradle : `dev.ffmpegkit-maintained:llama-android:0.1.1`.
AAR SHA-256 : `fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501`.
`NATIVE_AUDIT.json` conserve les empreintes du JAR/des bibliothèques ARM64 et les
signatures observées par javap/nm, sans charger les bibliothèques.
`SOURCE_REFERENCES.json` épingle les fichiers amont et leurs liens immuables.
Le tag amont v0.1.1 résout vers 823d1de2b3ef85ba9ef4847a03bac423f4aa1c44.
La correspondance reproductible source→binaire n'est pas démontrée : distinguer
les faits du binaire et l'algorithme lu dans cette révision source officielle.
Aucun modèle téléchargé, natif exécuté, dépôt amont modifié ou bibliothèque remplacée.

- Le JAR et le bridge ELF ont exactement cinq entrées JNI : LoadModel, Complete,
  Embed, ReleaseModel, GetSystemInfo. Aucune mesure préalable ni objet prepared.
- `libllama.so` exporte bien llama_tokenize, llama_chat_apply_template,
  llama_model_chat_template, llama_model_get_vocab et llama_n_ctx. Un symbole C
  n'est pas une API JNI utilisable sûrement depuis CORE : le handle wrapper est
  une structure propriétaire LlamaCtx, pas un pointeur vocab/context public.
- Le binaire contient la clé promptTokens ; le source la remplit après décodage
  et génération. Le bytecode parseResult/LlamaResult ne l'expose pas en Kotlin.
  L'ajouter au résultat ne fournirait toujours pas un budget avant admission.
- Le source build_prompt sélectionne le template du modèle, construit un message
  system et un message user, avec add_ass=true. Sans template ou sur erreur, il
  concatène system et user : ce fallback serait incompatible avec le nouveau
  parcours IDENTITY strict. Ne pas y convertir silencieusement SYSTEM.
- Le source tokenize utilise le vocabulaire du même modèle, add_special=true et
  parse_special=true. Il faut compter le rendu complet, pas sommer des fragments.
- nativeComplete efface le KV puis tokenise et décode. complete(maxTokens=0) n'est
  PAS une mesure : le prefill modifie déjà l'état natif. embed modifie également
  l'état KV/embeddings ; ce n'est pas un compteur de substitution.
- jstr utilise GetStringUTFChars (UTF-8 modifié JNI). Emoji et NUL peuvent différer
  de l'UTF-8 canonique Kotlin ; aucun comptage UTF-8 externe ne prouve l'identité
  des octets consommés. Le changement d'encodage est un point sensible à qualifier.
- Llama.complete commute sur Dispatchers.Default. Un Mutex CORE n'atteste pas
  l'identité d'un thread physique entre deux appels suspendus.

## Audit CORE

| Composant relu | Fait et conséquence |
|---|---|
| LlamaLocalEngine / AndroidLlamaBackend | Un handle conservé, Mutex et NonCancellable ; pas de tokenizer/template observable. Aucun branchement IDENTITY. |
| PromptBuilder.prepare | Documents sélectionnés, souvenirs et historique concaténés au contexte ; question non tronquée, provenances conservées. Borne UTF-8, pas tokens natifs. |
| InferenceRequest / validateInference | SYSTEM distinct, contexte et question rendus côté user ; limite octets+sortie+128 conservée, sans preuve du coût du template. |
| ProcessInferenceAdmission | Propriétaire et jeton monotone ; réservation gardée pendant rechargement, quarantaine après état incertain. Le jeton seul ne distingue pas deux chargements successifs du même propriétaire : futur loadGeneration séparé requis. |
| InferenceController | Gate, session/epoch, refus de réponse tardive, drainage et libération corrélée. La mesure ne doit jamais contourner ce chemin. |
| SessionCoordinator | Single-flight, timeout informatif et attente de fin effective. Ne pas ajouter un second coordinateur ni prétendre arrêter JNI. |
| IdentityComposer | SYSTEM seul, empreinte de requête/provenance et budget arithmétique. nativeVerified fourni par un port de confiance ne prouve pas modèle/template/worker/bail. Aucun objet courant n'autorise une génération préparée. |

La mesure réelle exige donc une extension du propriétaire natif ET une transaction
host scoped. Sans ces changements sensibles, la seule réponse prouvable du port
actuel reste TOKENIZER_UNAVAILABLE. Aucun adaptateur natif fictif ajouté.

## Branche CORE–VOICE : cohabitation documentaire

Référence distante vérifiée en lecture : `feat/core-voice-pilot`,
`7426665f2794cba83646008fa0f342d102141201`. Ancêtre commun avec ID-002 :
`226e2e8cc3eebd479805b89d035eb5c1cbcad540` ; branches divergentes, aucune fusion.
Commits pilotes 05c087b, ee4a1bf, 7426665 : SpeechOutputPort, session vocale,
NativeHandoffGate et documentation. Leur diff depuis l'ancêtre ajoute ces ports
et modifie AppGraph/MainActivity, pas le backend Llama lui-même.
Le gros diff direct entre les têtes comprend surtout les compléments 004A/B/C et
IDENTITY absents de la base pilote ; ce n'est pas une suppression effectuée ici.

NativeHandoffGate est un modèle isolé, sans attestation de production. Sa génération
ne doit pas être assimilée au jeton ProcessInferenceAdmission ni à loadGeneration.
Une fin de mesure n'est ni DONE_UNLOADED ni autorisation de lancer VOICE.
Une future reprise des changements pilotes doit préserver SessionCoordinator,
AppGraph, LocalTts et l'admission 004B, avec revue séparée sur SHA exact.
Aucun fichier VOICE, fixture /2 ou application personnelle modifié.

## Décision de périmètre

La consigne de l'issue impose un diff et une validation spécifique avant extension
JNI/protections. Voir `PROPOSED_JNI_API.patch.txt`, `EXACT_PORT.md` et
`REGRESSION_REVIEW.md`. Le diff est documentaire, non appliqué, non compilé,
non complet : il présente la frontière ABI à approuver, pas un correctif installable.
Aucun changement de code runtime, de dépendance ou d'admission n'a été réalisé.
