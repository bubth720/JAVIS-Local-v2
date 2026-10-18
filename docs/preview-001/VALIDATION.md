# PREVIEW-001 — validation cloud

Base exacte b2ec98238f70e89edc636c1c671f2ddf1b957d74 ; branche dédiée.
Ce rapport décrit le cloud de préparation. Le kit GitHub Actions fournit en plus
APK-INFO.json avec ses résultats propres et le certificat de **son** APK.

## Commandes exécutées

```sh
bash tools/identity-cloud-env.sh gradle :app:testPreviewUnitTest \
 --tests '*PreviewActivityTest*' :app:assemblePreview :app:lintPreview \
 --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest :app:assembleDebug :app:lintDebug \
 :p2-client:assembleDebug :p2-client:lintDebug --no-daemon --max-workers=4 \
 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/check-preview-001.py
```

| Suite | Réussis | Échecs / erreurs / ignorés | Portée |
|---|---:|---|---|
| CORE historique |263|0 /0 /0|JVM et Robolectric, suites existantes inchangées|
| Fixture /2 |36|0 /0 /0|JVM/Robolectric, pas raccordement natif|
| Preview dédiée |12|0 /0 /0|4 scénarios API24/25/35, graph réel sans modèle|
| Total |**311**|**0 /0 /0**|Aucun modèle ou appareil physique|

Seule la classe PreviewActivityTest est sélectionnée pour la variante Preview ;
les suites historiques complètes sont exécutées sur la variante debug normale.
Ce ne sont pas311 tests d'inférence native. Les douze cas Preview couvrent accueil
sans modèle, séparation package/permissions, confirmation, erreur/reprise/sortie,
absence d'écriture SQLite/préférences et redirection d'une entrée non autorisée.

Les trois premiers échecs d'opt-in provenaient de la lecture d'un clic de dialogue
avant traitement de son Handler dans la boucle Robolectric PAUSED. L'attente du
thread principal est explicite dans le test corrigé ; aucune assertion supprimée.

Builds debug/fixture/Preview et Lint réussis.27 avertissements app debug,
28 Preview,0 fixture ;0 erreur Lint. Logs locaux hors Git :
`/workspace/.jarvis-core-tools/preview-tests.log` et `preview-regression.log`.

## Contrôles APK

`tools/preview_kit.py` vérifie apksigner, aapt badging/permissions/manifest,
ABI de chaque ELF, hashes JNI Llama inchangés et absence d'assets de poids.
Il exige des suites XML non vides sans échec/erreur/ignoré et Lint sans erreur.
Il produit les empreintes réelles du kit et du certificat public. Variante Preview
séparée `.preview001`, launcher PreviewActivity, minSdk24/target35, ARM64 seulement.
Aucune permission Internet, ACCESS_NETWORK_STATE ou RECORD_AUDIO dans la Preview.
Signature debug valide, aucune garantie de clé stable entre fabrications Actions.

La copie locale de l'APK et celle d'Actions peuvent avoir des certificats et des
hashes différents. Pour un essai utilisateur, sélectionner exclusivement le kit
Actions autorisé et ses preuves, sans mélanger les métadonnées d'une autre build.

## Contrôles de préservation et limites

Le nouveau garde compare151 fichiers inchangés à la base exacte : JNI, moteur,
admission, budgets, ModelManager, SQLite, préférences, contrats IDENTITY et fixtures.
Seuls MainActivity et build.gradle.kts sont modifiés dans les fichiers applicatifs
existants ; nouvelles sources Preview et booléen false dans le défaut.
Anciens gardes de missions aux périmètres absolus incompatibles avec ces ajouts,
non réécrits pour masquer leur échec (trois refus consignés dans LEGACY-GUARDS.json). Le contrôle de cette mission reste distinct.

L'environnement n'a ni /dev/kvm, ni émulateur installé, ni system image/AVD.
**Tests émulateur non exécutés**. Les images x86 ne qualifieraient pas une AAR
ARM64 sans traduction attestée. Aucun appareil personnel ni APK installé.
Aucun GGUF chargé, aucune génération JNI réelle, aucune qualification thermique,
batterie/RAM ou mode avion avec inférence. Les alignements ELF sont consignés
sans promettre une compatibilité physique sur toutes tailles de pages Android.
Les sept ELF locaux sont ARM64 et leurs segments LOAD ont un alignement0x4000
(16Ko), contrôle statique seulement.
La validation API24/25 actuelle est Robolectric et build, pas appareil réel.

L'API api.github.com a refusé le tunnel proxy avec403. Aucune extraction de jeton,
contournement ou route réseau alternative. Le workflow est déclenché par push ;
son état/artefact doit être vérifié sur l'interface GitHub disponible. Aucun lien
d'artefact ne doit être annoncé comme créé avant observation de sa publication.

## Vérifications physiques à autoriser

La grille TESTS-POCO.md sépare l'essai d'accueil possible et les tests GGUF bloqués.
L'autorisation demandée concerne seulement une installation manuelle de cette APK
sans poids, sans migration, remplacement d'application ou modification des modèles.

## Premier passage Actions et correction

Exécution39123128198, commit d5381b095d37da80aa084a46d45e8b70388e10e3 :
étape « Android build tools only » en échec, annotation exit127 ; tests et upload
non exécutés. La commande sdkmanager n'était pas préparée explicitement dans le
workflow. Ajout de android-actions/setup-android@v3 pour installer/configurer les
outils et leur PATH avant leur utilisation. Cette correction ne touche pas l'APK.
Le statut de la nouvelle exécution et le lien de l'artefact sont donnés dans le
compte rendu final ; ne pas télécharger un kit supposé issu de la première.

Deuxième exécution39123203246 : l'action setup-android a échoué avant tests/upload ;
seuls son statut et les annotations publiques sont accessibles (journaux détaillés
non accessibles anonymement, endpoint de logs404). Pas de cause supplémentaire
inventée. Remplacement par tools/setup-preview-android.py : archive officielle
11076708 avec SHA-256 épinglé, SDK dans RUNNER_TEMP, Java17 existant, export des
chemins pour Gradle. Le script a été exécuté ici sur le SDK vérifié ; il conserve
proxy et TLS et installe seulement SDK35/build-tools35, aucun modèle/émulateur/APK.
Les deux premières exécutions n'ont publié aucun kit.


Troisième exécution39123386767, source d0e2808 : **réussie**. Préparation SDK,
préservation, tests historiques/fixtures et Preview, build/Lint, contrôles de
signature/permissions et upload : tous les steps réussis. PUBLICATION.json et
RAPPORT.md donnent l'artefact observé. Les comptes XML distants sont embarqués dans
APK-INFO.json ;311 cas ont été vérifiés localement. Aucune nouvelle génération
GGUF ou installation physique ne découle de cette réussite CI.
