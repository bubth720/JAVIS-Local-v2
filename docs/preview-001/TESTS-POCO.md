# PREVIEW-001 — fiche d'essai POCO X8 Pro

**NON EXÉCUTÉE sur téléphone. À remplir seulement après autorisation explicite.**
Utiliser uniquement des textes synthétiques. Les tests avec modèle restent bloqués
puisque le kit n'inclut ni GGUF ni importeur. Ne pas inventer un résultat pour les remplir.

## Identification non personnelle

- Commit et exécution GitHub du kit :
- Nom/package : JARVIS Preview001 / com.stark.jarvislocal.preview001
- Version Android et HyperOS :
- RAM annoncée, espace disponible, mode économie d'énergie :
- Batterie et température initiales (si mesurables, préciser l'outil) :
- Autre JARVIS installé avant essai (nom/version seulement) :

## Essai interface autorisable avec ce kit

| Test | Attendu | Résultat / message exact |
|---|---|---|
| Installation | Nouvelle application distincte, aucune proposition d'écraser une autre | À remplir |
| Démarrage | Accueil lisible, modèle absent, aucun crash/permission micro | À remplir |
| Petits/grands caractères et navigation | Boutons accessibles, texte lisible, pas de chevauchement barres système | À remplir |
| Info GGUF | Limite d'import clairement affichée | À remplir |
| Entrée laboratoire | Confirmation obligatoire, Athéna inactive | À remplir |
| Préparation sans modèle | Erreur compréhensible, aucun faux succès ni réponse prédéfinie | À remplir |
| Réessai | Même refus honnête ; pas de boucle infinie | À remplir |
| Rotation/arrière-plan | Interface cohérente, pas de lancement voix/micro | À remplir |
| Sortie par bouton/Retour | Retour accueil, pas de bascule dans le mode historique | À remplir |
| Redémarrage de processus | Nouvelle session RAM ; aucune mémorisation personnelle créée | À remplir |
| Ancienne application JARVIS | Toujours présente ; données/réglages inchangés, sans la désinstaller | À remplir |
| Mode avion | Accueil/erreurs utilisables ; cela ne valide pas une inférence hors ligne | À remplir |
| Chaleur/batterie/RAM au repos | Relever observations et méthode, arrêter si comportement anormal | À remplir |

## Essai avec modèle — BLOQUÉ dans PREVIEW-001

Après une mission distincte d'import/qualification : provenance et hash du modèle,
chargement réel, une réponse puis relance, contexte, mode avion, annulation avant
et pendant calcul, rejet tardif, attente du retour JNI, déchargement/reprise et
absence de second chargement. Relever RAM, batterie, chaleur, durées et erreurs
sans augmenter les seuils. Un affichage d'annulation ne prouve pas un arrêt natif.
Aucun test de ces fonctions n'est revendiqué ici.

## Retour à transmettre

Pour chaque problème : étape, attendu, observé, message exact, reproductible ou non.
Capture d'écran possible de l'accueil uniquement si elle ne contient aucune donnée
privée ; le laboratoire bloque les captures par précaution. Ne pas transmettre
numéro de série, comptes, conversations, fichiers GGUF/audio ou préférences privées.
Pas de journaux automatiques ni télémétrie ajoutés. En cas d'erreur d'installation,
arrêter et transmettre le message sans désinstallation de contournement.
