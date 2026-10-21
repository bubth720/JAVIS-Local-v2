# Reproduction et portée des validations

Environnement : Linux x86_64 cloud, JDK17, Gradle8.10.2, AGP8.7.3,
Kotlin2.0.21, SDK/build-tools35, dépendances existantes. Réutiliser la configuration
cloud documentée ; les chemins temporaires du poste ne sont pas des prérequis.
Les téléchargements de dépendances de build ne sont pas du réseau runtime.
Aucun workflow de téléchargement de poids n'a été déclenché.

## Commande de non-régression complète

```sh
gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
 :app:testPreviewUnitTest --tests '*PreviewActivityTest*' :app:assemblePreview :app:lintPreview \
 :app:testMvpUnitTest --tests '*Mvp*' :app:assembleMvp :app:lintMvp \
 :app:testMvp2UnitTest --tests '*Mvp2*' :app:assembleMvp2 :app:lintMvp2 \
 :app:testMvp3UnitTest --tests '*Mvp3*' :app:assembleMvp3 :app:lintMvp3 \
 --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/check-mvp-003.py
python3 tools/mvp3_kit.py --sdk "$ANDROID_HOME" --out /chemin/neuf/kit-mvp-003
```

Gradle installé, pas de wrapper livré. CI dédiée `.github/workflows/mvp-003.yml`,
sans modèles, permissions contents:read, artefact `JARVIS-MVP-003-Android`.
Un succès local n'établit pas le résultat du workflow distant. La CI vérifie
les mêmes suites, le kit exige leur présence et les nombres minimaux attendus.
Les trois contrôles APK se rejouent avec
`python3 tools/test-mvp3-apk.py --sdk "$ANDROID_HOME" --report /chemin/neuf/apk-tests.json`.
Ils inspectent la candidate authentique, rejettent la variante MVP002 signée et
une archive candidate modifiée après signature ; aucun APK n'est installé.
Le drapeau dirty-local est limité aux copies temporaires de ce banc, jamais au
kit de livraison. Kit final exige sources Git propres ; l'exception historique du dossier utilisateur
`docs/identity-002n/` n'autorise pas à le publier.

## Tests et preuves

Les compteurs réellement collectés, cas individuels et avertissements Lint se
trouvent dans TEST-RESULTS.json et RESULTS.json. `validation.log` conserve la
sortie du build complet. `LOG-SHA256.json` identifie les journaux conservés.
`tts-before.log` conserve le test échoué avant correction ;
cet échec intentionnel ne doit pas être compté comme suite finale réussie.

- Suites CORE : migrations1→2, interruption/rollback, versions inconnues,
  insertions échouées, historique/mémoire/documents/passages, moteur, sessions,
  admission et préservation API24/25 ; données synthétiques.
- MVP001 : hash/taille/en-tête/espace, interruptions d'import, conservation,
  reprises, gate et load concurrents, timeout/cancel/release simulés, UI/safety.
- MVP002 : vrai adaptateur CORE, backend simulé ; persistance et SYSTEM distinct,
  réponse annulée non enregistrée, voix/format Whisper, états UI.
- MVP003 : package/permissions/backup, message diagnostic, modèle absent,
  aucune écriture de conversation à l'échec, mémoire explicite, documents et
  réglages conservés à réouverture, arrière-plan/recréation/rotation,
  protections de ressources ; API24/25/35 Robolectric.
- Correctif TTS : faux service injecté, observation du mode avion invalide
  définitivement phrase et confirmation ; aucune audition Android réelle.

Les six anciens gardes absolus échouent : détails/trace dans LEGACY-GUARDS.json.
MVP002 rejette la nouvelle variante Gradle (Unapproved build change) ; les cinq plus anciens
rejettent déjà les évolutions cumulées. Ils restent intacts et ne sont pas
comptés comme réussis. PRESERVATION.json vérifie le périmètre MVP003 contre
sa base exacte ; il ne prétend pas transformer ces échecs en validations.

## APK

`mvp3_kit.py` exécute aapt, apksigner, zipalign et readelf : identité/package,
min/targetSdk, ABI, permissions exactes, backup désactivé, composants exportés,
absence de services IPC, hash des sept SO comparé MVP001 et six SO Llama
comparés à l'AAR0.1.1 épinglée, ELF AArch64/alignements16K et dépendances NEEDED,
absence de poids dans assets. Provider startup non exporté ; receiver de profil
AndroidX exporté mais protégé par android.permission.DUMP ; launcher seul point
UI public, sans génération par intent. Signature debug et debuggable explicités.
L'alignement contrôlé ne vaut pas test sur appareil à pages16K.

## Non exécuté / bloqué

Aucun émulateur, image système ou /dev/kvm disponible. Aucun appareil personnel
inspecté/autorisé/utilisé. Pas de tests instrumentés ni de Binder réel ; la
fixture P2 est testée seulement en simulation JVM/Robolectric.
Aucune génération GGUF/JNI, aucun modèle chargé/téléchargé, aucun test du moteur LLM natif Linux,
aucune audition TTS ou transcription/micro, aucun test réseau runtime ou service
externe, aucune coupure électrique, kill Android réel ou mesure RAM/thermique
physique. Réouverture SQLite/recréation Activity != redémarrage réel du processus.
Compatibilité configurée API24/ARM64 et UI simulée != qualification native API24.

Le critère conversation réelle reste bloquant. Cette livraison est uniquement
une candidate de diagnostic, sans activation Athéna ou intégration sensible.

Exécution complète conservée : 439 tests réussis (263 CORE +36 fixture +12
Preview +72 MVP001 +41 MVP002 +15 MVP003), 0 échec/erreur/ignoré. Trois contrôles
APK finaux réussis. Build SUCCESS en4m54,303 tâches exécutées. Lint : 0 erreur
ou fatale, avertissements32/0/33/36/40/40 respectivement. Les deux échecs initiaux
du contrôleur APK sont documentés dans REPORT.md et les journaux first/second ;
la réussite finale figure dans APK-NEGATIVE-TESTS.json / apk-controls-final.log.

Les journaux publiés sont normalisés uniquement sur les espaces de fin de ligne
et lignes vides finales. LOG-SHA256.json consigne les empreintes originales et
conservées ; aucun résultat ni message d’erreur n’est supprimé.
