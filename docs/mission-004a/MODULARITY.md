# Frontières futures — documentation uniquement

CORE reste l'orchestrateur cible selon la mission. Cette branche conserve
llama-android, Whisper, SQLite/RAG et TTS Android. Aucun transport, découverte
d'APK, binding service, adaptateur inter-APK ou chargement de JARVIS-LLM n'est
implémenté. Aucun autre dépôt JARVIS n'est modifié et aucune donnée n'est transférée.

## Contrats à définir avant extraction

Les noms et champs ci-dessous sont des propositions, pas des API installées.

| Frontière | Opérations et données à définir | Invariants nécessaires |
|---|---|---|
| CORE → LLM | Disponibilité/capacités ; requête avec requestId, texte, système, budget/contextSize/maxTokens et références des passages ; réponse avec texte, fin, erreur typée, provenance fournie | CORE possède ordre et persistance ; moteur ne décide pas de mémoriser ni d'effectuer des actions |
| LLM → CORE | États absent/chargement/prêt/occupé/erreur ; annulation demandée, acquittement d'arrêt réel, résultat corrélé au requestId | Un timeout signifie attente dépassée ; aucun verrou/libération avant acquittement réel ou fin du calcul. Un moteur sans arrêt doit déclarer cette limite |
| CORE ↔ VOICE | Transcription d'un audio avec format/durée/référence locale ; parler(text, utteranceId), interrompre et confirmer arrêt ; disponibilité/erreurs | Propriété et durée de vie de l'audio explicites, suppression après dernier utilisateur, voix locale requise, permission micro détenue par le composant qui capture |
| CORE ↔ NEXUS | Stockage explicite, requête lexicale ou future recherche, lecture d'historique ; résultats avec documentId/chunkId/page/score ; transaction et erreurs | Pas de transfert actuel ; versionnement/export/consentement avant déplacement ; aucun reset destructeur ; mémoire explicite conservée |
| CORE ↔ INTERFACE | Intentions utilisateur (envoyer/importer/micro/arrêter/préférences) et état observable avec requestId, phase, busy, capacités, messages/sources et erreur récupérable | Interface sans JNI/SQL long, collecteurs liés au cycle de vie, pas de référence Activity dans les opérations, saisie conservée si refus |

Le contrat VOICE doit préciser si capture et transcription sont distinctes.
Le modèle de consentement et le propriétaire de chaque permission doivent
être décidés avant tout transport ; l'absence de permission réseau de CORE
reste un invariant. Une capacité indisponible doit être affichable sans
remplacer silencieusement un moteur local par un service externe.

## Fichiers candidats et extraction progressive

| Fichier actuel | Frontière future | Étape réversible recommandée |
|---|---|---|
| `JarvisCore.kt` | Routage de conversation/cas d'usage | Séparer le contrat de génération et celui de persistance après tests de routage, conserver le noyau historique comme implémentation |
| `LlamaLocalEngine.kt` | Backend LLM local | Décrire disponibilité/chargement/contexte et fin effective ; étendre le point `LocalLanguageModel` par décision ultérieure, sans supposer une annulation native absente |
| `AppGraph.kt` | Composition et propriété des ressources | Garder le choix des implémentations au même endroit ; ne pas instancier un composant absent à la place du moteur existant |
| `MainActivity.kt` | Présentation Android | Déplacer progressivement dialogues/états vers présentation testable ; garder callbacks permissions/SAF et rendu Android |
| `PromptBuilder.kt` | Politique de contexte et provenance | Produire un contrat versionné, budget réel et références stables ; ne pas confondre passages fournis et citations prouvées |
| `SessionCoordinator.kt` | Ordonnancement CORE | Conserver l'exclusion pendant drainage, événements observables et requestId futur ; éviter les dépendances à une Activity |
| `LocalDatabase.kt`, `RagEngine.kt`, `PdfImporter.kt` | Persistance et recherche | Interfaces de transactions et recherche d'abord ; les données restent dans le stockage existant jusqu'à migration validée |
| `WavRecorder.kt`, `WavFiles.kt`, `WhisperLocalEngine.kt`, `LocalTts.kt` | Capacités vocales | Définir propriété des fichiers, arrêt confirmé, erreurs, disponibilité ; garder les implémentations locales opérationnelles |

`LocalLanguageModel` et `SessionActions` sont ici des points d'injection locaux
pour tester le comportement historique. Ils ne constituent pas le protocole
intermodules futur : pas de versions, transport, découverte ou transfert de données.

## Conditions avant un futur raccordement

1. Revue ADR et contrats versionnés dans une mission explicitement autorisée.
2. Matrice de capacités et sémantique d'annulation réelle, notamment moteurs
   sans interruption JNI ; modèle de concurrence et erreurs compatible.
3. Tests de contrat via doubles locaux et tests Android des implémentations.
4. Stratégie de sauvegarde/restauration démontrée avant toute évolution du stockage.
5. Validation complète du remplacement sur appareil, puis sélection réversible
   dans la composition. Garder le fonctionnement historique accessible tant
   que le remplacement n'est pas accepté.
