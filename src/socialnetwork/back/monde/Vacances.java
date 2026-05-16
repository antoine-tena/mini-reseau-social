package socialnetwork.back.monde;

/**
 * Données de vacances pour un bavard.
 *
 * Quand un bavard est en vacances :
 *  - Son lieu de travail habituel est temporairement désactivé
 *  - Sa destination de vacances remplace son lieu de travail
 *  - Son statut passe à {@link StatutPresence#EN_VACANCES}
 *  - Sa propension à la communication est modifiée (détente ↑, travail ↓)
 */
public final class Vacances {

    // ─── Champs ─────────────────────────────────────────────────────────────

    private final Lieu      destination;
    private final String    descriptionVoyage;
    private final int       jourDebut;   // jour simulé de début
    private final int       jourFin;     // jour simulé de fin
    private final String    pays;        // pays de destination
    private final boolean   estSolo;     // voyage seul ou en groupe

    // Bavards compagnons de voyage (partagent la même destination)
    private final java.util.List<socialnetwork.back.model.Bavard> compagnons
            = new java.util.ArrayList<>();

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public Vacances(Lieu destination, String descriptionVoyage,
                    int jourDebut, int jourFin, String pays, boolean estSolo) {
        this.destination       = destination;
        this.descriptionVoyage = descriptionVoyage;
        this.jourDebut         = jourDebut;
        this.jourFin           = jourFin;
        this.pays              = pays;
        this.estSolo           = estSolo;
    }

    /** Constructeur simplifié (vacances solo, durée = 7 jours à partir de jourDebut). */
    public Vacances(Lieu destination, String pays) {
        this(destination, "Vacances à " + destination.getNom(), 0, 7, pays, true);
    }

    // ─── Méthodes ────────────────────────────────────────────────────────────

    /** True si les vacances sont actives au jour simulé donné. */
    public boolean estActifAuJour(int jourSimule) {
        return jourSimule >= jourDebut && jourSimule <= jourFin;
    }

    /** Durée en jours. */
    public int getDureeJours() {
        return Math.max(1, jourFin - jourDebut);
    }

    public void ajouterCompagnon(socialnetwork.back.model.Bavard b) {
        if (b != null && !compagnons.contains(b)) compagnons.add(b);
    }

    // ─── Accesseurs ─────────────────────────────────────────────────────────

    public Lieu     getDestination()       { return destination; }
    public String   getDescriptionVoyage() { return descriptionVoyage; }
    public int      getJourDebut()         { return jourDebut; }
    public int      getJourFin()           { return jourFin; }
    public String   getPays()              { return pays; }
    public boolean  isEstSolo()            { return estSolo; }
    public java.util.List<socialnetwork.back.model.Bavard> getCompagnons()
                                           { return java.util.Collections.unmodifiableList(compagnons); }

    @Override
    public String toString() {
        return descriptionVoyage + " (" + pays + ", J" + jourDebut + "–J" + jourFin + ")";
    }
}
