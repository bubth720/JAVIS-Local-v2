# Architecture JARVIS Local v2

Flux principal :

Utilisateur → MainActivity → JarvisCore → mémoire/RAG → llama.cpp → réponse.

## Noyau
- llama.cpp Android : inférence GGUF locale.
- Whisper Android : voix vers texte locale.
- PDFBox Android : extraction du texte PDF.
- SQLite : mémoire, historique, documents et passages.
- RagEngine : recherche lexicale locale des passages.
- Android TTS : lecture des réponses avec voix locale si disponible.

## Sécurité
Le manifeste retire INTERNET et ACCESS_NETWORK_STATE. Le noyau v2 n'a donc pas de capacité réseau à l'exécution.

## Évolutions prévues
- OCR local pour PDF scannés.
- Embeddings + index vectoriel.
- TTS neuronal/clonage vocal local.
- Modules Gmail/Calendrier dans une variante réseau séparée.
- Registre d'outils avec confirmations pour les actions externes.
