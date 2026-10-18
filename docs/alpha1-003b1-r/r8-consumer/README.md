# Consommateur R8 préparé — non construit

À matérialiser **hors CORE**, après validation explicite du nouveau diff et de sa
construction expérimentale. Retirer seulement le suffixe `.fixture` et placer :

- settings.gradle.kts et build.gradle.kts à la racine du consommateur ;
- AndroidManifest.xml sous src/main/ ;
- CompileOnlyApplication.java sous src/main/java/fr/jarvis/experimental/r8consumer/.

Chaîne : JDK17.0.16+8, Gradle8.9, AGP8.7.3, SDK35, minSdk24/ARM64. Ne pas ajouter
une règle JNI au consommateur pour faire passer le test. L'AAR seule doit les fournir.

Après construction **autorisée** de la candidate corrigée :

```sh
python3 docs/alpha1-003b1-r/check_future_aar.py "$CORRECTED_AAR"
# Depuis le consommateur isolé, jamais depuis CORE :
"$GRADLE89" assembleRelease -PcandidateAar="$CORRECTED_AAR" --offline --no-daemon
```

Le cache doit fournir les dépendances épinglées ; ne pas substituer une version.
Capturer le rapport de dépendances/R8, mapping.txt, configuration.txt et SHA APK.
Vérifier les règles consommateur dans la configuration fusionnée, les8 méthodes
native* et leur classe `dev.ffmpegkit.llama.LlamaJNI` inchangées dans le DEX via
`apkanalyzer dex code --class dev.ffmpegkit.llama.LlamaJNI APK`. Comparer les
signatures JVM/JNI à inspection.json de B1 ; vérifier les API historiques retenues.
Vérifier manifeste sans permission réseau, ABI/API et absence de GGUF dans l'APK.
Ne pas signer avec une clé personnelle, installer ni exécuter cet APK.

Contrôles négatifs à construire ultérieurement dans un autre laboratoire : AAR B1
sans règles, puis archive synthétique dont on retire seulement proguard.txt. Le
vérificateur d'archive doit les refuser. Le DEX minifié ne doit pas être accepté
s'il perd/renomme une méthode JNI ; ne pas supposer que la seule réussite R8 suffit.
L'éventuelle conservation due aux règles par défaut doit être explicitée : le
contrôle du packaging reste indépendant du résultat de rétention DEX.

Ce test est une qualification de compilation/minification, pas un essai JNI
Android. Aucun task Gradle de cette fixture n'a été lancé dans B1-R.
