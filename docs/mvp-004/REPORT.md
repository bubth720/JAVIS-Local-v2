# JARVIS CORE — MVP-004

**Décision : NON QUALIFIÉ / NO-GO pour l'usage conversationnel personnel.**
Aucune génération JNI/GGUF Android réelle ni réponse native n'a été obtenue.
Il s'agit des préparatifs autorisés, pas d'une conversation remplacée par une
simulation. Aucun modèle, APK personnel ou donnée utilisateur installé/modifié.

## Références et périmètre

Dépôt bubth720/JAVIS-Local-v2. Branche `test/mvp-004-native-qualification`,
base distante vérifiée `release/mvp-003-android-candidate`, commit exact
`90215258ad3914cd6576971334ecb8ca48db74ff`. Les commits de livraison sont
consultables dans l'historique de cette branche ; l'APK locale éventuelle est
liée au commit de fabrication dans LOCAL-APK-INFO.json, distinct du commit
ultérieur de preuves. Aucune fusion main ni autre dépôt modifié.

Tous les fichiers applicatifs, manifestes, configurations Gradle, modèles,
protections de ressources, signatures configurées et dépendances sont inchangés.
Aucun défaut de production démontré par une exécution native ne justifie de
correction : aucune correction JNI, admission ou runtime appliquée. Seules des
connaissances, preuves et outils de qualification hors ligne sont ajoutés.

## Qualification du candidat

| Critère | Résultat réel |
|---|---|
| Dépôt/révision/fichier | Vérifiés via API du fournisseur à la révision épinglée |
| Taille/LFS SHA annoncé | 396704416 octets ; b0638f…81eea0e, identique à la demande |
| Licence | LICENSE épinglé lu, Apache2.0/copyright2025 Alibaba Cloud ; hash conservé |
| Cohérence de carte | Anomalie : lien de licence/base_model mentionnent1.7B |
| SHA calculé sur les octets GGUF | NON EXÉCUTÉ : téléchargement non autorisé explicitement à la clôture |
| Architecture/quantification du fichier obtenu | NON EXÉCUTÉ : aucun GGUF obtenu |
| Architecture selon fournisseur | qwen3 ; déclaration distincte d'une lecture du fichier |
| Support moteur statique | AAR0.1.1 vérifiée ; marqueur qwen3/symboles natifs présents |
| Chargement et tokenisation natifs | NON EXÉCUTÉS |
| Template SYSTEM | Chemin séparé présent ; source de référence ChatML, Jinja complet non garanti |
| Deux questions françaises nouvelles | Protocole préparé ; aucune réponse ni réussite fabriquée |
| Annulation/fin/release réelles | NON EXÉCUTÉES ; stop historique UNSUPPORTED, blocage conservé |
| Mesures Android RAM/batterie/thermique | NON EXÉCUTÉES ; seuls les seuils théoriques ont été calculés |

MODEL-PROVENANCE.json et NATIVE-COMPATIBILITY.md distinguent les déclarations
HF, les sources et symboles lus, les octets effectivement vérifiés et les
validations manquantes. Le SHA attendu n'est jamais présenté comme un hash
indépendamment calculé du modèle.

## CI MVP003 examinée

API GitHub réellement consultée : run39634143182 **completed/success**, source
`faf0d4fde9103b039ffd208361ccfccfbe603658`.
https://github.com/bubth720/JAVIS-Local-v2/actions/runs/39634143182
Artefact JARVIS-MVP-003-Android12360079506 présent/non expiré à l'observation.
Digest ZIP fourni par GitHub enregistré. Son téléchargement redirige vers Azure,
refusé HTTP403 par le proxy ; ZIP/empreinte/certificat APK CI non vérifiés ici.
Voir MVP003-CI.json. Ne pas assimiler ce succès de tests logiciels à du JNI réel,
ni employer le certificat local pour une éventuelle APK produite sur Actions.

## Tests réellement exécutés pendant MVP004

