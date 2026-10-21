# Interfaces réservées et suite du développement

| Domaine | Point présent | Limite actuelle / raccordement futur |
|---|---|---|
| UI → CORE | MvpConversation / CoreConversationAdapter → JarvisCore | recent/needsModel/chat, historique SQLite, aucune personnalité injectée |
| Modèle | InferenceController → LocalLanguageModel / LlamaLocalEngine | Sélection historique, admission du processus ; pas d'arbitre inter-APK |
| NEXUS | FutureCapability.NEXUS, DisabledExtension available=false | Aucun adaptateur NEXUS. RagEngine/LocalDatabase restent locaux ; contrat de recherche/stockage à concevoir avec consentement |
| SERVICES | FutureCapability.SERVICES, available=false | Aucun exécuteur ni permission ; futur port d'actions exige capacité, validation utilisateur et résultat authentique |
| EVOLUTION | FutureCapability.EVOLUTION, available=false | Aucune boucle d'apprentissage, mise à jour autonome ou stockage implicite ; audit/approbation à spécifier |
| Voix | Mvp2Speech → LocalTts, stockage Whisper privé sans moteur | Pas de pilote Binder ; micro interdit tant que modèle/admission/libération non qualifiés |
| Avatar | ImageView statique avatar_static_v1 | Pas de dépendance ANIMATION, aucun état moteur fabriqué |

Les trois marqueurs ne sont pas des modules fonctionnels et n'offrent aucun
résultat synthétique à l'utilisateur. Aucun IPC CORE–LLM/CORE–VOICE activé.
CORE004D et JNI expérimentale gardent leurs gates distincts.

Suite priorisée : (1) autoriser un GGUF public compatible et un environnement
Android de laboratoire ; prouver génération/annulation/release réelle,
(2) qualifier les appareils après accord, (3) qualifier hors réseau le TTS,
(4) traiter séparément l'admission et la fin native Whisper avant micro,
(5) concevoir export/restauration sans migration destructive,
(6) signature de distribution durable puis revue de durcissement,
(7) contrats des extensions et adaptateurs sur missions distinctes.
