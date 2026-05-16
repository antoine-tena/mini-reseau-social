package socialnetwork.back.monde;

import java.awt.Color;

/**
 * Type d'un lieu physique dans la ville virtuelle.
 *
 * Chaque type possède :
 *  - un nom court affiché sur la carte
 *  - une icône Unicode (symbole lisible même sans police d'icônes)
 *  - une couleur dominante sur la carte
 *  - un rayon visuel par défaut sur la carte
 *  - une capacité d'accueil typique
 *  - une plage horaire d'ouverture par défaut (heure ouverture, heure fermeture)
 *  - un indicateur : le lieu est-il accessible 24h/24 ?
 */
public enum TypeLieu {

    // ─── Enseignement ──────────────────────────────────────────────────────
    UNIVERSITE(
        "Université",       "🎓",
        new Color(60,  140, 220),  55,  2000,
        7, 22, false,
        "Lieu d'enseignement supérieur et de recherche. " +
        "Favorise les liens entre étudiants et enseignants."
    ),
    LYCEE(
        "Lycée",            "📚",
        new Color(80,  160, 240),  45,  1200,
        7, 18, false,
        "Établissement secondaire. Lieu de rencontres adolescentes."
    ),
    ECOLE_PRIMAIRE(
        "École",            "🏫",
        new Color(100, 170, 255),  35,   400,
        8, 17, false,
        "École élémentaire. Lieu de socialisation de l'enfance."
    ),

    // ─── Travail ────────────────────────────────────────────────────────
    ENTREPRISE(
        "Entreprise",       "🏢",
        new Color(90,  90, 140),   50,   800,
        8, 19, false,
        "Bureau ou usine. Concentre les bavards actifs professionnellement."
    ),
    CENTRALE_ENERGIE(
        "Centrale",         "⚡",
        new Color(255, 200,  50),  60,   300,
        6, 22, false,
        "Site industriel d'énergie. Horaires décalés, travail par équipes."
    ),
    HOPITAL(
        "Hôpital",          "🏥",
        new Color(200,  50,  80),  70,  2500,
        0, 24, true,
        "Centre hospitalier. Ouvert en permanence, stress élevé."
    ),
    LABORATOIRE(
        "Laboratoire",      "🔬",
        new Color(120, 220, 180),  45,   150,
        8, 20, false,
        "Laboratoire de recherche. Ambiance calme et concentrée."
    ),
    ADMINISTRATION(
        "Mairie",           "🏛️",
        new Color(180, 150, 100),  50,   400,
        8, 17, false,
        "Service public administratif."
    ),

    // ─── Loisirs & Culture ──────────────────────────────────────────────
    SALLE_CONCERT(
        "Concert",          "🎸",
        new Color(200,  80, 200),  65,   800,
        18, 3, false,
        "Salle de spectacle et concerts. Ambiance festive nocturne."
    ),
    SALLE_ESCALADE(
        "Escalade",         "🧗",
        new Color(220, 130,  60),  50,   200,
        9, 22, false,
        "Salle d'escalade indoor. Lieu de dépassement de soi."
    ),
    PISCINE(
        "Piscine",          "🏊",
        new Color(30,  180, 220),  55,   500,
        7, 22, false,
        "Complexe aquatique. Ambiance sportive et détendue."
    ),
    SALLE_SPORT(
        "Salle de sport",   "🏋️",
        new Color(255, 120,  40),  50,   300,
        6, 23, false,
        "Salle de fitness / musculation. Lieu de rencontres sportives."
    ),
    YOGA_BIENETRE(
        "Yoga",             "🧘",
        new Color(180, 220, 150),  40,   100,
        7, 21, false,
        "Studio de yoga et bien-être. Ambiance douce et apaisante."
    ),
    CINEMA(
        "Cinéma",           "🎬",
        new Color(150,  60, 180),  55,  1200,
        13, 24, false,
        "Complexe cinématographique."
    ),
    MUSEE(
        "Musée",            "🖼️",
        new Color(160, 130,  80),  50,   600,
        10, 18, false,
        "Musée ou galerie. Lieu de réflexion culturelle."
    ),
    BIBLIOTHEQUE(
        "Bibliothèque",     "📖",
        new Color(100, 160, 120),  45,   400,
        9, 20, false,
        "Bibliothèque publique. Calme et propice aux échanges intellectuels."
    ),
    THEATRE(
        "Théâtre",          "🎭",
        new Color(220,  90, 130),  55,   600,
        19, 23, false,
        "Théâtre municipal. Lieu de vie culturelle intense."
    ),

    // ─── Restauration & Commerce ────────────────────────────────────────
    CAFE_RESTAURANT(
        "Café",             "☕",
        new Color(180, 120,  60),  40,   100,
        7, 23, false,
        "Café ou restaurant. Hub social par excellence."
    ),
    MARCHE(
        "Marché",           "🛒",
        new Color(220, 180,  60),  60,  2000,
        7, 13, false,
        "Marché de plein air. Lieu d'échange convivial hebdomadaire."
    ),
    SUPERMARCHE(
        "Supermarché",      "🛍️",
        new Color(130, 200, 100),  45,  1500,
        8, 21, false,
        "Grande surface alimentaire."
    ),
    BOULANGERIE(
        "Boulangerie",      "🥖",
        new Color(255, 200, 100),  30,    80,
        6, 19, false,
        "Artisan boulanger. Lieu de convivialité matinale."
    ),

