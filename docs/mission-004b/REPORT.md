# JARVIS CORE — MISSION 004B

## Référence, audit et portée

Dépôt unique : `bubth720/JAVIS-Local-v2`. Branche de départ
`mission-004a-core-stability`, commit vérifié localement et via FETCH_HEAD :
`306a89b7cc33803aba9b7af9c6a553f1bc40f08f`.
Branche de travail/livraison : `mission-004b-inference-contract`.
Le remote ne créait pas automatiquement de référence de suivi pour la branche
004A lors du fetch ; c'est son FETCH_HEAD, et non une référence supposée, qui a
été vérifié. Aucune modification ou fusion de main n'est effectuée.

L'audit porte sur LocalLanguageModel, AppGraph, LlamaLocalEngine, JarvisCore,
SessionCoordinator, PromptBuilder, leurs appelants et les 140 tests 004A.
Ces tests ont été relancés avant modification, puis intégralement dans la
validation finale. AGENTS et PROJECT_KNOWLEDGE ont été lus et actualisés.

Les textes officiels UNIFIED D4-02 et LLM 003D ne figurent ni dans le dépôt
ni dans les documents locaux disponibles. Leurs liens/extraits ont été demandés.
Les exigences explicitement formulées dans la mission sont implémentées ;
**la conformité textuelle complète à O1, O2/P2 et O4 n'est pas attestée**.
La règle P2 citée dans la mission (pas de conversion silencieuse du système en
message utilisateur) est respectée et testée. Aucun contenu absent de D4-02 ou
003D n'est inventé. Voir [LLM_003D_MAPPING.md](LLM_003D_MAPPING.md).

## Défauts/écarts observés avant modification

| Fichier | Fait confirmé dans 004A | Réponse 004B |
|---|---|---|
| LocalLanguageModel | Trois méthodes seulement, pas de corrélation ou contrat de libération | Extension compatible, requête/réponse structurée, capacités et résultats explicites |
| LlamaLocalEngine | `releaseInternal` absorbe l'exception de release puis oublie le handle | Handle conservé, quarantaine après libération échouée/inconnue ; pas de nouveau chargement aveugle |
| AppGraph | Construit directement le moteur historique | Factory paresseuse + contrôleur exclusif, mode historique par défaut |
| JarvisCore | Prompt aplati et texte retourné sans identifiants de session/requête | Champs séparés et contrôle des IDs/fin avant persistance de l'assistant |
| SessionCoordinator | Draine déjà les opérations, mais ne transmet pas de demande d'arrêt au port | Hook d'arrêt à la fermeture/délai/annulation, erreur si état natif non attesté |
| PromptBuilder | Métadonnées limitées aux noms des documents | Contexte sans la question et références des passages inclus, texte historique conservé |

## Contrat et fonctionnement livré

`InferenceRequest` conserve séparément : version locale 1, sessionId,
requestId, systemPrompt configurable, question, context, sources structurées
(documentId/chunkId/documentName/chunkIndex), contextSize et maxTokens.
`JarvisCore` crée une session UUID par instance et un UUID par requête. La
session du graphe reste stable à la rotation ; une nouvelle instance de Core
ouvre une nouvelle session et invalide les réponses de l'ancienne.

Le prompt rendu reste `context + UTILISATEUR: question + ASSISTANT:`. Le
système est passé par l'argument distinct `systemPrompt` de la bibliothèque
historique ; il n'est pas inséré dans ce rendu. Le budget conservateur UTF-8
004A reste utilisé et est revérifié au port avec la réserve de 128 et les
limites de génération. Ce contrôle n'est pas un tokenizer exact de Qwen.
Les noms affichés restent des sources fournies, pas une citation sémantique attestée.

`LocalLanguageModel` garde ses trois méthodes historiques. Les implémentations
existantes utilisent un pont par défaut `infer` → `generate` à système distinct.
Le déchargement par défaut est UNKNOWN : une ancienne implémentation ne peut
pas autoriser un changement de moteur simplement avec `isLoaded=false`.
Le contrôleur garde aussi un chemin legacy préservant exactement le prompt
rendu et l'argument système, sans les reparser ou les fusionner.
La compatibilité est vérifiée à la recompilation des implémentations Kotlin
existantes ; aucune ABI binaire inter-APK n'est définie ou revendiquée.

Refus avant appel du moteur : contrat autre que la version locale 1, IDs vides,
capacité système séparé absente, limites invalides/dépassées, budget dépassé,
session ancienne, opération occupée, simulateur non configuré ou quarantaine.
Refus après appel : nativeDone non attesté, arrêt demandé, réponse avec mauvais
IDs ou epoch/session invalidée. Une réponse refusée n'est pas enregistrée
comme réponse assistant ; la question déjà persistée est conservée.

## Cycle de vie et sélection exclusive

`InferenceEngineFactory` construit sans préchauffer. `InferenceController`
possède un seul backend à la fois dans le graphe applicatif. AppGraph n'injecte
que le fournisseur historique. Le mode DELEGATED_SIMULATOR n'est ni exposé dans
l'interface ni activé par un réglage ; il n'est utilisable que si un simulateur
est explicitement fourni, comme dans les tests.

