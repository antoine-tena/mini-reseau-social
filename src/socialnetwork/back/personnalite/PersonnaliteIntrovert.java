package socialnetwork.back.personnalite;

/**
 * Personnalité introvertie : riche intérieurement mais fatigable socialement.
 *
 * Profil psychologique :
 *  - Préfère la qualité à la quantité : transforme peu de messages mais profondément
 *  - Fatigue sociale : après trop de messages, se retire et ne retransmet plus
 *  - Réflexion approfondie : analyse le contenu, pénalise les messages superficiels
 *  - Recharge en silence : récupère vite s'il y a du calme
 *  - Filtre naturel : atténue les messages très positifs (exubérance = inconfort)
 *    et amplifie légèrement les messages profonds/négatifs (résonance intérieure)
 *
 * Analogie humaine : quelqu'un qui préfère une conversation intime à une fête,
 * qui donne son meilleur dans les échanges 1:1, et qui se retrouve épuisé
 * après une grande soirée sociale.
 */
public class PersonnaliteIntrovert extends PersonnaliteHumaine {

    /** Seuil de fatigue sociale : nb messages au-delà duquel on se retire. */
    private final int seuilFatigue;

    /** Pénalité appliquée aux messages superficiels (trop courts / trop exclamatifs). */
    private final float penaliteSuperficiel;

    /** Fenêtre de messages locaux pour mesurer l'intensité sociale récente. */
    private int messagesRecentsCompte = 0;
    private long fenetreDebut = System.currentTimeMillis();

    // Constantes prédéfinies
    public static final PersonnaliteIntrovert TRES_INTROVERTI = new PersonnaliteIntrovert("Très introverti", 8,  0.7f);
    public static final PersonnaliteIntrovert INTROVERTI      = new PersonnaliteIntrovert("Introverti",      15, 0.4f);
    public static final PersonnaliteIntrovert RESERVE         = new PersonnaliteIntrovert("Réservé",         25, 0.2f);

    /**
     * @param nom                 nom de la personnalité
     * @param seuilFatigue        nb messages avant fatigue sociale (retire retransmission)
     * @param penaliteSuperficiel pénalité [0, 1] pour les messages jugés trop courts
     */
    public PersonnaliteIntrovert(String nom, int seuilFatigue, float penaliteSuperficiel) {
        super(nom,
              -0.1f,  // légèrement négatif (l'introverti est souvent plus pessimiste socialement)
              0.6f,   // sensibilité modérée (il traite les choses en profondeur)
              0.4f,   // bonne résilience SI calme
              0.4f,   // biais de confirmation modéré
              0.5f    // peu charismatique en masse, mais fort en individuel
        );
        this.seuilFatigue        = Math.max(1, seuilFatigue);
        this.penaliteSuperficiel = Math.max(0f, Math.min(1f, penaliteSuperficiel));
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 0. Fenêtre de 30 secondes : mesure l'intensité sociale récente
        // -------------------------------------------------------
        long maintenant = System.currentTimeMillis();
        if (maintenant - fenetreDebut > 30_000L) {
            // Réinitialiser la fenêtre
            messagesRecentsCompte = 0;
            fenetreDebut = maintenant;
        }
        messagesRecentsCompte++;

        // -------------------------------------------------------
        // 1. Fatigue sociale : si trop de messages, se referme
        // -------------------------------------------------------
        float facteurFatigue = 1f;
        if (nbMessagesRecus > seuilFatigue) {
            // Après le seuil : réduction progressive jusqu'à quasi-silence
            int excedent = nbMessagesRecus - seuilFatigue;
            facteurFatigue = Math.max(0.1f, 1f - excedent * 0.04f);
        }

        // Si le bavard est vraiment épuisé, les messages passent quasi sans modification
        if (energie < 0.2f) {
            return bienveillance * 0.3f; // se tait intérieurement
        }

        // -------------------------------------------------------
        // 2. Analyse du contenu : superficiel vs profond
        // -------------------------------------------------------
        float facteurProfondeur = 1f;
        if (contenu != null && !contenu.isBlank()) {
            int longueur = contenu.trim().length();

            if (longueur < 10) {
                // Message très court = superficiel
                facteurProfondeur = 1f - penaliteSuperficiel;
            } else if (longueur > 80) {
                // Message long = profond, l'introverti apprécie
                facteurProfondeur = 1.2f;
            }

            // Exclamations multiples = bruit social (inconfortable pour l'introverti)
            long nbExclam = contenu.chars().filter(c -> c == '!').count();
            if (nbExclam > 2) {
                facteurProfondeur *= (1f - penaliteSuperficiel * 0.5f);
            }
        }

        // -------------------------------------------------------
        // 3. Filtre émotionnel introverti
        //    - Atténue les messages très exubérants (trop intenses socialement)
        //    - Traite sobrement les messages positifs ordinaires
        //    - Réfléchit plus longuement aux messages négatifs (rumination)
        // -------------------------------------------------------
        float resultat;
        if (bienveillance > 5f) {
            // Très positif → atténue (sur-stimulation = inconfort)
            resultat = 5f + (bienveillance - 5f) * 0.3f;
        } else if (bienveillance < -2f) {
            // Négatif → rumination légère (tend à "coller" dans la mémoire)
            resultat = bienveillance * (1f + penaliteSuperficiel * 0.3f);
        } else {
            resultat = bienveillance;
        }

        // -------------------------------------------------------
        // 4. Application de la fatigue et de la profondeur
        // -------------------------------------------------------
        resultat *= facteurFatigue * facteurProfondeur;

        // -------------------------------------------------------
        // 5. Coloration par l'humeur (subtile chez l'introverti)
        // -------------------------------------------------------
        resultat += humeur * 0.8f;

        // -------------------------------------------------------
        // 6. Biais de confirmation léger
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        resultat += scoreTexte * biais * 0.3f;

        return resultat;
    }

    /** Remet les compteurs à zéro (simuler une bonne nuit de sommeil). */
    public void recharger() {
        energie           = 1f;
        stress            = 0f;
        humeur            = tendance;
        messagesRecentsCompte = 0;
        fenetreDebut      = System.currentTimeMillis();
        nbMessagesRecus   = 0;
        historiqueRecent.clear();
    }

    /** Vrai si l'introverti est en état de fatigue sociale active. */
    public boolean estFatigue() {
        return nbMessagesRecus > seuilFatigue || energie < 0.3f;
    }

    public int getSeuilFatigue()            { return seuilFatigue; }
    public int getNbMessagesAuSeuil()       { return Math.max(0, seuilFatigue - nbMessagesRecus); }

    @Override
    public String typePersonnalite() { return "Introverti"; }

    @Override
    public String descriptionCourte() { return "Se fatigue socialement"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité introvertie : préfère la qualité à la quantité. "
             + "Filtre les messages superficiels, s'épuise après " + seuilFatigue + " messages. "
             + (estFatigue() ? "⚠ Actuellement en état de fatigue sociale. "
                             : "Actuellement en état opérationnel. ")
             + "Messages restants avant retrait : " + getNbMessagesAuSeuil() + ". "
             + "Energie : " + String.format("%.0f%%", energie * 100);
    }
}
