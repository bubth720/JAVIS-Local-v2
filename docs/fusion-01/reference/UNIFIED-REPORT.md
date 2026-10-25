# Livraison UNIFIED — FUSION-01 / F0

Date : 25 octobre 2026 UTC. Dépôt `bubth720/JARVIS-UNIFIED`.
Branche neuve : `feat/fusion-01-integration-lock`.
Base : `329f304115c9bc5b6dd6ccc2d68d19bf455b5e6c`.
Le HEAD de livraison est le commit contenant ce rapport, fourni exactement dans
le compte rendu final et vérifiable par `git rev-parse HEAD` sur cette branche.

**F0 livrée sur preuve logicielle locale ; FUSION-01 globale PARTIAL/BLOCKED.**
Aucune clôture globale de l'issue, aucune intégration Android/natif présentée comme
opérationnelle. Cette mission est la tranche propriétaire UNIFIED ; les sous-lots
CORE/LLM/NEXUS/SERVICES/VOICE/INTERFACE/EVOLUTION ne sont pas développés dans leurs
dépôts par cette livraison. Les étapes applicatives restent à leurs propriétaires.

## Code livré, pas uniquement un plan

- `integration-lock.json` et schéma fermé : huit modules, vingt références complètes,
  parents Git et 160 empreintes de fichiers, cinq plateformes, CI liée au SHA exact,
  licences UNKNOWN, conflits et dépendances F0-F8. Une primaire unique par module,
  alternatives conservées mais jamais composées implicitement.
- `tooling/integration` : CLI Python standard `validate/report/plan/compare/proposals`.
  Lecture stricte et bornée ; vérification réelle de fichiers/parents depuis objets
  Git locaux, refus des chemins/symlinks/preuves altérées, distinction validation
  structurelle/provenance. Pas de shell, fetch paresseux/transports Git interdits,
  aucun code tiers exécuté. Les données du worktree ne remplacent pas les blobs Git.
- Refus d'un GO avec conflits, dépendances manquantes et CI inadéquates pour les
  propriétaires nécessaires à la phase. Un blocage LLM/VOICE ne bloque pas
  artificiellement une tranche JVM indépendante ; il n'est pas levé non plus.
- Proposition `jarvis.technical-health/1-draft` fermée sans texte personnel ni
  autorité, exemples positifs/négatifs et tests. **Non approuvée, non déployée**,
  aucune modification des schémas UNIFIED v1 ou de l'observateur EVOLUTION.
- Audits et snapshot des branches distantes, architecture/ownership/menaces,
  correspondance des contrats/quotas, matrice contrat/code/JVM/émulateur/appareil/
  production/merge et scénarios F8. README et CI étendus, anciens travaux préservés.

## Résultats exécutés ici

Linux x86_64, Python3. Bibliothèque standard pour les nouveaux outils ; la
régression de sauvegarde réutilise `cryptography` déjà présente. TLS/proxy conservés.

```sh
python3 -m unittest discover -s tests -q
python3 -m unittest discover -s registry/tests -q
python3 -m unittest discover -s tooling/campaign/tests -q
python3 -m unittest discover -s backup_prototype/tests -q
python3 -m unittest discover -s tooling/integration/tests -q
python3 scripts/validate_unified.py
python3 -m tooling.integration.cli validate --repositories /chemin/local/repositories.json
python3 -m tooling.integration.cli report
python3 -m tooling.integration.cli plan
python3 -m tooling.integration.cli compare --against integration-lock.json
python3 -m tooling.integration.cli proposals
git diff --check
```

**147 tests distincts réussis, zéro échec/erreur/saut** : 41 audit/contrats +23
registre +32 campagne +30 sauvegarde +21 intégration/BOM/proposition. Les vingt
premiers tests de la nouvelle suite ont été exécutés avant ajout du cas sur
l'indépendance des gates, puis la suite finale de 21 a été réexécutée ; les
exécutions répétées ne sont pas additionnées.

Le validateur historique accepte cinq exemples et rejette cinq invalides sur
cinq contrats/neuf dépôts historiques. Le périmètre FUSION est huit dépôts, sans
suppression du neuvième historique `JARVIS-INTERFACE-`.
Le lecteur Git FUSION vérifie réellement **20 références, 160 fichiers et 5
schémas gelés** ; ce ne sont pas 185 tests unitaires additionnels. Les exemples
techniques proposés passent/refusent comme prévu, sans approbation ni connexion.
Les références distantes des vingt candidats concordent au snapshot daté.

