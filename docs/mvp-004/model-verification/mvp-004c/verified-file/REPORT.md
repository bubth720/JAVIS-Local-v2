# CORE — MVP-004C / issue10 : vérification des octets réussie

**MODÈLE VÉRIFIÉ — ANDROID NON QUALIFIÉ.**

Branche `test/mvp-004-native-qualification`, base de reprise
`80d6ad3129fc6f3fe7713f894043c25bbe8ab42f`, vérifiée localement et sur GitHub.
Seule MVP004C est exécutée. Aucun début de MVP005/006/007, aucun laboratoire,
appareil personnel, moteur natif, installation, fusion ou modification sensible.
Les anciens rapports bloqués sont conservés comme historiques ; ce rapport
consigne la reprise réussie du22 octobre2026.

## Accès réseau effectivement observé

Le service de statut cloud reste en network_state=unknown/spec_revision1 et
ne permet pas à lui seul d'attester toute la configuration publiée. La requête
initiale HEAD vers l'URL épinglée, suivie sur `us.aws.cdn.hf.co`, obtient HTTP200
et Content-Length396704416 sans lecture de corps par le script de contrôle.
La requête de téléchargement complète obtient ensuite HTTP200 au même CDN.
C'est la preuve opérationnelle d'accès à cette destination, sans déduire que
tous les domaines ou la politique globale ont été qualifiés. Aucun proxy/TLS
contourné ou désactivé, aucune nouvelle règle/configuration modifiée.
NETWORK-ACCESS.json et DOWNLOAD.json consignent les hôtes, horaires et résultats ;
aucune URL CDN signée ni credential n'est publié.

## Fichier réel et SHA-256 indépendant

Source unique autorisée : Qwen/Qwen3-0.6B-GGUF.
Révision : `1208e45d782fe18602c5eaf10e5758d5b0f24c03`.
Fichier : `Qwen3-0.6B-Q4_K_M.gguf`.

| Contrôle | Résultat mesuré |
|---|---|
| Taille reçue et stat local | **396704416 octets**, exacte |
| SHA-256 attendu | b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e |
| SHA-256 recalculé sur tout le fichier | **b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e** |
| Téléchargement | Complet,24.133s ; fichier temporaire synchronisé puis renommé |
| Calculs indépendants | SHA au fil du téléchargement, sha256sum relisant tous les octets, puis hash complet du vérificateur |
| Vérificateur existant | Exit0, LOCAL_BYTES_VERIFIED_NOT_NATIVE_QUALIFIED |

Le hash réel n'est plus une simple déclaration LFS du fournisseur.
`tools/mvp4_model_probe.py` est inchangé, SHA
`96c13ed014b90755c0ef4585908f0eb151c554692a65f05659b1bafd7fb7d41b`.
GGUF-REPORT.json est sa sortie brute JSON, sans modification du fonctionnement.
La lecture supplémentaire des seuls libellés BOS/EOS est hors ligne, sans moteur.

## Métadonnées effectivement lues

| Champ | Valeur |
|---|---|
| GGUF | Version3, little-endian ;28 métadonnées,310 tenseurs |
| Architecture | qwen3 |
| general.file_type | 15 : Q4_K_M |
| Version de quantification | 2 |
| Types de tenseurs | F32:113 ; Q4_K:168 ; Q6_K:29 |
| Début des données de tenseurs | 5951136 octets |
| Blocs / embeddings | 28 /1024 |
| Attention heads / heads KV | 16 /8 |
| Contexte déclaré du modèle | 40960 ; aucune augmentation du contexte CORE2048 |
| Tokenizer | model=gpt2 ; pre=qwen2 |
| Vocabulaire / types de tokens | 151936 /151936 éléments |
| Merges | 151387 éléments |
| BOS / padding | ID151643, `<\|endoftext\|>` |
| EOS | ID151645, `<\|im_end\|>` |
| add_bos_token | false |
| Template | 4100 octets UTF-8 ; SYSTEM, ChatML, branches tools/enable_thinking présents |

