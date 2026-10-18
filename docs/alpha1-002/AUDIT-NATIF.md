# ALPHA1-002 — audit natif et provenance

18 octobre 2026. **Proposition de revue, non appliquée au runtime.**
Base CORE locale et distante revérifiée :
[b2ec98238f70e89edc636c1c671f2ddf1b957d74](https://github.com/bubth720/JAVIS-Local-v2/commit/b2ec98238f70e89edc636c1c671f2ddf1b957d74),
branche `feature/alpha1-001-text-lab`. Nouvelle branche documentaire :
`design/alpha1-002-jni-review`. Arbre initial propre. Les sept documents imposés
ID-002T/ALPHA1-001 ont été relus, ainsi que les appelants actuels et les instructions.
L'ancien patch de signatures incomplet reste intact. Le nouveau contient les corps
proposés et appels CORE, sans les activer.

## Artefact inspecté à nouveau

Coordonnées : `dev.ffmpegkit-maintained:llama-android:0.1.1`.

| Élément | SHA-256 |
|---|---|
| AAR | `fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501` |
| classes.jar | `bce22368c0d8b22a2f2241fdf24305f9f03dcfffabddff171edf4078a14714ef` |
| libllama_jni.so ARM64 | `4d139d2d95569d8d9336c9687f70eb4b5b8d2daf23ca4cfa7ce5a75721c8de20` |
| libllama.so ARM64 | `3d5997d85444f46b6659ab1db9967212ac7221ecbbe7323bbfb64fdcdcff7993` |

Sept composants JAR/ELF épinglés dans SOURCE_LOCK.json. Le checksum
[publié sur Maven](https://repo.maven.apache.org/maven2/dev/ffmpegkit-maintained/llama-android/0.1.1/llama-android-0.1.1.aar.sha256)
a répondu HTTP200 et correspond. Ce checksum sur le même canal vérifie les octets
publiés ; il n'est pas une attestation indépendante de compilation. `javap`/`nm`
retrouvent cinq entrées : LoadModel, Complete, Embed, ReleaseModel, GetSystemInfo.
Aucun prepare/consume/discard ni getter promptTokens dans LlamaResult. JNI non chargé.

## Sources immuables et limites

- [llama-android823d1de2](https://github.com/ffmpegkit-maintained/llama-android/tree/823d1de2b3ef85ba9ef4847a03bac423f4aa1c44).
- Objet du tag annoté v0.1.1 : `574b5731b4e0605f2b31f4fb6713f18f0ccf25b8` ;
  commit résolu `823d1de2b3ef85ba9ef4847a03bac423f4aa1c44` ;
  tree `f5c2026376c07dbac2609b865491207d70547585`.
- Gitlink llama.cpp lu dans ce tree :
  [2da668617612d2df773f966e3b0ee22dc2beef7b](https://github.com/ggml-org/llama.cpp/tree/2da668617612d2df773f966e3b0ee22dc2beef7b).
- Les quatre empreintes SOURCE_REFERENCES.json ID-002T correspondent ; 31 fichiers
  de l'archive amont sont épinglés dans SOURCE_LOCK.json.
- `git verify-tag v0.1.1` : exit1, `error: no signature found`. Tag annoté ne signifie
  pas tag signé. Aucun contournement de vérification.

Le source confirme template system/user avec préfixe assistant, fallback ancien
par concaténation, tokenize(add_special=true,parse_special=true), clear KV/decode
dans complete, UTF-8 modifié JNI et Dispatchers.Default. Un symbole llama_tokenize
exporté ne fournit pas une API sûre sur LlamaCtx. Aucun dlsym/JNA/réflexion proposé.

**Correspondance reproductible source→AAR publiée : non démontrée.** Ni hash,
ni tag, ni lecture du source ne suffisent. Aucune reconstruction de l'AAR ou
attestation de construction vérifiée. Les workflows amont emploient ubuntu-latest,
actions par tags, JDK17 sans build précis ; wrapper8.9 sans distributionSha256Sum,
absence de verrou complet des dépendances. Pas de reproductibilité bit à bit acquise.

## Propriétaire technique proposé — soumis à décision

Le mainteneur CORE serait responsable d'une copie source contrôlée de cette
bibliothèque et du sous-module épinglés, par exemple sous
`native/vendor/llama-android-experimental/` lors de la future mission autorisée.
Elle serait construite séparément puis consommée
comme un seul AAR expérimental. Aucun fork distant, dépôt ou sous-module créé ici.
Aucun code du dépôt JARVIS-LLM importé.

Coordonnées proposées : `fr.jarvis.experimental:llama-android-exact:0.1.1-jarvis-alpha1.002-review.1`.
Nom/version non publiés. Namespace JVM et noms ELF restent ceux d'amont pour
préserver les anciens appels : les deux AAR ne peuvent jamais être embarqués
ensemble. Le patch CORE exige un fichier AAR explicitement fourni et son hash
revu, sans fallback Maven. Aujourd'hui, la dépendance de production reste0.1.1.

API Kotlin historique conservée en source et constructeur LlamaModel(Long,Config)
maintenu dans la proposition. Compatibilité binaire/Android réelle non qualifiée.
Worker série, registre natif et refus de second chargement changent le comportement
interne : changements sensibles à approuver avant application.

## Construction future reproductible à qualifier — non exécutée

1. Après autorisation, matérialiser les arbres Git exacts et le patch approuvé dans
   un répertoire contrôlé ; conserver licences MIT et notices.
2. Épingler image OS par digest, JDK17 par build/checksum, Gradle8.9 et checksum
   officiel, AGP8.7.3, Kotlin2.0.21, NDK27.2.12479018, CMake3.22.1,
   SDK35, build-tools et dépendances résolues/verrouillées/vérifiées. Ne pas inventer
   leurs empreintes manquantes ; compléter le lock avant construction.
3. `:library:assembleRelease`, ARM64 seulement, minSdk24, C++17 ; CPU/NEON,
   OpenMP/Vulkan/curl désactivés comme amont. Aucun modèle incorporé/téléchargé.
4. Deux constructions dans deux répertoires propres avec mêmes entrées ; contrôler
   AAR et chaque JAR/ELF. Expliquer toute différence de chemins/timestamps/build-id,
   sans maquiller les fichiers. Vérifier NEEDED, SONAME, symboles, alignement16Ko et
   APK final. Équivalence fonctionnelle ne signifie pas égalité bit à bit.
5. Produire SBOM, manifest source/gitlink/patch/toolchain, hashes de composants,
   tests et signature/attestation avec clé hors Git. Consommer ensuite le seul hash
   revu ; aucune publication Maven Central automatique.

NDK/CMake Android absents de l'environnement courant. Les contrôles hôte utilisent
g++14.2 et les vrais en-têtes épinglés, avec une déclaration stub android/log.h :
pas de lien Android ou bibliothèque installable. **Gate binaire toujours fermé.**
