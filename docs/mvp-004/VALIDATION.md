# Rejouer les contrôles sans simulation d'inférence

Prérequis logiciels existants : JDK17, Gradle8.10.2, AGP8.7.3, Kotlin2.0.21,
SDK/build-tools35, Python3.10+ sans dépendance supplémentaire. Les scripts de
préparation Android existants n'installent aucun modèle. Aucune nouvelle
configuration de chaîne, dépendance ou bibliothèque native n'est introduite.

```sh
python3 -B -m unittest discover -s tools/tests -p 'test_mvp4_model_probe.py' -v
python3 tools/check-mvp-004.py
gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
 :app:testPreviewUnitTest --tests '*PreviewActivityTest*' :app:assemblePreview :app:lintPreview \
 :app:testMvpUnitTest --tests '*Mvp*' :app:assembleMvp :app:lintMvp \
 :app:testMvp2UnitTest --tests '*Mvp2*' :app:assembleMvp2 :app:lintMvp2 \
 :app:testMvp3UnitTest --tests '*Mvp3*' :app:assembleMvp3 :app:lintMvp3 \
 --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/test-mvp3-apk.py --sdk "$ANDROID_HOME" --report /chemin/neuf/apk-controls.json
```

Les439 tests Android simulés ne chargent ni GGUF ni llama JNI. La fixture Binder
est un contrôle de non-régression simulé, sans liaison effective avec LLM/VOICE.
Les13 cas Python inspectent des en-têtes minuscules, sans production de texte.
Les3 contrôles APK utilisent zipalign/apksigner/aapt/readelf sur Linux ; leur
succès ne valide ni l'API24 native, ni l'absence de réseau d'un service Android.
Les avertissements Lint et absences natives figurent dans REPORT.md.

## Futur fichier local, uniquement après autorisation

Aucun outil livré ne télécharge de modèle. Après autorisation et obtention du
GGUF public dans un dossier hors dépôt, la commande indépendante sera :

```sh
python3 tools/mvp4_model_probe.py \
 --model /laboratoire/public/Qwen3-0.6B-Q4_K_M.gguf \
 --report /laboratoire/preuves/qwen-gguf.json
```

Le chemin est un exemple, non exécuté. Le lecteur exige les396704416 octets et
le SHA attendu complet ; puis GGUF2/3,qwen3,file_type15 et Q4_K. Il contrôle
l'identité/stabilité du fichier et borne sa lecture de métadonnées. Les tableaux
tokens/merges restent des données, pas un moteur : aucune tokenisation, aucun
rendu Jinja/ChatML ou mesure native. Même une réussite LOCAL_BYTES_VERIFIED reste
**NOT_NATIVE_QUALIFIED**. La licence fait l'objet d'une vérification séparée.

## Vérifications impossibles dans cette session

Pas de GGUF autorisé/téléchargé ; hash indépendant, quantification et template du
fichier réel non inspectés. Pas de runtime Android ARM64 : pas de chargement,
conversation réelle, mesures, stop/release JNI attestés ni test réseau runtime.
Pas de preuve de deux réponses, aucune validation personnelle. Gabarit EVIDENCE
vide et protocole documentaire ne sont pas des résultats réussis.
