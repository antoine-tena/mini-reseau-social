package socialnetwork.back.personnalite;

/**
 * Personnalité rationnelle : analyse avant de réagir.
 *
 * Profil psychologique :
 *  - Résistance aux biais émotionnels : corrige systématiquement vers le neutre
 *  - Prise en compte du contenu : cherche les arguments, pas les émotions
 *  - Scepticisme naturel : méfiance envers les messages trop positifs ou alarmistes
 *  - Stabilité émotionnelle : humeur quasi constante, peu affectée
 *  - Lenteur délibérative : "traitement approfondi" simulé par une légère pénalité de transmission
 *  - Bonne mémoire : compare les messages actuels aux précédents
 *
 * Analogie humaine : l'ingénieur, le scientifique, l'avocat qui vérifie
 * les sources avant de partager, qui dit "attends, tu as des preuves de ça ?"
 * et qui fait baisser la tension dans les débats.
 */
public class PersonnaliteRationnelle extends PersonnaliteHumaine {

    /** Force du retour vers la neutralité [0, 1]. */
    private final float forceNeutralisation;

    /** Facteur de correction des extremes (>5 ou <-5 bienveillance). */
    private final float correctionExtreme;

    // Constantes prédéfinies
    public static final PersonnaliteRationnelle TRES_RATIONNEL = new PersonnaliteRationnelle("Très rationnel", 0.8f, 0.7f);
    public static final PersonnaliteRationnelle RATIONNEL      = new PersonnaliteRationnelle("Rationnel",      0.6f, 0.5f);
    public static final PersonnaliteRationnelle PRAGMATIQUE    = new PersonnaliteRationnelle("Pragmatique",    0.4f, 0.3f);

    public PersonnaliteRationnelle(String nom, float forceNeutralisation, float correctionExtreme) {
        super(nom,
              0f,     // tendance parfaitement neutre
              0.2f,   // très peu sensible émotionnellement
              0.8f,   // récupère vite (n'accumule pas d'états négatifs)
              0.1f,   // biais de confirmation très faible
              0.7f    // charisme modéré (persuasif par les arguments)
        );
        this.forceNeutralisation = Math.max(0f, Math.min(1f, forceNeutralisation));
        this.correctionExtreme   = Math.max(0f, Math.min(1f, correctionExtreme));
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Correction vers la neutralité (noyau rationnel)
        // -------------------------------------------------------
        // Tire le message vers 0 proportionnellement à sa déviation
        float correction = -bienveillance * forceNeutralisation;
        float resultat   = bienveillance + correction;

        // -------------------------------------------------------
        // 2. Correction des extrêmes (scepticisme envers le sensationnel)
        // -------------------------------------------------------
        if (bienveillance > 5f) {
            // "C'est trop beau pour être vrai" → aplatit les messages euphoriques
            float surplus = bienveillance - 5f;
            resultat -= surplus * correctionExtreme;
        } else if (bienveillance < -5f) {
            // "Attendons les faits" → atténue la panique
            float defaut = -bienveillance - 5f;
            resultat += defaut * correctionExtreme;
        }

        // -------------------------------------------------------
        // 3. Analyse du contenu (longueur ≈ argumentation)
        // -------------------------------------------------------
        if (contenu != null && !contenu.isBlank()) {
            int longueur = contenu.trim().length();

            if (longueur > 50) {
                // Message argumenté → accepté plus naturellement (légère amplification)
                float bonusArgumentation = Math.min(0.5f, longueur / 200f);
                resultat += resultat * bonusArgumentation * 0.3f;
            } else if (longueur < 10) {
                // Message trop court = non argumenté → scepticisme
                resultat *= 0.7f;
            }

            // Urgence = signal d'alarme = scepticisme rationnel
            if (estUrgent(contenu)) {
                resultat *= (1f - correctionExtreme * 0.4f);
            }
        }

        // -------------------------------------------------------
        // 4. Vérification par mémoire (cohérence avec le passé)
        // -------------------------------------------------------
        float moyennePassee = moyenneRecente();
        float ecartAvecPasse = bienveillance - moyennePassee;

        if (Math.abs(ecartAvecPasse) > 4f) {
            // Message très incohérent avec l'historique → réduction supplémentaire
            resultat -= ecartAvecPasse * correctionExtreme * 0.3f;
        }

        // -------------------------------------------------------
        // 5. Humeur quasi stable (rationnel ≈ équilibre permanent)
        // -------------------------------------------------------
        // L'humeur rationnelle est fixe : on ajoute juste une micro-variation
        resultat += humeur * 0.2f;

        return resultat;
    }

    /** Retourne le degré actuel de neutralisation appliqué. */
    public float getForceNeutralisation() { return forceNeutralisation; }

    @Override
    public String typePersonnalite() { return "Rationnel"; }

    @Override
    public String descriptionCourte() { return "Tempère, analyse, neutralise"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité rationnelle : recentre les messages vers la neutralité. "
             + "Force de neutralisation : " + String.format("%.0f%%", forceNeutralisation * 100) + ". "
             + "Méfiant envers les messages extrêmes (correction : "
             + String.format("%.0f%%", correctionExtreme * 100) + "). "
             + "Humeur stable à " + String.format("%.2f", humeur) + " (variations quasi nulles).";
    }
}