| Situation | stop_requested | native_done | model_unloaded | Conséquence |
|---|---|---|---|---|
| Avant tout chargement | faux | vrai | vrai | Sélection autorisée |
| Préparation/génération démarrée | faux | faux | faux | Opération exclusive |
| Arrêt/délai pendant calcul | vrai | faux | faux | Toujours occupé, pas d'autre moteur |
| Retour effectif après arrêt | vrai | vrai | faux | Réponse rejetée, modèle encore détenu |
| Retour normal | faux | vrai | faux | Même moteur réutilisable, changement refusé |
| Déchargement confirmé | faux | vrai | vrai | Choix explicite d'un moteur autorisé |
| Déchargement échoué/inconnu | selon demande | fin d'appel observable | faux | Quarantaine, aucune opération moteur suivante |
| Réponse sans fin attestée | selon demande | faux | faux | Quarantaine, aucune nouvelle opération moteur |

La demande d'arrêt du moteur historique retourne UNSUPPORTED. Il n'existe pas
d'API d'interruption JNI démontrée dans la bibliothèque utilisée. Le contrôleur
attend le retour du travail même après annulation ; le coordinateur conserve
le comportement TIMEOUT occupé jusqu'à ce retour. La quarantaine conserve
l'interdiction logique si un backend ne peut pas attester sa fin ou sa libération.
Un échec remonté avec preuve de fin peut autoriser une nouvelle requête au même
moteur ; il ne prouve pas son déchargement. Un échec de load sans handle est
traité comme ressources inconnues et bloque le rechargement jusqu'à un nouveau
processus. Aucun mécanisme de reset forcé de quarantaine n'est livré.

Pour changer : attendre la fin, appeler unload, exiger CONFIRMED, sélectionner
explicitement le mode puis seulement préparer. Aucun fallback automatique.
Même le retour au mode historique exige cette libération. Un contrôleur local
ne coordonne ni deux contrôleurs indépendants ni deux applications Android.
Conserver une seule composition de production dans AppGraph ; aucune garantie
inter-APK n'est revendiquée.

## Fichiers et justification du périmètre

| Fichier | Modification |
|---|---|
| LocalLanguageModel.kt | Extension compatible du port et pont legacy |
| LlamaLocalEngine.kt | Backend injectable pour tests, libération attestée ou quarantaine, moteur/bibliothèque inchangés |
| AppGraph.kt | Fournisseur historique unique, contrôle et hooks d'arrêt/erreur |
| JarvisCore.kt | Requête structurée et filtrage avant stockage de réponse |
| SessionCoordinator.kt | Transmet la demande d'arrêt, conserve le drainage, distingue erreur de moteur/quarantaine d'un état prêt |
| PromptBuilder.kt | Nécessaire en plus des cinq fichiers prioritaires : expose le contexte sans question et les références incluses, sans modifier le budget/rendu 004A |
| InferenceContract.kt | Nouveau : structures et refus testables sans Android |
| InferenceController.kt | Nouveau : factory, exclusivité, états, corrélation et drainage |
| InferenceControllerTest.kt | Nouveau : sélection, concurrence, arrêts, erreurs, refus et corrélation |
| LlamaLifecycleTest.kt | Nouveau : vraie classe historique avec faux backend, réutilisation/échecs de release/load et annulation |
| SessionInferenceLifecycleTest.kt | Nouveau : coordinateur relié au contrôleur simulé, délai et fermeture |
| CoreInferenceContractTest.kt | Nouveau : Core et SQLite natif Robolectric sur API 24/25/35, contrat et absence d'assistant tardif |
| docs/mission-004b/* | Rapport, correspondance documentaire et validation |
| AGENTS.md / docs/PROJECT_KNOWLEDGE.md | Actualisation requise des connaissances et invariants |

Les 140 tests/fixtures 004A sont inchangés octet pour octet. Aucun changement de
schéma SQLite, préférences, modèle installé, import/recherche, capture/Whisper,
TTS, manifeste, layout, dépendance native/Gradle ou workflow. Aucun téléchargement,
installation, migration réelle, transfert privé, IPC ou modification d'autre dépôt.
Les seules migrations exécutées sont les tests synthétiques déjà autorisés.

## Vérifications, risques et prochaine action

[VALIDATION.md](VALIDATION.md) détaille les 174 cas, compilation et Lint.
Les tests du backend Llama sont des simulations : aucune inférence JNI réelle,
aucun chargement GGUF ou déchargement matériel n'est revendiqué. L'observation
du retour d'une API est le signal de fin utilisable ici ; elle ne prouve pas
l'absence de threads internes non exposés. La libération native réelle doit être
qualifiée sur appareil avant une intégration. Le protocole intermodules et la
coordination de ressources entre APK restent absents.

Priorités avant raccordement : obtenir D4-02/003D et comparer les contrats,
qualifier le backend historique sur Android ARM64, préciser les preuves natives
d'arrêt/libération et le budget tokenizer, puis autoriser séparément tout
transport/adaptateur. Aucun raccordement opérationnel n'est déclaré. Revue de
cette branche et validation utilisateur requises avant une suite ; aucune fusion.

L'identifiant du commit livré et le résultat effectif du push sont fournis
avec le bilan final et les artefacts de livraison, sans prétendre que ce fichier
peut contenir son propre hash de commit.
