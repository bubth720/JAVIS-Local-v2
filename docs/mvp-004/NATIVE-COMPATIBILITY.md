# Audit du candidat Qwen et du moteur historique

## Faits vérifiés et niveaux de preuve

Base CORE : `90215258ad3914cd6576971334ecb8ca48db74ff`.
Aucune modification de MvpModelStore, MvpRuntime, MvpSafety, LlamaLocalEngine,
JNI, signature, admission, paramètres, base SQLite ou fixture Binder.

L'AAR réellement inspectée est `dev.ffmpegkit-maintained:llama-android:0.1.1`,
SHA-256 `fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501`.
Le binaire ARM64 libllama.so contient les marqueurs qwen3/qwen3moe et les
symboles tokenize/chat_apply_template/model_load. Ce constat est statique :
il ne démontre aucun chargement du GGUF candidat.

Sources amont de référence relues et comparées aux fichiers distants épinglés :
llama-android `823d1de2b3ef85ba9ef4847a03bac423f4aa1c44`, llama.cpp
`2da668617612d2df773f966e3b0ee22dc2beef7b`. Empreintes dans
UPSTREAM-SOURCE-CHECKS.json. Ce contrôle authentifie les fichiers source
analysés, sans revendiquer une reconstruction bit à bit de l'AAR publiée.

## Modèle : fournisseur vérifié, octets non obtenus

Dépôt Qwen/Qwen3-0.6B-GGUF, révision
`1208e45d782fe18602c5eaf10e5758d5b0f24c03`.
Le fournisseur expose le fichier `Qwen3-0.6B-Q4_K_M.gguf`, taille exacte
396704416, objet LFS SHA-256
`b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e`.
**Ce SHA annoncé n'est pas un calcul indépendant sur le GGUF.** Aucun poids
n'a été téléchargé, lu ou chargé sans autorisation.

Le fichier LICENSE de cette même révision a été lu : texte Apache2.0,
copyright2025 Alibaba Cloud, SHA dans MODEL-PROVENANCE.json. Copie exacte
QWEN-LICENSE.txt. La carte déclare apache-2.0, mais son lien de licence et son
base_model désignent Qwen3-1.7B : anomalie documentaire à conserver, sans
réattribuer ce fichier de0.6B à un modèle1.7B. Les champs GGUF du fournisseur
annoncent architecture qwen3,596049920 paramètres, contexte40960 ; ils ne
constituent pas une lecture indépendante des métadonnées du fichier sélectionné.
Le champ global totalFileSize639446688 ne remplace pas la taille LFS396704416.

Le fichier annoncé tient dans les bornes historiques d'import (>100Mio,
<=512Mio). Seuil RAM inchangé pour cette taille :1458548704 octets disponibles
minimum, ni lowMemory ni lowRam. Espace libre avant copie minimum665139872
octets (taille+réserve256Mio). Ces calculs ne sont pas des mesures Android,
ni une garantie que le modèle/context tiendra réellement en mémoire.

## GGUF et quantification

`tools/mvp4_model_probe.py` est un lecteur hors ligne, sans téléchargement,
chargement, allocation de modèle ou appel JNI. Après autorisation et présence
locale du fichier, il exige taille et SHA exacts, stabilité du fichier, puis
inspecte GGUF2/3 little-endian, clés, tableaux tokenizer, template, descripteurs
et types de tenseurs. Le candidat doit déclarer architecture qwen3 et
file_type15 (Q4_K_M), avec tenseurs Q4_K. Une quantification mixte est normale :
Q4_K_M n'implique pas que tous les tenseurs soient Q4_K.

Le lecteur borne ses métadonnées à64Mio et n'exécute aucun template. Les tests
utilisent de minuscules données synthétiques, jamais un modèle simulant une
inférence. Il ne valide pas le contenu mathématique de tous les tenseurs et
ne remplace pas le loader historique. Il ne change aucune règle d'acceptation
des imports Android. Les empreintes des tableaux sérialisés ne sont ni une
exécution du tokenizer ni des IDs effectivement décodés.

## SYSTEM, template et tokenisation : limites importantes

`LlamaLocalEngine.AndroidLlamaBackend` conserve SYSTEM séparé du prompt transmis
à Llama.complete. Dans les sources natives de référence :

- build_prompt récupère llama_model_chat_template, fournit les rôles system et
  user à llama_chat_apply_template ; chaque appel est un tour système/utilisateur.
- Le template publié contient `<|im_start|>` ; le classifieur de référence le
  reconnaît comme ChatML et formate ces rôles. Il ne lance pas un interpréteur
  Jinja général. Les branches tools/enable_thinking du template ne sont donc
  pas prouvées comme exécutées par cette API historique.
- Si template absent ou application négative, le bridge concatène SYSTEM et
  user. Risque historique confirmé par lecture : ce fallback ne doit pas être
  assimilé à une conformité P2. Aucune correction JNI appliquée ; rendre un
  éventuel refus natif explicite nécessite une revue/approbation distincte.
- llama_tokenize reçoit add_special=true et parse_special=true. Les délimiteurs
  de rôle dans du texte utilisateur doivent être qualifiés ; aucun test privé
  ou d'injection exécuté sur un modèle réel ici.
- GetStringUTFChars/NewStringUTF emploient le Modified UTF-8 JNI, alors que le
  vocabulaire attend du texte UTF-8. Les caractères hors BMP/NUL présentent un
  risque à vérifier ; aucun défaut de sortie Android n'est revendiqué comme
  reproduit. Le lecteur Python ne teste pas ce passage JNI.
- La KV cache est effacée à chaque complete ; le contexte conversationnel
  provient du prompt CORE construit par PromptBuilder, pas d'une mémoire KV
  persistante implicite. Le budget reste conservateur, sans compteur exact.
- Le nombre de tokens est connu après calcul dans le JSON natif mais n'est
  pas exposé par LlamaResult comme mesure préflight. Ne pas appeler complete(0)
  ou embed pour fabriquer une mesure non destructive.

Qwen3 peut produire du raisonnement avant sa réponse et atteindre128 tokens
sans réponse finale. `/no_think` peut être essayé explicitement dans une
question synthétique, sans promesse de support ni changement silencieux du
SYSTEM. Ne pas augmenter contexte2048/sortie128 pour contourner une limite.

## Arrêt et libération

requestStop retourne UNSUPPORTED. Annuler une coroutine/timeout ne termine pas
JNI ; MvpRuntime garde busy et InferenceController draine le vrai retour.
Release reste un appel JNI void ; le code de référence libère contexte puis
modèle et handle. Seul son retour observé, sans erreur, permet la confirmation
CORE. Une baisse PSS seule, l'état UI ou la mort du client ne sont pas une preuve.
Échec/inconnue conserve la quarantaine et l'interdiction d'un second moteur.
Aucune attestation native n'a été obtenue dans cette session.

**Décision actuelle : NON QUALIFIÉ / NO-GO pour conversation personnelle.**
Aucun défaut de production reproduit avec modèle réel ; aucune correction
applicative ou sensible appliquée.
