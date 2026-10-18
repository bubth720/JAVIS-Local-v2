# ALPHA1-002 — architecture JNI proposée

Statut : `core.identity.measure/1-proposed`, non approuvé/non activé.
Aucun changement jarvis.identity/1, UNIFIEDv1 ou fixture Binder/2. Le /0 ID-002T
reste une proposition historique ; pas de nouvelle version runtime approuvée.

## Propriété et transaction

CORE conserve permissions, sessions, budgets et révocations. ProcessInferenceAdmission
garde son bail acquis avant chargement ; InferenceController son gate/drainage ;
LlamaLocalEngine son Mutex. Le registre natif ne donne aucune permission : il
associe pointeur vivant, génération et worker aux buffers préparés. Ce n'est pas
un autre arbitre ni un remplacement du bail CORE.

```text
CORE gate + bail -> Mutex moteur -> unique worker bibliothèque
 -> modèle/contexte détenus -> prepare SYSTEM + user final
 -> rendu complet -> IDs retenus -> reçu -> admission CORE
 -> consume mêmes IDs -> retour JNI -> rejet éventuel de sortie tardive
 -> discard buffers -> modèle toujours résident
```

Un executor à un thread pour la bibliothèque, également utilisé par ses anciens
appels dans le patch proposé. Un seul modèle/contexte, jamais de compteur externe.
La table native refuse un second modèle vivant. Rechargement après release sous
le même bail CORE, avec nouvelle loadGeneration ; un thread remplacé ne récupère
pas silencieusement le propriétaire.

## Modèle, encodage et template

ExactArtifact exige hashes de poids et template approuvés ; aucun hash de poids
qualifié n'est inventé. loadFrozen fait une copie privée du GGUF, hash, fsync et
read-only avant l'unique chargement. Original inchangé ; copie disque, pas second
modèle résident. Coût disque et nettoyage à qualifier. Hash attaché au modèle
chargé, pas déduit tardivement d'un chemin mutable. Compromission du même UID/root
hors de cette garantie ; aucune attestation absolue de mémoire Android.

SYSTEM/user séparés, ByteArray UTF-8 strict, sans NFC/NFD ni remplacement des
surrogates invalides. NUL refusé car llama_chat_message utilise des chaînes C.
Le chemin ancien garde son encodage JNI modifié et n'est jamais un fallback exact.
Template modèle obligatoire et rendu borné64Ko, préfixe assistant activé ; aucun
fallback de SYSTEM en user. Hash du template comparé à la valeur qualifiée.
La sémantique SYSTEM de chaque template doit être testée, pas déduite du seul rendu.

Même vocabulaire : add_special=true, parse_special=true. Deux appels llama_tokenize
(taille puis remplissage) produisent un seul vecteur, pas une somme de fragments.
Consume ne rend/tokenise jamais à nouveau ; il préremplit par batches sur ces IDs.
Le [llama-batch.cpp épinglé](https://github.com/ggml-org/llama.cpp/blob/2da668617612d2df773f966e3b0ee22dc2beef7b/src/llama-batch.cpp)
initialise les positions implicites depuis seq_pos_max+1. La continuité réelle
des batches reste à vérifier au banc natif, pas seulement par lecture.

## prepare / consume / discard

| Étape | Effets et contrôles |
|---|---|
| prepare | Handle vivant, worker, absence de ticket, limites, UTF-8, template, budget ; aucun decode/sampler/clear KV/chargement/contexte supplémentaire |
| Ticket | 256bits SecureRandom, hex, privé au wrapper ; un seul par modèle, jamais loggé ou persisté |
| Reçu | Révision native1 vérifiée, loadGeneration, kvEpoch, compte exact, contexte effectif/capacité ; hashes modèle/template/rendu/IDs et binding par le wrapper contrôlé |
| Admission | Epoch/session, stop, quarantaine CORE ; binding SHA-256 de ownerEpoch, bail et digest complet de requête, limites et provenances structurées |
| consume | Retire le ticket avant validation : génération, worker, binding, nonce, expiration monotone, kvEpoch ; toute tentative le brûle, aucun ID accepté depuis l'appelant |
| Calcul | Incrément kvEpoch, clear KV, decode des IDs retenus, sortie bornée ; une erreur de decode n'est pas une réponse partielle réussie |
| discard | Détruit les seuls buffers correspondants ; idempotent après consommation ; mauvais ticket refusé ; ne libère ni modèle, contexte ni bail |

TTL proposé maximum5s via steady_clock native, sans prolongation. Callback final
synchrone sur worker, réservé au code CORE de confiance, sans I/O/suspension/réentrée.
Même portée et Mutex entre mesure et consommation. Aucun JSON n'émet un ticket.

Complete/embed/release historiques refusés pendant une préparation. Les mutations
KV historiques incrémentent kvEpoch ; consume vérifie l'epoch capturée. Génération
monotone contre ABA même si pointeur/bail réutilisés. Nouveau processus : table vide,
nonce neuf, jamais restauration de ticket. Compteurs épuisés : refus fermé.

## Équation exacte

```text
capacité = min(contexte demandé, capacité admise CORE,
               llama_n_ctx effectif, llama_n_ctx_seq effectif)
inputTokens + outputReserve + marginReserve <= capacité
```

Contexte <=2048, sortie1..256, marge128 déjà réservée par CORE. Calcul natif par
soustractions après bornes, sans overflow. Reçu et CORE revérifient l'équation.
Les contrôles UTF-8 historiques restent conservateurs et peuvent sur-refuser ;
ni troncature SYSTEM ni remesure de contournement. Aucun seuil matériel relevé.

## Annulation, erreurs et libération

Avant admission : rejet/discard sans calcul ; retour connu et nettoyage réussi
permettent de conserver le modèle résident. Après admission, une annulation peut
précéder l'entrée matérielle JNI : aucun engagement d'empêcher tout calcul tardif.
CORE draine puis rejette la sortie révoquée. requestStop demeure UNSUPPORTED.

Échec à partir de consume : réutilisation conservativement inconnue, même si le
JNI est revenu ; quarantaine et bail conservés. Cela inclut une expiration détectée
à cette étape. Échec discard/thread/handle : refus fermé. Ticket perdu : une fin
réelle de processus peut être nécessaire ; aucun reset de registre/bail.
Retour JNI n'est pas unload ; effacement volontaire des vecteurs/ByteArray n'est
pas une preuve d'effacement cryptographique de toutes copies.

## Frontière IDENTITY

Le patch traite une requête SYSTEM déjà assemblée. Il ne branche pas IdentityComposer,
ne change pas son manifeste B/contrat et ne remplace pas son port indisponible.
NativeVerified seul n'est pas un permis. Une future composition scoped doit mesurer
une seule fois le SYSTEM final approuvé dans cette transaction et consommer les
mêmes IDs. Interdit de remesurer puis appeler generate(String), entrée refusée par
le backend exact. L'intégration de persona et Athéna exige un gate distinct.