Le nom de pré-tokenizer qwen2 n'est pas une architecture qwen2 : l'architecture
lue est qwen3. Le mélange F32/Q4_K/Q6_K est compatible avec le type de fichier
Q4_K_M attendu ; tous les tenseurs ne sont pas censés être Q4_K.
Empreinte du template :
`57f1fd00f0013a2be96aa79b857391f27e23df5b5f847072b524c897e24d0361`.
Le texte exact correspond au template fournisseur enregistré lors de MVP004.
Les tableaux tokens/types/merges ont été lus et résumés avec leur hash sérialisé,
sans publier leur contenu entier ni aucun poids. Aucun texte tokenisé ou généré.

## Licence, anomalies et comparaison à l'audit précédent

La licence épinglée enregistrée a été consultée, son hash confirmé :
`5de36594c10839788a8c589443a8ef9d8b8d17c65a1b5807206ae037fc36c6bd`.
Apache2.0, copyright2025 Alibaba Cloud. Aucun changement du texte de licence.
L'anomalie de carte demeure : base_model et lien de licence mentionnent1.7B.

Nouvelle anomalie de libellé réellement lue : general.name vaut
« Qwen3 0.6B Instruct Awq » et general.finetune « Instruct-awq ». Le fichier
est néanmoins exactement celui autorisé (taille/SHA), avec ftype15 et types
Q4_K/Q6_K/F32 observés. Le libellé AWQ ne démontre pas une quantification AWQ
active dans ce GGUF. La provenance de conversion de ces étiquettes reste à
éclaircir ; aucune chaîne de conversion ou réattribution n'est inventée.
Aucune divergence critique de taille/SHA/format/architecture/type attendu
n'est détectée. Ce constat concerne les octets et métadonnées seulement.

Les attentes de NATIVE-COMPATIBILITY.md sur GGUF2/3,qwen3,Q4_K_M, vocabulaire
et template sont désormais vérifiées dans le fichier sélectionné. Les risques
natifs restent entièrement ouverts : interprétation du template Jinja par
l'API historique ChatML, comportement de raisonnement/budget128, encodage JNI,
capacité du loader, qualité des sorties, préflight tokens exact, arrêt et release.
Le lecteur ne décode pas mathématiquement les poids et ne qualifie pas le moteur.

## Préservation, localisation et limites

Fichier public temporaire actuellement disponible hors dépôt et hors APK :
`/workspace/model-laboratory/mvp-004c-1208e45/Qwen3-0.6B-Q4_K_M.gguf`.
Sa disponibilité dépend de la durée de vie du stockage cloud, pas d'une promesse
de conservation permanente. Aucun poids copié dans Git, source app, APK, patch
ou archive de livraison. Aucun autre modèle téléchargé.

292 fichiers suivis de la base comparés octet par octet : tous inchangés,
y compris vérificateur, Kotlin, JNI, manifestes, dépendances, signatures,
SQLite, anciens prompts, admission, budgets et fixtures. Aucun GGUF dans
l'index Git ou sous app ; APK diagnostique préexistante inspectée sans mutation,
aucune entrée GGUF. PRESERVATION.json conserve les empreintes et le résultat.
Le dossier utilisateur non suivi docs/identity-002n/ reste préservé.

Contrôles exécutés : accès réseau réel, téléchargement complet, taille/hash
complets, vérificateur sur le vrai fichier, lecture des labels spéciaux et
comparaison du template/licence, préservation de la base/APK. Les439 tests
JVM/Robolectric et13 tests synthétiques des missions antérieures ne sont pas
rejoués ni présentés comme preuve de cette vérification. Aucun build requis
pour cette livraison documentaire. Ni tokenizer natif, Jinja, LLM Linux,
JNI Android, benchmark de ressources, TTS ou appareil n'est exécuté.

## Décision et arrêt

**GO : vérification du fichier public sélectionné uniquement.**
**ANDROID NON QUALIFIÉ ; aucune conversation personnelle validée.**
MVP004C est terminée au niveau fichier. Aucun développement de MVP005/006/007.
La prochaine décision appartient à une mission séparée avec autorisation
explicite d'un laboratoire Android ARM64 ; aucun essai n'est lancé ici.
Même avec un laboratoire autorisé, conserver les signatures, budgets et gates,
et soumettre séparément toute correction JNI/admission sensible.
