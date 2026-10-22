# JARVIS CORE — MVP-004B : téléchargement bloqué

**Statut : BLOQUÉ — MODÈLE NON VÉRIFIÉ ; ANDROID NON QUALIFIÉ.**
Le statut « MODÈLE VÉRIFIÉ » n'est pas acquis. L'autorisation explicite est bien
accordée ; le blocage est réseau, pas un manque d'autorisation utilisateur.

## Référence et tentative

Branche `test/mvp-004-native-qualification`, référence exacte
`69b65685f0c721c313a1d0cde1eed4379834ebe6`. HEAD, FETCH_HEAD et référence
GitHub vérifiés identiques avant le téléchargement. Seul le dossier documentaire
`docs/mvp-004/model-verification/` est ajouté. Les rapports antérieurs restent
les preuves de leur session et ne sont pas réécrits rétroactivement.

Source HTTPS demandée, sans révision flottante :

https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/1208e45d782fe18602c5eaf10e5758d5b0f24c03/Qwen3-0.6B-Q4_K_M.gguf

Une seule tentative le22 octobre2026, via urllib.request et proxy hérités,
validation TLS conservée. La redirection HTTPS vers `us.aws.cdn.hf.co` est
observée ; la connexion CONNECT est refusée par le proxy :
`<urlopen error Tunnel connection failed: 403 Forbidden>`.
Ce domaine n'apparaît pas dans les règles d'egress du fichier de politique
sélectionné. Le statut cloud ne confirme pas l'état d'application de la politique
(state=unknown) ; l'erreur de proxy est l'observation effective.

**0 octet reçu.** Aucun fichier GGUF, même partiel, créé. Aucun retry, contournement
de proxy, autre source, révision ou modèle. La tentative est arrêtée conformément
à la consigne d'arrêt en cas d'échec. DOWNLOAD.json conserve les horaires,
source exacte et hôtes observés ; aucune URL signée de CDN ou valeur de secret
n'est enregistrée.

Dossier temporaire prévu :
`/workspace/model-laboratory/mvp-004b-1208e45/`.
Il contient seulement le journal download.json ; **le modèle n'y est pas présent**.
Aucun lien de téléchargement des poids ni poids n'est publié dans GitHub.

## Résultats, sans confusion avec les déclarations du fournisseur

| Contrôle | Résultat de cette mission |
|---|---|
| Référence de branche/commit | Vérifiée |
| Téléchargement complet | ÉCHEC : HTTP403 proxy après redirection |
| Taille attendue | 396704416 octets |
| Taille réelle du GGUF | Indisponible : aucun fichier obtenu |
| SHA-256 attendu | b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e |
| SHA-256 recalculé du GGUF | NON EXÉCUTÉ ; aucune valeur fabriquée |
| tools/mvp4_model_probe.py | Lu et préservé, NON EXÉCUTÉ sur un modèle absent |
| GGUF/version/architecture/qwen3 | NON LUS sur fichier réel |
| general.file_type15/Q4_K_M/types de tenseurs | NON LUS sur fichier réel |
| Vocabulaire/tokenizer/template | NON LUS sur fichier réel |
| Licence enregistrée | Consultée et hash vérifié, Apache2.0 |
| Inférence/native/Android/personnel | NON EXÉCUTÉS, aucun appareil utilisé |

La copie épinglée QWEN-LICENSE.txt est inchangée : SHA-256
`5de36594c10839788a8c589443a8ef9d8b8d17c65a1b5807206ae037fc36c6bd`.
Cette vérification porte sur la licence enregistrée, pas sur les poids absents.
L'anomalie de carte demeure : base_model et lien de licence mentionnent
Qwen3-1.7B. Aucune substitution par un modèle1.7B n'est effectuée.

## Comparaison à NATIVE-COMPATIBILITY.md

Les hypothèses restent à vérifier indépendamment : format GGUF2/3, architecture
qwen3, file_type15, types Q4_K avec éventuel mélange Q6_K/F32, vocabulaire,
paramètres et template. Les métadonnées déclarées dans MODEL-PROVENANCE.json
lors de MVP004 ne sont pas une inspection des octets. Aucune incompatibilité
du modèle n'est démontrée ; aucun chargement réussi n'est démontré non plus.

Les risques déjà documentés restent ouverts : template Jinja/ChatML, raisonnement
et budget128, Modified UTF-8 JNI, préflight exact absent et stop UNSUPPORTED.
Aucune mesure de ressources ni compatibilité native Android ne découle de cette
mission. Aucun changement de limites ni correction JNI/admission/signature.

## Préservation et contrôles exécutés

282 fichiers suivis de la référence comparés octet par octet : **tous inchangés**,
y compris vérificateur, code applicatif, signatures configurées, dépendances,
manifestes, budgets, données de référence et anciens rapports. Empreintes des
quatre fichiers prioritaires dans PRESERVATION.json.

Aucun GGUF dans les sources app ; APK diagnostique locale existante uniquement
lue, empreinte conservée et absence de GGUF inspectée. Aucun build, signature,
installation, téléchargement d'APK ou exécution native. Le modèle n'ayant pas été
obtenu, aucun de ses poids ne peut être ajouté au commit de cette mission.
Le dossier utilisateur préexistant docs/identity-002n/ reste non suivi et intact.

Aucun test Android/JVM ou test synthétique n'est rejoué : aucune modification de
code, aucun nouveau modèle à valider. Les439 tests de MVP004 sont historiques,
non exécutés dans MVP004B. Les contrôles réellement exécutés sont ceux de
référence Git, téléchargement échoué, licence et préservation documentaire.

## Déblocage et prochaine action

Dans les paramètres réseau de l'environnement cloud, faire autoriser
`us.aws.cdn.hf.co` en préservant les domaines existants. Ne pas désactiver TLS,
retirer le proxy ou fournir un token personnel pour contourner ce refus.
Aucune configuration réseau n'est modifiée par cette mission. NETWORK.json
conserve le besoin précis ; une configuration souhaitée ne prouve pas son
application effective.

Après correction effective du réseau, reprendre **le même fichier et la même
révision**, dans le dossier temporaire hors dépôt. L'autorisation de téléchargement
accordée dans MVP004B reste valable : pas de nouvelle demande pour ce périmètre.
Vérifier alors taille et SHA complets avant tout examen, puis lancer le
vérificateur inchangé selon COMMANDS.md. Toute divergence critique impose arrêt,
sans modèle de remplacement. La qualification JNI Android restera une mission
séparée sur ARM64 autorisé, même si la vérification du fichier réussit ensuite.

Aucune fusion main, installation personnelle, publication de poids ou modification
applicative. Le commit de clôture de cette branche n'ajoute que ces preuves.
