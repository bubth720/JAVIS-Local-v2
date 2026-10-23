# MEM-01 — consentement exact et mémoire de laboratoire isolée

CORE `bubth720/JAVIS-Local-v2`, branche `feat/mem-01-intensive`, parent
`9b04ecd8794da54e571d95f28a1de02512e86b5d` (ID-01). Base technique de fiche
`a6f13dd101990c6b5e3992be506c9a5b99873621`, planning `1b7ac9b35b5e6e133167fcaf1873e58b9db7288e`.

## Fonctions livrées

`memory/MemoryApprovalWorkflow.kt` : propositions RAM uniquement, contenu exact
non tronqué, session/nonce/expiration et confirmation séparée. ACCEPT/REJECT et
REPLACE lié à la révision pour conflit ; aucune approbation issue d'un LLM,
document ou événement. La révocation ne bloque pas sur le moniteur de décision.
Un jeton de session identifié par objet et génération interdit la réutilisation
ABA d'une ancienne proposition après recréation du même identifiant de session.

`ConfirmedMemoryStore.kt` : véritable SQLite Android privée, transaction
mémoire/reçu/audit, SHA de contenu, conflits optimistes, édition/suspension,
expiration et suppression sélective propriétaire. Reçu et audit consultables,
réouverture vérifiée ; corruption refusée sans suppression/réinitialisation.
`MemoryLookupPort` refuse par défaut ; projection autorisée explicite et bornée,
toujours sans SENSITIVE, cache absent donc révocation/expiration immédiates.
Aucune connexion automatique au LLM, au snapshot IDENTITY ou à NEXUS.

`MemoryLabUi.kt` + MVP2/MVP3 : commande distincte sous activation explicite du
laboratoire, saisie sans état Android sauvegardé, aperçu exact puis confirmation,
refus, liste/édition et confirmation d'effacement. Le gate existant de MvpRuntime
reste unique. Arrière-plan révoque les propositions. Une interruption après
commit peut perdre l'ACK : l'UI indique de consulter la liste avant de réessayer,
elle ne promet pas de rollback. Le nonce consommé ne peut être réutilisé.

## Préservation et limites

Le namespace `noBackupFilesDir/confirmed_memory_lab.sqlite` n'est ouvert que sur
usage explicite ; aucune migration/importation des bases, souvenirs, messages ou
préférences historiques. Ceux-ci restent inchangés. Aucun modèle ou profil vocal
chargé, pas de JNI/admission/seuil/permission/contrat v1 modifié, pas d'installation.
Athéna demeure inactive. Les données des tests sont entièrement synthétiques.
SQLite privée n'est pas un chiffrement applicatif ; suppression logique n'est
pas un effacement cryptographique. Ces API sont dans le processus de confiance,
pas un endpoint exporté. Quotas 200 souvenirs/2000 opérations d'audit : atteinte
=> refus explicite, y compris édition/suspension/suppression, sans effacer le
journal pour débloquer. Une procédure d'archivage journal nécessite une mission
séparée avant utilisation durable à grande échelle.

## Validation

Résultat final : `BUILD SUCCESSFUL in 2m 32s`, 167 tâches dont 32 exécutées.
**426 tests réexécutés : 376 debug + 50 MVP2, zéro échec/erreur/ignoré.**
La fixture /2 conserve 36 résultats UP-TO-DATE, non comptés comme réexécutés.
Builds debug/MVP2/MVP3 réussis. Lint debug/MVP2 : zéro erreur, 30/40 warnings
historiques (rapports partiellement réutilisés). Commandes exactes dans
`VALIDATION-MEM-01.json`.
Les tests utilisent SQLite NATIVE sous Robolectric API24/25/35, pas un appareil
Android ni une preuve JNI. La restauration testée consiste à fermer la base,
conserver un fichier synthétique corrompu puis restaurer une copie de fixture
isolée : ce n'est pas encore une fonctionnalité d'export/restauration produit.
Un premier build après durcissement a refusé l'opérateur Kotlin `in` sur
ConcurrentHashMap ; correction en `containsKey`, sans assouplir les tests.
La revue croisée a identifié l'interleaving ABA ; test à latches ajouté avant
publication, ainsi que perte d'ACK après commit et refus de nonce rejoué.

Démo sans installation : suite `ConfirmedMemoryWorkflowTest` pour consentement,
conflit/édition/révocation, panne transactionnelle et récupération ;
`MemoryLabUiTest` pour vrai clic aperçu/refus/confirmation et arrière-plan.
Garde `tools/check-mem-01.py` protège 327 fichiers de la branche ID-01.
Aucune fusion. Prochaine mission : ID-02, lecteur de préférences confirmé en
lecture seule et styles d'éditeur ; activation native demeure hors périmètre.
