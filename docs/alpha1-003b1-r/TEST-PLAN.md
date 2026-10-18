# Vérifications réalisées et plan après validation

## Exécuté dans B1-R

- Recalcul SHA et reconstruction en mémoire du patch bibliothèque003A.
- Reconstruction déterministe du correctif2 fichiers ; application **à blanc** sur
  les sources B1 inchangées et du composé complet sur l'amont épinglé.
- 11 tests Python réussis,0 échec/ignoré : scope, source/hash, reconstruction,
  invariants de quarantaine, chemins des règles et vérificateur d'archive.
- 15 tests helper JVM réussis,0 échec/ignoré, Os factice : ouverture en échec,
  fermeture unique, exceptions read/write/fstat/fsync/constructeur simulées,
  close en échec seul ou double, imbrication, interruption, annulation et Error.
- Types Kotlin2.0.21/coroutines1.9.0, SDK35, JVM11 :7 fichiers Kotlin de revue
  hors des sources réelles. Signatures historiques9/6/6 conservées.
- Préservation224 fichiers de base, AAR historique et B1, sources B1 et ancien
  patch. Aucun C++ candidat matérialisé, aucun build AAR/JNI.

Exemples de commandes, avec la chaîne préparée et des chemins de revue hors sources :

```sh
python3 docs/alpha1-003b1-r/proposal.py --upstream "$PRISTINE_UPSTREAM"
python3 docs/alpha1-003b1-r/test_review.py --upstream "$PRISTINE_UPSTREAM"
python3 docs/alpha1-003b1-r/typecheck.py --upstream "$PRISTINE_UPSTREAM" --tools "$TOOLS" --scratch "$REVIEW_SCRATCH"
python3 docs/alpha1-003b1-r/test_descriptors.py --tools "$TOOLS" --scratch "$REVIEW_SCRATCH"
```

Le vérificateur conserve un contrôle négatif sur l'AAR B1 préservée sous le chemin
cloud de la mission ; pour une autre machine, fournir cet artefact à cet endroit
ou adapter explicitement ce chemin documentaire sans changer son SHA attendu.
Aucune commande ci-dessus ne télécharge un outil/modèle ni n'applique un patch.
Les fichiers Kotlin compilés sont des copies de revue uniquement. Aucun C++/AAR
n'est construit. Les sous-cas de règles manquantes sont inclus dans les11 tests,
pas additionnés artificiellement au total.

## À exécuter après accord d'application et de compilation isolée

1. Revalider le patch003A SHA329fc195… et le correctif SHA de PATCH.sha256.
   Appliquer003A puis l'overlay dans une copie fraîche hors CORE, jamais en production.
2. Construire proprement la candidate ; relancer inspection JNI/JVM/ELF et
   comparaison des composants, avec nouveaux hashes (ne pas réutiliser le SHA B1).
3. Exécuter check_future_aar.py ; présence et contenu des3 règles attendues obligatoires.
4. Construire le consommateur R8 décrit dans r8-consumer/README.md, examiner
   configuration fusionnée, mapping et DEX ; noms/signatures JNI inchangés.
   Ni installation ni exécution APK nécessaires à ce test de compilation.
5. Préparer des fixtures Android publiques synthétiques, sans GGUF : refus de
   source absente/lien/permissions, FD épuisés, croissance/rétrécissement source,
   read/EINTR/écriture partielle/zéro, ENOSPC, fstat/fsync/close/unlink en erreur.
   Injecter les pannes par shadow de test ou instrumentation explicitement revue,
   sans modifier silencieusement le candidat de production.
6. Vérifier après chaque panne : fermeture des FD effectivement possédés, erreur
   primaire/supprimées, source intacte, identité du fichier avant suppression,
   slot bloqué en cas d'identité inconnue ou de nettoyage incomplet.
7. Vérifier annulation avant ouverture, pendant lecture et entre écritures partielles ;
   interruption de thread sans perte du drapeau, zéro octet au-delà de la borne.
8. Observer le nombre de FD avec données synthétiques et reprise du processus ;
   ne pas confondre fermeture vérifiée en test et promesse en cas de crash physique.

Le lancement des fixtures Android et toute instrumentation supplémentaire doivent
respecter le périmètre approuvé. Aucun test de ce deuxième groupe n'est exécuté ici.
Les vrais tickets, tokens, llama_decode et libération native relèvent de B2,
qui reste soumise à une autorisation distincte et à l'accord sur le GGUF/environnement.
