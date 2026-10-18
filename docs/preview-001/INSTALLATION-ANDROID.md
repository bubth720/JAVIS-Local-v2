# JARVIS Preview 001 — guide téléphone

Cette APK est un laboratoire **sans modèle**. Elle permet d'essayer l'accueil et
les erreurs du laboratoire texte. Elle ne permet pas encore de converser avec
Athéna ou de charger un GGUF depuis Téléchargements.

**Attends l'autorisation explicite d'essai de ce kit sur ton POCO X8 Pro avant
l'étape d'installation.** La disponibilité du téléchargement n'est pas une
validation physique ni une autorisation d'installation automatique.

## Télécharger sur le téléphone

1. Connecte-toi à ton compte GitHub dans le navigateur du téléphone.
2. Ouvre le dépôt `bubth720/JAVIS-Local-v2`, onglet **Actions**.
3. Choisis **Preview 001 Android kit**, puis l'exécution verte de la branche
   `release/preview-001-android`. N'utilise pas « Build JARVIS Local APK » : c'est
   l'ancien workflow qui télécharge des modèles.
4. En bas, sous **Artifacts**, touche **JARVIS-PREVIEW-001-Android**. Si nécessaire,
   active « Version pour ordinateur » dans le menu du navigateur.
5. Dans **Fichiers → Téléchargements**, touche le ZIP puis **Extraire**.
   Il contient `JARVIS-PREVIEW-001.apk`, les guides et les preuves.

Le SHA exact du code figure dans APK-INFO.json et le résumé de l'exécution GitHub.
SHA256SUMS.txt fournit les empreintes ; CERTIFICAT.txt contient le certificat public.
Aucune clé privée n'est livrée. L'artefact est conservé30jours ; après expiration,
une nouvelle fabrication sera nécessaire, avec une nouvelle vérification de signature.

## Installer — uniquement après accord

1. Vérifie que le kit correspond au commit et à l'exécution autorisés.
2. Touche **JARVIS-PREVIEW-001.apk** dans les fichiers extraits.
3. Si Android le demande, autorise cette source d'installation dans les paramètres
   proposés, puis reviens au fichier. Les libellés varient selon HyperOS/Android.
4. L'application doit s'appeler **JARVIS Preview 001**. Son identifiant est
   `com.stark.jarvislocal.preview001`, distinct de JARVIS Local.
5. Si Android propose une **mise à jour** d'une application déjà présente, arrête
   et transmets le message exact. Ne désinstalle aucune application existante.
6. Après installation autorisée, révoque si souhaité l'autorisation temporaire de
   cette source. Si Android bloque l'APK, transmets le message ; aucun contournement
   ni désactivation des protections n'est demandé.

Signature de développement/debug, pas signature de production durable. Une autre
exécution Actions peut utiliser un autre certificat : **ne tente pas de mettre à
jour cette Preview avec une autre build sans instructions validées**. Une erreur
de signature ne justifie pas une désinstallation qui supprimerait des données.

## Premier parcours autorisé

1. Ouvre **JARVIS Preview 001** : « Modèle absent » est le résultat attendu.
2. Touche **À propos du modèle GGUF** pour lire la limite d'import.
3. Touche **Ouvrir le laboratoire texte**, puis confirme explicitement.
4. L'état **ERREUR** indiquant l'absence de modèle est attendu. Aucun faux texte de
   conversation ne doit apparaître ; la saisie n'est pas envoyée sans préparation.
5. Touche l'état pour réessayer : le modèle reste absent, sans réponse inventée.
6. **Quitter le laboratoire** ou Retour efface l'historique RAM lorsque le travail
   est terminé. Si un traitement est occupé, l'annulation est demandée et il faut
   attendre son retour réel. Accueil Android/arrière-plan ne signifie pas unload.

Ne copie pas de modèle, conversation ou profil vocal dans l'application pour ce
premier essai. Aucune permission micro, Internet ou stockage partagé n'est nécessaire.
Ne désinstalle pas JARVIS Local et ne supprime pas ses fichiers.

## Ce qu'il faut nous retourner

Remplis TESTS-POCO.md avec version Android/HyperOS, RAM annoncée, version du kit,
messages exacts et étapes. Aucun journal privé, conversation personnelle ou numéro
de série n'est demandé. Pas de promesse de performance ni de conversation réelle
à tirer de ce seul essai d'interface.
