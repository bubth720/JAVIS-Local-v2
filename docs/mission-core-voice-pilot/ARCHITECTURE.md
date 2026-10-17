# C-VOICE-001 — frontières et conservation

Branche `feat/core-voice-pilot`, base réelle `115dd3fe9b8e82b295b076dc045601a04d21a06f` : mission documentaire au-dessus de main `226e2e8cc3eebd479805b89d035eb5c1cbcad540`. Les branches004A/B/C ne sont pas fusionnées ici ; leurs187 tests ne sont donc pas les tests de référence de cette branche. La base contient40 tests (33 SQLite/API24,25,35 +7 heure).

## C1 — port de sortie compatible

MainActivity conserve `speakReplies` et le même emplacement d'appel après chat/persistance ; elle appelle `graph.speechOutput.speak(answer.text)`. AppGraph compose `SpeechOutputSession(HistoricalSpeechOutput(tts::speak))`. LocalTts reste byte-identique, ses appels/voix/QUEUE_FLUSH/cycle de vie ne sont pas réécrits. Le texte historique, même long, est transmis intégralement ; le plafond300 UTF-16 ne s'applique qu'au pilote désactivé.

Chaque demande reçoit session/corrélation/annulation UUID ; une nouvelle corrélation ou invalidation rejette les callbacks anciens. LOCAL_TTS_REQUESTED est une demande soumise, pas une preuve de lecture, disponibilité offline ou audibilité. L'annulation d'une attente invalide ses résultats, ne prouve pas l'arrêt natif. Aucun nouveau log contenant le texte.

Le port DisabledVoicePilot refuse systématiquement ; API24/25 reçoit API_UNSUPPORTED et conserve le chemin historique. Sur API26+, les entrées malformées/trop longues sont refusées sans troncature. Aucun flag distant/build ne permet de l'activer.

## Limites historiques conservées

SQLite/mémoires/commandes/PDF/RAG/Whisper/paramètres/historique non modifiés. Le TTS historique peut garder ses limites d'initialisation et rotation connues ; ce petit adaptateur n'en prouve pas la correction. Les modèles sont toujours gérés par les wrappers historiques. Aucun service, manifeste, permission, ABI, minSdk, dépendance ou schéma modifié pour C1.

C2 doit prouver le déchargement natif avant C3. Aucun client Binder de production ni activation du pilote ne peut être ajouté en contournant cette étape. Voir SECURITE-RESSOURCES.md pour le stop de sécurité et CONTRAT-SHA.md pour le contrat fixture coordonné avec VOICE.
