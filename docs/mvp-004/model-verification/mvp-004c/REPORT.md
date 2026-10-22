# CORE — MVP-004C / issue10 : attente de publication réseau

**Statut : BLOQUÉ — MODÈLE NON VÉRIFIÉ ; ANDROID NON QUALIFIÉ.**
Mission active uniquement : https://github.com/bubth720/JAVIS-Local-v2/issues/10
Issue ouverte et lue depuis GitHub ; mise à jour observée22 octobre2026.
Aucune mission dépendante n'est lancée en parallèle ou anticipée.

## Base et contrôles exécutés

Branche `test/mvp-004-native-qualification`, base locale et distante
`4977681ae5cb5bbff08517c6d5363b8e8b2321fd`.
Les288 fichiers suivis de cette base sont identiques octet par octet : code,
JNI, dépendances, signatures configurées, budgets, admission, SQLite, imports,
anciens rapports et vérificateur inchangés. Le dossier utilisateur non suivi
`docs/identity-002n/` est préservé. Seuls les documents du présent sous-dossier
sont ajoutés. Aucun APK construit, modifié ou installé.

Contrôles nouveaux : lecture issue10, comparaison Git, inspection du statut
cloud et de la configuration officielle en brouillon, enregistrement de la
modification minimale et nouvelle observation du statut. Aucun test Android,
JVM ou GGUF exécuté ; les439 tests antérieurs ne sont pas réétiquetés.

## Différence entre brouillon et environnement actif

Le brouillon officiel existant contient neuf règles personnalisées, dont les
hôtes Hugging Face/Xet utilisés pour le modèle. Il manque `us.aws.cdn.hf.co`,
la destination du refus CONNECT HTTP403 constaté dans MVP004B.

L'ajout de ce seul domaine est préparé et enregistré via le service officiel de
configuration. Les neuf règles existantes et les presets sont conservés ;
aucun script, dépôt, secret, proxy, TLS ou protection applicative modifié.
NETWORK-DRAFT.json conserve le changement exact et les observations.

Réponse du service : **status=saved, requires_publish=true**.
Un brouillon sauvegardé ne publie ni n'applique la règle réseau. Après sauvegarde,
le statut courant confirme la connexion cloud mais conserve network_state=unknown,
aucun domaine personnalisé actif observé et spec_revision1. La nouvelle règle
n'est donc pas attestée comme appliquée. Aucune nouvelle tentative du même
403 connu n'est lancée avant changement réel. Aucun contournement ou substitution.

L'issue10 exige une modification réseau officielle validée puis son application
observée avant téléchargement. Le blocage n'est pas une absence d'autorisation du
GGUF : celle-ci est déjà acquise pour le fichier/révision précis et reste valable.
L'étape manquante concerne la publication de l'environnement par son propriétaire.

## Action exacte du propriétaire

1. Ouvrir les paramètres de cet environnement cloud.
2. Vérifier le brouillon réseau : un seul domaine ajouté, `us.aws.cdn.hf.co` ;
   conserver les règles existantes et les presets.
3. Enregistrer les changements, puis **publier l'environnement** comme demandé
   par le service. Une simple réponse dans le chat ne prouve pas l'application.
4. Reprendre MVP004C après cette opération. Recontrôler d'abord l'état/règle
   effective ; seulement ensuite télécharger le fichier autorisé hors Git/APK.

Aucun jeton personnel ou VPN n'est nécessaire pour résoudre ce refus de domaine.
Ne pas désactiver TLS/proxy, autoriser tous les domaines ou remplacer le modèle.
Si l'interface ne permet pas cette publication, signaler cette limite avant toute
nouvelle tentative. Aucun changement réseau actif n'est revendiqué ici.

## Vérification GGUF restant à effectuer

Source unique autorisée : Qwen/Qwen3-0.6B-GGUF, révision
`1208e45d782fe18602c5eaf10e5758d5b0f24c03`,
`Qwen3-0.6B-Q4_K_M.gguf`, taille attendue396704416 octets,
SHA attendu `b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e`.
Aucun fichier obtenu dans cette mission, aucun hash réel calculé, aucun
vocabulaire/template ou tensor lu. Le vérificateur demeure inchangé et non exécuté.
Les hypothèses et l'anomalie de carte1.7B du rapport précédent restent ouvertes.

Après obtention et SHA conformes, exécuter le vérificateur existant, publier
uniquement métadonnées/empreintes et préserver les poids hors dépôt/APK.
Même une réussite n'attestera ni chargement JNI ni conversation Android.

## Séquence imposée et gates

| Mission | État actuel | Gate obligatoire avant lancement |
|---|---|---|
| MVP004C / issue10 | EN COURS, bloquée réseau ; preuves publiées sur branche actuelle | Publication/règle effective, octets/SHA/GGUF vérifiés |
| MVP005 / issue11 | NON COMMENCÉE, aucun commit de mission | MVP004C réussie et laboratoire Android ARM64 explicitement autorisé |
| MVP006 / issue12 | NON COMMENCÉE, aucun commit de mission | Moteur Android réellement validé |
| MVP007 / issue13 | NON COMMENCÉE, aucun commit de mission | Étapes précédentes validées ; aucun essai/installation POCO/Redmi sans accord explicite |

Aucune demande d'autorisation de laboratoire ni préparation de développement
ultérieur dans cette session : arrêt au premier blocage, conformément à l'ordre
utilisateur. Aucune fusion main, PR vers main ou communication intermodules.
