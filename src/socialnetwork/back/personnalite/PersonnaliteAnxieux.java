package socialnetwork.back.personnalite;

/**
 * Personnalité anxieuse : catastrophise, amplifie les menaces perçues.
 *
 * Profil psychologique :
 *  - Amplifie tous les messages négatifs (tout devient une menace)
 *  - Anticipe le pire dans les messages neutres ou ambigus
 *  - Hyper-vigilance : les mots d'urgence déclenchent une réaction disproportionnée
 *  - Boucles d'anxiété : le stress s'auto-alimente (pire → plus anxieux → voit tout pire)
 *  - Rumination : les messages négatifs persistent dans la mémoire
 *  - Évitement sélectif : parfois refuse de traiter les messages trop stressants
 *  - Besoin de réassurance : les messages positifs ont un effet temporaire (court)
 *
 * Analogie humaine : la personne qui lit une notification et commence à "imaginer
 * les scénarios catastrophes", qui a du mal à dormir après une mauvaise nouvelle,
 * et qui cherche constamment des signes de danger.
 */
public class PersonnaliteAnxieux extends PersonnaliteHumaine {

    /** Facteur d'amplification des messages négatifs/menaçants. */
    private final float amplificationMenace;

    /** Seuil à partir duquel un message déclenche une réaction d'anxiété aiguë. */
    private final float seuilAnxieteAigue;

    /** Vrai si la personnalité est en état d'anxiété aiguë. */
    private boolean anxieteAigue = false;

    /** Compteur de messages très stressants consécutifs. */
    private int streakStressant = 0;

    // Constantes prédéfinies
    public static final PersonnaliteAnxieux INQUIET   = new PersonnaliteAnxieux("Inquiet",          1.4f, 4f);
    public static final PersonnaliteAnxieux ANXIEUX   = new PersonnaliteAnxieux("Anxieux",           1.9f, 3f);
    public static final PersonnaliteAnxieux PANIQUE   = new PersonnaliteAnxieux("En état de panique", 2.5f, 2f);

    /**
     * @param nom               nom de la personnalité
     * @param amplificationMenace facteur d'amplification des menaces [1, 3]
     * @param seuilAnxieteAigue valeur de bienveillance déclenchant l'anxiété aiguë
     */
    public PersonnaliteAnxieux(String nom, float amplificationMenace, float seuilAnxieteAigue) {
        super(nom,
              -0.25f, // légèrement négatif (anticipation du pire)
              1.3f,   // très sensible (hyper-vigilance)
              0.15f,  // très lente récupération (rumination)
              0.7f,   // fort biais de confirmation (cherche les dangers)
              0.6f    // charisme modéré (ses peurs sont "contagieuses")
        );
        this.amplificationMenace  = Math.max(1f, Math.min(3f, amplificationMenace));
        this.seuilAnxieteAigue    = Math.max(1f, seuilAnxieteAigue);

        this.humeur  = -0.3f;
        this.stress  = 0.5f;  // stress de base élevé
        this.energie = 0.6f;  // énergie réduite (l'anxiété épuise)
    }

    @Override
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        super.mettreAJourEtat(bienveillance, contenu);

        if (bienveillance < -seuilAnxieteAigue) {
            // Anxiété aiguë déclenchée
            anxieteAigue = true;
            streakStressant++;
            // L'anxiété s'auto-alimente
            stress = Math.min(1f, stress + 0.1f * amplificationMenace);
            energie = Math.max(0f, energie - 0.08f);
        } else if (bienveillance > 3f) {
            // Message très positif : calme temporaire (mais bref)
            streakStressant = Math.max(0, streakStressant - 1);
            if (streakStressant == 0) {
                anxieteAigue = false;
            }
        }

        // L'urgence seule déclenche déjà l'anxiété
        if (contenu != null && estUrgent(contenu)) {
            stress = Math.min(1f, stress + 0.05f);
        }
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Mode anxiété aiguë : tout semble catastrophique
        // -------------------------------------------------------
        if (anxieteAigue && stress > 0.8f) {
            // En pleine panique : amplifie massivement le négatif
            if (bienveillance < 0f) {
                return bienveillance * amplificationMenace * (1f + stress);
            } else {
                // Même le positif semble "trop beau" = menace cachée
                return bienveillance * (1f - stress * 0.5f) - stress * 1.5f;
            }
        }

        // -------------------------------------------------------
        // 2. Traitement des messages menaçants
        // -------------------------------------------------------
        float resultat;

        if (bienveillance < 0f) {
            float facteurtMenace = amplificationMenace;
            // Streak stressant : chaque message négatif amplifie le suivant
            if (streakStressant > 2) {
                facteurtMenace *= (1f + streakStressant * 0.1f);
                facteurtMenace = Math.min(facteurtMenace, 5f);
            }
            resultat = bienveillance * facteurtMenace;
        } else {
            // Positif : réassurance temporaire (mais l'anxieux "attend le prochain coup")
            float effetReasurance = bienveillance * (0.6f - stress * 0.3f);
            resultat = Math.max(-0.5f, effetReasurance);
        }

        // -------------------------------------------------------
        // 3. Hyper-vigilance aux mots d'urgence
        // -------------------------------------------------------
        if (estUrgent(contenu)) {
            // L'urgence décuple l'anxiété
            resultat -= amplificationMenace * 2f;
        }

        // -------------------------------------------------------
        // 4. Catastrophisation des messages ambigus
        // -------------------------------------------------------
        if (Math.abs(bienveillance) < 1f) {
            // Message ambigu → l'anxieux imagine le pire
            resultat -= stress * amplificationMenace * 0.8f;
        }

        // -------------------------------------------------------
        // 5. Rumination (mémoire des mauvaises nouvelles)
        // -------------------------------------------------------
        float moyenneNegative = moyenneRecente();
        if (moyenneNegative < -1f) {
            resultat += moyenneNegative * 0.5f;
        }

        // -------------------------------------------------------
        // 6. Humeur anxieuse
        // -------------------------------------------------------
        resultat += humeur * 2f;

        // -------------------------------------------------------
        // 7. Épuisement : l'anxiété chronique réduit les réactions extrêmes
        // -------------------------------------------------------
        if (energie < 0.2f) {
            // Épuisé : anesthésie partielle
            resultat *= 0.6f;
        }

        return resultat;
    }

    /** Tente de calmer l'anxiété (respiration, méditation simulée). */
    public void seCalmer() {
        anxieteAigue  = false;
        streakStressant = 0;
        stress        = Math.max(0.2f, stress - 0.3f);
        humeur        = Math.min(0f, humeur + 0.2f);
    }

    public boolean estEnAnxieteAigue()  { return anxieteAigue; }
    public int     getStreakStressant() { return streakStressant; }

    @Override
    public String typePersonnalite() { return "Anxieux"; }

    @Override
    public String descriptionCourte() { return "Catastrophise les menaces"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité anxieuse : amplifie les menaces ×"
             + String.format("%.1f", amplificationMenace) + ". "
             + (estEnAnxieteAigue() ? "⚠ EN ANXIÉTÉ AIGUË (streak=" + streakStressant + "). "
                                    : "Vigilante mais stable. ")
             + "Stress : " + String.format("%.0f%%", stress * 100)
             + " | Humeur : " + String.format("%.2f", humeur)
             + " | Energie : " + String.format("%.0f%%", energie * 100);
    }
}
