package socialnetwork.back.personnalite;

/**
 * Personnalité cynique : sceptique de tout, surtout du positif.
 *
 * Profil psychologique :
 *  - Dégonfle l'enthousiasme : les messages très positifs sont suspects
 *  - Légèrement négatif sur tout : "le monde est fondamentalement décevant"
 *  - Humour acide : les messages négatifs passent mieux car "ils disent la vérité"
 *  - Résistance au changement : ignore les messages d'appel à l'optimisme
 *  - Perspicacité : parfois voit les choses telles qu'elles sont vraiment
 *  - Stagnation émotionnelle : peu de variations, mais légèrement en dessous de zéro
 *  - Effet de désenchantement : réduit la bienveillance de tout ce qui passe
 *
 * Analogie humaine : le vieux sage amer, le blogueur sarcastique, la personne
 * qui répond "on verra bien" à toute bonne nouvelle et "je l'avais dit"
 * à toute mauvaise nouvelle.
 */
public class PersonnaliteCynique extends PersonnaliteHumaine {

    /** Intensité du cynisme : force de réduction du positif. */
    private final float intensiteCynisme;

    /** Degré de perspicacité : bonus sur les messages "vrais négatifs". */
    private final float perspicacite;

    // Constantes prédéfinies
    public static final PersonnaliteCynique DESABUSE   = new PersonnaliteCynique("Désabusé",     0.4f, 0.3f);
    public static final PersonnaliteCynique CYNIQUE    = new PersonnaliteCynique("Cynique",      0.7f, 0.5f);
    public static final PersonnaliteCynique ACIDE      = new PersonnaliteCynique("Acide",        0.9f, 0.7f);

    /**
     * @param nom              nom de la personnalité
     * @param intensiteCynisme force de réduction du positif [0, 1]
     * @param perspicacite     bonus "vérité" sur les messages réellement négatifs [0, 1]
     */
    public PersonnaliteCynique(String nom, float intensiteCynisme, float perspicacite) {
        super(nom,
              -0.2f,  // légèrement négatif
              0.3f,   // peu sensible (il a "tout vu")
              0.7f,   // bonne résilience (rien ne le touche vraiment)
              0.5f,   // biais de confirmation modéré (confirme sa vision sombre)
              0.8f    // charisme acide (les remarques cyniques marquent)
        );
        this.intensiteCynisme = Math.max(0f, Math.min(1f, intensiteCynisme));
        this.perspicacite     = Math.max(0f, Math.min(1f, perspicacite));

        this.humeur  = -0.2f;
        this.stress  = 0.15f;
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        float resultat;

        // -------------------------------------------------------
        // 1. Traitement du positif (désenchantement)
        // -------------------------------------------------------
        if (bienveillance > 0f) {
            if (bienveillance > 6f) {
                // Très positif = ridicule aux yeux du cynique
                // "Encore les gens qui font semblant que tout va bien..."
                float reduction = (bienveillance - 6f) * intensiteCynisme;
                resultat = bienveillance - bienveillance * intensiteCynisme - reduction;
            } else if (bienveillance > 2f) {
                // Positif ordinaire → atténue
                resultat = bienveillance * (1f - intensiteCynisme * 0.7f);
            } else {
                // Légèrement positif : "au moins c'est honnête"
                resultat = bienveillance * (1f - intensiteCynisme * 0.3f);
            }

            // Soustraction du fond cynique
            resultat -= intensiteCynisme * 0.5f;

        // -------------------------------------------------------
        // 2. Traitement du négatif (perspicacité)
        // -------------------------------------------------------
        } else if (bienveillance < 0f) {
            // "Ah, voilà quelqu'un qui dit la vérité"
            // Peut légèrement amplifier si perspicace et contenu négatif justifié
            float scoreTexte = scoreContenu(contenu);
            if (scoreTexte < -1f) {
                // Contenu vraiment négatif : validation de la vision cynique
                resultat = bienveillance * (1f + perspicacite * 0.3f);
            } else {
                // Négatif mais sans substance → passe tel quel
                resultat = bienveillance;
            }
        } else {
            // Neutre : le cynique ajoute sa petite touche
            resultat = -intensiteCynisme * 0.8f;
        }

        // -------------------------------------------------------
        // 3. Réaction au contenu enthousiaste
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        if (scoreTexte > 2f) {
            // Trop d'enthousiasme dans le texte → réduction
            resultat -= scoreTexte * intensiteCynisme * 0.4f;
        }

        // -------------------------------------------------------
        // 4. Urgence = manipulation probable (pour le cynique)
        // -------------------------------------------------------
        if (estUrgent(contenu)) {
            resultat -= intensiteCynisme * 1f;
        }

        // -------------------------------------------------------
        // 5. Humeur légèrement négative permanente
        // -------------------------------------------------------
        resultat += humeur * 1.5f;

        return resultat;
    }

    public float getIntensiteCynisme() { return intensiteCynisme; }
    public float getPerspicacite()     { return perspicacite; }

    @Override
    public String typePersonnalite() { return "Cynique"; }

    @Override
    public String descriptionCourte() { return "Sceptique de tout positif"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité cynique : réduit le positif (-"
             + String.format("%.0f%%", intensiteCynisme * 100)
             + "), valorise le négatif réaliste (perspicacité : "
             + String.format("%.0f%%", perspicacite * 100) + "). "
             + "Humeur stable à " + String.format("%.2f", humeur)
             + " | Stress : " + String.format("%.0f%%", stress * 100);
    }
}
