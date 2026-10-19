# MVP-001 — guide du laboratoire Android

Application expérimentale distincte `com.stark.jarvislocal.mvp001`, ARM64,
Android7/API24 minimum. **Aucune installation personnelle n'est autorisée par
cette livraison.** Faire valider séparément l'appareil et l'essai. Ne désinstaller
ni mettre à jour aucune ancienne version JARVIS ; les signatures debug ne sont
pas une identité de mise à jour stable entre environnements.

L'APK n'embarque aucun poids. Ne télécharger aucun modèle automatiquement.
Il faut un GGUF public, de confiance, déjà obtenu légalement, compatible avec
llama-android0.1.1, strictement supérieur à100Mio et <=512Mio. Aucun modèle précis
n'est qualifié par ce livrable. Le SHA attendu et la licence doivent être vérifiés
auprès du producteur ; la saisie ne constitue pas une authentification de celui-ci.

Après autorisation d'essai seulement :

1. Saisir provenance publique (sans token/identifiant), licence, SHA-256 attendu
   en minuscules et taille exacte en octets. Ne pas calculer « attendu » à partir
   du fichier suspect lui-même en prétendant l'avoir authentifié.
2. Choisir le fichier déjà local via le sélecteur Android. L'application demande
   EXTRA_LOCAL_ONLY, ne persiste pas le droit URI et ne contacte aucun serveur.
   Un fournisseur de documents tiers reste indépendant ; choisir un fournisseur
   local et activer le mode avion pour une qualification hors ligne.
3. Attendre copie bornée, en-tête, taille, SHA, synchronisation et sélection.
   Un refus conserve le modèle sélectionné précédent. Prévoir poids+256Mio libres,
   en plus des anciennes versions conservées.
4. Configurer le SYSTEM (<=1024 octets), puis charger explicitement. L'accueil et
   une réouverture de processus ne chargent jamais automatiquement un modèle.
5. Envoyer une question courte. L'historique RAM est fourni au modèle dans les
   limites du contexte2048 ; génération128 tokens au plus, SYSTEM distinct.
   Aucune réponse prédéfinie ni commande court-circuit dans ce parcours.
6. Annuler demande le rejet de sortie et attend le retour natif. Ne pas forcer
   un second chargement pendant cette attente. Décharger explicitement avant de
   sélectionner/importer un autre modèle. Une quarantaine interdit toute reprise
   native dans le processus ; fermer/redémarrer le processus ne valide pas un
   déchargement interprocessus et ne doit pas contourner Binder/2.

Les conversations sont en RAM : rotation conservée, perte à la mort du processus.
Le SYSTEM est stocké dans les seules préférences MVP. Les anciens modèles importés
sont conservés ; cette version ne fournit ni suppression/export/restauration des
poids ni export de conversation. Ne pas effacer les données d'une application
existante pour contourner un refus. Pas de voix, PDF, mémoire durable, personnalité
Athéna ou communication CORE–LLM dans ce parcours expérimental ; leurs sources et
fonctionnalités historiques restent disponibles dans les autres variantes.

## Qualification future sur Android ARM64 autorisé

Fermer les autres moteurs LLM avant cet essai : ProcessInferenceAdmission
n'arbitre que le processus MVP, pas les autres applications. Aucun arbitrage
inter-APK n'est prétendu ici.

Conserver APK-INFO.json et SHA du modèle public sans contenu personnel. Vérifier
identifiant/certificat, mode avion, import invalide/cancel/reprise, bouton load,
question synthétique imprévisible au préalable, réponse native observée, seconde
question avec contexte, annulation et unload. Surveiller mémoire/batterie/chaleur,
rotation/arrière-plan et reprise sans auto-load. Si un appel reste actif, attendre
ou arrêter le processus selon le protocole d'essai ; ne jamais déclarer l'unload
sur un simple timeout. Aucun test cloud actuel ne constitue cette preuve.
