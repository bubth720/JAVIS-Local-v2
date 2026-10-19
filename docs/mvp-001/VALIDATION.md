# MVP-001 — résultats du cloud

19 octobre2026. JDK17.0.16+8, Gradle8.10.2, Kotlin2.0.21, AGP8.7.3, SDK35.
SDK/caches/TLS/proxy existants réutilisés. Aucun modèle ni système Android téléchargé.
Le cache Robolectric utilise un user.home JVM accessible en écriture ; il ne
s'agit pas d'un changement de HOME ou des données d'un utilisateur Android.

Commande finale réellement exécutée, avec recompilation et réexécution forcées :

```sh
gradle -PincludeP2Fixture=true \
 :app:testDebugUnitTest :p2-client:testDebugUnitTest \
 :app:assembleDebug :app:lintDebug :p2-client:assembleDebug :p2-client:lintDebug \
 :app:testPreviewUnitTest --tests '*PreviewActivityTest*' :app:assemblePreview :app:lintPreview \
 :app:testMvpUnitTest --tests '*Mvp*' :app:assembleMvp :app:lintMvp \
 --rerun-tasks --no-daemon --max-workers=4 -Pkotlin.compiler.execution.strategy=in-process
python3 tools/check-mvp-001.py
```

| Suite | Tests réussis | Échecs/erreurs/ignorés | Nature |
|---|---:|---|---|
| CORE historique |263|0/0/0|JVM/Robolectric, tests existants inchangés|
| Binder fixture /2 |36|0/0/0|JVM/Robolectric, aucun Binder entre APK réels|
| Preview |12|0/0/0|Robolectric API24/25/35|
| Imports MVP |39|0/0/0|13 cas×API24/25/35, petites données synthétiques|
| Coordination MVP |24|0/0/0|8 cas×API24/25/35, moteur simulé uniquement en tests|
| Activity MVP |6|0/0/0|Accueil/isolation/rotation API24/25/35|
| Refus ressources MVP |3|0/0/0|Politique pure avec snapshots synthétiques|
| Total |**383**|**0/0/0**|Aucune qualification JNI/GGUF|

Imports : bornes, métadonnées, format/version, hash, taille courte/longue, disque,
annulation et lecture échouée avec ancien modèle intact, reprise du temporaire,
restauration AtomicFile interrompu, fichier corrompu et interruption après rename
avant sélection. Les minuscules fixtures de test ont une policy injectée ; les
seuils de production ne sont pas abaissés. Ce ne sont pas des GGUF exécutables.
Coordination : chargement explicite, SYSTEM distinct et historique fourni,
concurrence refusée, annulation avant/pendant chargement et génération, timeout
retenant le gate, sortie tardive écartée, quarantaine après unload échoué,
refus de réimport résident et reprise après release confirmé simulé, dégradation
ressources sans libération prématurée.

Builds debug/Preview/MVP/fixture réussis. Lint0 erreur/Fatal :32 avertissements
debug,33 Preview,35 MVP,0 fixture. Ils sont conservés et non traités comme succès
sans réserve. Le log Gradle complet est joint, son hash brut/normalisé également.
TEST-RESULTS.json conserve les classes/comptes/timestamps XML de cette exécution.

Le garde MVP passe :169 fichiers de base inchangés,2 documents seulement complétés,
2 fichiers applicatifs avec ajouts exactement contrôlés. Les anciens gardes de
périmètre absolu échouent (4 refus consignés dans LEGACY-GUARDS.json) ; leurs
assertions et fichiers sont inchangés. Ce sont des refus de périmètre contre
leurs anciennes bases, distincts d'un échec des383 tests. Ne pas les annoncer verts.

## APK et limites

`tools/mvp_kit.py` vérifie signature debug, identifiant distinct, launcher, minSdk24,
target35, ARM64, absence Internet/réseau/micro et poids, tous les ELF, hashes des
six SO de l'AAR historique exacte fdccf04c…31e7501, comptes de tests et Lint.
Le kit final est produit depuis un commit sans changement applicatif non enregistré.
APK-INFO.json consigne SHA APK/certificat/commit et les preuves propres au kit.
Le kit CI peut avoir un certificat/SHA différents du kit local ; ne pas les mélanger.

**Non exécutés :** émulateur Android, appareil physique, import d'un vrai GGUF,
chargement/génération JNI réelle, qualification RAM/thermique/batterie physique,
preuve d'exclusivité entre APK ou panne électrique réelle. /dev/kvm et binaire
emulator absents ; aucun appareil Android ARM64 fourni. Aucun modèle réel embarqué.
La véritable génération est **NON VALIDÉE**. Le chemin de production du MVP appelle
l'AAR native historique ; seules les implémentations de test simulent une réponse.
Aucune installation sur appareil personnel, activation Athéna ou fusion main.

CI : workflow mvp-001 model-free ajouté. Un push sur la branche de mission peut
construire un nouveau kit ; ne déclarer un artefact Actions disponible qu'après
observation de sa réussite. Le vieux workflow de téléchargement reste manuel et
n'est pas déclenché ici.
