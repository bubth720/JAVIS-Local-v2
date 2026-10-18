# JARVIS CORE — ALPHA1-003B1-R

Base validée : `feature/alpha1-003b1-isolated-native-build`,
`d63462bcaffca8d84e42ca9987a2ad22a27dd102`.
Branche de livraison : `design/alpha1-003b1-r-corrections`.
Date :18 octobre2026. Commit : `git log -1 --format=%H -- docs/alpha1-003b1-r/REPORT.md`.

## Décision proposée

Le correctif documentaire est prêt pour revue ; **il n'est pas appliqué**.
[PATCH-CORRECTIONS-A-VALIDER.patch](PATCH-CORRECTIONS-A-VALIDER.patch) a SHA-256 :

```text
7e2c3aa8a2e1556a46ddbdaea1ca1e652a671a2de0d97a0cfe3aeb0da31bdeec
```

Il s'agit d'un **overlay incrémental sur le candidat003A utilisé par B1**, pas
un remplacement du patch précédent ni un patch autonome sur l'amont vierge.
L'ancien patch garde SHA329fc1959a2ac990f84e7b63f61239b75dd63d13b478b1f974f37b9c4a3ef97b.
L'amont reste llama-android823d1de2b3ef85ba9ef4847a03bac423f4aa1c44 et
llama.cpp2da668617612d2df773f966e3b0ee22dc2beef7b.

Les seuls fichiers ciblés sont :

1. `library/build.gradle.kts` : consumerProguardFiles pointe sur src/proguard-rules.pro.
2. `library/src/main/kotlin/dev/ffmpegkit/llama/LlamaMeasured.kt` : propriétaire FD
   unique avec finally et erreurs supprimées ; suppression des streams, IO via
   Os.read/Os.write avec écritures partielles, EOF POSIX, même propriétaire pour
   le fsync du répertoire. Aucun changement d'admission/quarantaine/native JNI.

Le choix de supprimer les constructeurs résout le risque de leur échec après open
sans dépendre d'un transfert de propriété vers les streams. Il introduit une
surface IO POSIX à qualifier explicitement ; voir DESCRIPTOR-REVIEW.md.

## Résultats actuels

11 tests de revue/vérificateur et15 tests JVM du helper avec Os factice :
**26 réussis,0 échec,0 ignoré**. Typecheck Kotlin réussi, signatures historiques
préservées. Application à blanc de l'overlay sur B1 et du composé sur l'amont
vierge réussies ; aucun `git apply` effectif. Les hashes avant/après sont dans
PATCH-FILES.json. Le binaire B1 est correctement refusé par le nouveau vérificateur
car il n'embarque pas proguard.txt : contrôle négatif attendu, pas défaut masqué.

Les224 fichiers suivis de la base sont inchangés, y compris toutes les preuves
B1. Les sept fichiers dans chacune des deux copies B1 et les AAR B1/production
restent inchangés. Seul ce nouveau dossier documentaire est livré.
Les documents ID-002N préexistants non suivis restent exclus.

Le contrôle Git brut des espaces retourne2 uniquement pour quatre lignes vides
de contexte préfixées par une espace dans le diff unifié documentaire. Ces
octets font partie du patch et son SHA est conservé. Le contrôle hors artefact
patch et celui des lignes de code ajoutées passent ; aucune règle Git permanente
n'est modifiée pour masquer ce résultat.

## Non exécuté, risques et suite

Aucune construction de candidate corrigée, consommateur R8, exécution Android,
JNI, GGUF, CORE tests/Lint, installation ou activation. Les résultats B1 demeurent
historiques, sans nouvelle prétention de reproductibilité ni comptage exact.
Le consommateur R8 compile-only et le vérificateur de packaging sont préparés ;
leur réussite sur une AAR corrigée reste à démontrer.

Le helper propose une tentative de fermeture unique ; un syscall de fermeture
en échec n'est pas transformé en succès. Le relevé d'identité échoué peut conserver
un fichier/slot bloqué ; aucun effacement aveugle. Les pannes réelles du stockage,
EINTR, fsync/unlink et crashs restent à qualifier sur Android. La quarantaine
native est conservée sans reset. Voir RISK-MATRIX.md et TEST-PLAN.md.

Aucun autre dépôt, fichier CORE, profil Athéna, SQLite, contrat IDENTITY, fixture
Binder ou APK n'est modifié ; aucune bibliothèque ni dépendance de production
remplacée. Pas de fusion main, pas d'installation personnelle.

**Arrêt après publication.** Décision attendue : approbation explicite du SHA et
des deux fichiers avant application à une nouvelle copie expérimentale et sa
compilation. La validation de cet overlay ne devra pas être interprétée comme
une autorisation B2 ou de raccordement CORE : ces deux étapes restent distinctes.
