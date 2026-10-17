# ID-002T — résultats réellement obtenus

## Commandes et sorties

Depuis CORE, avec les outils déjà préparés en mission ID-002 :

```sh
bash tools/identity-cloud-env.sh gradle -PincludeP2Fixture=true \
  :app:testDebugUnitTest :p2-client:testDebugUnitTest \
  :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
  --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
```

Sortie réelle : `BUILD SUCCESSFUL in 1m 29s` ; `107 actionable tasks: 107 executed`.
Java17, Gradle8.10.2, SDK35, mêmes dépendances. Aucune installation d'APK.

| Suite | Tests | Échecs | Erreurs | Ignorés |
|---|---:|---:|---:|---:|
| App (187 historiques +39 ID-002 +6 nouveaux cas) |232|0|0|0|
| Fixture /2 préservée |36|0|0|0|
| Total |268|0|0|0|

TEST_RESULTS.json conserve les résultats par classe et les gates non exécutés.
Deux méthodes nouvelles × API24/25/35 =6 cas. Les six vecteurs normatifs sont
réexécutés dans les39 cas ID-002 existants, sans changer leurs attendus.
Aucun test existant modifié, retiré ou désactivé. Les tâches Gradle de configuration
marquées SKIPPED ne sont pas des tests JUnit ignorés.

- App assembleDebug et lintDebug : réussis,0 erreur/25 avertissements Lint.
- Fixture assembleDebug et lintDebug : réussis,0 erreur/0 avertissement Lint.
- Signature APK historique vérifiée avec apksigner : v2, clé debug locale non
  commitée, aucune identité de laboratoire revendiquée.
- aapt : minSdk24, target35, ARM64, permissions réseau absentes, aucune permission
  ajoutée. Pas de GGUF/Whisper embarqués : assistant autonome non qualifié.
- Aucun essai LLM réel, tokenisation native, endpoint Binder connecté ou appareil
  physique ; les tests historiques utilisent leurs doubles natifs documentés.

## Inspection statique reproductible de l'AAR

```sh
bash tools/identity-cloud-env.sh python3 tools/audit-identity-tokenizer.py --aar CHEMIN_DU_CACHE/llama-android-0.1.1.aar
python3 tools/check-identity-002.py
git diff --check
```

Le chemin est celui de l'AAR résolu par Gradle, pas un téléchargement automatique.
Le script d'audit n'exécute ni JNI ni modèle ; utilise javap et nm. Il refuse un
AAR d'empreinte différente afin de ne pas inventer de capacité. Résultat observé :
5 entrées JNI, primitives C présentes mais mesure JNI préalable absente,
promptTokens non exposé dans LlamaResult, port de production bloqué.
Il compare aussi **93 fichiers** existants à 8adec3e, dont code runtime, anciens
tests, fixtures, dépendances, paramètres Gradle et anciens validateurs : identiques.
Le validateur ID-002 confirme le bundle normatif exact,6 vecteurs et84 fichiers
historiques inchangés par rapport à da40ed7. Exécution répétée de l'audit identique.

## Échec réel du vérificateur historique 004C

Commande exécutée : `python3 tools/check-004c.py`, sortie1 :

```text
AssertionError: historical CORE changed
```

Ce script vérifie d'abord les4 fichiers canoniques de la fixture (succès), puis
exige un diff ENTIER de app/ vide contre29646448. Le commit de départ8adec3e
contient déjà7 ajouts IDENTITY dans app/ ; sa condition échoue donc également
avant tout changement ID-002T. Vérifié par :

```sh
git diff --name-status 29646448 8adec3e -- app build.gradle.kts .github
```

Sortie : uniquement les ajouts manifeste B,4 sources identity, test ID-002 et
vecteurs. Aucun fichier applicatif ancien modifié. Le fichier du vérificateur
est identique à celui de la base et n'a pas été modifié pour obtenir un succès.
L'audit ID-002T compare les fichiers de la base demandée octet par octet ; ce
contrôle indépendant de préservation réussit, sans requalifier l'échec004C en succès.
Une maintenance distincte pourrait faire évoluer la règle004C pour autoriser les
seuls ajouts IDENTITY épinglés tout en refusant toute régression historique.

## Traces dans cet environnement

- Log complet : `/workspace/.jarvis-core-tools/identity-002t/validation.log`.
- XML : `app/build/test-results/testDebugUnitTest/TEST-*.xml` et
  `fixtures/p2-client/build/test-results/testDebugUnitTest/TEST-*.xml`.
- Rapports Lint : `app/build/reports/lint-results-debug.xml` et équivalent fixture.
- Inspection binaire/source : `/workspace/.jarvis-core-tools/identity-002t/`.

Ces chemins sont locaux ; les JSON de résultats et d'empreintes sont versionnés
pour consultation GitHub. Les caches, AAR, APK, clés et journaux bruts ne sont pas
commités. Aucun secret inspecté ou publié. La configuration cloud existante est
réutilisée ; aucun nouveau réglage réseau, secret, permission ou service requis.
