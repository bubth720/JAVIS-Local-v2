# ALPHA1-002 — risques, régressions et protections

Aucun patch appliqué. Toutes les surfaces ci-dessous décrivent les risques d'une
future application ; les tests actuels sur CORE ne qualifient pas le candidat natif.

## Fichiers sensibles proposés dans PATCH-A-VALIDER.patch

Le patch est composite, avec préfixes `upstream/` et `core/`. Il doit être examiné
sur leurs bases exactes, jamais appliqué à la racine du dépôt courant.
PATCH_FILES.json donne les SHA-256 avant/après des douze fichiers.

| Préfixe et chemin | Changement proposé / risque |
|---|---|
| upstream/library/src/main/jni/llama_jni.cpp | Barrières/verrou pour les cinq JNI ; propriété des opérations modèle, génération de chargement, epoch KV, une séquence ; refus second modèle |
| upstream/library/src/main/jni/measured.inc | Corps prepare/consume/discard, UTF-8 strict, rendu/tokenisation/budget, ticket, prefill/génération |
| upstream/library/src/main/kotlin/dev/ffmpegkit/llama/LlamaJNI.kt | Trois nouvelles signatures JNI, anciennes conservées |
| upstream/library/src/main/kotlin/dev/ffmpegkit/llama/Llama.kt | Worker partagé et release synchronisée ; suppression du seul snapshot privé après release |
| upstream/library/src/main/kotlin/dev/ffmpegkit/llama/LlamaModel.kt | Métadonnées immuables du snapshot ; constructeur historique deux arguments conservé |
| upstream/library/src/main/kotlin/dev/ffmpegkit/llama/LlamaMeasured.kt | Wrapper scoped, copie modèle figée, hash, ticket privé, reçu, nettoyage, refus connu/inconnu |
| upstream/library/build.gradle.kts | Coordonnées expérimentales distinctes ; aucun changement ABI/minSdk/options matérielles |
| core/app/src/main/java/com/stark/jarvislocal/LlamaLocalEngine.kt | Appel mesuré sous Mutex/bail existants, factory interne explicite non utilisée, ancien backend par défaut |
| core/app/src/main/java/com/stark/jarvislocal/ExactAndroidBackend.kt | Hash de modèle/template exigé, binding complet et validation du reçu |
| core/app/src/main/java/com/stark/jarvislocal/ExactLocalInference.kt | Port expérimental interne, pas contrat v1/IPC |
| core/app/src/main/java/com/stark/jarvislocal/InferenceController.kt | Requête/provenances figées et revalidation session/epoch/stop avant consommation |
| core/app/build.gradle.kts | Consommation exclusive du futur AAR explicitement fourni et revu ; aucune coexistence ni fallback |

Le dernier changement signifie que le patch complet nécessitera un AAR expérimental
approuvé pour compiler CORE ; ce fichier n'est pas livré. Cela empêche d'appliquer
quelques appels puis de croire que l'AAR0.1.1 les supporte. Aucun import de ces
nouvelles classes n'a été appliqué aux sources réelles.

## Matrice de risques

