package socialnetwork.back.personnalite;

/**
 * Personnalité médiateur : apaise les conflits, construit des ponts.
 *
 * Profil psychologique :
 *  - Adoucit systématiquement les messages négatifs
 *  - Conserve les messages positifs (ne détruit pas la bonne humeur)
 *  - Ajoute une touche empathique à tout ce qu'il transmet
 *  - Résistance à la contagion négative (protège son réseau du poison émotionnel)
 *  - Fatigue du médiateur : après trop de messages négatifs, s'épuise et perd son effet
 *  - Rôle de pont : amplifie légèrement les messages qui semblent diviseurs pour les réconcilier
 *
 * Analogie humaine : le diplomate, l'assistante sociale, le thérapeute de groupe
 * qui dit "je comprends les deux côtés" et qui transforme les tensions en dialogue.
 */
public class PersonnaliteMediateur extends PersonnaliteHumaine {

    /** Force d'adoucissement des messages négatifs [0, 1]. */
    private final float forceAdoucissement;

    /** Nombre de messages très négatifs traités avant épuisement du médiateur. */
    private final int seuilEpuisementMediateur;

    /** Compteur de messages négatifs consécutifs (mesure la charge du médiateur). */
    private int compteurMessageNegatifs = 0;

    // Constantes prédéfinies
    public static final PersonnaliteMediateur TRES_PACIFICATEUR = new PersonnaliteMediateur("Pacificateur",  0.85f, 10);
    public static final PersonnaliteMediateur MEDIATEUR         = new PersonnaliteMediateur("Médiateur",     0.65f, 18);
    public static final PersonnaliteMediateur DIPLOMATE         = new PersonnaliteMediateur("Diplomate",     0.45f, 30);

    /**
     * @param nom                      nom de la personnalité
     * @param forceAdoucissement       force de neutralisation du négatif [0, 1]
     * @param seuilEpuisementMediateur nb messages négatifs avant épuisement
     */
    public PersonnaliteMediateur(String nom, float forceAdoucissement, int seuilEpuisementMediateur) {
        super(nom,
              0.2f,   // légèrement positif (empathie naturelle)
              0.5f,   // sensibilité modérée (doit l'être pour aider)
              0.5f,   // résilience modérée
              0.15f,  // peu de biais (essaie d'être équitable)
              1.0f    // charisme modéré (influence par la douceur)
        );
        this.forceAdoucissement       = Math.max(0f, Math.min(1f, forceAdoucissement));
        this.seuilEpuisementMediateur = Math.max(1, seuilEpuisementMediateur);
    }

    @Override
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        super.mettreAJourEtat(bienveillance, contenu);

        if (bienveillance < -2f) {
            compteurMessageNegatifs++;
            // Les messages très négatifs épuisent particulièrement le médiateur
            stress = Math.min(1f, stress + 0.06f);
        } else if (bienveillance > 1f) {
            // Les messages positifs rechargent le médiateur
            compteurMessageNegatifs = Math.max(0, compteurMessageNegatifs - 1);
        }
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Vérification de l'épuisement du médiateur
        // -------------------------------------------------------
        float capaciteMediation;
        if (compteurMessageNegatifs > seuilEpuisementMediateur) {
            // Le médiateur est épuisé : son effet s'affaiblit
            int exces = compteurMessageNegatifs - seuilEpuisementMediateur;
            capaciteMediation = Math.max(0.1f, forceAdoucissement - exces * 0.05f);
        } else {
            capaciteMediation = forceAdoucissement * (energie * 0.5f + 0.5f);
        }

        // -------------------------------------------------------
        // 2. Traitement différencié selon la valence du message
        // -------------------------------------------------------
        float resultat;

        if (bienveillance < 0f) {
            // Message négatif : adoucit en le tirant vers 0
            float cible = 0f;
            resultat = bienveillance + (cible - bienveillance) * capaciteMediation;

            // Si le message est extrêmement négatif, ajoute une "touche d'espoir"
            if (bienveillance < -6f) {
                resultat += 1.5f * capaciteMediation;
            }
        } else if (bienveillance > 0f) {
            // Message positif : conserve et même amplifie légèrement
            resultat = bienveillance * (1f + charisme * 0.15f);
        } else {
            // Message neutre : ajoute une touche chaleureuse
            resultat = bienveillance + 0.5f * capaciteMediation;
        }

        // -------------------------------------------------------
        // 3. Analyse du contenu : conflictuel vs réconciliatoire
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        if (scoreTexte < -1f) {
            // Contenu très conflictuel : médiateur travaille plus fort
            resultat += (-scoreTexte) * capaciteMediation * 0.4f;
        }

        // -------------------------------------------------------
        // 4. Empathie ajoutée (signature du médiateur)
        //    Même épuisé, il ajoute toujours une micro-touche positive
        // -------------------------------------------------------
        resultat += tendance * 0.3f * (1f - stress * 0.5f);

        // -------------------------------------------------------
        // 5. Humeur du médiateur : quand stressé, moins efficace
        // -------------------------------------------------------
        resultat += humeur * 0.5f;

        return resultat;
    }

    /** Recharge le médiateur après une pause. */
    public void seRessourcer() {
        compteurMessageNegatifs = 0;
        energie = 0.9f;
        stress  = 0.1f;
        humeur  = tendance;
        historiqueRecent.clear();
    }

    /** Vrai si le médiateur est en état d'épuisement de compassion. */
    public boolean estEpuise() {
        return compteurMessageNegatifs > seuilEpuisementMediateur || energie < 0.2f;
    }

    public int getCompteurMessagesNegatifs()    { return compteurMessageNegatifs; }
    public float getForceAdoucissementActuel() {
        if (estEpuise()) return forceAdoucissement * 0.2f;
        return forceAdoucissement * (energie * 0.5f + 0.5f);
    }

    @Override
    public String typePersonnalite() { return "Médiateur"; }

    @Override
    public String descriptionCourte() { return "Apaise les tensions"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité médiateur : adoucit les conflits et préserve la bienveillance. "
             + "Puissance d'adoucissement : " + String.format("%.0f%%", getForceAdoucissementActuel() * 100) + ". "
             + (estEpuise() ? "⚠ En état d'épuisement de compassion ! "
                            : "Opérationnel (" + compteurMessageNegatifs + " messages négatifs absorbés). ")
             + "Energie : " + String.format("%.0f%%", energie * 100);
    }
}
