# ALPHA1-003 — construction amont isolée et reproductibilité

## Chaîne réellement utilisée

| Élément | Version / contrainte |
|---|---|
| JDK hôte | Temurin 17.0.16+8 |
| Gradle amont | 8.9, distribution vérifiée |
| Gradle CORE | 8.10.2, inchangé |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 ; cible JVM 11 dans la bibliothèque |
| Android SDK | compileSdk35, build-tools34.0.0 par défaut AGP ; 35.0.0 également installé |
| NDK | 27.2.12479018, r27c |
| CMake | 3.22.1, Ninja distribué avec ce package |
| C++ | C++17, `RelWithDebInfo`, flags amont conservés |
| Bibliothèque | ARM64 uniquement, Android API24 |
| Dépendances directes | coroutines1.9.0, core-ktx1.13.1 |
| Plugin publication amont | vanniktech0.34.0, aucune tâche de publication exécutée |

`RESULTS.json/toolchain` contient versions et SHA-256 des exécutables sélectionnés,
android.jar et métadonnées de packages. Distributions vérifiées :

- JDK : `166774efcf0f722f2ee18eba0039de2d685b350ee14d7b69e6f83437dafd2af1`.
- Gradle8.9 : `d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab`.
- Command-line tools : `2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258` ;
  104 fichiers installés comparés à l'archive, checksum officiel SDK également vérifié.
- NDK/CMake installés par SDK Manager avec vérification du dépôt officiel ; versions
  et outils inspectés ensuite. Les SHA-1 officiels des archives sont enregistrés.

Le lock observé contient **181 entrées de configurations, 168 artefacts distincts**,
coordonnées et SHA-256 : `RESULTS.json/dependencyLock`. Il inclut les classpaths
release et buildscript racine/bibliothèque. Inventaires identiques sur les trois builds.
Le script init refuse un lock différent **avant toute tâche** ; test positif et
test négatif exécutés. Les corps POM, métadonnées de dépôt, OS et totalité du SDK
ne constituent pas encore une image hermétique attestée. Pas de prétention de SBOM
exhaustif de toutes les licences transitives.

## Isolation et méthode

Sources extraites hors du checkout CORE, dans deux racines distinctes. Le sous-module
est peuplé depuis l'archive exacte. Aucun fichier source Gradle/Kotlin/C++ amont
n'est modifié. SDK, JDK et cache de dépendances existants sont réutilisés ; aucune
dépendance CORE ni son lock ne change. Le cache de résultats de build est désactivé.

Chaque build part sans `library/build`, `library/.cxx` et cache de projet précédents.
Le troisième contrôle réutilise seulement le **chemin** du premier, après conservation
de l'AAR et suppression de ses seuls produits générés. Pas d'exécution JNI/GGUF.
L'init script observe les dépendances et ajoute une tâche de rapport, sans changer
les sources ou coordonnées amont.

| Build propre | Résultat | Durée | Tâches exécutées | SHA-256 AAR |
|---|---|---|---:|---|
| 1, racine A | Réussi | 2m08s | 33 | `6ae08f9932066bfee26307ce43df5945475b3d29907fd3cc33b6c6370cf12557` |
| 2, racine B | Réussi | 1m24s | 33 | `414ebf34dfb7114e07c0ee89851f303f3d725fb63f2bfc5bf7c133f1cdb14630` |
| 3, racine A, nettoyage complet | Réussi | 1m23s | 33 | `6ae08f9932066bfee26307ce43df5945475b3d29907fd3cc33b6c6370cf12557` |

## Comparaison et portée

| Composant | Deux racines | Comparaison AAR publiée |
|---|---|---|
| classes.jar | Identique | Identique |
| AndroidManifest.xml, R.txt, aar-metadata | Identiques | Identiques |
| libc++_shared.so | Identique | Identique |
| libggml-base, libggml-cpu, libggml, libllama | Différents | Différents |
| libllama_jni | Différent ; seule section build-id diffère entre nos builds | Différent |

Les quatre bibliothèques ggml/llama diffèrent dans `.rodata`, `.text`, build-id,
et parfois `.rela.dyn`. Des chaînes de chemins absolus propres à chaque racine
sont présentes dans les binaires. Une simple substitution des chemins ne rend
pas toutes les sections identiques ; l'effet exact sur le placement des chaînes
et instructions n'a pas été complètement isolé. Ne pas réduire ces différences
à des timestamps ZIP ou prétendre une équivalence fonctionnelle prouvée.

Le contrôle 1/3 est byte-identique pour l'AAR entière et chaque membre. **Reproductibilité
observée sur cet hôte à chemin fixe**, avec mêmes outils/dépendances. **Pas de
reproductibilité indépendante du chemin ni de l'hôte**, ni d'équivalence native
avec l'AAR Maven. Les sources archivées n'ont pas de métadonnées Git : CMake émet
`not a git repository`, sans faire échouer les builds. Leur provenance est vérifiée
séparément ; les métadonnées de version embarquées ne sont pas une attestation.

Autres avertissements amont conservés : métriques Android non initialisables dans
le home système ; chemin consumerProguardFiles visant un fichier absent à la racine
library, alors que le fichier existe sous src/. Aucun contournement ni correction
amont appliqué. Ces limites devront être revues avant redistribution.

Les six ELF inspectés sont AArch64, segments LOAD alignés au moins16K. Les cinq
exports JNI historiques sont présents ; aucun nouvel export mesuré. minSdk24 dans
le manifeste et `ANDROID_PLATFORM=android-24` dans CMakeCache. Ces inspections ne
prouvent pas le chargement effectif sur Android24/25 ni les performances matérielles.

## Reproduction paramétrée

Préparer un espace `WORK` hors CORE et un SDK/JDK vérifiés. Télécharger les archives
aux URLs codeload des commits de PROVENANCE.md ; vérifier leurs hashes **avant**
extraction. Dans chaque racine, placer llama.cpp sous `llama.cpp/`, puis exécuter :

```sh
export JAVA_HOME=/chemin/jdk-17.0.16+8
export ANDROID_HOME=/chemin/android-sdk
export GRADLE_USER_HOME=/chemin/cache-gradle
export ALPHA1_DEPENDENCY_LOCK="$CORE/docs/alpha1-003/RESULTS.json"
# Garder CA et proxy de l'environnement ; aucun changement de HOME.
bash "$CORE/tools/alpha1-003/baseline_build.sh" "$WORK/racine-A" "$GRADLE89/bin/gradle" "$WORK/preuves" A
bash "$CORE/tools/alpha1-003/baseline_build.sh" "$WORK/racine-B" "$GRADLE89/bin/gradle" "$WORK/preuves" B
python3 "$CORE/tools/alpha1-003/verify_phase_a.py" compare \
 "$WORK/racine-A/library/build/outputs/aar/library-release.aar" \
 "$WORK/racine-B/library/build/outputs/aar/library-release.aar" --published "$AAR_011"
```

Les répertoires de preuves doivent exister. Les références de sources, outils et
variables sont des paramètres à vérifier, pas des chemins privés à recopier aveuglément.
Le wrapper Gradle amont ne porte pas de distributionSha256Sum ; le script utilise
donc la distribution8.9 vérifiée explicitement. Aucun workflow APK/modèles lancé.

Étape future proposée : examiner un remapping reproductible des chemins et les
métadonnées Git/build dans un overlay expérimental distinct, puis refaire les builds.
Ce changement de configuration native n'a pas été appliqué ici.
