# Rapport de livraison — JARVIS CORE MVP-003

**Décision : CANDIDATE DE DIAGNOSTIC, NON OPÉRATIONNELLE.** Le critère de
conversation JNI/GGUF Android réelle demeure bloqué. Ne pas confondre les
réponses synthétiques des tests avec l'exécution d'un modèle. Aucun appareil
personnel, modèle privé, autre dépôt, APK installé ou donnée utilisateur modifié.

## Références et livraison

Module CORE, dépôt `bubth720/JAVIS-Local-v2`.
Base MVP002 vérifiée : `c7fec293791dc0b30c83d712e0fae0bbf59736a0`.
Branche dédiée : `release/mvp-003-android-candidate`, sans fusion main.
Le commit de fabrication, SHA APK et certificat sont indiqués dans
LOCAL-APK-INFO.json et dans APK-INFO.json **du kit concerné**. La clôture des
preuves peut être un commit documentaire ultérieur au commit de fabrication.
Le build Actions n'est pas déclaré réussi sur la seule base du build local.

## Changements

- `app/build.gradle.kts` : variante mvp3 debug isolée, package `.mvp003`,
  mêmes sources MVP/MVP002, SDK24–35/ARM64 ; aucune dépendance changée.
- `app/src/mvp3/AndroidManifest.xml` : application de diagnostic distincte,
  pas de permission réseau/micro, launcher unique, même backend historique.
- `Mvp2Activity.kt` + nouvelle ressource strings MVP002 : intro utilise le
  label effectif et décrit les limites natives, sans modifier les contrôles UI.
- `Mvp2Speech.kt` : perte observée du prérequis invalide phrase/confirmation.
  Un test reproduit l'échec avant correction et vérifie le refus après retour.
- `Mvp2SpeechTest.kt`, `Mvp3QualificationTest.kt` : régression et parcours
  de qualification simulés API24/25/35 ; aucune modification des anciens tests.
- `tools/check-mvp-003.py`, `mvp3_kit.py`, `test-mvp3-apk.py` : périmètre
  exact, contrôle signé de l'APK et cas négatifs sans installation.
- `.github/workflows/mvp-003.yml` : build/test/inspection sans poids de modèle,
  permission contents:read, upload d'un kit ; aucun déploiement.
- `docs/mvp-003/` : audit, résultats, journaux, provenance APK/modèle, procédures
  d'installation/qualification/retour arrière, inventaire des extensions.
- `AGENTS.md` et `docs/PROJECT_KNOWLEDGE.md` : compléments seulement.

Aucun changement main/Kotlin historique, schéma SQLite, JNI, ModelManager,
InferenceController, SessionCoordinator, admission, budgets, IDENTITY, VOICE ou
fixtures Binder. Les fichiers de la bibliothèque ALPHA candidate ne sont pas
appliqués. Aucune suppression ni migration inter-packages.

## Matrice de fonctionnalité

« Opérationnel en tests logiciels » ne vaut pas essai réel sur téléphone.

| Fonction | État de la candidate | Preuve / limite |
|---|---|---|
| Écran, avatar statique, états/erreurs | Opérationnel en simulation Android | Robolectric API24/25/35 ; affichage physique à qualifier |
| Import GGUF avec taille/hash/espace | Opérationnel sur fixtures synthétiques | Tests store et protections ; aucun vrai GGUF compatible qualifié |
| Chargement/conversation native locale | Partiel : chemin réel présent, NON VALIDÉ | Llama0.1.1 inchangée ; pas de modèle/runtime Android disponible |
| Absence modèle | Opérationnel en tests logiciels | Pas de warmup, refus sans écrire user/assistant |
| Annulation/exclusivité/quarantaine | Opérationnel avec backend simulé | Pas d'arrêt natif historique attesté ; attente de retour conservée |
| Historique, souvenirs confirmés, réglages | Opérationnel en tests logiciels | Persistance/réouverture/confirmation ; pas de mémorisation automatique |
| SQLite documents/passages/RAG CORE | Préservé et testé logiciellement | Pas de parcours d'import PDF dans cette UI, pas de citation native qualifiée |
| Arrière-plan/rotation/réouverture | Opérationnel en simulation Android | Pas de kill OS, coupure ni redémarrage réel attesté |
| TTS local manuel | Partiel | Métadonnées/confirmation/refus testés ; audition et réseau du service non qualifiés |
| Import modèle Whisper | Opérationnel sur fixtures synthétiques | Copie/hash/en-tête ; aucune transcription native attestée |
| Microphone/transcription dans la candidate | Désactivé / absent du parcours utilisateur | RECORD_AUDIO absent ; saisie textuelle disponible |
| Absence réseau de JARVIS | Permission statique contrôlée | Runtime/service TTS/fournisseur SAF non observés ; aucune télémétrie ajoutée |
| NEXUS/SERVICES/EVOLUTION | Absents, marqueurs désactivés | Aucun faux résultat ou adaptateur activé |
| Athéna/IDENTITY, pilotes Binder | Non activés | Gates préservés ; pas d'intégration sensible |
| Export/restauration Android | Absent | Conserver les apps et données ; aucune promesse de restauration |

