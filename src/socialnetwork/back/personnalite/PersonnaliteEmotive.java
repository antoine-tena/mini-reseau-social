package socialnetwork.back.personnalite;

/**
 * Personnalité émotive : hypersensible aux émotions des messages.
 *
 * Profil psychologique :
 *  - Amplifie fortement les messages positifs quand elle est de bonne humeur
 *  - Amplifie fortement les messages négatifs quand elle est de mauvaise humeur
 *  - Contagion émotionnelle : se laisse entraîner par l'ambiance générale
 *  - Mémoire émotionnelle : les messages récents colorent toute perception
 *  - Récupère lentement après un pic émotionnel
 *
 * Analogie humaine : quelqu'un qui rit aux larmes quand tout va bien et qui
 * s'effondre pour peu de chose quand il est en bas.
 */
public class PersonnaliteEmotive extends PersonnaliteHumaine {

    /** Amplitude maximale d'amplification émotionnelle. */
    private final float amplitudeMax;

    /** Seuil à partir duquel un message déclenche une réaction forte. */
    private final float seuilReaction;

    // Constantes prédéfinies pour usage facile
    public static final PersonnaliteEmotive TRES_EMOTIF  = new PersonnaliteEmotive("Très émotif",   1.8f, 0.15f);
    public static final PersonnaliteEmotive EMOTIF       = new PersonnaliteEmotive("Émotif",        1.4f, 0.20f);
    public static final PersonnaliteEmotive SENSIBLE     = new PersonnaliteEmotive("Sensible",      1.1f, 0.25f);

    /**
     * @param nom          nom de la personnalité
     * @param amplitudeMax facteur d'amplification max (1.0 = normale, 2.0 = double)
     * @param resilience   vitesse de récupération émotionnelle
     */
    public PersonnaliteEmotive(String nom, float amplitudeMax, float resilience) {
        super(nom,
              0.1f,             // tendance légèrement positive (les émotifs sont expressifs)
              1.5f,             // très sensible
              resilience,
              0.3f,             // biais de confirmation modéré
              amplitudeMax      // charisme = amplitude (les émotifs influencent les autres)
        );
        this.amplitudeMax   = Math.max(1f, Math.min(3f, amplitudeMax));
        this.seuilReaction  = 2f;
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Amplification basée sur l'humeur courante
        // -------------------------------------------------------
        // Si humeur > 0 : amplifie le positif, atténue le négatif
        // Si humeur < 0 : atténue le positif, amplifie le négatif
        float facteurHumeur = 1f + humeur * amplitudeMax * 0.5f;

        // -------------------------------------------------------
        // 2. Réaction aux messages extrêmes (choc émotionnel)
        // -------------------------------------------------------
        float resultat = bienveillance * facteurHumeur;

        if (bienveillance > seuilReaction) {
            // Message très positif : joie amplifiée
            float surplus = bienveillance - seuilReaction;
            resultat += surplus * amplitudeMax * 0.8f * (1f + humeur * 0.5f);
        } else if (bienveillance < -seuilReaction) {
            // Message très négatif : détresse amplifiée
            float defaut = -bienveillance - seuilReaction;
            resultat -= defaut * amplitudeMax * 0.8f * (1f - humeur * 0.5f);
        }

        // -------------------------------------------------------
        // 3. Contagion émotionnelle (mémoire courte)
        // -------------------------------------------------------
        float tendanceRecente = moyenneRecente();
        resultat += tendanceRecente * 0.4f;

        // -------------------------------------------------------
        // 4. Analyse des mots-clés émotionnels
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        resultat += scoreTexte * sensibilite * 0.6f;

        // -------------------------------------------------------
        // 5. État d'énergie : quand épuisé, les émotions s'émoussent
        // -------------------------------------------------------
        if (energie < 0.3f) {
            float facteurEpuisement = 0.5f + energie; // [0.5, 0.8]
            resultat *= facteurEpuisement;
        }

        // -------------------------------------------------------
        // 6. Ajoute sa propre couleur émotionnelle (humeur → émission)
        // -------------------------------------------------------
        resultat += humeur * charisme * 1.5f;

        return resultat;
    }

    @Override
    public String typePersonnalite() { return "Émotif"; }

    @Override
    public String descriptionCourte() { return "Très réactif émotionnellement"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité émotive : amplifie tout ce qu'il ressent. "
             + "Peut être d'une joie explosive ou d'une tristesse profonde selon l'ambiance. "
             + "Amplitude d'amplification : " + String.format("%.1f", amplitudeMax) + "x. "
             + "Humeur actuelle : " + String.format("%.2f", humeur) + ". "
             + "Energie : " + String.format("%.0f%%", energie * 100);
    }
}
