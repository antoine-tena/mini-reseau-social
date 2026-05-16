package socialnetwork.back.monde;

import java.awt.Color;

/**
 * Types de relations sociales entre bavards.
 *
 * Chaque type définit :
 *  - un label affiché sur l'interface
 *  - un modificateur de confiance (influence la propagation des messages)
 *  - un modificateur de fréquence de contact (combien de fois/jour ils se parlent)
 *  - si la relation implique une adresse commune (cohabitation)
 *  - la couleur de l'arête sur la visualisation
 *  - la bidirectionnalité (la relation est-elle automatiquement réciproque ?)
 */
public enum TypeRelation {

    // ─── Relations familiales ──────────────────────────────────────────────

    COUPLE(
        "Couple",               "❤️",
        new Color(255, 100, 120),
        0.98f,   // confiance quasi absolue
        8.0f,    // 8 interactions/jour
        true,    // adresse commune
        true,    // bidirectionnel
        "Partenaires romantiques. Confiance maximum, communication quotidienne intense."
    ),
    EPOUX(
        "Époux",                "💍",
        new Color(255, 150, 100),
        0.99f,
        10.0f,
        true,
        true,
        "Mariés. Lien le plus fort du réseau social."
    ),
    PARENT_ENFANT(
        "Parent/Enfant",        "👨‍👧",
        new Color(255, 200, 80),
        0.95f,
        5.0f,
        true,
        false,   // non réciproque (le sens est important)
        "Lien parent → enfant. Forte influence éducative."
    ),
    ENFANT_PARENT(
        "Enfant/Parent",        "👧‍👨",
        new Color(255, 220, 100),
        0.90f,
        5.0f,
        true,
        false,
        "Lien enfant → parent. Confiance et dépendance affective."
    ),
    FRERE_SOEUR(
        "Frère/Sœur",           "👫",
        new Color(120, 200, 255),
        0.85f,
        3.0f,
        false,  // peuvent habiter séparément
        true,
        "Fratrie. Complicité et rivalité mêlées."
    ),
    GRAND_PARENT(
        "Grand-parent",         "👴",
        new Color(200, 180, 120),
        0.88f,
        2.0f,
        false,
        false,
        "Grand-parent → petit-enfant. Sagesse transmise."
    ),
    PETIT_ENFANT(
        "Petit-enfant",         "👶",
        new Color(220, 200, 140),
        0.80f,
        2.0f,
        false,
        false,
        "Petit-enfant → grand-parent."
    ),
    COUSIN(
        "Cousin(e)",            "🤝",
        new Color(160, 200, 160),
        0.65f,
        0.5f,
        false,
        true,
        "Cousinage. Lien familial éloigné, contacts peu fréquents."
    ),
    ONCLE_TANTE(
        "Oncle/Tante",          "👥",
        new Color(170, 190, 150),
        0.70f,
        1.0f,
        false,
        false,
        "Lien oncle/tante → neveu/nièce."
    ),

    // ─── Relations amicales ─────────────────────────────────────────────────

    AMI_PROCHE(
        "Ami proche",           "💙",
        new Color(80,  180, 255),
        0.85f,
        3.0f,
        false,
        true,
        "Amitié profonde. Grande influence mutuelle."
    ),
    AMI(
        "Ami",                  "👋",
        new Color(100, 200, 200),
        0.70f,
        1.5f,
        false,
        true,
        "Amitié ordinaire. Relations régulières et cordiales."
    ),
    COLLEGUE(
        "Collègue",             "💼",
        new Color(140, 140, 200),
        0.55f,
        4.0f,  // beaucoup d'interactions au travail
        false,
        true,
        "Collègue de travail. Contact fréquent mais confiance limitée."
    ),
    CAMARADE(
        "Camarade",             "🎒",
        new Color(160, 220, 160),
        0.50f,
        2.5f,
        false,
        true,
        "Camarade de classe ou d'études. Liens créés par la proximité."
    ),
    VOISIN(
        "Voisin",               "🏘️",
        new Color(200, 180, 120),
        0.45f,
        0.8f,
        false,
        true,
        "Voisinage. Relation de courtoisie et entraide ponctuelle."
    ),
    CONNAISSANCE(
        "Connaissance",         "🙂",
        new Color(180, 180, 180),
        0.30f,
        0.3f,
        false,
        true,
        "Simple connaissance. Contacts rares et superficiels."
    ),

