# Plan B2 — non exécuté, autorisation distincte obligatoire

Préconditions : décision utilisateur sur le SHA exact de la candidate ou sur un
nouveau patch corrigé ; environnement Android ARM64 de laboratoire approuvé,
GGUF public déjà autorisé et vérifié, aucune donnée personnelle, un seul moteur,
aucune connexion au CORE de production. Aucune installation personnelle implicite.
Les défauts B1-01/B1-04 doivent recevoir une décision explicite avant extension du
périmètre. Une observation nécessitant de modifier le candidat exige un nouveau
diff et un nouveau SHA approuvés.

| Groupe | Cas / oracle attendu |
|---|---|
| ABI/JNI | Charger les6 SO sur Android ARM64 API24 puis API35 ; vérifier résolution des8 exports et ancienne API, sans mélanger les deux bibliothèques |
| Historique | load/complete/embed/release sur modèle autorisé, résultat puis libération réelle ; essais appel UI/BUSY/réentrée et consommateur minifié R8 après correction approuvée |
| Mesure exacte | SYSTEM distinct, question, Unicode/emoji/accents/NUL/UTF-8 invalide ; vérifier template rendu et IDs préparés contre IDs effectivement fournis à llama_decode |
| Tickets | prepare puis consume une fois, discard, réutilisation, ticket falsifié/ancien, modèle/génération/réservation changé : refus sans décodage |
| Contexte | Budget exactement à la limite puis dépassé, arithmétique bornée, aucun contournement par maxTokens0 ou embedding |
| Concurrence | Deux callers, callback réentrant, double chargement, release pendant opération : refus contrôlé, même modèle/worker/réservation, aucune seconde résidence |
| Annulation | Avant allocation, après allocation avant livraison wrapper, avant consume, pendant JNI et lors release ; ne rendre le slot qu'après fin observable et release confirmée |
| Fichiers | Stockage privé UID/modes/inode, lien symbolique, source croissante/décroissante, hash erroné, accès refusé/disque plein, exception de construction stream, suppression échouée/retry |
| Nettoyage | Erreur native et état incertain : handle conservé/quarantaine ; fichier supprimé seulement après release attestée ; pas de reset pour contourner |
| Reprise | Processus tué, fichier orphelin, redémarrage, corruption : aucun handle/ticket réutilisé, aucune fausse preuve de release |

L'égalité d'un compteur ou du digest du résultat prepare ne suffit pas : il faut
une observation indépendante des IDs réellement décodés, corrélée au même modèle,
worker et ticket. L'instrumentation doit être spécifiée puis autorisée si elle
change les sources. Conserver les preuves techniques synthétiques, sans prompts
personnels ni chemin sensible ; mesurer RAM/latence sans hausser les budgets.

Rapporter séparément JVM/doubles, Android émulateur ARM64 et matériel. Un timeout,
un ACK ou un caller annulé ne prouve jamais la fin native ou le déchargement.
Aucun test de ce tableau n'est déclaré réussi dans B1. Le raccordement CORE,
IDENTITY/Athéna et Binder/2 nécessitent leurs validations propres.
