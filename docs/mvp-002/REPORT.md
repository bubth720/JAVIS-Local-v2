# JARVIS CORE — MVP-002 : interface et interaction vocale conditionnelle

Branche `feature/mvp-002-unified-interaction`.
Base MVP001 vérifiée `7368f41bbcec389e4b76c99ea27819645d94a50d`.
Commit applicatif exact dans APK-INFO.json du kit ; dernier compte rendu de
livraison par `git log -1 --format=%H -- docs/mvp-002/REPORT.md`.

## Livré et limites

| Fonction | État réellement livré | Validation disponible |
|---|---|---|
| Interface unique | Vues Android, conversation/états/refus/modèles et paramètres, panneaux compacts | Build et tests UI Robolectric24/25/35 |
| CORE conversationnel | Adaptateur vers JarvisCore et contrôleur historique, SYSTEM distinct | Tests réels CORE/SQLite avec LLM simulé ; JNI/GGUF physique non validé |
| Historique/souvenirs | SQLite existante, réouverture, confirmation humaine des souvenirs ; pas d'apprentissage auto | Tests SQLite/reprise/annulation ; mémoire/messages restent des écritures séparées |
| Avatar V1 | Palette et sous-ensemble de dessin INTERFACE adaptés en vector drawable statique | Inflation UI ; aucune animation/horloge/native, fluidité physique non mesurée |
| Lecture vocale | Android LocalTts, voix française locale installée, avion requis, test entendu attesté par l'utilisateur, lecture manuelle | Tests de refus/protocole avec doubles ; **audition réelle non validée ici** |
| Import vocal | GGML privé, provenance/licence déclarées, taille/hash/bornes/espace, modèle séparé | Petites fixtures synthétiques, pas de modèle Whisper exécuté |
| Microphone/Whisper | **Désactivé, aucune permission micro** : modèle/admission/fin native non qualifiés | Texte maintenu ; transcription réelle non exécutée |
| NEXUS/SERVICES/EVOLUTION | Descriptions indisponibles, aucun chargement ni résultat simulé | Tests de désactivation |
| Pilotes Binder | Inactifs ; pas de service intermodules ajouté | Contrôle de sources/manifeste, fixtures historiques inchangées |

TTS READY est une déclaration du moteur Android, pas une preuve acoustique. Le
mode avion ne garantit pas l'absence Wi-Fi ; l'utilisateur doit le couper et
attester l'audition réelle avant lecture des réponses. Un retour speak=true
signifie seulement demande acceptée. Aucun repli vers VOICE005K, téléchargement,
profil B, lecture automatique de l'historique ou voix réseau. Le SDK TTS utilise
son service Android standard ; cela n'active pas un pilote Binder JARVIS.

## Fichiers et justification

Les8 fichiers préexistants modifiés sont : app/build.gradle.kts (mvp2/source-sets),
LocalTts.kt (refus d'une voix non installée), les quatre fichiers partagés MVP
MvpActivity.kt (hooks/sections), MvpApplication.kt (factory de composition),
MvpRuntime.kt (port optionnel persistant et gate d'import auxiliaire),
MvpModelStore.kt (format GGML séparé, compatibilité des manifestes GGUF), puis
AGENTS.md et docs/PROJECT_KNOWLEDGE.md (compléments seulement).
Nouveaux fichiers : src/mvp2, testMvp2, outils/CI model-free et ce dossier de preuves.

MainActivity, AppGraph, JarvisCore, SessionCoordinator, WhisperLocalEngine,
WavRecorder, ModelManager, LlamaLocalEngine, schéma SQLite, limites natives,
contrats IDENTITY et fixtures Binder sont inchangés. Les primitives sont réutilisées
sans changer leur fonctionnement historique. Le port optionnel n'est pas injecté
dans MVP001 : son mode RAM reste inchangé. Aucune dépendance ajoutée/remplacée.

INTERFACE139bfdc et VOICE6a86d84 restent des références en lecture seule (hashes
précis et attribution dans REFERENCES.json). Aucun module/app Compose, ONNX,
service, modèle ou pack VOICE importé. Le nouvel APK `.mvp002` coexiste avec les
anciennes applications ; leurs données ne sont ni migrées ni transférées.

## Résultats et livraison

**423 tests réussis,0 échec/erreur/ignoré**, builds et Lint réussis avec avertissements
conservés. Cinq anciens gardes refusent leur ancien périmètre, sans modification
de leurs règles ; garde MVP002 vert. Détails : VALIDATION.md, TEST-RESULTS.json,
LEGACY-GUARDS.json et PRESERVATION.json. Les sources externes sont restées propres.

La branche publie code, tests et preuves. Le kit local contient APK debug, SHA,
certificat, manifeste/permissions et guides ; le workflow dédié sans modèles peut
produire un artefact Actions séparé. Les statuts distants ne sont pas présumés verts.
Ni données personnelles, ni fichiers SQLite réels, audio utilisateur, poids ou
clé de signature ne sont publiés dans Git.

## Prochaine action

Revue de ce périmètre, puis qualification autorisée sur Android ARM64 avec modèle
public vérifié pour la conversation et une voix Android installée testée hors réseau.
Le microphone demande encore une chaîne Recorder/Whisper coordonnée sans seconde
résidence et des preuves de load/release ; l'import seul ne débloque jamais cette
fonction. Aucun essai personnel ni activation sensible implicite. Aucune fusion main.
