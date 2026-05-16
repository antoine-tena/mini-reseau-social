package socialnetwork.back.personnalite;

/**
 * Personnalité dépressive : filtre négatif sur la réalité.
 *
 * Profil psychologique :
 *  - Interprète systématiquement les messages dans leur aspect le plus sombre
 *  - Amplifie les messages négatifs (résonance avec l'état intérieur)
 *  - Atténue les messages positifs (difficulté à ressentir la joie)
 *  - Anesthésie émotionnelle : après de nombreux messages, plus de variation
 *  - Vulnérabilité aux spirales descendantes : le négatif appelle le négatif
 *  - Transmission réduite (manque d'énergie, isolement progressif)
 *  - Lucidité occasionnelle : parfois voit la vérité que les autres ignorent
 *
 * Note : Ce modèle simule des traits dépressifs pour étudier leur impact
 * dans les réseaux sociaux. Ce n'est pas un diagnostic médical.
 *
 * Analogie humaine : quelqu'un qui, dans les moments difficiles, ne voit que
 * le verre à moitié vide, qui ruminent, et qui se retire progressivement.
 */
public class PersonnaliteDepressive extends PersonnaliteHumaine {

    /** Facteur d'amplification des messages négatifs [1, 3]. */
    private final float amplificationNegatif;

    /** Facteur d'atténuation des messages positifs [0.1, 0.9]. */
    private final float attentuationPositif;

    /** Profondeur de l'anesthésie émotionnelle (après nbMessages > seuil). */
    private final int seuilAnesth\u00e9sie;

    // Constantes prédéfinies
    public static final PersonnaliteDepressive MELANCOLIQUE     = new PersonnaliteDepressive("Mélancolique",    1.3f, 0.6f, 30);
    public static final PersonnaliteDepressive DEPRESSIF        = new PersonnaliteDepressive("Dépressif",       1.8f, 0.4f, 20);
    public static final PersonnaliteDepressive TRES_DEPRESSIF   = new PersonnaliteDepressive("Très dépressif",  2.3f, 0.2f, 12);

    /**
     * @param nom                  nom de la personnalité
     * @param amplificationNegatif facteur sur les messages négatifs [1, 3]
     * @param attenuationPositif   facteur sur les messages positifs [0.1, 1]
     * @param seuilAnesthesie      nb messages avant anesthésie émotionnelle
     */
    public PersonnaliteDepressive(String nom, float amplificationNegatif,
                                   float attenuationPositif, int seuilAnesthesie) {
        super(nom,
              -0.5f,  // tendance négative
              1.2f,   // très sensible aux messages négatifs
              0.1f,   // très lente récupération
              0.6f,   // fort biais de confirmation (cherche le négatif)
              0.4f    // peu charismatique (repli sur soi)
        );
        this.amplificationNegatif = Math.max(1f, Math.min(3f, amplificationNegatif));
        this.attentuationPositif  = Math.max(0.1f, Math.min(1f, attenuationPositif));
        this.seuilAnesth\u00e9sie       = Math.max(5, seuilAnesthesie);

        // État initial : humeur basse
        this.humeur  = -0.4f;
        this.stress  = 0.4f;
        this.energie = 0.5f;
    }

    @Override
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        super.mettreAJourEtat(bienveillance, contenu);

        // Les messages négatifs accentuent davantage la déprime
        if (bienveillance < -2f) {
            humeur = Math.max(-1f, humeur - 0.06f * sensibilite);
            energie = Math.max(0f, energie - 0.04f);
        }

        // Les messages positifs ont peu d'effet (incapacité à ressentir la joie)
        if (bienveillance > 3f) {
            humeur = Math.min(0.1f, humeur + 0.02f); // augmentation plafonnée
        }
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Anesthésie émotionnelle (après trop de messages)
        // -------------------------------------------------------
        if (nbMessagesRecus > seuilAnesth\u00e9sie) {
            // Plus rien ne touche vraiment : réponse plate et légèrement négative
            return bienveillance * 0.3f + tendance;
        }

        // -------------------------------------------------------
        // 2. Filtre dépressif de base
        // -------------------------------------------------------
        float resultat;

        if (bienveillance <= 0f) {
            // Messages négatifs : résonance avec l'état intérieur
            resultat = bienveillance * amplificationNegatif;

            // Spirale descendante : si déjà très bas, descend encore plus
            if (humeur < -0.5f) {
                resultat *= (1f + (-humeur - 0.5f) * 0.5f);
            }
        } else {
            // Messages positifs : anesthésie partielle
            // "Je vois que c'est censé être positif, mais je ne le ressens pas vraiment"
            resultat = bienveillance * attentuationPositif;

            // Parfois, la lucidité perce : réinterprète le positif comme superficiel
            float scoreTexte = scoreContenu(contenu);
            if (scoreTexte > 2f) {
                // Trop beau pour être vrai → scepticisme
                resultat *= (1f - biais * 0.4f);
            }
        }

        // -------------------------------------------------------
        // 3. Rumination (les messages négatifs récents colorent tout)
        // -------------------------------------------------------
        float moyennePassee = moyenneRecente();
        if (moyennePassee < -1f) {
            // Tendance récente négative → assombrit encore
            resultat += moyennePassee * 0.3f;
        }

        // -------------------------------------------------------
        // 4. Filtre de l'humeur dépressive
        // -------------------------------------------------------
        resultat += humeur * 2f; // humeur très négative → fort biais négatif

        // -------------------------------------------------------
        // 5. Fatigue de transmission (manque d'énergie)
        // -------------------------------------------------------
        if (energie < 0.4f) {
            // Peu d'énergie : transmet moins (retrait social)
            resultat *= (0.3f + energie * 1.75f);
        }

        // -------------------------------------------------------
        // 6. Lucidité dépressive : parfois voit juste
        //    (les dépressifs ont parfois un realisme brutal)
        // -------------------------------------------------------
        float scoreNegatif = scoreContenu(contenu);
        if (scoreNegatif < -2f && bienveillance > 0f) {
            // Message officiellement positif mais contenu négatif :
            // le dépressif voit la contradiction
            resultat -= 1f;
        }

        return resultat;
    }

    /** Vrai si en anesthésie émotionnelle. */
    public boolean estAnesth\u00e9si\u00e9() {
        return nbMessagesRecus > seuilAnesth\u00e9sie;
    }

    @Override
    public String typePersonnalite() { return "Dépressif"; }

    @Override
    public String descriptionCourte() { return "Filtre tout en négatif"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité dépressive : amplifie le négatif (×"
             + String.format("%.1f", amplificationNegatif)
             + "), atténue le positif (×" + String.format("%.1f", attentuationPositif) + "). "
             + (estAnesth\u00e9si\u00e9() ? "En état d'anesthésie émotionnelle. "
                             : "Messages avant anesthésie : " + (seuilAnesth\u00e9sie - nbMessagesRecus) + ". ")
             + "Humeur : " + String.format("%.2f", humeur)
             + " | Energie : " + String.format("%.0f%%", energie * 100);
    }
}
