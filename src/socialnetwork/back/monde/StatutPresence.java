package socialnetwork.back.monde;

/**
 * Statut de présence d'un bavard à un instant donné.
 *
 * Ce statut est mis à jour par le {@link GestionnaireDeplacements} en
 * fonction de la routine journalière et de l'heure simulée.
 */
public enum StatutPresence {

    /** Bavard actuellement présent à son domicile. */
    A_DOMICILE(
        "À domicile", "🏠",
        "Le bavard est chez lui, disponible pour communiquer dans son cercle familial.",
        0.8f,   // propension à envoyer un message
        0.90f   // propension à répondre
    ),

    /** Bavard en transit entre deux lieux (en déplacement). */
    EN_DEPLACEMENT(
        "En déplacement", "🚶",
        "Le bavard se déplace entre deux lieux. Communication réduite.",
        0.3f,
        0.40f
    ),

    /** Bavard sur son lieu de travail ou d'études. */
    AU_TRAVAIL(
        "Au travail", "💼",
        "Le bavard est concentré sur son travail. Échanges professionnels actifs.",
        0.5f,
        0.60f
    ),

    /** Bavard dans un lieu de loisir. */
    EN_LOISIR(
        "En loisir", "⚽",
        "Le bavard profite d'une activité récréative. Ambiance détendue.",
        0.7f,
        0.75f
    ),

    /** Bavard en vacances (lieu habituel de travail désactivé). */
    EN_VACANCES(
        "En vacances", "🌴",
        "Le bavard est en vacances. Plus disponible mais moins connecté au réseau local.",
        0.6f,
        0.85f
    ),

    /** Bavard en pause (repas, café, etc.). */
    EN_PAUSE(
        "En pause", "☕",
        "Moment de pause. Très propice aux échanges informels.",
        0.9f,
        0.95f
    ),

    /** Bavard endormi (nuit). */
    EN_SOMMEIL(
        "En sommeil", "😴",
        "Le bavard dort. Communication quasiment nulle.",
        0.02f,
        0.05f
    ),

    /** Bavard dans un lieu culturel/événement. */
    EN_EVENEMENT(
        "En événement", "🎭",
        "Le bavard assiste à un événement. Communication intensive avec les présents.",
        0.8f,
        0.70f
    );

    // ─── Champs ─────────────────────────────────────────────────────────────

    public final String label;
    public final String icone;
    public final String description;

    /**
     * Propension à initier une communication [0,1].
     * Multiplie la probabilité de base d'envoi de messages.
     */
    public final float propensionEmission;

    /**
     * Propension à répondre / retransmettre un message reçu [0,1].
     * Multiplie la probabilité de retransmission.
     */
    public final float propensionReponse;

    StatutPresence(String label, String icone, String description,
                   float propensionEmission, float propensionReponse) {
        this.label              = label;
        this.icone              = icone;
        this.description        = description;
        this.propensionEmission = propensionEmission;
        this.propensionReponse  = propensionReponse;
    }

    /** True si le bavard est considéré comme "actif" (peut envoyer des messages). */
    public boolean estActif() {
        return this != EN_SOMMEIL;
    }

    /** True si le bavard est physiquement quelque part (pas en transit). */
    public boolean estLocalise() {
        return this != EN_DEPLACEMENT;
    }

    @Override
    public String toString() { return label; }
}
