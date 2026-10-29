# M50-S1 A3 — CORE, campagne de régression exacte

Branche : `codex/m50-s1-core-ci-20261029`, PR brouillon vers
`mission/m50-integrated-beta-20261029`. Gel intégral du HEAD de mission
`d754a6c19e3c94e415e8506d0d37b8d943951d88`, arbre
`8d22a663574c611fa108f9656f6dd285083a6f71`, descendant lot4
`3eafb4acd82255f4578d4a3bc113d418508add86`. Aucun fichier historique modifié.

Le nouveau garde admet exactement quatre nouveaux fichiers de CI, test et rapport.
Les tests sur Git synthétique refusent changement runtime/manifeste/permission,
suppression, symlink, mode exécutable inattendu, test/ancien garde/ancien workflow
modifié, fichier additionnel et route autre que la branche exacte ou sa PR
intra-dépôt vers mission. Les gardes historiques sont exécutés dans des clones
isolés aux SHAs lot3 `aed84860f2ca85713140a687ba7d86efc0203dc8`, lot2
`a3113797a3c481bc516c5ef1fd4d7a74b919de40` et FUSION02
`f6a3b2d0c280c69ef1570544fdbf5bcf5818810c`. Leur code et leurs empreintes restent intacts.

La campagne reprend les producteurs JVM historiques NEXUS
`777e6afa75ecb99b85e52d68837cb0fefcf161cd` et SERVICES
`3decdc47d82f9b43713c48cab244762941770a81` ; elle ne substitue pas les nouveaux
producteurs M50. Les cas lot3 629 restent une référence historique. La cible
actuelle est au moins 641 identités exécutées distinctes entre suites Debug,
MVP2 filtrée, fixture et JVM, incluant les douze cas synthétiques lot4. Ne pas
additionner un ciblage ou une répétition des mêmes tests. Le lecteur XML impose
présence, identité, absence de failure/error/skipped et conservation de l'inventaire
historique. Le JSON d'artefact contient les nombres observés, HEAD et arbre réels.

Commandes dans `.github/workflows/m50-s1-core.yml` :

```sh
python3 -m unittest discover -s tests/m50_s1 -v
python3 tools/check-m50-s1-core.py --historical
# Préparation sources exactes puis test/run integration-lab, voir workflow.
# SDK vérifié par tools/setup-preview-android.py, puis suites Android hôte/build/lint.
python3 tools/check-m50-s1-core.py --results
```

Versions conservées : Kotlin2.0.21, AGP8.7.3, Gradle8.10.2, JDK17, SDK35,
minSdk24, ARM64 ; aucune dépendance modifiée. Builds/Lint Debug, Preview, MVP,
MVP2 et MVP3 prévus. Aucun modèle téléchargé, appareil installé ou moteur natif
chargé par cette campagne. AppGraph et SessionCoordinator restent identiques.

Exécution locale : vingt tests Python de garde sur dépôts synthétiques ; la
régression Gradle locale est BLOCKED par l'accès proxy Git et l'absence de SDK.
Publication BLOCKED : les appels d'écriture du connecteur n'ont pas terminé et
ont été interrompus ; branche distante absente (404), aucune PR M50-S1 CORE
observée lors de la vérification en lecture seule. Aucun commit nouveau ni run
Actions ne peut être annoncé. La configuration de CI proposée
publie XML et Lint même après échec ; une erreur ne devient jamais un PASS.
Les tests Android instrumentés, génération native, physique et mesures thermiques
restent NOT_RUN. Cette campagne ne valide ni G2 ni G3 ni G7 complet, ni M50.

Lot B : SessionCoordinator est le coordinateur réutilisable, avec SessionActions
injectable dans le même package et événements CORE réels. AppGraph initialise
stockage et moteurs historiques et ne doit pas être composé dans la coquille.
ReadOnlyCoreUiLabPort ne véhicule que des métadonnées non authentifiées, pas un
ConversationPort. Aucune licence projet n'est présente dans l'arbre mission
(QWEN-LICENSE concerne uniquement un modèle documentaire) : provenance/licence
d'import source à faire examiner avant assemblage. A3 n'importe aucune source.

Rollback : fermer la PR ou retirer les quatre ajouts de cette branche. Aucun
schéma ni donnée ni variante historique à restaurer. Prochaine action : vérifier
la CI au SHA final, puis revue lot B et décisions de licence/assemblage.
