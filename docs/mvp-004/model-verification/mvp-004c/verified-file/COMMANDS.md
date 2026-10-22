# Reproduction des contrôles — MVP004C

La base de cette exécution est80d6ad3129fc6f3fe7713f894043c25bbe8ab42f.
Les sources sont contrôlées avant et après les lectures. Aucun outil de modèle
n'est modifié. La licence et l'audit natif épinglés sont lus en local.

1. Appel environment_status et lecture du fichier réseau local : état global
   unknown. Requête initiale HEAD via urllib.request au resolve HTTPS épinglé ;
   suivi officiel de redirection et HTTP200 sur us.aws.cdn.hf.co. Aucun corps
   lu par ce script. Voir NETWORK-ACCESS.json.
2. Téléchargement Python urllib.request de cette seule URL :

```text
https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/1208e45d782fe18602c5eaf10e5758d5b0f24c03/Qwen3-0.6B-Q4_K_M.gguf
```

Le script éphémère cloud suit uniquement HTTPS, conserve proxy/CA, exige HTTP200
et Content-Length attendu, lit par blocs1Mio avec une borne taille+1, écrit un
.part créé exclusivement hors dépôt, calcule SHA256, synchronise et renomme
seulement après taille/hash conformes. Aucun téléchargement de revision main,
pas de substitution, d'installation ou de chargement. Les paramètres et horaires
exécutés sont dans DOWNLOAD.json ; les URLs CDN signées ne sont pas conservées.

3. Commandes effectivement exécutées sur le fichier complet :

```sh
stat -c '%s' /workspace/model-laboratory/mvp-004c-1208e45/Qwen3-0.6B-Q4_K_M.gguf
sha256sum /workspace/model-laboratory/mvp-004c-1208e45/Qwen3-0.6B-Q4_K_M.gguf
python3 tools/mvp4_model_probe.py \
 --model /workspace/model-laboratory/mvp-004c-1208e45/Qwen3-0.6B-Q4_K_M.gguf \
 --report docs/mvp-004/model-verification/mvp-004c/verified-file/GGUF-REPORT.json
```

Sorties :396704416, empreinte attendue exacte, exit0 et
LOCAL_BYTES_VERIFIED_NOT_NATIVE_QUALIFIED. Le vérificateur refait lui-même un
hash complet et vérifie l'identité/stabilité du fichier durant sa lecture.

4. Lecture Python supplémentaire : import du Reader existant sans modification,
   lecture des clés GGUF et uniquement des libellés tokens151643/151645 ; aucune
   lecture des valeurs de tenseurs, aucune tokenisation de prompt. Comparaison
   exacte du template au fournisseur enregistré et SHA du texte UTF-8. Résultat
   TOKENIZER-TEMPLATE.json. Aucun exécutable natif lancé.
5. Comparaison Git de tous les292 fichiers de base au disque, empreinte du
   vérificateur/licence, absence de GGUF de l'index et de app, inspection ZIP
   de l'APK historique sans la modifier ; sorties dans PRESERVATION.json.

Le script temporaire de téléchargement et les vérifications auxiliaires ne sont
pas des modifications CORE. Le rapport de métadonnées ne prouve ni la lecture
de poids par llama.cpp ni le rendu natif de SYSTEM. Ne pas rejouer le téléchargement
pour consulter les preuves : elles sont publiées sans aucun poids du modèle.