- **439/439** tests JVM/Robolectric historiques/adaptateurs : CORE263,
  fixture Binder simulée36, Preview12, MVP00172, MVP00241, MVP00315.
  Zéro échec, erreur ou ignoré. Données synthétiques, aucun moteur LLM réel.
- **13/13** tests Python du lecteur GGUF : bornes/métadonnées, types, UTF-8,
  tableaux tokenizer, descripteurs, refus de taille et protection du fichier
  contre l'alias de rapport. Aucun tokenizer ou modèle exécuté.
- **3/3** contrôles APK avec vrais build-tools : candidate signée acceptée,
  autre variante refusée, archive altérée refusée. Aucun appareil/installation.
- Build complet **SUCCESS**,5m5,303 tâches exécutées. Lint aucune erreur/fatale ;
  avertissements debug32,fixture0,Preview33,MVP00136,MVP00240,MVP00340.
- Comparaison exacte à la base via tools/check-mvp-004.py ; fichiers
  applicatifs identiques. Les anciens gardes de périmètre restent inchangés.

Résultats : RESULTS.json / TEST-RESULTS.json / APK-CONTROLS.json. Journaux
normalisés seulement sur espaces/lignes finales ; hashes originaux/conservés
dans LOG-SHA256.json. Les tests et hashes anciens ne sont pas réétiquetés comme
une nouvelle preuve native. Pas de test instrumenté, Linux LLM, émulateur ou
appareil physique exécuté ; aucun benchmark réel de ressources.

## Fichiers livrés

- tools/mvp4_model_probe.py : vérificateur strictement local, sans téléchargement
  ou chargement ; taille/SHA exacts puis examen borné GGUF. Garde le modèle hors
  dépôt. Refuse de remplacer le modèle par son propre rapport.
- tools/tests/test_mvp4_model_probe.py : tests de métadonnées synthétiques.
- tools/check-mvp-004.py : préservation exacte de la base.
- docs/mvp-004/ : licence/provenance, audit statique, état CI/environnement,
  protocole Android/Xiaomi, gabarit de preuves, validation et journaux.
- AGENTS.md / docs/PROJECT_KNOWLEDGE.md : compléments de connaissances seulement.

## Blocages, risques et prochaine action

Aucun Android ARM64 exécutable disponible : hôte x86_64, pas d'émulateur/image
système ni /dev/kvm. Aucun appareil personnel consulté ni installation autorisée.
L'autorisation du téléchargement cloud du modèle a été demandée séparément,
aucune réponse affirmative reçue à la clôture. Ne pas déduire une autorisation
du seul nom de fichier proposé. Son obtention ne résoudrait pas le blocage Android.

Risques ouverts : template simplifié, raisonnement consommant le budget128,
encodage Modified UTF-8 JNI pour hors-BMP/NUL, absence de préflight tokenizer exact,
stop JNI indisponible et libération non observée, capteurs/budgets réels à mesurer.
Pas de relèvement de limites ni correctif sensible implicite.

Prochaine action : autoriser précisément le GGUF cloud puis vérifier ses octets,
et fournir/autoriser un laboratoire Android ARM64. Exécuter ANDROID-PROTOCOL.md,
consigner deux réponses publiques et les retours JNI/ressources. Toute correction
JNI/admission/signature devra recevoir sa propre revue et autorisation.
POCO et Redmi restent NON TESTÉS ; aucune installation personnelle automatique.
Le retour arrière conserve applications et stockages, sans désinstallation ni
migration. Voir INSTALL-ROLLBACK.md. Le statut demeure NO-GO jusqu'aux preuves.

Le garde MVP003, rejoué après indexation des nouveaux fichiers, échoue sur son
périmètre de nouveaux fichiers (LEGACY-GUARD.json). Il reste inchangé ; le
garde MVP004 compare toute la base et confirme253 fichiers inchangés plus
deux documents uniquement complétés. Cet échec de périmètre n’est pas masqué
ni compté comme un test réussi. La licence Qwen conserve ses fins de ligne CRLF
exactes ; contrôle Git des espaces avec cr-at-eol et SHA original conservé.
