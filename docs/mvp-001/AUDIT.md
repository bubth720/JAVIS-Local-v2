# MVP-001 — audit de départ et périmètre

Dépôt `bubth720/JAVIS-Local-v2`. La branche distante Preview a été vérifiée par
`git ls-remote`, récupérée, puis la branche MVP créée au commit exact
`86ea8bfa94deea0bab94914cd7214bc395a83f9b`.
La clôture de publication Preview documente un kit Actions ; ce résultat ancien
ne prouve aucune conversation GGUF. Les documents ALPHA1 des autres branches ne
sont pas appliqués au moteur de cette base.

| Composant examiné | État confirmé dans les sources | Conséquence MVP |
|---|---|---|
| PreviewActivity, src/preview | Accueil sans chargement, confirmation pour laboratoire ; aucun import | Conserver cette variante ; créer une autre application/Activity |
| ModelManager | jarvis.gguf couplé à la taille d'asset, seuil strict >100Mio, signature et renommage atomique | Aucune modification ; store MVP séparé et seuil non abaissé |
| LlamaLocalEngine | Llama.loadModel/complete/releaseModel de l'AAR0.1.1, SYSTEM distinct, mutex, quarantaine après load/release échoué | Ajouter seulement un constructeur de chemin/validateur, même backend/admission |
| InferenceController | tryLock, sessions/epochs, refus BUSY, drainage NonCancellable, rejet sortie annulée, unload confirmé avant sélection | Réutilisé sans changement ; aucun reset, aucun backend alternatif |
| ProcessInferenceAdmission | Propriétaire partagé du processus, jeton retenu si inconnue | Conservé exactement, pas un arbitre inter-APK |
| SessionCoordinator | Coordination app-owned de la préparation/chat/voix ; annulation attend le retour, rotation ne recrée pas le travail | Conservé ; MVP a une coordination limitée import/load/chat/unload, même discipline de drainage via le contrôleur |
| AppGraph | Initialise aussi données/voix et prépare les assets historiques | Conservé ; MvpApplication compose seulement le contrôleur/store/runtime MVP |
| PromptBuilder | Budget UTF-8 conservateur, historique borné, SYSTEM séparé via requête | Réutilisé sans mémoires/documents privés, pas de comptage exact fabriqué |

## Limites natives confirmées

`requestStop=UNSUPPORTED` dans le moteur historique. La fin de coroutine ne prouve
ni arrêt JNI ni unload. Un chargement natif échoué sans handle laisse la résidence
inconnue et bloque la prochaine admission. Un prompt ou GGUF compatible avec les
vérifications structurelles peut encore être refusé ou faire échouer la bibliothèque.
Le JNI expérimental prepare/consume/discard n'est ni dépendance ni backend du MVP.

La base ne contient **aucune politique thermique/batterie applicative explicite**
(recherche des sources, moteur et coordinator). Ses protections de contexte,
concurrence, mémoire OOM et quarantaine ne doivent pas être confondues avec des
limiteurs matériels. Le MVP ajoute des refus prudents : non-lowRam/lowMemory,
RAM disponible >=max(1Gio,3×poids+256Mio), batterie >20% hors charge, température
batterie connue <40°C et état thermique Android inférieur à MODERATE sur API29+.
API24–28 n'expose pas cette dernière API : la batterie est seulement un proxy.
Ces seuils ne changent ni le système Android, ni les budgets des autres variantes.

Le contrôle est effectué avant chargement/envoi puis surveillé pendant l'opération.
Si le seuil se dégrade, arrêt logique demandé et sortie invalidée, mais le gate
reste fermé jusqu'au retour natif. **Aucune promesse d'arrêt thermique immédiat**
n'est possible avec ce JNI. L'estimation RAM n'est pas une réservation mémoire.

## Isolation et préservation

Package MVP `.mvp001`, stockage noBackupFilesDir/mvp-models et préférences
mvp_options distincts ; ni mise à jour d'un APK existant ni migration SQLite.
Pas d'initialisation d'AppGraph/Whisper/TTS/IDENTITY. MainActivity est désactivée
dans le seul manifeste MVP. Debug/Preview gardent leurs launchers et fonctions.
Aucune modification de leurs règles d'acceptation ou de leur préparation.

Les seuls ajouts aux sources applicatives préexistantes sont le build type et
le constructeur de chemin/validateur. Le reste est dans src/mvp et testMvp.
Les instructions racine reçoivent seulement un complément documentaire.
`tools/check-mvp-001.py` vérifie la base et la forme exacte de ces deux ajouts.
Les anciens gardes à périmètre absolu sont exécutés séparément et leurs refus
restent visibles ; ils ne sont pas assouplis pour masquer ces ajouts autorisés.

## Risques ouverts de l'import

Provenance/licence sont déclarées par l'utilisateur et stockées localement, pas
certifiées automatiquement. Le hash attendu authentifie seulement le fichier
par rapport à cette référence ; il faut une source externe de confiance. Le
contrôle GGUF couvre taille, version2/3, compteurs bornés et hash, pas tous les
offsets/tensors ni la compatibilité d'architecture du moteur natif. Un GGUF
malveillant ou non pris en charge reste un risque de crash/épuisement natif.
Le stockage privé suppose l'absence d'attaquant root/même UID. Aucun déchiffrement
ni téléchargement distant n'est ajouté.

Le fichier est synchronisé puis renommé ; la sélection est écrite via AtomicFile
après synchronisation du manifeste. Les anciennes générations ne sont pas
supprimées. Une mort entre renommage et sélection laisse un fichier vérifié
orphelin réutilisable, jamais un modèle partiel sélectionné. Une coupure physique
ou une panne du filesystem n'est pas reproduite par les tests d'exception ;
aucune promesse de durabilité matérielle ou d'export/restauration Android.
Un fournisseur SAF peut rester bloqué ; l'application garde son gate fermé
jusqu'au retour/erreur et ne prétend pas interrompre ce fournisseur.