    // ─── Relations professionnelles ─────────────────────────────────────────

    MENTOR(
        "Mentor",               "🌟",
        new Color(255, 220, 80),
        0.80f,
        1.0f,
        false,
        false,
        "Mentor → mentoré. Influence pédagogique unidirectionnelle."
    ),
    MENTOR_INVERSE(
        "Mentoré",              "⭐",
        new Color(240, 210, 70),
        0.60f,
        1.0f,
        false,
        false,
        "Mentoré → mentor. Respect et admiration."
    ),
    SUPERIEUR(
        "Supérieur",            "📊",
        new Color(150, 130, 200),
        0.65f,
        3.0f,
        false,
        false,
        "Lien hiérarchique descendant."
    ),
    PARTENAIRE_SPORT(
        "Partenaire sportif",   "🏃",
        new Color(255, 150, 60),
        0.60f,
        2.0f,
        false,
        true,
        "Partenaire régulier d'activité physique."
    ),
    PARTENAIRE_CLUB(
        "Compagnon de club",    "🎯",
        new Color(100, 220, 180),
        0.55f,
        1.5f,
        false,
        true,
        "Membre du même club ou association."
    );

    // ─── Champs ─────────────────────────────────────────────────────────────

    /** Libellé affiché. */
    public final String label;

    /** Icône Unicode. */
    public final String icone;

    /** Couleur de l'arête sur la visualisation. */
    public final Color couleur;

    /**
     * Indice de confiance [0,1]. Influence la probabilité de transmettre
     * un message reçu de cette personne.
     */
    public final float confianceBase;

    /**
     * Fréquence de contact (interactions/jour en moyenne).
     * Influence la probabilité qu'un bavard envoie un message à ce contact.
     */
    public final float frequenceContact;

    /** True si les deux personnes partagent la même adresse. */
    public final boolean adresseCommune;

    /**
     * True si la relation est automatiquement réciproque (A→B implique B→A
     * avec le même type).
     */
    public final boolean bidirectionnel;

    /** Description humaine. */
    public final String description;

    TypeRelation(String label, String icone, Color couleur,
                 float confianceBase, float frequenceContact,
                 boolean adresseCommune, boolean bidirectionnel,
                 String description) {
        this.label           = label;
        this.icone           = icone;
        this.couleur         = couleur;
        this.confianceBase   = confianceBase;
        this.frequenceContact = frequenceContact;
        this.adresseCommune  = adresseCommune;
        this.bidirectionnel  = bidirectionnel;
        this.description     = description;
    }

    /** Modificateur de bienveillance lors de la réception d'un message de ce type de relation. */
    public float modificateurBienveillance() {
        return switch (this) {
            case EPOUX, COUPLE           ->  1.5f;
            case PARENT_ENFANT           ->  1.2f;
            case ENFANT_PARENT           ->  1.0f;
            case FRERE_SOEUR             ->  0.8f;
            case GRAND_PARENT            ->  0.9f;
            case PETIT_ENFANT            ->  0.6f;
            case AMI_PROCHE              ->  0.7f;
            case AMI                     ->  0.4f;
            case COLLEGUE                ->  0.1f;
            case CAMARADE                ->  0.2f;
            case VOISIN                  ->  0.1f;
            case CONNAISSANCE            ->  0.0f;
            case MENTOR                  ->  0.5f;
            case MENTOR_INVERSE          ->  0.3f;
            case SUPERIEUR               -> -0.1f;  // légère pression
            case PARTENAIRE_SPORT        ->  0.3f;
            case PARTENAIRE_CLUB         ->  0.2f;
            default                      ->  0.0f;
        };
    }

    @Override
    public String toString() { return label; }
}
