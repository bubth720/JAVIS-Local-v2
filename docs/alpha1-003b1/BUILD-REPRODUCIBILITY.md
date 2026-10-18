# Construction et reproductibilité

Chaîne utilisée : JDK17.0.16+8, Gradle8.9 amont, AGP8.7.3, Kotlin2.0.21,
NDK27.2.12479018, CMake3.22.1, SDK35, minSdk24, ABI arm64-v8a, cible JVM11.
Douze fichiers de chaîne ont été revérifiés contre le verrou ALPHA1-003.
Les inventaires de dépendances sont joints : 181 lignes identiques au verrou
historique et entre les trois builds. Aucun téléchargement de modèle.

| Build propre | Source | Résultat | AAR SHA-256 |
|---|---|---|---|
| A | candidate-a | SUCCESS,33 tâches,1m34s | `7e4e667d4a2f597fb7a78e7f29eb8b6298696812f68eee5e0549eec283ce02b0` |
| B | candidate-b, autre chemin | SUCCESS,33 tâches,1m29s | `5fa0698c9a5f68272c8de9af5643ae2274a82fbed0567a9b0a2f49a7aaa299af` |
| C | candidate-a, sorties effacées | SUCCESS,33 tâches,1m28s | identique à A |

`build.sh` refuse le checkout CORE, exige les sept hashes candidats et des sorties
propres, impose Gradle8.9 et la vérification des dépendances. Il ne prépare pas
les sources et ne télécharge aucun outil. Après préparation de la chaîne, exemple :

```sh
bash docs/alpha1-003b1/build.sh "$LAB_COPY" "$GRADLE89" "$EVIDENCE" a
```

La préparation consiste à extraire les archives vérifiées dans un laboratoire,
placer llama.cpp sous son gitlink, comparer le verrou, appliquer exactement le
patch approuvé. Ne jamais cibler le checkout CORE ni y copier l'AAR.
Les builds utilisent `--offline --no-build-cache --rerun-tasks --max-workers=4`.
Pour C, seuls les répertoires générés build/.gradle/library/build/library/.cxx de
la copie A ont été supprimés après conservation de l'artefact A.

**Reproductibilité constatée à chemin/hôte/chaîne fixes seulement.** A et C
sont byte-identiques, membres ZIP compris. A et B ont des ELF différents ;
classes.jar, manifeste, métadonnées, R.txt et libc++ sont identiques.
Les comparaisons de sections sont dans comparison-ab.json et comparison-ac.json.
Des chemins absolus et métadonnées de compilation sont une explication cohérente
avec les différences .rodata/.text/build-id observées ; leur causalité exhaustive
n'est pas prouvée. Ce n'est pas une différence limitée aux timestamps ZIP.
Les sources extraites sans .git produisent des avertissements non bloquants de
métadonnées Git absentes. Aucune reproductibilité entre machines n'est démontrée.

L'AAR historique Maven a SHA-256
`fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501`.
La candidate diffère dans classes.jar et cinq ELF ; libc++ et les autres membres
communs sont identiques. Les différences natives cumulent patch et conditions de
fabrication : pas de preuve d'équivalence au binaire Maven ni d'attribution de
chaque différence au patch. Inventaire intégral : inspection.json.

Les logs publiés retirent uniquement les espaces de fin de ligne ; hashes bruts
et publiés figurent dans RESULTS.json. L'artefact reste dans le laboratoire cloud,
non ajouté à Git et non déployé. Les hashes identifient des octets, pas une preuve
de sûreté ou de tokenisation.
