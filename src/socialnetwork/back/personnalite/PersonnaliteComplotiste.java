package socialnetwork.back.personnalite;

import java.util.Random;

/**
 * Personnalité complotiste : filtre paranoïaque, défiance envers les sources officielles.
 *
 * Profil psychologique :
 *  - Interprète toute nouvelle positive officielle comme une manipulation
 *  - Amplifie les "révélations" et les informations alarmantes
 *  - Fabrique des connexions entre événements sans rapport
 *  - Réseau de confiance fermé : amplifie les messages de "son groupe"
 *  - Méfiance totale envers les messages trop lisses ou consensuels
 *  - Effet Dunning-Kruger : certitude absolue de détenir la "vraie" information
 *  - Propagation virale des théories : fort taux de retransmission pour "les théories"
 *  - Escalade : plus un message est négatif, plus il semble "révéler la vérité"
 *
 * Analogie humaine : la personne qui voit un "loup" dans chaque événement,
 * qui partage des vidéos "qu'on ne veut pas vous montrer", et dont le monde
 * se divise entre "ceux qui savent" et "les moutons".
 */
public class PersonnaliteComplotiste extends PersonnaliteHumaine {

    /** Facteur de défiance envers les sources officielles. */
    private final float facteurMefiance;

    /** Facteur d'amplification des "révélations" (messages négatifs alarmants). */
    private final float amplificationRevelation;

    /** Fréquence de fabrication de connexions improbables. */
    private final double probaFauxLien;

    private final Random rng = new Random();

    // Mots-clés qui "confirment le complot"
    private static final String[] MOTS_COMPLOT = {
        "gouvernement", "élite", "banque", "vaccin", "contrôle", "manipulation",
        "pouvoir", "secret", "caché", "vérité", "réveil", "conspirat",
        "médias", "propagande", "lobbyiste", "pharma", "puces", "surveillance"
    };

    // Mots-clés de sources "officielles" (méfiance maximale)
    private static final String[] SOURCES_OFFICIELLES = {
        "officiel", "gouvernement", "ministre", "etat", "autorité", "institution",
        "scientifique", "expert", "oms", "ue", "sénat", "assemblée", "médias"
    };

    // Constantes prédéfinies
    public static final PersonnaliteComplotiste SCEPTIQUE    = new PersonnaliteComplotiste("Sceptique radical",    0.6f, 1.5f, 0.08f);
    public static final PersonnaliteComplotiste COMPLOTISTE  = new PersonnaliteComplotiste("Complotiste",          0.8f, 2.0f, 0.15f);
    public static final PersonnaliteComplotiste PARANOIAQUE  = new PersonnaliteComplotiste("Paranoïaque",          0.95f, 2.8f, 0.25f);

    /**
     * @param nom                    nom de la personnalité
     * @param facteurMefiance        méfiance envers les sources officielles [0, 1]
     * @param amplificationRevelation facteur d'amplification des "révélations" [1, 3]
     * @param probaFauxLien          probabilité de connexion improbable [0, 0.3]
     */
    public PersonnaliteComplotiste(String nom, float facteurMefiance,
                                    float amplificationRevelation, double probaFauxLien) {
        super(nom,
              -0.3f,  // légèrement négatif (méfiance générale)
              0.9f,   // très réactif aux "révélations"
              0.2f,   // lente récupération (obsession persistante)
              0.8f,   // fort biais de confirmation
              1.5f    // charisme modéré (convaincu = convaincant)
        );
        this.facteurMefiance        = Math.max(0f, Math.min(1f, facteurMefiance));
        this.amplificationRevelation = Math.max(1f, Math.min(3f, amplificationRevelation));
        this.probaFauxLien          = Math.max(0, Math.min(0.3, probaFauxLien));

        // État initial : méfiance de base
        this.humeur  = -0.2f;
        this.stress  = 0.3f;
    }

