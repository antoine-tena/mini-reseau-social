package socialnetwork.back.personnalite;

import java.util.Random;

/**
 * Personnalité influenceur : maximise l'engagement et la portée.
 *
 * Profil psychologique :
 *  - Transforme chaque message pour maximiser son impact émotionnel
 *  - Stratégie de contenu : positif → euphorique, négatif → dramatique
 *  - Ego et narcissisme : ajoute une "signature" personnelle à chaque message
 *  - Algorithme mental : calcule intuitivement ce qui va "buzzin"
 *  - Sensible aux tendances : amplifie les messages sur des sujets populaires
 *  - Burn-out possible : pic d'activité suivi d'effondrements
 *  - Compétitivité : amplifie davantage quand il perçoit d'autres influenceurs actifs
 *
 * Analogie humaine : le youtubeur/tiktoker qui optimise pour les views,
 * qui "clickbait" même quand ce n'est pas nécessaire, et qui vit pour les likes.
 */
public class PersonnaliteInfluenceur extends PersonnaliteHumaine {

    /** Facteur d'amplification de base pour créer l'"engagement". */
    private final float facteurEngagement;

    /** Seuil de bienveillance à partir duquel passe en mode "dramatique". */
    private final float seuilDrame;

    /** Fréquence aléatoire pour les "bursts" de créativité. */
    private final double probaCreativiteBurst;

    /** Générateur aléatoire pour simuler l'imprévisibilité créative. */
    private final Random rng = new Random();

    /** Compteur de "likes" simulés (messages très bien reçus en retour). */
    private int likesAccumules = 0;

    /** Seuil de fatigue du buzz (burn-out). */
    private static final int SEUIL_BURNOUT = 50;

    // Constantes prédéfinies
    public static final PersonnaliteInfluenceur MEGA_INFLUENCEUR = new PersonnaliteInfluenceur("Mega-influenceur", 2.2f, 3f, 0.15f);
    public static final PersonnaliteInfluenceur INFLUENCEUR      = new PersonnaliteInfluenceur("Influenceur",      1.7f, 4f, 0.10f);
    public static final PersonnaliteInfluenceur MICRO_INFLUENCEUR = new PersonnaliteInfluenceur("Micro-influenceur", 1.3f, 5f, 0.07f);

    /**
     * @param nom                  nom de la personnalité
     * @param facteurEngagement    amplification de base [1, 3]
     * @param seuilDrame           seuil de bienveillance pour le mode dramatique
     * @param probaCreativiteBurst probabilité de burst créatif par message [0, 0.3]
     */
    public PersonnaliteInfluenceur(String nom, float facteurEngagement,
                                    float seuilDrame, double probaCreativiteBurst) {
        super(nom,
              0.4f,   // tendance positive (les influenceurs projettent le positif)
              1.0f,   // très sensible aux tendances
              0.3f,   // récupère lentement (burn-out progressif)
              0.5f,   // biais de confirmation modéré (cherche la validation)
              2.0f    // charisme maximal
        );
        this.facteurEngagement    = Math.max(1f, Math.min(3f, facteurEngagement));
        this.seuilDrame           = Math.max(1f, seuilDrame);
        this.probaCreativiteBurst = Math.max(0, Math.min(0.3, probaCreativiteBurst));
    }

    @Override
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        super.mettreAJourEtat(bienveillance, contenu);

        // Si message positif reçu → l'influenceur se sent validé
        if (bienveillance > 3f) {
            likesAccumules++;
            humeur = Math.min(1f, humeur + 0.05f);
        }

        // Burn-out : trop de messages sans validation
        if (nbMessagesRecus > SEUIL_BURNOUT && nbMessagesRecus % 10 == 0) {
            energie = Math.max(0f, energie - 0.05f);
        }
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Vérification du burn-out
        // -------------------------------------------------------
        if (energie < 0.15f) {
            // En burn-out : poste n'importe quoi, très incohérent
            float chaos = (rng.nextFloat() - 0.5f) * 8f;
            return bienveillance * 0.5f + chaos;
        }

        // -------------------------------------------------------
        // 2. Stratégie de contenu basée sur la valence
        // -------------------------------------------------------
        float resultat;

        if (bienveillance >= 0) {
            // Positif → amplifie pour créer l'euphorie ("amazing!", "incredible!")
            resultat = bienveillance * facteurEngagement * (1f + humeur * 0.5f);
        } else {
            // Négatif → "dramatise" pour générer de l'indignation
            // (l'influenceur sait que le drama crée de l'engagement)
            float seuilEffet = Math.min(seuilDrame, -bienveillance);
            float amplificationDrama = 1f + seuilEffet / seuilDrame * facteurEngagement * 0.5f;
            resultat = bienveillance * amplificationDrama;
        }

        // -------------------------------------------------------
        // 3. Analyse du contenu : keywords "tendance"
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        resultat += scoreTexte * facteurEngagement * 0.6f;

        // Urgence = trending topic = boost supplémentaire
        if (estUrgent(contenu)) {
            resultat *= 1.3f;
        }

        // -------------------------------------------------------
        // 4. Burst créatif aléatoire (inspiration soudaine)
        // -------------------------------------------------------
        if (rng.nextDouble() < probaCreativiteBurst && energie > 0.5f) {
            float burst = (rng.nextFloat() * 3f + 1f) * Math.signum(resultat);
            resultat += burst;
        }

        // -------------------------------------------------------
        // 5. Ajout de la "signature" de l'influenceur
        //    (son charisme personnel + son humeur du moment)
        // -------------------------------------------------------
        resultat += charisme * humeur * 2f;

        // -------------------------------------------------------
        // 6. Les likes accumulés gonflent l'ego (feed-forward)
        // -------------------------------------------------------
        float facteurEgo = 1f + Math.min(0.5f, likesAccumules * 0.01f);
        resultat *= facteurEgo;

        return resultat;
    }

    /** Simule un retour positif (comme un "like"). */
    public void recevoirLike() {
        likesAccumules++;
        humeur = Math.min(1f, humeur + 0.03f);
        energie = Math.min(1f, energie + 0.01f);
    }

    /** Remet l'influenceur en mode normal après un burn-out. */
    public void seReposer() {
        energie = 0.7f;
        stress  = 0.2f;
        humeur  = tendance;
    }

    public boolean estEnBurnout()       { return energie < 0.15f; }
    public int     getLikesAccumules()  { return likesAccumules; }
    public float   getFacteurEngagement() { return facteurEngagement; }

    @Override
    public String typePersonnalite() { return "Influenceur"; }

    @Override
    public String descriptionCourte() { return "Maximise l'engagement"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité influenceur : optimise chaque message pour l'impact maximal. "
             + "Facteur d'engagement : ×" + String.format("%.1f", facteurEngagement) + ". "
             + (estEnBurnout() ? "⚠ EN BURN-OUT ! Publications incohérentes. "
                               : "En forme (" + likesAccumules + " likes accumulés). ")
             + "Energie : " + String.format("%.0f%%", energie * 100) + ". "
             + "Humeur : " + String.format("%.2f", humeur);
    }
}
