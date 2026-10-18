# ALPHA1-003A — matrice de revue, bibliothèque seule

Statut : proposition documentaire, aucune application JNI. Les mesures ci-dessous
sont présentes dans le diff et contrôlées statiquement ; leur efficacité Android/native
reste à démontrer après autorisation. Le patch et sa seule empreinte normative sont
dans PATCH-LIBRARY-A-VALIDER.patch et PATCH.sha256.

| Risque | Changement proposé | Contrôles obtenus | Qualification restant nécessaire |
|---|---|---|---|
| R01 — destination GGUF non imposée | Nouveau loadFrozen exige Context Android privé, SHA et taille approuvés. noBackupFilesDir est résolu sous le dataDir de l'application ; répertoire réel, UID courant et absence de droits groupe/autres vérifiés. Aucun source.parentFile comme destination. | Contrôle de types Android public ; checks de structure et mutations de la destination/UID/O_EXCL. | Context réel API24/25, stockage chiffré/protégé, droits atypiques, symlinks, échec fsync. Un Context fourni par du code hôte malveillant n'est pas une autorité indépendante. |
| R01 — fichier remplacé | Création nom aléatoire, O_EXCL/O_NOFOLLOW, mode0600 puis0400 ; identité device/inode/UID mémorisée. Destination attribuée au propriétaire avant ouverture. | Vérification des flags, de l'ordre d'attribution et de l'identité avant suppression. | Courses et E/S réelles ; même UID/root hors garantie. Pas de prétention d'immuabilité absolue. |
| R02 — handle non livré après annulation | suspendCancellableCoroutine avec hook d'abandon ; le handle brut est retenu dans OwnerSlot AVANT construction/livraison du wrapper. L'annulation pose une intention ; le worker attend le retour effectif puis libère ou conserve l'inconnue. | Types Kotlin/coroutines1.9.0 ; checks du hook, de l'ordre de transfert et de la conservation de gate. | Annulation avant file, copie, JNI, retour/réveil du caller ; OOM de construction ; panne d'executor ; preuve réelle de fin native. |
| R02 — nouvelle admission trop tôt | Un seul slot est réservé avant allocation ; il n'est retiré qu'après release native revenue ET nettoyage du fichier synchronisé. Un chargement échoué sans handle récupérable reste quarantainé. | Inspection des transitions ; aucun reset proposé ; gate d'appel libéré avant réveil du caller sans écraser celui d'un appel suivant. | Concurrence et ABA, panne du worker. Le slot est une protection locale de bibliothèque, pas un bail CORE ou un arbitre Android. |
| R03 — source qui évolue | Taille attendue issue du manifeste hôte ; fstat initial ; lectures limitées au restant, écritures <=taille attendue ; une seule lecture EOF supplémentaire ; fstat final et SHA attendu. | Checks/mutations borne, EOF et hash. Le code lit au plus N+1 octets et écrit au plus N, sauf erreur d'E/S. | Fichier qui croît/rétrécit/change à taille constante, disque plein, lecture bloquée. Une taille bornée ne garantit pas une durée d'E/S bornée. |
| R04 — raccordement CORE implicite | Nouveau diff avec sept chemins library/ uniquement ; les cinq hunks CORE ne sont pas copiés. Nouvelle coordonnée expérimentale distincte. | Sept cibles exactes ; reconstruction en mémoire ; mutation d'un en-tête b/core refusée ; contrôle à blanc réussi. | Aucun raccordement CORE autorisé. Le nouveau loadFrozen(Context,...,expectedBytes) est volontairement incompatible avec l'appel de l'ancien composite non appliqué. |
| R05 — réentrée/deadlock | Toute entrée publique prend un gate atomique avec refus BUSY. Callback final gardé ; appel depuis le worker refusé ; aucune attente derrière un appel lourd. Release/getSystemInfo synchrones refusés sur le thread UI. Future interrompu jamais annulé ; gate rendu par le vrai worker. | Types, contrôles de guards et mutations ; getSystemInfo passe aussi par le propriétaire. | Callbacks réentrants/multithreads, interruptions et worker remplacé. Callback hôte de confiance, sans I/O/suspension/attente d'un autre thread. |
| R06 — retry ne nettoie plus le snapshot | Handle mis à zéro uniquement après retour réel de release. deleteOwned est exécuté même lors d'un retry avec handle0. Échec suppression/fsync conserve le slot. Retry explicite du seul fichier abandonné autorisé uniquement handle0 et état natif connu. | Inspection de l'ordre, identité du fichier et fsync ; mutations de l'identité et du retry. | Échec avant/après unlink/fsync, fichier absent, inode différent ; aucun retry de release native inconnue. |

## Restrictions comportementales à approuver

Les signatures historiques loadModel/complete/embed/releaseModel/getSystemInfo et
le constructeur historique LlamaModel sont conservés. La comparaison JVM stricte
inclut l'ancien bridge public synthétique access$parseResult, maintenu explicitement.

Cela ne signifie pas compatibilité comportementale déjà prouvée : les appels
synchrones UI, les appels réentrants et les appels concurrents sont désormais
refusés dans la proposition. L'ancien SDK pouvait laisser ces appels attendre.
Ces restrictions doivent être acceptées et testées avant tout raccordement hôte.
Un double release d'un modèle déjà libéré ne doit jamais toucher un nouveau modèle.

## Invariants natifs préservés, toujours non qualifiés

llama_jni.cpp, measured.inc et LlamaJNI.kt ont exactement les mêmes octets candidats
que les sept éléments amont du composite ALPHA1-002. Ils restent un diff contre
823d1de2, pas des sources appliquées. Même modèle/contexte/worker, ticket unique,
génération/KV, SYSTEM séparé, budget2048/sortie256/marge128/TTL5s, prepare sans decode
et consume sans rerendu sont conservés dans la proposition. L'égalité réelle des
tokens et les garanties de nettoyage natif ne sont pas obtenues par cette mission.

La bibliothèque ne remplace ni ProcessInferenceAdmission, ni les verrous CORE, ni
F3, ni le blocage CORE004D/Binder/2. Elle ne couvre pas Whisper/VOICE/autres APK.
Le manifeste B, IDENTITY, Athéna, SQLite, les prompts et les budgets restent inchangés.