| Risque | Protection proposée et qualification obligatoire |
|---|---|
| Provenance binaire incertaine | Lock source/gitlink, build contrôlé double, hashes/SBOM/attestation ; gate encore fermé |
| Collision de classes/SONAME | Un seul AAR explicitement sélectionné ; vérifier graphe Gradle, APK et ELF, y compris dépendances transitives |
| Allocation concurrente | Bail CORE avant load, même Mutex/worker, table native refuse second modèle ; mesurer nombre réel de modèles/contextes |
| Nouveau registre pris pour un arbitre | Table d'intégrité uniquement ; aucune acquisition/libération de permission ; ProcessInferenceAdmission reste inchangé |
| Mauvais modèle ou chemin remplacé | Copie privée hashée avant load, original conservé ; hash attendu approuvé, disque borné ; compromis même UID/root non couvert |
| ABA / ticket réutilisé / changement contexte | Génération native monotone, ownerEpoch et bail dans binding, nonce frais, usage unique, worker et kvEpoch |
| SYSTEM perdu ou double tokenisation | Template qualifié sans fallback ; seuls IDs natifs retenus décodés ; comparer réellement les deux séquences au futur banc |
| Encodage différent | Nouveau ByteArray strict de bout en bout, NUL/surrogate invalides refusés ; historique non converti silencieusement |
| Injection via marqueurs spéciaux | parse_special reste explicite ; documents restent données non fiables ; tester adversarial et permissions CORE hors modèle |
| Budget/overflow | min capacités réelles/demandées/admises, marge128, sortie complète, bornes ; 4214 vérifications pures seulement, vrai modèle restant à tester |
| Annulation pendant prepare/admission | Revalidation CORE synchrone ; avant admission discard, après admission drainage puis refus tardif |
| Native bloqué ou cleanup inconnu | Pas de timeout de libération, reset ou nouveau bail ; quarantaine et attente/fin réelle du processus |
| Refus à tort trop bloquant | Consume échoué est conservativement quarantainé, même expiration pré-decode ; mesurer ce coût avant toute activation |
| Deadlock/réentrée/thread différent | Ordre gate CORE→Mutex→worker→verrou natif ; callback final sans JNI/suspension/I/O ; tester release/reload/cancel concurrents |
| Copie GGUF lente ou manque disque | Copie unique temporaire et fsync, espace préalable, erreur fermée ; tester disque plein/source mutée/crash et snapshots orphelins |
| Données privées dans buffers | Aucune persistance/log du prompt, IDs ou tickets ; effacement volontaire sans promesse cryptographique ; pas de données personnelles dans les preuves |
| API24/25 ou ABI | ARM64/API24 conservés ; tests JVM ≠ essai Android ; minSdk, symboles, libc++ et alignement à contrôler sur nouvel AAR |

## Non-régressions par module

- ALPHA1-001 : laboratoire, opt-in, historique RAM, état UI, annulation, quarantaine
  et mode historique inchangés. Aucun changement AppGraph/MainActivity/SessionCoordinator
  dans le patch. Factory exacte interne non appelée ; aucun réglage d'activation.
- IDENTITY : contrat001B.1, ses six vecteurs et bundle approuvé inchangés. Aucun
  manifeste Athéna ni nouveau curseur ; nativeVerified n'autorise rien à lui seul.
- SQLite/préférences : aucune migration, suppression ou écriture proposée ; pas
  de copie de conversations/souvenirs dans les preuves. Les chemins d'erreur doivent
  toujours rejeter les résultats tardifs avant persistance ou TTS.
- Historique : les fonctions et signatures restent mais le worker/registre partagé
  et le refus d'un second load sont sensibles. Rejouer les tests avec le nouvel
  artefact réel, pas seulement compiler le wrapper contre les classes anciennes.
- VOICE/Whisper : aucun fichier changé. Le registre Llama ne couvre pas Whisper ou
  un autre APK ; une fin de mesure ne donne aucun handoff VOICE. Les gates natifs
  de COORD/F3 demeurent indépendants et non résolus par cette proposition.
- Fixture /2 : tous fichiers/contrats inchangés. Aucun token, reçu ou preuve de
  ce chemin local n'est converti en attestation interprocessus. CORE004D reste bloquée.

## Retour arrière

Avant future application : sauvegarde Git et hashes de dépendances ; aucune fusion
main. Revenir au commit/binaire antérieurs uniquement après fin réelle de processus
et démarrage propre, sans reset à chaud du bail. Aucune migration de données.
La suppression d'un snapshot privé n'autorise jamais la suppression du GGUF source,
ni un nettoyage global de models/, SQLite ou profils vocaux. Pas de mise à jour
logicielle automatique ni d'installation sur appareil personnel.

Les protections matérielles déjà présentes restent inchangées. Cette extension
ne fournit pas à CORE la SafetyPolicy thermique/batterie du LLM séparé : aucune
qualification matérielle supplémentaire déduite des tests hôte.
