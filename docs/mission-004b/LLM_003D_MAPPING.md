# Correspondance CORE 004B ↔ LLM 003D / UNIFIED D4-02

## Statut documentaire

Les versions officielles de D4-02 (décisions O1, O2/P2, O4) et LLM 003D ne
sont pas présentes dans les fichiers disponibles. Les références ont été
demandées ; **la correspondance ci-dessous est une liste de points à comparer,
pas une certification de conformité au texte absent**. Aucun dépôt LLM ou
UNIFIED n'a été modifié ni aucun moteur externe contacté.

P2 est appliquée selon l'instruction explicite de la mission : système
configurable séparé ; refus si le backend ne peut pas le conserver séparément ;
aucun repli silencieux en message utilisateur. Les contenus officiels d'O1 et
O4 ne sont pas déduits de leurs seuls numéros.

## Table de rapprochement à valider

| Besoin de la mission CORE | Réalisation locale 004B | Vérification requise dans LLM 003D / D4-02 |
|---|---|---|
| Choix exclusif avant préchauffage | Factory, sélection et admission partagée dans le processus, historique par défaut, simulateur injecté inactif en production | Noms/modes officiels, propriétaire de la sélection, coordination globale éventuelle |
| Système distinct, stratégie P2 | `InferenceRequest.systemPrompt` → argument système historique ; capacité requise | Support réel du backend/format de chat et refus officiel en cas d'incompatibilité |
| Question et contexte distincts | Champs `question` et `context` ; rendu uniquement à la frontière historique | Schéma exact, ordre et règles de confiance/normalisation du protocole cible |
| Sources | IDs document/passage, nom et index des seuls passages inclus | Format officiel, pages, provenance et politique de citations ; aucun transfert actuel |
| Limites | contextSize, maxTokens, budget conservateur UTF-8 + réserve | Unité de budget/tokenizer, réserves du template et négociation des capacités |
| Session/requête | UUID du Core, UUID par requête, epoch locale et IDs de réponse vérifiés | Durée de session cible, règles de rejeu, réponses partielles et erreurs tardives |
| Préparation | Méthode prepare et état distinct de génération | Signaux officiels de disponibilité et échecs de chargement |
| Arrêt demandé | StopResult REQUESTED/UNSUPPORTED/NOT_RUNNING et stopRequested | Nom et sémantique des accusés d'arrêt ; pas une preuve de native_done |
| Fin effective | nativeDone sur résultat/erreur attestée, drainage local | Définition native exacte, acquittement réel et sort des calculs sans arrêt natif |
| Déchargement | CONFIRMED/UNKNOWN/FAILED ; modelUnloaded distinct | Preuve native de release, mémoire réclamée et processus propriétaire |
| Échec/inconnu | Quarantaine et refus de tout nouveau backend, sans fallback | Politique de récupération officielle, possibilité de redémarrage et autorité de décision |
| Retour historique | unload confirmé puis sélection explicite et préparation | Conditions officielles, consentement et preuve d'exclusivité après délégation |

Les noms Kotlin et la version **locale 1** ne sont pas le schéma wire d'un
service LLM 003D. Ils ne fixent ni AIDL, Binder, JSON, découverte de paquet,
permissions inter-APK, version distante, streaming ou transport réseau.

## Conditions pour déclarer une correspondance validée

1. Fournir les textes exacts et leur révision ; résoudre les divergences dans
   une mission contrôlée, sans renommer un contrat pour masquer une différence.
2. Établir pour chaque champ et refus une équivalence explicite dans les deux
   sens. Une capacité absente implique un refus, pas une transformation silencieuse.
3. Qualifier natif et gestion mémoire sur appareil. Les simulateurs prouvent
   l'ordonnancement local, pas le comportement d'un autre APK ou ses ressources.
4. Autoriser séparément un futur adaptateur et sa coordination entre processus.
   Garder jusque-là le moteur historique et les données locales en place.

## Garantie locale complémentaire

L'admission ProcessInferenceAdmission est partagée par les factories de
production et les instances historiques, sans dépendre de la durée de vie d'un
contrôleur. Elle interdit un autre backend tant que le propriétaire précédent
n'a pas confirmé son déchargement. En tests, une admission privée est injectée
pour chaque processus simulé ; le singleton de production n'est jamais réinitialisé.
Cette garantie ne peut pas arbitrer la RAM ou l'exécution de JARVIS LLM dans un
autre processus. Un arbitre interprocessus, ses permissions, ses attestations et
sa politique mémoire/batterie/température restent à spécifier et qualifier ;
aucun contournement ni transport ne sont ajoutés ici.
