# CORE-03 — bibliothèque cliente JVM expérimentale

Ce projet autonome n'est inclus dans aucun APK. Il réutilise les vraies API NEXUS
et SERVICES, sans copier leurs moteurs ni modifier leurs dépôts. JDK17,
Gradle8.10.2 et Kotlin2.0.21 ; dépendances de build habituelles, aucun modèle.

Depuis la racine CORE, avec les objets Git exacts déjà présents dans les checkouts :

```sh
python3 tools/prepare-core03-sources.py nexus /tmp/core03-nexus --checkout ../JARVIS-NEXUS
python3 tools/prepare-core03-sources.py services /tmp/core03-services --checkout ../JARVIS-SERVICES
gradle -p integration-lab -PcoreNexusSource=/tmp/core03-nexus -PcoreServicesSource=/tmp/core03-services test run --offline --no-daemon --max-workers=2
```

Le mode `--offline` suppose les dépendances de construction déjà en cache. Les
sources ne sont jamais téléchargées automatiquement ; les snapshots existants
sont vérifiés, jamais écrasés. Versions et empreintes sont figées dans
[CORE03-SOURCES.json](../docs/intensive-2026-10-22/CORE03-SOURCES.json).
Le helper cloud `tools/identity-cloud-env.sh gradle ...` fournit les outils déjà
installés dans cet environnement, sans installer d'APK.

`NexusSearchAdapter` délègue recherche et résolution de chaque citation.
`ContextAssembler` préserve la provenance complète dans un JSON de données non
fiables ; aucun prompt SYSTEM ni génération. Les limites d'octets restent
séparées de l'admission native/exact tokenizer. `ServicesToolAdapter` réutilise
ToolRegistry, Guard et ApprovalGate : lecture sans droit refusée, mutation sans
revue exacte refusée, consommation unique et réponses périmées ignorées.
Les appels `confirmAndExecute` appartiennent exclusivement à un futur hôte CORE
qui recueille l'accord humain. Le CLI utilise une autorisation **synthétique**
sur ses propres fichiers fictifs : aucune décision utilisateur n'est attestée.

Les objets de contrôle du CLI sont des fixtures et ne remplacent ni
SessionCoordinator ni l'admission de production. Une intégration ultérieure doit
qualifier l'autorité de session, sa révocation et leur linéarisation avec les
actions mutantes ; les vérifications de ce client ne prouvent pas ce raccordement.
Annulation/timeout ne libèrent aucun moteur et n'autorisent pas un rejeu automatique.

Limite bloquante : NEXUS est JVM17/JDBC ; SERVICES utilise java.nio.file et n'est
pas qualifié API24. Aucun de ces modules ne doit être ajouté à l'APK historique,
ni le minSdk augmenté pour les faire fonctionner. CORE-03 reste **PARTIAL**.
