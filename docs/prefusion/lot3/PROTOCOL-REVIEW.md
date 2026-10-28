# CORE-PF-02 — protocole local de revue, OFF / UNTRUSTED

`jarvis.core-ui.readonly-lab/1-draft` est une API Kotlin de laboratoire, pas un
wire approuvé ni une extension de UI03, UNIFIED v1/v2 ou CORE004C. Aucune dépendance
au dépôt INTERFACE n'est ajoutée. Consommateur UI existant : origine NONE/LABORATORY,
ContentKey sans epoch ; un mapping doit être revu avant sérialisation/IPC.

Le port lit des instantanés du CoreEventBus existant et la projection
PinnedDocumentReadiness existante. Il n'appelle ni SessionCoordinator.send,
connector.connect, modèle, DB ou provider. AppGraph n'est pas modifié et ne crée
pas ce port. Constructeur par défaut OFF, aucune découverte/connexion de production.
Un opt-in de fixture injecté à la construction permet seulement des métadonnées
RAM, toujours UNTRUSTED et LABORATORY ; realCoreAuthenticated/canExecute sont
constants faux. READY, texte de réponse et citation ne font pas partie du résultat.
RESPONSE_COMPLETED est le type d'un événement, pas un contenu ou une attestation
native. Une source synthétique ne devient donc pas une vraie réponse du CORE.

## Corrélation et bornes

Binding UUID canonique epoch/session/request, génération>=1 et version exacte.
Chaque métadonnée conserve eventId UUID, génération et séquence source. Epoch
fraîche du port n'atteste pas à elle seule l'epoch du bus historique : le bus ne
possède pas d'epoch signée. La qualification système et la nouvelle autorité de
session demeurent bloquées avant tout usage réel. TTL laboratoire<=30s monotone,
64 événements/instantané, 256 IDs maximum et refus explicite après épuisement.
Les répétitions identiques d'un instantané ne sont pas réémises. Un autre curseur,
replay d'ID à nouvelle séquence, trou, changement rétroactif, epoch/génération/session
incorrecte ou fenêtre perdue ferme cette instance UNKNOWN sans reset automatique.

ACK est local et ne prouve ni livraison distante ni authentification. Annulation
avant ACK reste terminale ; process death = UNKNOWN, pas unload ou reprise autorisée.
La lecture est synchrone : la lambda de fixture doit seulement lire l'instantané
RAM existant, sans I/O bloquante ni appel externe. Le moniteur du port ne fournit
pas de timeout d'une lambda hostile ; ce prototype ne prouve donc pas un arrêt
interprocessus borné à cinq secondes. L'arrêt du consommateur UI et celui du
calcul natif sont des responsabilités distinctes à qualifier séparément.
Les observations pinned NEXUS/SERVICES gardent leurs SHA de lot2 et canExecute=false,
sans substitution des anciennes bibliothèques JVM réellement consommées.

## Identité : banc synthétique seulement

SyntheticPeerPolicy vérifie cohérence UID/package/hash certificat/expiration
synthétiques ; même une correspondance parfaite ne produit que UNTRUSTED. Les
champs revendiqués par payload, y compris UID/paquet ou READY/canExecute, sont
refusés. L'expiration est celle de l'assertion sur l'horloge de fixture, pas une
validation X.509 ni une comparaison à l'horloge civile du système Android.
Aucun certificat personnel, secret, signature ni clé n'est lu ou enregistré.

## Adaptateur Android proposé — NON APPLIQUÉ

Avant un vrai adaptateur : choix de transport/paquet/certificat et approbation
spécifique ; acquisition de l'UID depuis Binder dans un appel réel, résolution
PackageManager+signature active autorisée, jeton/nonce frais et génération liés
à l'autorité CORE, révocation, preuve d'origine du producteur et code consommateur.
Un UID de payload ou une égalité de package ne remplace pas cette preuve. Ne pas
choisir un service exporté général ou une permission supplémentaire par défaut.

Le CORE reste min24. Signatures/permissions/déploiement/IPC et production demeurent
OFF. Les tests JVM ne qualifient ni Binder ni certificate chain ni Android physique.
Les apps, SQLite, préférences, mémoires/profils, JNI/admission et ressources sont
préservés. Rollback : retirer ces seuls ajouts via revue ; aucune donnée à migrer.
