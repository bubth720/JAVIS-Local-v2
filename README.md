# JARVIS Local v2

Assistant Android **local-first** : modèle GGUF, mémoire SQLite, PDF/RAG, Whisper hors ligne, synthèse vocale locale et architecture d'outils extensible.

## Objectif

Après compilation avec le workflow GitHub fourni, l'APK contient les deux modèles nécessaires :

- `Qwen3-1.7B-Q4_K_M.gguf` → renommé `jarvis.gguf` (~1,28 Go)
- `ggml-base.bin` Whisper → renommé `whisper.bin` (~148 Mo)

L'application finale n'a donc rien à télécharger pour faire tourner le noyau IA.

## Noyau v2

- Chat GGUF local via llama.cpp Android.
- Historique et mémoire persistants en SQLite.
- `/remember ...` pour mémoriser une information.
- `/docs` pour lister les PDF importés.
- Extraction locale du texte des PDF.
- RAG lexical local pour injecter les passages pertinents dans le prompt.
- Reconnaissance vocale locale via Whisper.
- TTS Android en privilégiant les voix qui ne nécessitent pas de connexion réseau.
- Outils locaux : calculatrice et date/heure.
- Prompt/personnalité modifiable depuis l'application.
- Registre d'outils prévu pour Gmail, calendrier et autres modules futurs.

## Confidentialité du noyau

Le manifeste retire explicitement les permissions `INTERNET` et `ACCESS_NETWORK_STATE`. Cette version du noyau ne peut donc pas utiliser le réseau à l'exécution. Les futurs modules réseau devront être ajoutés volontairement dans une version séparée.

## Construire l'APK

Sur GitHub :

1. Ouvrir **Actions**.
2. Ouvrir **Build JARVIS Local APK**.
3. Cliquer **Run workflow**.
4. Attendre que le workflow soit vert.
5. Ouvrir **Releases**.
6. Télécharger `JARVIS-Local-v2.apk`.

Aucun Android Studio n'est nécessaire sur le téléphone.

## Premier démarrage

Les modèles sont livrés dans l'APK puis copiés une seule fois vers le stockage privé de l'application afin que llama.cpp et Whisper puissent les ouvrir comme fichiers locaux. Cette première préparation peut prendre un moment et exige de l'espace libre supplémentaire.

## Limites v2

- PDF scanné sans couche texte : OCR local non encore intégré.
- RAG : recherche lexicale, pas encore vectorielle.
- Voix personnalisée/clonage : moteur TTS neuronal dédié à ajouter plus tard.
- Gmail/calendrier : volontairement absents du noyau offline actuel.
