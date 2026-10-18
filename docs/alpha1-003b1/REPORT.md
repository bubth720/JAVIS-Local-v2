# JARVIS CORE — ALPHA1-003B1

Phase B1 réalisée le18 octobre2026 sur
`feature/alpha1-003b1-isolated-native-build`, base exacte
`7f591f0e041159bb773011d69fdd2b1fe94b5b88`.
Commit de livraison : `git log -1 --format=%H -- docs/alpha1-003b1/REPORT.md`.

Le patch approuvé de sept fichiers a été appliqué uniquement dans deux copies
isolées des sources épinglées. Trois compilations propres de la candidate ont
réussi. L'AAR expérimentale est construite mais aucune tokenisation native réelle
n'est démontrée. La livraison Git contient uniquement ce dossier documentaire,
les preuves textuelles et les scripts/fixtures de contrôle ; aucun binaire natif.

## Résultats vérifiés dans cette mission

- SHA patch exact : `329fc1959a2ac990f84e7b63f61239b75dd63d13b478b1f974f37b9c4a3ef97b`.
- SHA AAR retenue A/C : `7e4e667d4a2f597fb7a78e7f29eb8b6298696812f68eee5e0549eec283ce02b0`.
- 3 builds release propres réussis ;181 dépendances verrouillées identiques.
- 13 tests JVM réels sur classes candidates avec Looper simulé,0 échec/ignoré.
- 8 symboles JNI présents ; signatures historiques Llama9/LlamaModel6/LlamaJNI6 conservées.
- 6 ELF ARM64 ; segments LOAD alignés16K, pile non exécutable et GNU_RELRO.
- Manifest minSdk24 sans permission ; note AndroidAPI24 sur les5 ELF compilés,
  API21 sur libc++ provenant du NDK. Dépendances ELF toutes livrées ou système Android.
- A/C identiques à chemin/hôte fixes ; A/B différents à chemins distincts.
- 200 fichiers CORE de base inchangés,3058 fichiers originaux comparés par copie,
  seuls les7 changements autorisés ; AAR historique inchangée.

Ces observations statiques ne valident pas le linker Android ni l'exécution sur
API24/25, les pages16K sur appareil ou les méthodes mesurées. Une AAR n'est pas
un APK : ni zipalign d'APK, ni signature d'application, ni installation testée.

## Limites et blocages

Le chemin consumerProguardFiles amont est erroné : règles non embarquées malgré
un build réussi. Correction proposée sans application dans [DEFECTS.md](DEFECTS.md).
L'intégration minifiée reste non qualifiée. Les restrictions UI/BUSY/réentrée
changent le comportement attendu même si les signatures restent compatibles.
Le stockage privé, l'allocation réelle et les erreurs natives restent à tester.

**Non exécutés :** JNI/GGUF/tokenisation, essais Android réels ou Robolectric,
compilation APK, Lint CORE, suite historique299 tests dans cette mission,
installation, intégration CORE/VOICE/Binder/IDENTITY. Les résultats299 tests de
la phase A sont historiques uniquement. Aucun test B1 échoué au bilan final ;
les points non exécutés et défauts de qualification ne sont pas des succès.

Aucun modèle téléchargé/chargé, aucun second moteur, aucune AAR de production
remplacée. CORE/SQLite/IDENTITY/VOICE/PREVIEW et fixtures Binder préservés,
aucune hausse de budget ni permission ajoutée. Le patch approuvé reste inchangé.

## Preuves et suite

Lire [PROVENANCE.md](PROVENANCE.md), [BUILD-REPRODUCIBILITY.md](BUILD-REPRODUCIBILITY.md),
[JVM-COMPATIBILITY.md](JVM-COMPATIBILITY.md), [DEFECTS.md](DEFECTS.md),
[B2-TEST-PLAN.md](B2-TEST-PLAN.md) et [RESULTS.json](RESULTS.json).
inspection.json fournit les empreintes de chaque membre, DT_NEEDED, SONAME,
signatures et alignements ; les3 logs de build et le log des13 tests sont joints.

Artefact cloud local non déployé :
`/workspace/alpha1-003b1-isolated/llama-android-exact-0.1.1-jarvis-alpha1.003a-review.1.aar`.
Son nom/coordonnée distinct ne permet pas la coexistence des classes/SONAME avec
la bibliothèque historique. Le laboratoire est jetable ; les preuves Git et
empreintes assurent la traçabilité, pas la conservation durable du binaire local.

**Arrêt au gate B1.** Prochaine action : revue utilisateur des défauts, décision
sur un éventuel patch corrigé avec nouvelle empreinte, puis autorisation séparée
pour B2 et son modèle/environnement. Aucun raccordement CORE ni merge main.