    // ─── Nature & Plein air ─────────────────────────────────────────────
    PARC(
        "Parc",             "🌳",
        new Color(60,  180,  80),  80,  5000,
        6, 22, false,
        "Parc public. Espace de détente et de rencontres informelles."
    ),
    MONTAGNE(
        "Montagne",         "🏔️",
        new Color(120, 160, 200), 100, 99999,
        5, 20, false,
        "Espace montagnard. Destination de randonnée et alpinisme."
    ),
    ASSOCIATION(
        "Association",      "🤝",
        new Color(100, 200, 180),  45,   200,
        9, 21, false,
        "Local associatif. Club, bénévolat, passion commune."
    ),

    // ─── Domicile ───────────────────────────────────────────────────────
    DOMICILE(
        "Domicile",         "🏠",
        new Color(200, 180, 120),  35,    10,
        0, 24, true,
        "Lieu de résidence. Base de la routine quotidienne."
    ),

    // ─── Transports & Autres ────────────────────────────────────────────
    GARE(
        "Gare",             "🚉",
        new Color(160, 160, 160),  60,  5000,
        5, 23, false,
        "Gare ferroviaire ou routière. Lieu de passage et de départ."
    ),
    AEROPORT(
        "Aéroport",         "✈️",
        new Color(200, 200, 200),  80, 20000,
        4, 23, false,
        "Aéroport. Porte vers le monde et les vacances."
    ),
    COWORKING(
        "Coworking",        "💻",
        new Color(80,  160, 200),  40,   150,
        8, 20, false,
        "Espace de travail partagé. Très propice au réseautage."
    ),

    // ─── Destinations vacances ──────────────────────────────────────────
    DESTINATION_VACANCES(
        "Vacances",         "🌴",
        new Color(255, 210,  80),  60, 99999,
        0, 24, true,
        "Destination de vacances. Le bavard est hors de sa routine habituelle."
    );

    // ─── Champs ─────────────────────────────────────────────────────────

    /** Nom affiché sur la carte. */
    public final String label;

    /** Icône Unicode. */
    public final String icone;

    /** Couleur dominante sur la carte. */
    public final Color couleur;

    /** Rayon visuel par défaut (en unités carte). */
    public final int rayonDefaut;

    /** Capacité d'accueil typique. */
    public final int capacite;

    /** Heure d'ouverture par défaut (0–23). */
    public final int heureOuverture;

    /** Heure de fermeture par défaut (0–24, 24 = minuit+). */
    public final int heureFermeture;

    /** True si le lieu est ouvert 24h/24. */
    public final boolean ouvert24h;

    /** Description humaine. */
    public final String description;

    TypeLieu(String label, String icone, Color couleur,
             int rayonDefaut, int capacite,
             int heureOuverture, int heureFermeture, boolean ouvert24h,
             String description) {
        this.label          = label;
        this.icone          = icone;
        this.couleur        = couleur;
        this.rayonDefaut    = rayonDefaut;
        this.capacite       = capacite;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
        this.ouvert24h      = ouvert24h;
        this.description    = description;
    }

    /**
     * Renvoie true si le lieu est ouvert à l'heure donnée.
     * @param heure heure de 0 à 23
     */
    public boolean estOuvertA(int heure) {
        if (ouvert24h) return true;
        if (heureOuverture <= heureFermeture) {
            return heure >= heureOuverture && heure < heureFermeture;
        } else {
            // Nocturne : ouvert de 18h à 3h par exemple
            return heure >= heureOuverture || heure < heureFermeture;
        }
    }

    /**
     * Catégorie de lieu pour le regroupement visuel.
     */
    public Categorie categorie() {
        return switch (this) {
            case UNIVERSITE, LYCEE, ECOLE_PRIMAIRE, BIBLIOTHEQUE, LABORATOIRE
                    -> Categorie.EDUCATION;
            case ENTREPRISE, CENTRALE_ENERGIE, HOPITAL, ADMINISTRATION, COWORKING
                    -> Categorie.TRAVAIL;
            case SALLE_CONCERT, CINEMA, MUSEE, THEATRE
                    -> Categorie.CULTURE;
            case SALLE_ESCALADE, PISCINE, SALLE_SPORT, YOGA_BIENETRE, MONTAGNE, ASSOCIATION
                    -> Categorie.SPORT_LOISIR;
            case CAFE_RESTAURANT, MARCHE, SUPERMARCHE, BOULANGERIE
                    -> Categorie.COMMERCE;
            case PARC, GARE, AEROPORT
                    -> Categorie.INFRASTRUCTURE;
            case DOMICILE
                    -> Categorie.DOMICILE;
            case DESTINATION_VACANCES
                    -> Categorie.VACANCES;
        };
    }

    public enum Categorie {
        EDUCATION, TRAVAIL, CULTURE, SPORT_LOISIR, COMMERCE, INFRASTRUCTURE, DOMICILE, VACANCES
    }

    @Override
    public String toString() { return label; }
}
