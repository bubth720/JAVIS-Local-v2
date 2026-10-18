# ALPHA1-003B1 — provenance

Base CORE : `design/alpha1-003a-library-patch-review` au commit
`7f591f0e041159bb773011d69fdd2b1fe94b5b88`.
Patch approuvé : `../alpha1-003a/PATCH-LIBRARY-A-VALIDER.patch`.
SHA-256 revérifié : `329fc1959a2ac990f84e7b63f61239b75dd63d13b478b1f974f37b9c4a3ef97b`.

Sources épinglées :

- ffmpegkit-maintained/llama-android : `823d1de2b3ef85ba9ef4847a03bac423f4aa1c44`.
- llama.cpp : `2da668617612d2df773f966e3b0ee22dc2beef7b`.
- Archives SHA-256 : respectivement `0e386b0f79ac29ffffdc4b3a9061ca2467c2b1b872420a3c1a8f4a7f04555fde`
  et `927d1ab5daa353ec18d415355fd491f9def014f2157d7b1527553f60effd6671`.

Les archives conservées et les 31 fichiers amont /22 headers du verrou source
ont été revérifiés. Les deux licences MIT et notices restent dans les sources ;
la provenance des licences et le tag non signé sont documentés dans
[la phase A](../alpha1-003/PROVENANCE.md). Aucune nouvelle signature producteur
n'est démontrée. Les notices LLVM/libc++ restent nécessaires à une redistribution.

Deux copies neuves hors checkout, `candidate-a` et `candidate-b`, ont reçu
`git apply --check`, puis `git apply` du seul patch approuvé. Les sept chemins,
hashes avant/après et comparaisons sont dans [provenance.json](provenance.json).
Aucun patch correctif additionnel n'est appliqué.

L'expérimentation relève du mainteneur JARVIS CORE sous autorisation utilisateur,
pas du producteur amont. Coordonnée candidate déclarée :
`fr.jarvis.experimental:llama-android-exact:0.1.1-jarvis-alpha1.003a-review.1`.
Aucune publication Maven ni substitution de dépendance n'est effectuée.

Après les builds, les 3058 fichiers originaux par copie restent identiques aux
sources, sauf les sept changements autorisés. Les 200 fichiers suivis à la base
CORE et l'AAR historique en cache sont inchangés : [preservation.json](preservation.json).
Les documents ID-002N préexistants non suivis sont exclus de la livraison.
