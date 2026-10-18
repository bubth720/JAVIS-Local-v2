# Défauts constatés et risques restant ouverts

| ID | Nature / preuve | Impact / décision |
|---|---|---|
| B1-01 | Confirmé : library/build.gradle.kts référence consumerProguardFiles("proguard-rules.pro"), fichier réel library/src/proguard-rules.pro ; avertissement dans les3 logs et aucune entrée proguard.txt dans l'AAR | Risque élevé pour un consommateur minifié R8 utilisant les noms JNI ; intégration minifiée non qualifiée |
| B1-02 | Confirmé : A/B non identiques, A/C identiques ; comparaison des sections jointe | Reproductibilité limitée au chemin/hôte, ne pas revendiquer une fabrication universellement reproductible |
| B1-03 | Restriction confirmée en JVM : UI/BUSY/réentrée refusés | Signatures préservées mais appelants futurs à adapter/qualifier explicitement |
| B1-04 | Risque de revue, non reproduit : construction FileInputStream/FileOutputStream après Os.open et avant prise en charge par use dans LlamaMeasured.kt | Une exception de construction, notamment OOM, pourrait laisser un FD sans finally explicite ; tester les transferts de propriété/erreurs Android avant intégration |
| B1-05 | Non qualifié : prepare/consume/discard, tickets, token IDs, stockage privé Android, annulation/release JNI | B2 requise avec autorisation et moteur réel ; aucune garantie acquise par la compilation |

B1-01 existe également dans l'AAR Maven historique (règles absentes) : il ne faut
pas l'attribuer à l'introduction du nouveau JNI. Le fichier de règles contient
notamment la conservation LlamaJNI, des méthodes natives et des API publiques.
**Correction proposée, non appliquée** dans library/build.gradle.kts :

```diff
-    consumerProguardFiles("proguard-rules.pro")
+    consumerProguardFiles("src/proguard-rules.pro")
```

Ce changement exige un nouveau diff/une nouvelle empreinte et une autorisation ;
il faudrait ensuite reconstruire et tester un consommateur R8 isolé. Le patch
329fc195… reste intact, aucun correctif implicite n'est inclus dans la candidate.
B1-04 demande une revue ciblée d'ownership FD et éventuellement un try/finally
englobant la construction ; aucun correctif n'est appliqué sans nouveau diff.

Les permissions/UID/identité du stockage ne protègent pas contre un attaquant du
même UID ou root. Les fautes disque et crashs Android réels restent à qualifier.
Les coordonnées Maven distinctes ne séparent pas les noms de classes ni SONAME :
**ne jamais cocharger** historique et candidate. Le slot bibliothèque est local
au processus, pas un arbitre CORE–VOICE/Binder. CORE004D/F3 ne sont pas débloqués.
