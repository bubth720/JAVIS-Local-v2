# Contrat coordonné avec VOICE

Source005K inchangée : `005k.1-proposed`, bundleSHA256 `9c018ecfee1b62eb0c3cea36b580edb9edcbda43f3772135b01191018d5eacad`, dépôtVOICE base005K6a86d847ff57fd9f695ee87eb5bff21c0c8862ca, mission pilote4479afb0c5604bb82ae8d84dd0267c4d6e7cb953.

Extension distincte **de fixture uniquement** : `voice.bridge.fixture/1`, SHA256 **cfbdff471ed0403113803a0f4c86ec75c57f723cc828f5af3c13fb3ef8042cf2**. Copies exactes de CONTRACT.md, IVoicePilotFixture.aidl et PIN.json sous `fixture-contract/`, identiques aux fichiers de `JARVIS-VOICE/voiceBridgePilot`. Version, canonisation et empreintes individuelles dans PIN.json. Les schémas UNIFIED et le bundle005K restent inchangés.

Cet alignement local des deux branches n'est ni une approbation rétroactive d'UNIFIED, ni une revue indépendante, ni un E2E. Le contrat V1 n'autorise aucun moteur : identité/permission et consentement local sont nécessaires, jamais suffisants pour une admission native. hello annonce global_admission_available=false. Une référence de bail envoyée par le client ne crée aucune autorité.

Le client de laboratoire `com.jarvis.core.voicepilot.fixture` est un **APK de test dans le dépôtVOICE**, pas une modification du CORE installé. CORE C3 est arrêté par C2 ; les AIDL présents ici sont documentaires et ne sont compilés/intégrés dans aucun module CORE. Aucun service, permission Binder, profil B, PCM, JVFP1 ou archive n'entre dans CORE.

V2/V3 nécessitent un complément négocié pour le profil exact, la liaison bail/session/réponse, les preuves fraîches, la révocation et la récupération. Ne pas utiliser la fixture comme endpoint de synthèse, ne pas introduire les champs manquants dans UNIFIEDv1.

Vérification croisée hors ligne :

```sh
python3 tools/check-core-voice-pilot.py --voice-root ../JARVIS-VOICE
```
