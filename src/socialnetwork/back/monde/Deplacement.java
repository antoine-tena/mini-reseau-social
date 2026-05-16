package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;

/**
 * Déplacement d'un bavard entre deux lieux.
 *
 * Un Deplacement est créé par le {@link GestionnaireDeplacements} quand
 * un bavard doit se rendre d'un lieu A à un lieu B. Il est résolu
 * progressivement en fonction de la vitesse de déplacement et de la
 * distance entre les deux lieux.
 *
 * La progression [0.0 → 1.0] est utilisée par le canvas pour interpoler
 * la position du bavard sur la carte.
 */
public final class Deplacement {

    // ─── Types de déplacement ────────────────────────────────────────────────

    public enum Mode {
        PIED    ("À pied",    "🚶",  80.0),   // 80 unités carte / min simulée
        VELO    ("Vélo",      "🚲", 200.0),
        VOITURE ("Voiture",   "🚗", 500.0),
        TRAIN   ("Train",     "🚉", 800.0),
        AVION   ("Avion",     "✈️", 2000.0);  // pour les vacances

        public final String label;
        public final String icone;
        public final double vitesseUnitesParMs;  // unités carte par milliseconde simulée

        Mode(String label, String icone, double vitesseUnitesParMin) {
            this.label   = label;
            this.icone   = icone;
            // Convertit vitesse par minute simulée en unités par ms simulée
            this.vitesseUnitesParMs = vitesseUnitesParMin / 60_000.0;
        }
    }

    // ─── Champs ─────────────────────────────────────────────────────────────

    private final Bavard bavard;
    private final Lieu   lieuSource;
    private final Lieu   lieuDestination;
    private final Mode   mode;

    /** Heure simulée de départ (ms simulées depuis le début de la simulation). */
    private final long msDepart;

    /** Durée totale calculée (ms simulées). */
    private final long dureeTotaleMs;

    /** Progression normalisée [0, 1]. 0 = au départ, 1 = arrivé. */
    private float progression;

    /** True si l'arrivée a été traitée (callback déclenché). */
    private boolean arriveeTraitee;

    // ─── Constructeur ────────────────────────────────────────────────────────

    public Deplacement(Bavard bavard, Lieu source, Lieu destination,
                       Mode mode, long msDepart) {
        this.bavard          = bavard;
        this.lieuSource      = source;
        this.lieuDestination = destination;
        this.mode            = mode;
        this.msDepart        = msDepart;
        this.progression     = 0f;
        this.arriveeTraitee  = false;

        // Calcul de la durée selon la distance et la vitesse
        double distance = source.distanceCarte(destination);
        if (distance == Double.MAX_VALUE || distance < 1) {
            distance = 1.0;
        }
        // Temps = distance / vitesse (en ms simulées)
        this.dureeTotaleMs = Math.max(500L, (long)(distance / mode.vitesseUnitesParMs));
    }

    // ─── Avancement du déplacement ───────────────────────────────────────────

    /**
     * Met à jour la progression en fonction du temps simulé courant.
     * @param msCourant temps simulé courant (ms)
     * @return true si le déplacement vient de se terminer (progression atteint 1.0)
     */
    public boolean avancer(long msCourant) {
        if (arriveeTraitee) return false;

        long elapsed = msCourant - msDepart;
        if (elapsed < 0) elapsed = 0;

        float nouvProg = (float)((double) elapsed / dureeTotaleMs);
        progression = Math.min(1.0f, nouvProg);

        if (progression >= 1.0f && !arriveeTraitee) {
            arriveeTraitee = true;
            return true;
        }
        return false;
    }

    // ─── Interpolation de position ───────────────────────────────────────────

    /** Position X interpolée sur la carte. */
    public double getXCarte() {
        if (lieuSource.estHorsCarte() || lieuDestination.estHorsCarte()) {
            // Si l'une des extrémités est hors carte, on reste sur celle qui est visible
            if (!lieuSource.estHorsCarte()) return lieuSource.getXCarte();
            if (!lieuDestination.estHorsCarte()) return lieuDestination.getXCarte();
            return -1;
        }
        double ease = easeInOut(progression);
        return lieuSource.getXCarte() + (lieuDestination.getXCarte() - lieuSource.getXCarte()) * ease;
    }

    /** Position Y interpolée sur la carte. */
    public double getYCarte() {
        if (lieuSource.estHorsCarte() || lieuDestination.estHorsCarte()) {
            if (!lieuSource.estHorsCarte()) return lieuSource.getYCarte();
            if (!lieuDestination.estHorsCarte()) return lieuDestination.getYCarte();
            return -1;
        }
        double ease = easeInOut(progression);
        return lieuSource.getYCarte() + (lieuDestination.getYCarte() - lieuSource.getYCarte()) * ease;
    }

    /** Courbe d'accélération/décélération pour un mouvement naturel. */
    private static double easeInOut(double t) {
        return t < 0.5
            ? 2 * t * t
            : -1 + (4 - 2 * t) * t;
    }

    // ─── Accesseurs ─────────────────────────────────────────────────────────

    public Bavard getbavard()           { return bavard; }
    public Lieu   getLieuSource()       { return lieuSource; }
    public Lieu   getLieuDestination()  { return lieuDestination; }
    public Mode   getMode()             { return mode; }
    public long   getMsDepart()         { return msDepart; }
    public long   getDureeTotaleMs()    { return dureeTotaleMs; }
    public float  getProgression()      { return progression; }
    public boolean isArriveeTraitee()   { return arriveeTraitee; }
    public boolean estTermine()         { return progression >= 1.0f; }

    @Override
    public String toString() {
        return bavard.getNom() + " " + mode.icone + " "
             + lieuSource.getNom() + " → " + lieuDestination.getNom()
             + " (" + Math.round(progression * 100) + "%)";
    }
}
