package socialnetwork.back.personnalite;

/**
 * Personnalité bienveillante : toujours voir le positif, aider les autres.
 *
 * Profil psychologique :
 *  - Amplifie systématiquement les messages positifs
 *  - Adoucit toujours les messages négatifs (ne peut pas être méchant)
 *  - Altruisme actif : ajoute spontanément de la bienveillance à chaque message
 *  - Résistance à l'infection émotionnelle négative (bouclier empathique)
 *  - Naïveté potentielle : peut amplifier des messages manipulateurs positifs
 *  - Épuisement altruiste : à force de donner, peut s'épuiser
 *  - Contagion positive : sa bonne humeur est naturellement contagieuse
 *
 * Analogie humaine : la personne qui répond toujours "ça va bien aller",
 * qui encourage tout le monde, qui n'arrive pas à dire du mal de quelqu'un,
 * et qui fait du bénévolat le week-end.
 */
public class PersonnaliteBienveillant extends PersonnaliteHumaine {

    /** Quantité de bienveillance "générée" spontanément à chaque message. */
    private final float generationBienveillance;

    /** Facteur d'amplification des messages positifs. */
    private final float amplificationPositif;

    // Constantes prédéfinies
    public static final PersonnaliteBienveillant OPTIMISTE       = new PersonnaliteBienveillant("Optimiste",       1f,   1.3f);
    public static final PersonnaliteBienveillant BIENVEILLANT    = new PersonnaliteBienveillant("Bienveillant",    1.5f, 1.6f);
    public static final PersonnaliteBienveillant TRES_BIENVEILLANT = new PersonnaliteBienveillant("Très bienveillant", 2.5f, 2.0f);

    /**
     * @param nom                      nom de la personnalité
     * @param generationBienveillance  points de bienveillance ajoutés spontanément [0, 3]
     * @param amplificationPositif     facteur d'amplification des messages positifs [1, 2.5]
     */
    public PersonnaliteBienveillant(String nom, float generationBienveillance, float amplificationPositif) {
        super(nom,
              0.6f,   // tendance très positive
              0.7f,   // sensibilité modérée (empathie)
              0.5f,   // bonne résilience
              0.2f,   // peu de biais de confirmation (ouvert à tous)
              1.3f    // charisme positif
        );
        this.generationBienveillance = Math.max(0f, Math.min(3f, generationBienveillance));
        this.amplificationPositif    = Math.max(1f, Math.min(2.5f, amplificationPositif));

        // État initial positif
        this.humeur  = 0.5f;
        this.energie = 0.9f;
        this.stress  = 0.05f;
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Génération spontanée de bienveillance
        //    La personnalité ajoute toujours quelque chose de positif
        // -------------------------------------------------------
        float bonusGenere = generationBienveillance * (0.5f + energie * 0.5f);

        // Si épuisé altruistement, génère moins
        if (energie < 0.2f) {
            bonusGenere *= 0.3f;
        }

        // -------------------------------------------------------
        // 2. Traitement différencié selon la valence
        // -------------------------------------------------------
        float resultat;

        if (bienveillance >= 0f) {
            // Positif → amplifie avec joie
            resultat = bienveillance * amplificationPositif * (1f + humeur * 0.3f);
        } else {
            // Négatif → réinterprète vers le moins négatif possible
            // "Il y a sûrement quelque chose de bien là-dedans"
            float correction = (-bienveillance) * 0.6f * (0.5f + energie * 0.5f);
            resultat = bienveillance + correction;

            // Le bienveillant ne peut jamais transmettre quelque chose de très négatif
            if (resultat < -2f) resultat = -2f;
        }

        // -------------------------------------------------------
        // 3. Application du bonus de bienveillance spontané
        // -------------------------------------------------------
        resultat += bonusGenere;

        // -------------------------------------------------------
        // 4. Analyse du contenu : amplifie les messages d'aide et de partage
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        if (scoreTexte > 0f) {
            resultat += scoreTexte * amplificationPositif * 0.4f;
        } else {
            // Contenu négatif : essaie de le contre-balancer
            resultat += (-scoreTexte) * generationBienveillance * 0.2f;
        }

        // -------------------------------------------------------
        // 5. Contagion positive de l'humeur
        // -------------------------------------------------------
        resultat += humeur * 2f;

        // -------------------------------------------------------
        // 6. Épuisement altruiste réduit l'efficacité
        // -------------------------------------------------------
        if (energie < 0.3f && stress > 0.5f) {
            // "Je veux bien faire mais je n'ai plus d'énergie"
            resultat *= (0.4f + energie);
        }

        return resultat;
    }

    @Override
    public String typePersonnalite() { return "Bienveillant"; }

    @Override
    public String descriptionCourte() { return "Répand la positivité"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité bienveillante : génère spontanément +" + String.format("%.1f", generationBienveillance)
             + " de bienveillance par message. Amplifie le positif ×"
             + String.format("%.1f", amplificationPositif) + ". "
             + (energie < 0.2f ? "⚠ Épuisement altruiste en cours. "
                               : "Plein d'énergie positive. ")
             + "Humeur : " + String.format("%.2f", humeur)
             + " | Energie : " + String.format("%.0f%%", energie * 100);
    }
}