La vérification des sources se reproduit avec une map JSON locale des huit noms
GitHub exacts vers leurs snapshots Git, tous objets présents. Aucun chemin privé
de configuration n'est requis dans Git. Les 160 chemins/SHA sont dans le verrou ;
les URL immuables se construisent `https://github.com/<repository>/blob/<commit>/<path>`.
`tooling/integration/README.md` expose les limites et commandes complètes.

Les tests utilisent de vrais objets Git temporaires pour preuve/altération, sans
modifier de branche source ni appeler le réseau. Autres négatifs : faux SHA CI,
propriétaire/dépôt incorrect, branch/path unsafe, JSON dupliqué/NaN, cycles,
préconditions, champ diagnostic personnel, métrique booléenne/hors bornes.

## Différences réelles et CI observée

- CORE03 préserve MEM02/ID02 mais utilise encore NEX777/SERV3de ; la paire API24
  candidate NEX374/SERVf000 n'est pas consommée. F1 nouvelle composition NOT_RUN.
- NEXUS final v4/JDBC et port v3/API24 divergent ; SERVICES final17 outils et port
  search/CSV divergent ; les contrats ArchiveReader et l'ABI JVM exigent revue.
- CI NEX374 [40115049468](https://github.com/bubth720/JARVIS-NEXUS/actions/runs/40115049468)
  FAILURE : setup Android échoue, pas de nouvelle qualification Android distante.
- CI SERVf000 [40115305244](https://github.com/bubth720/JARVIS-SERVICES/actions/runs/40115305244)
  FAILURE : étape des régressions API24 échoue ; message précis anonyme indisponible.
- UI db459 et EVOL c018 ont une CI exacte réussie observée. VOICE et SERVICES final
  ont un parent de code vert, pas une preuve héritée automatiquement à leur HEAD.
- CORE/LLM : CI fiable non extraite, UNKNOWN. Les comptes applicatifs anciens et
  certains essais émulateurs publiés sont relus et datés, **pas relancés ici**.

Sources/limites détaillées : [audits](README.md) et JSON sous `evidence/`.
Ces nouveaux constats CI complètent la livraison locale des ports ; ils ne
réécrivent ni ne nient les résultats locaux historiques de NEX-06A/SERV-04A.

## Publication, PR et restrictions

Publication demandée uniquement sur la branche de travail, sans force ; le SHA
remote et l'état de `main` seront vérifiés et fournis dans le compte rendu final.
Main de référence : `e4435f4bd60c5fdc5595c411cc8cab9da7ef020b`.
**PR brouillon non créée à cette étape : accès API GitHub refusé.** Le diagnostic
réel `gh api user --silent` renvoie `Get "https://api.github.com/user": Forbidden`.
`gh auth status` signale un jeton invalide ; cela n'établit pas sa cause en présence
du refus réseau. Aucun jeton affiché, remplacé ni connexion interactive entreprise.
Le réseau impose son proxy, et `api.github.com` n'est pas une destination autorisée
par le snapshot de politique courant. Git HTTPS et API sont des accès distincts.
La préparation de PR est fournie dans [PR_DRAFT.md](PR_DRAFT.md) ; aucune tentative
risquée ou contournement, aucun numéro de PR inventé. Réouvrir l'accès API via la
configuration autorisée avant création, ou utiliser l'écran Compare de GitHub.

Aucun autre dépôt source modifié ; WIP/branches conservés. Aucun merge, installation,
activation d'Athéna/IPC/JNI, modèle téléchargé, seuil matériel modifié, donnée
personnelle ajoutée/migrée. Les tests des autres dépôts, builds Android, nouvelles
mesures physiques et scènes E2E n'ont pas été exécutés par cette tranche.

## Prochaine action

Recevoir F0, puis lancer la tranche propriétaire CORE C0/C1 pour rejouer et porter
le vrai laboratoire JVM, en parallèle de la correction des CI et qualification
FTS5 dans NEXUS/SERVICES. Conserver F2 bloquée jusqu'aux nouvelles preuves API24/25.
F3/F5 demandent toujours preuves natives et autorisations sensibles séparées.
Revoir le schéma technique proposé avant toute intégration EVOLUTION. Résoudre
licences projet/actifs avant distribution ; ne pas marquer FUSION-01 terminée.
