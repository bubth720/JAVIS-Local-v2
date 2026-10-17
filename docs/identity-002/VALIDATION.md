# CORE IDENTITY 002 — vérifications réelles

Exécution cloud du 17 octobre 2026, Linux x86_64, Java Temurin17.0.16+8,
Gradle8.10.2, Android SDK35, AGP8.7.3/Kotlin2.0.21 existants.
Aucun modèle téléchargé/chargé, aucune APK installée, aucun appareil ni émulateur.

## Commandes reproductibles

Depuis la racine CORE :

```sh
python3 tools/setup-identity-cloud.py
python3 tools/check-identity-002.py
python3 tools/check-004c.py
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true \
  :app:testDebugUnitTest :p2-client:testDebugUnitTest --rerun-tasks \
  --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true \
  :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
  --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
git diff --check
```

`--rerun-tasks` a servi à prouver la réexécution après les corrections de la
chaîne cloud. Les flags de ressources limitent les workers sans changer les tests.
Les résultats XML sont sous `app/build/test-results/testDebugUnitTest/` et
`fixtures/p2-client/build/test-results/testDebugUnitTest/`. Le relevé versionné
`TEST_RESULTS.json` conserve les comptes par classe ; les répertoires build sont ignorés.

## Résultats

| Vérification | Résultat observé |
|---|---|
| App, tests historiques | 187 réussis |
| Nouveaux tests IdentityRuntimeTest | 39 réussis (13 scénarios × API24/25/35) |
| Fixture p2-client préservée | 36 réussis |
| Total | **262 réussis, 0 échec, 0 erreur, 0 ignoré** |
| App assembleDebug | Réussi |
| App lintDebug | Réussi, 0 erreur / 25 avertissements |
| Fixture assembleDebug / lintDebug | Réussis, 0 erreur / 0 avertissement |
| Contrat approuvé | 14 fichiers normatifs et empreinte globale exacts |
| Codec Android | Les 6 valeurs canoniques et leurs SHA-256 correspondent sur 3 API |
| Préservation | 84 fichiers préexistants comparés octet par octet à da40ed7 |
| Contrat 004C | 4 fichiers, SHA `fab6a66a6b99302884849cd1d8aef97cd761082dc20680a4b036d7bc99d5c3c9`, inchangés |

Les mentions Gradle `SKIPPED` pour les tâches de configuration Kotlin ou
préparation AndroidTest ne sont pas des tests ignorés : les compteurs ci-dessus
proviennent des XML JUnit. Aucune assertion, test ou protection préexistante retirée.

## APK inspectés, sans installation

APK app : `app/build/outputs/apk/debug/app-debug.apk`.
`aapt dump badging/permissions` : minSdk24, target35, ARM64 seulement,
RECORD_AUDIO et permission interne AndroidX ; INTERNET/ACCESS_NETWORK_STATE absents.
7 bibliothèques ARM64 présentes, asset technique IDENTITY présent.
`apksigner verify --verbose` : vérification réussie, signature debug v2.
Celle-ci n'est pas une signature de laboratoire ni une identité inter-APK approuvée.
GGUF et Whisper absents : cet APK ne prouve pas un assistant autonome fonctionnel.

Fixture : `fixtures/p2-client/build/outputs/apk/debug/p2-client-debug-unsigned.apk`.
MinSdk29, target35, permission BIND_P2 existante, permissions réseau absentes.
Non signée, conformément à 004C et à l'absence de clé laboratoire provisionnée.
Aucune clé, aucun secret, cache, fichier local.properties ou APK ajouté au commit.

## Scénarios et portée

- Canonicalisation exacte, domaine SHA-256, UTF-8 invalide, doublons, fractions,
  exponentiels, dépassements, profondeur, Unicode et syntaxe JSON malformée.
- Versions/empreintes erronées, faux droits dans le manifeste, styles fermés,
  immutabilité et même identité technique après recréation du lecteur.
- Snapshot technique vide, session et durée de validité ; refus hors périmètre personnel.
- Défaut désactivé, absence de tokenizer, capacité SYSTEM absente.
- Composition simulée : seul SYSTEM change, provenance conservée, traits B présents.
- Budget de test à frontière exacte, contexte demandé/chargé/admis, mauvaise mesure,
  révocation/expiration pendant mesure, quarantaine et dépassement SYSTEM.

Les doubles de comptage sont explicitement simulés : aucune valeur fabriquée n'est
branchée au runtime. Les tests de traits B vérifient le texte du compositeur, pas
la réponse d'un LLM. Les tests historiques incluent SQLite et migrations avec
bases synthétiques, lifecycle, concurrence, admission, annulation et audio simulé.
Robolectric API24/25/35 n'est ni un appareil Android physique ni une preuve JNI Android.

## Difficultés rencontrées et corrections

Le premier environnement exposait un JRE21 sans compilateur. Installation vérifiée
de Java17 et outils épinglés dans `/workspace/.jarvis-core-tools`, hors Git.
Les premiers workers Robolectric ne reprenaient pas le proxy Java (échecs réseau,
notamment 23 tests de fixture). Le helper transmet désormais proxy et CA de session
aux JVM filles, sans désactiver TLS. Une dépendance AndroidX TestCore absente a causé
un échec de compilation du nouveau test : utilisation de RuntimeEnvironment déjà
fourni par Robolectric, sans nouvelle dépendance ni retrait de test.
Un test lexical supplémentaire a révélé que JsonReader accepte des caractères
LF/TAB non échappés dans une chaîne (3 échecs, un par API). Le codec vérifie
désormais cette règle JSON avant le parseur Android ; le contrôle conserve
les caractères correctement échappés des vecteurs approuvés.
Les suites complètes ont ensuite été réexécutées avec succès.

Les scripts d'installation ont été exécutés puis réexécutés avec cache vérifié.
Les instructions `install_script`/`start_skill` de l'environnement ont été
sauvegardées en brouillon ; cela ne publie pas le snapshot et ne prouve pas sa
restauration dans une nouvelle tâche.

## Blocage restant

`AndroidLlamaBackend` (LlamaLocalEngine.kt) expose load/complete/release seulement.
`InferenceContract.kt` et PromptBuilder utilisent une borne UTF-8, pas le nombre
de tokens du rendu final. Aucun raccordement sûr du compositeur n'est livré.
`UnavailableIdentityTokenizer` refuse explicitement. Le futur port devra mesurer
et générer sous la même réservation native, puis revalider session/annulation
avant toute persistance ou sortie vocale. Aucun contrat UNIFIED ou fixture /2
n'a été modifié pour contourner ce blocage.
