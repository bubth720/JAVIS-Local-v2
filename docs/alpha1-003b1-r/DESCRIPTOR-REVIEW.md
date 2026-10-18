# Descripteurs : proposition et limites

La proposition corrige exclusivement `LlamaMeasured.kt` dans le document patch.
Aucun fichier Kotlin de la bibliothèque effective n'est modifié.

## Propriété unique

`withOwnedDescriptor` ouvre puis devient seul responsable de l'appel `Os.close`.
Son bloc n'est atteint qu'après ouverture réussie. Une exception du bloc, y compris
annulation, interruption ou Error, emprunte le finally. Une erreur de fermeture
se propage si elle est seule ; sinon elle est ajoutée à l'erreur initiale comme
exception supprimée. Aucun retry d'un close ambigu, aucune seconde fermeture par
un stream. Une erreur OS de fermeture reste une incertitude à diagnostiquer :
le helper ne peut promettre le succès d'un syscall qui échoue.

Les constructeurs FileInputStream/FileOutputStream sont **supprimés** du candidat
proposé. Le risque d'échec de ces constructeurs après ouverture est donc éliminé
structurellement, plutôt que de dépendre de leur politique de propriété du FD.
Les lectures/écritures passent par Os.read/Os.write. Une boucle traite les écritures
partielles et refuse un zéro anormal. La vérification EOF attend0, convention
POSIX de Os.read, au lieu du -1 de InputStream. Cette différence est volontaire
et devra être vérifiée sur Android. La limite expectedBytes+1 lus reste conservée,
avec au plus expectedBytes écrits ; un byte de détection n'est jamais écrit.

La copie annule entre les lectures et les écritures partielles ; elle ne prétend
pas interrompre un syscall bloqué. Un EINTR/une exception remontant par Os
fait échouer la copie, sans boucle de retry implicite ajoutée. Les descripteurs
source, destination puis répertoire utilisent tous le même propriétaire.
Les flags O_NOFOLLOW/O_EXCL, modes0600/0400, bornes, hash et fsync restent présents.
Le répertoire gagne O_CLOEXEC. Le hash est mis à jour après écriture complète du
bloc, jamais pour un fragment non écrit.

## Fichiers et état natif

L'identité inode/device est relevée avant la première écriture. Si ce relevé
échoue après création, `created=true` reste retenu ; l'identité non attestée
interdit une suppression aveugle. C'est un blocage conservateur, pas un nettoyage
réussi. L'échec d'un unlink/fsync reste susceptible de conserver le slot.

Le code OwnerSlot/LlamaOwner et la transaction prepare/consume/discard restent
identiques octet pour octet (hors suppression des deux imports en tête). Les
gardes nativeUncertain, release avant remise du handle à zéro, nettoyage avant
slot=null et refus de reset ne sont pas modifiés. Annuler un caller ne prouve
jamais une fin native, une suppression de fichier ou une libération mémoire.

La fermeture FD et la fin native sont des événements distincts. Le helper ne
produit aucun reçu natif et ne touche pas à ProcessInferenceAdmission/Binder.
Une exception de close ne doit pas être interprétée par un futur appelant comme
un succès de copie. La qualification de crash, manque de FD, disque plein et
sémantique de close/EINTR sous Android reste ouverte.

## Preuves actuelles

15 cas JVM utilisent **le texte exact du helper proposé**, avec un Os factice,
pour vérifier propriété/exceptions/suppression/interruption. Les scénarios nommés
constructeurs sont des exceptions injectées dans le bloc, pas des constructions
FileInputStream réelles (elles sont supprimées). Ils ne testent ni Os.open/close
Android, ni la boucle de copie réelle sur appareil, ni JNI. Les11 tests de revue
contrôlent en plus le périmètre, la reconstruction et la préservation de la
quarantaine. Les types sont vérifiés contre SDK35 ; aucune exécution API24 acquise.
