# Validations locales C-VOICE-001

## C1 exécuté

Java17, Gradle8.10.2, SDK35 :

```sh
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon --max-workers=3 -Pkotlin.compiler.execution.strategy=in-process
```

Résultat : **47 tests réussis,0 échec,0 ignoré** (40 existants +7 SpeechOutputPortTest), compilation et Lint réussis. Les tests SQLite utilisent Robolectric/SQLite natif sur API24,25,35 ; pas un appareil Android.

Les nouveaux tests vérifient le texte historique exact y compris au-delà300 caractères, l'absence de faux événement de fin/annulation, le pilote inactif et le refus API24/25, les bornes/Unicode/tokens réservés, les callbacks obsolètes/session différente, l'invalidation sans preuve native, et les diagnostics expurgés.

Les validations C2 et les preuves de préservation sont ajoutées à la clôture. Binder Android, JNI/GGUF/Whisper, audio physique et Xiaomi ne sont pas validés par ces tests.
