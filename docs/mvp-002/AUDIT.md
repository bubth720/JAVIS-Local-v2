# MVP-002 — audit et choix de périmètre

Base distante MVP-001 vérifiée : `7368f41bbcec389e4b76c99ea27819645d94a50d`.
Les résultats383 tests/kit MVP001 sont des références historiques avant cette
mission ; les réexécutions actuelles sont consignées dans VALIDATION.md.
Les sources externes INTERFACE139bfdc4aac5d2fc9b841c5b804c3b89bfc57ec6 et
VOICE6a86d847ff57fd9f695ee87eb5bff21c0c8862ca ont été lues dans des clones propres,
sans commit, build, modification ou activation de ces dépôts. REFERENCES.json
épingle les fichiers examinés et leurs hashes. Aucun pack/profil B n'a été ouvert.

## État réellement constaté

- MVP001 : moteur historique et import GGUF contrôlé, conversation en RAM,
  SYSTEM distinct et contrôleur exclusif. Pas de JNI Android réel validé.
- JarvisCore/LocalDatabase/RagEngine/SettingsStore : conversations et commandes
  `/remember`/`/docs` existantes, stockage SQLite schema2 ; aucune mémorisation
  automatique. Mémoire et messages sont des écritures séparées (échec partiel possible).
- AppGraph/SessionCoordinator : chemins historiques avec voix, PDF et commandes,
  mais préparation des assets et modèle lié à ModelManager ; ne pas les remplacer
  globalement pour intégrer un import expérimental dans une autre application.
- LocalTts : sélection française sans réseau et refus si absente ; la fonction
  choisissait néanmoins une voix marquée non installée. Correction unique :
  exclure KEY_FEATURE_NOT_INSTALLED, sans téléchargement ni repli réseau.
- WhisperLocalEngine/WavRecorder : chemin local historique présent. La transcription
  charge un modèle, retourne puis libère ; une exception de chargement/release
  ne constitue pas une attestation d'absence de résidence. Pas de qualification
  ici du modèle importé ni d'admission Whisper coordonnée avec le LLM.
- INTERFACE : Compose, thèmes et dessin procédural en couches ; pas compatible
  directement avec le module Views sans dépendances/réécriture. Palette et
  sous-ensemble de géométrie adaptés en style/vector drawable statiques, attribution
  et sources exactes conservées. Aucun ViewModel/module/application Compose importé.
- VOICE005K : contrat proposé et harness, pas de pont CORE opérationnel. Distinction
  nativeDone/playback/unload et absence d'audition humaine attestée réutilisées
  comme principes ; aucun transport, profil, moteur ou contrat Binder activé.

## Composition retenue

Variante mvp2/package `com.stark.jarvislocal.mvp002`, minSdk24/ARM64, distincte
pour préserver toutes les installations. Mvp2Application dérive de la composition
MVP et injecte CoreConversationAdapter dans son unique MvpRuntime/contrôleur.
Le port optionnel reste absent de MVP001 : pas de passage implicite du mode RAM
au mode persistant dans cette ancienne variante. Le constructeur CORE ouvre la
session ; le runtime persistant ne l'écrase pas. JarvisCore, schéma, ModelManager,
LlamaLocalEngine et ProcessInferenceAdmission restent inchangés.

Import d'un autre GGUF : historique DB conservé, uniquement cache RAM de prompt
MVP pur réinitialisé. Historique affiché limité40 messages, toutes les lignes
SQLite antérieures sont conservées. Pas de transfert des bases d'autres packages ;
une conversation volatile MVP001 perdue n'est pas récupérée fictivement.

## Voix et limites assumées

TTS désactivé par défaut. Mode avion exigé avant binding/lecture ; erreur de
lecture du réglage = refus. Voix française locale installée exigée. Phrase test
publique puis confirmation humaine d'audition avec Wi-Fi/données coupés ; preuve
volatile, lecture manuelle bornée300 unités UTF16 sans troncature ni repli.
READY/retour speak=true signifient configuration/acceptation Android, pas écoute,
fin PCM, arrêt attesté d'un modèle externe ou mesure de réseau. Le mode avion
peut coexister avec Wi-Fi : son absence est attestée par l'utilisateur, pas mesurée
par l'application. Les services TTS système ont leurs propres UID/permissions ;
aucune preuve acoustique ou qualification physique n'est acquise dans le cloud.
Arrêt/désactivation à l'arrière-plan, pas de lecture automatique de l'historique.

Whisper : import GGML privé séparé >50Mio/<=160Mio, provenance/licence déclarées,
SHA attendu, taille exacte, en-tête borné et réserve disque, via le même installateur
atomique que le GGUF. Les manifestes GGUF antérieurs sans champ format sont lus
sans réécriture ; aucune sélection croisée GGUF/Whisper. L'intégrité d'un import
n'est ni compatibilité native ni qualification d'une transcription. Le microphone
est **désactivé et sans permission** dans cette livraison. Aucune capture audio,
aucune allocation Whisper en parallèle du LLM. Texte conservé. Pas de faux ASR.
Avant activation future : admission heavy commune, unload LLM confirmé, traitement
fail-closed de load/release Whisper, puis modèle et chaîne WAV qualifiés sur Android.
Ne pas déduire ces garanties d'un simple flag ou d'une simulation.

NEXUS/SERVICES/EVOLUTION ne sont que des descriptions de capacités indisponibles,
sans méthode d'activation, données fictives ni service. Les pilotes Binder restent
exclus de l'APK. L'avatar V1 n'a aucune horloge, travail natif ou accès au moteur.