## Vérifications et preuves

Résultats réellement exécutés : RESULTS.json, TEST-RESULTS.json,
APK-NEGATIVE-TESTS.json et VALIDATION.md. Les sorties échouées avant correction
ou par périmètre historique restent séparées. Les empreintes natives et composants
APK finaux sont dans LOCAL-APK-INFO.json, pas déduites d'un build antérieur.

La signature debug garantit la cohérence de l'APK contrôlée, pas une identité de
production durable ni l'absence de vulnérabilités natives. L'APK debuggable reste
un outil de diagnostic. Aucun runtime Android / application TTS / fournisseur
SAF réel n'a été observé. L'absence INTERNET ne suffit pas à qualifier ces UID
externes ; le runtime 100% hors ligne doit être vérifié avant usage personnel.

## Installation, conservation et suite

GUIDE.md décrit uniquement une installation manuelle **après autorisation**,
avec package distinct, contrôle de certificat et refus de conflit. Pas de
remplacement d'une version existante. Retour arrière : fermer la candidate,
conserver son stockage, reprendre l'ancienne application. Aucun désinstall,
effacement de données ou restauration entre packages demandé.

DEVICE-QUALIFICATION.md définit les profils conditionnels POCO X8 Pro / Redmi
Pad SE, avec les mêmes seuils de refus. Les deux appareils sont NON TESTÉS.
MODEL-PROVENANCE.json demeure vide car aucun modèle n'est sélectionné/autorisé.
EXTENSIONS.md décrit les réserves NEXUS/SERVICES/EVOLUTION sans les implémenter.

Prochaine action : revue de cette candidate, puis autorisation séparée d'un
modèle public compatible et d'un environnement Android de qualification pour
prouver génération native, drainage et libération. Les essais sur appareils
personnels et toute fusion demeurent soumis à validation explicite.

## Bilan logiciel exécuté

Build complet : **SUCCESS**, 4 min54, 303 tâches exécutées.

| Suite JVM/Robolectric | Réussis | Échecs/erreurs/ignorés |
|---|---:|---:|
| CORE historique | 263 | 0/0/0 |
| Fixture Binder simulée | 36 | 0/0/0 |
| Preview | 12 | 0/0/0 |
| MVP001 | 72 | 0/0/0 |
| MVP002, dont régression TTS | 41 | 0/0/0 |
| MVP003 | 15 | 0/0/0 |
| **Total** | **439** | **0/0/0** |

Contrôles APK Linux/build-tools : **3/3 réussis** (candidate signée acceptée,
autre variante refusée, archive altérée refusée). Lint : aucune erreur/fatale ;
avertissements debug32, fixture0, Preview33, MVP00136, MVP00240, MVP00340.
Les avertissements subsistent, la compilation n'est pas décrite « sans avertissement ».

Échecs conservés séparément : test TTS avant correctif (1/1), six anciens
gardes de périmètre, deux premières exécutions du contrôleur APK (2/3 chacune).
Pour celles-ci, l'outil zipalign était bien en succès mais son libellé contient
« succesful » ; ensuite la liste initiale d'assets omettait les 112 ressources
historiques PDFBox. Le contrôleur final exige le code de retour de zipalign et
compare les 114 assets exacts aux deux fichiers applicatifs et à l'AAR PDFBox
2.0.27.0, SHA observé `30277f879cfd571db2a137582c95516a0d4ea6778e945519bc58ca93d57d88c7`.
Ces erreurs du contrôleur ont été corrigées sans changer l'APK ou les dépendances.
Aucune preuve JNI Android ou physique n'en découle.
