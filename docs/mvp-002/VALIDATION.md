# MVP-002 — vérifications du 20 octobre2026

JDK17.0.16+8, Gradle8.10.2, AGP8.7.3/Kotlin2.0.21, SDK35, minSdk24/ARM64.
Environnement et caches existants réutilisés ; aucun modèle téléchargé ni chargé.
Commande finale (log complet joint), avec réexécution forcée :

```sh
gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
 :app:testPreviewUnitTest --tests '*PreviewActivityTest*' :app:assemblePreview :app:lintPreview \
 :app:testMvpUnitTest --tests '*Mvp*' :app:assembleMvp :app:lintMvp \
 :app:testMvp2UnitTest --tests '*Mvp2*' :app:assembleMvp2 :app:lintMvp2 \
 --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/check-mvp-002.py
```

| Suite | Réussis | Échecs/erreurs/ignorés | Portée |
|---|---:|---|---|
| CORE historique |263|0/0/0|JVM/Robolectric, suites inchangées|
| Fixture Binder |36|0/0/0|Pas de liaison réelle entre APK|
| Preview |12|0/0/0|API24/25/35 Robolectric|
| MVP001 |72|0/0/0|Ancien mode RAM/import/annulation conservé|
| Adaptateur CORE MVP002 |12|0/0/0|4 cas×API24/25/35, vraie SQLite/CORE, LLM simulé|
| Store vocal MVP002 |12|0/0/0|4 cas×API24/25/35, petits en-têtes synthétiques|
| UI/voix déclarées MVP002 |9|0/0/0|3 cas×API24/25/35, pas d'audio physique|
| Politique TTS/extensions |7|0/0/0|Doubles SDK/avion, aucune parole réelle|
| Total |**423**|**0/0/0**|**383 régressions et40 nouveaux tests**|

CORE : session conservée entre composition/adaptateur, SYSTEM distinct, réponse
simulée persistée puis DB rouverte, souvenirs seulement via commande explicite,
restauration d'historique, réponse annulée non ajoutée à SQLite. UI : avatar
présent, package/permissions isolés, aucun chargement ni TTS à l'ouverture,
annulation du dialogue mémoire sans écriture. Le rendu physique/latence n'est pas
mesuré par ces tests d'inflation et d'état.
TTS : absence de preuve humaine bloque, voix réseau/non installée refusées,
test préalable, preuve révoquée à shutdown, borne300 sans modification du texte,
SDK en échec sans repli, mode avion faux/inaccessible refusé avant création du
moteur. Ces doubles ne vérifient ni réseau réel ni audition. Whisper : format,
hash, reprise/cancel, refus du croisement GGUF/vocal, seuils de production conservés.
Aucun de ces petits en-têtes n'est un modèle natif exécutable.

Tous les builds/debug/Preview/MVP001/MVP002/fixture et Lint réussissent.
Lint0 Error/Fatal :32 avertissements debug,33 Preview,36 MVP001,40 MVP002,0 fixture.
Aucune assertion historique ni avertissement désactivé. Les cinq anciens gardes
de périmètre absolu refusent les modifications autorisées (LEGACY-GUARDS.json),
ils restent inchangés ; le garde exact MVP002 passe et protège190 fichiers de base.
Les8 fichiers existants modifiés et leur justification sont dans le rapport.

Le kit vérifie signature, package/launcher, minSdk24/target35, ARM64 seulement,
absence Internet/réseau/micro/poids et hashes des sept bibliothèques natives
contre l'APK MVP001 ; les six SO Llama sont aussi comparés à l'AAR0.1.1 exacte.
Aucune AAR JNI expérimentale n'est substituée. Le kit CI doit être distingué du
kit local (certificat/SHA potentiellement différents) ; aucune réussite Actions
ne doit être inférée du succès local.

**Non exécutés/non validés :** conversation GGUF réelle sur Android, audition TTS
physique hors ligne, transcription microphone/Whisper, émulateur/appareil Android,
mesure thermique/batterie/RAM native ou durabilité sous coupure physique. Aucun
appareil ARM64 fourni ; /dev/kvm et émulateur absents. Aucun APK installé, profil B
lu, pilote Binder activé, autre dépôt modifié ou main fusionné.