    /**
     * Détecte si le contenu contient des mots-clés liés aux complots.
     */
    private float scoreComplot(String contenu) {
        if (contenu == null) return 0f;
        String low = contenu.toLowerCase();
        float score = 0f;
        for (String mot : MOTS_COMPLOT) if (low.contains(mot)) score += 1f;
        return score;
    }

    /**
     * Détecte si le contenu mentionne des sources officielles (méfiance accrue).
     */
    private boolean contientSourceOfficielle(String contenu) {
        if (contenu == null) return false;
        String low = contenu.toLowerCase();
        for (String source : SOURCES_OFFICIELLES) if (low.contains(source)) return true;
        return false;
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        float resultat = bienveillance;

        // -------------------------------------------------------
        // 1. Inversion des sources officielles
        //    "Si c'est officiel, c'est suspect"
        // -------------------------------------------------------
        if (contientSourceOfficielle(contenu)) {
            if (bienveillance > 0f) {
                // "Ils essaient de nous manipuler avec des bonnes nouvelles"
                resultat = -bienveillance * facteurMefiance;
            } else {
                // "Au moins ils avouent une partie de la vérité"
                resultat = bienveillance * (1f + facteurMefiance * 0.3f);
            }
        }

        // -------------------------------------------------------
        // 2. Amplification des "révélations" (messages négatifs alarmants)
        // -------------------------------------------------------
        float scoreComplot = scoreComplot(contenu);
        if (scoreComplot > 0f || bienveillance < -2f) {
            // "Enfin la vérité sort !"
            float facteurRevel = 1f + scoreComplot * 0.3f;
            if (bienveillance < -2f) {
                resultat = bienveillance * amplificationRevelation * facteurRevel;
            } else {
                // Mot-clé de complot mais message pas si négatif → le rend négatif
                resultat -= scoreComplot * 1.5f;
            }
        }

        // -------------------------------------------------------
        // 3. Connexions improbables (paranoïa)
        //    Aléatoirement, crée un lien entre ce message et "quelque chose de suspect"
        // -------------------------------------------------------
        if (rng.nextDouble() < probaFauxLien) {
            // "Ça me fait penser au grand remplacement / aux puces / à l'agenda 21..."
            float amplificationParanoia = (float) (rng.nextGaussian() * 1.5f - 2f);
            resultat += amplificationParanoia;
        }

        // -------------------------------------------------------
        // 4. Messages très positifs = manipulation évidente
        // -------------------------------------------------------
        if (bienveillance > 6f) {
            // Trop positif = trop lisse = faux
            resultat = -bienveillance * facteurMefiance * 0.7f;
        }

        // -------------------------------------------------------
        // 5. Urgence = signal d'alerte = amplification forte
        // -------------------------------------------------------
        if (estUrgent(contenu)) {
            resultat *= (1f + facteurMefiance * 0.8f);
        }

        // -------------------------------------------------------
        // 6. Humeur paranoïaque : teinte tout le négatif
        // -------------------------------------------------------
        resultat += humeur * amplificationRevelation * 0.5f;

        // -------------------------------------------------------
        // 7. Certitude absolue : le complotiste ne doute jamais
        //    → amplifie sa propre conviction
        // -------------------------------------------------------
        resultat += charisme * Math.signum(resultat) * 0.5f;

        return resultat;
    }

    @Override
    public String typePersonnalite() { return "Complotiste"; }

    @Override
    public String descriptionCourte() { return "Voit la manipulation partout"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité complotiste : interprète les messages officiels positifs "
             + "comme des manipulations (méfiance : " + String.format("%.0f%%", facteurMefiance * 100)
             + "). Amplifie les révélations ×" + String.format("%.1f", amplificationRevelation) + ". "
             + "Connexions improbables : " + String.format("%.0f%%", probaFauxLien * 100) + "/message. "
             + "Humeur : " + String.format("%.2f", humeur) + " | Stress : "
             + String.format("%.0f%%", stress * 100);
    }
}
