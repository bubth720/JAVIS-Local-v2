# Signatures et restrictions d'exécution

Inspection `javap` du classes.jar candidat contre l'AAR Maven0.1.1 : toutes les
9 déclarations publiques historiques de Llama, les6 de LlamaModel et les6 de
LlamaJNI sont présentes, y compris le bridge synthétique access$parseResult.
Le détail des ajouts et signatures est dans inspection.json. Trois déclarations
JNI mesurées sont ajoutées et leurs trois exports ELF sont présents, en plus des
cinq exports historiques. Cela ne démontre pas leur bonne exécution JNI.

Les signatures load/complete/release restent disponibles. La compatibilité de
comportement comporte des restrictions explicites : appels synchrones hors thread
UI, refus BUSY sans attente si une autre opération possède le worker, refus des
réentrées worker et callback. Aucun appelant ne doit supposer une file implicite
ou pouvoir appeler release directement depuis un callback propriétaire.
Le chemin historique CORE n'utilise pas cette candidate et reste inchangé.

## Contrôles réellement exécutés

13 tests réussis,0 échec,0 ignoré, dans guards-validated.log :

1. Refus getSystemInfo depuis UI simulée avant JNI.
2. Refus release depuis UI simulée avant JNI.
3. Opérations séquentielles sur un seul worker propriétaire.
4. Retour d'un contrôle synchrone hors UI sans appel natif.
5. Refus de réentrée sur le worker.
6. Refus de réentrée depuis le callback d'admission.
7. Restauration du garde après callback en exception.
8. Refus BUSY d'un concurrent synchrone.
9. Refus BUSY d'un concurrent suspendu.
10. Annulation logique conservant le verrou jusqu'au retour du worker bloqué synthétique.
11. Interruption du caller Future sans annulation du propriétaire ni ouverture du gate.
12. Double release d'un wrapper historique vide handle0, hors UI, sans JNI.
13. Échec précoce loadModel sur chemin inexistant, puis nouvelle opération autorisée.

Ce sont les classes JVM réelles de la candidate, avec accès interne friend-path
et un **double manuel Looper**. Aucun SO n'est extrait par le runner, la trace de
chargement confirme LlamaJNI absent. Ni Robolectric, ni Android réel, ni modèle
natif. Les cas de worker synthétique ne prouvent pas l'arrêt, le déchargement ou
le nettoyage d'un véritable modèle. Le test13 n'ouvre aucun GGUF.

Reproduction, avec la chaîne préparée :

```sh
python3 docs/alpha1-003b1/guards.py --aar "$CANDIDATE_AAR" --tools "$TOOLS_ROOT" --scratch "$SCRATCH"
python3 docs/alpha1-003b1/inspect_aar.py --aar "$CANDIDATE_AAR" --historical "$HISTORICAL_AAR" --tools "$TOOLS_ROOT" --scratch "$INSPECTION"
```

Les outils supposent les versions/cache décrits par BUILD-REPRODUCIBILITY.md ;
ils n'installent rien et ne provisionnent pas de modèle.
Les erreurs nativeComplete/nativeReleaseModel et les refus avec modèle résident
ne sont pas testés en B1. Une annulation de coroutine reste distincte de la fin
JNI et de la libération du modèle.
