package socialnetwork.back.seirs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a physical place of interest (café, work, gym, cinema, etc.)
 * with a time-based activity schedule.
 */
public final class PlaceInteret {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    public enum TypeLieu {
        DOMICILE      ("Domicile",         "#8899cc"),
        TRAVAIL       ("Travail",           "#cc9944"),
        CAFÉ          ("Café",              "#b07040"),
        RESTAURANT    ("Restaurant",        "#dd5533"),
        BAR           ("Bar",              "#8844cc"),
        SUPERMARCHÉ   ("Supermarché",       "#44aa55"),
        PHARMACIE     ("Pharmacie",         "#22ccaa"),
        ÉCOLE         ("École",             "#5588ee"),
        SALLE_SPORT   ("Salle de sport",    "#ee6622"),
        CINÉMA        ("Cinéma",            "#cc4488"),
        PARC          ("Parc",              "#33bb44"),
        MARCHÉ        ("Marché",            "#dd9922"),
        CONCERT       ("Concert",           "#ee3366"),
        DISCOTHÈQUE   ("Discothèque",       "#aa22ff"),
        PLAGE         ("Plage",             "#44bbee"),
        BIBLIOTHÈQUE  ("Bibliothèque",      "#6677aa"),
        MAIRIE        ("Mairie",            "#888855");

        public final String label;
        public final String hexColor;

        TypeLieu(String label, String hexColor) {
            this.label    = label;
            this.hexColor = hexColor;
        }
    }

    /** Hours of the day (0..24) when this place is attractive.
     *  Stored as pairs: [open1, close1, open2, close2, ...]. */
    public static final double[][] DEFAULT_HORAIRES = new double[TypeLieu.values().length][];

    static {
        DEFAULT_HORAIRES[TypeLieu.DOMICILE.ordinal()]     = new double[]{0, 7.0, 22.0, 24};
        DEFAULT_HORAIRES[TypeLieu.TRAVAIL.ordinal()]      = new double[]{8.0, 12.0, 13.0, 18.0};
        DEFAULT_HORAIRES[TypeLieu.CAFÉ.ordinal()]         = new double[]{7.0, 10.0, 14.0, 16.5};
        DEFAULT_HORAIRES[TypeLieu.RESTAURANT.ordinal()]   = new double[]{12.0, 14.0, 19.0, 22.5};
        DEFAULT_HORAIRES[TypeLieu.BAR.ordinal()]          = new double[]{17.0, 24.0};
        DEFAULT_HORAIRES[TypeLieu.SUPERMARCHÉ.ordinal()]  = new double[]{9.0, 13.0, 16.5, 20.0};
        DEFAULT_HORAIRES[TypeLieu.PHARMACIE.ordinal()]    = new double[]{9.0, 12.5, 14.0, 19.0};
        DEFAULT_HORAIRES[TypeLieu.ÉCOLE.ordinal()]        = new double[]{8.0, 12.0, 13.5, 17.0};
        DEFAULT_HORAIRES[TypeLieu.SALLE_SPORT.ordinal()]  = new double[]{7.0, 9.0, 18.0, 21.0};
        DEFAULT_HORAIRES[TypeLieu.CINÉMA.ordinal()]       = new double[]{14.0, 17.5, 19.5, 23.5};
        DEFAULT_HORAIRES[TypeLieu.PARC.ordinal()]         = new double[]{10.0, 20.0};
        DEFAULT_HORAIRES[TypeLieu.MARCHÉ.ordinal()]       = new double[]{8.0, 13.0};
        DEFAULT_HORAIRES[TypeLieu.CONCERT.ordinal()]      = new double[]{19.0, 23.5};
        DEFAULT_HORAIRES[TypeLieu.DISCOTHÈQUE.ordinal()]  = new double[]{23.0, 24.0, 0.0, 4.0};
        DEFAULT_HORAIRES[TypeLieu.PLAGE.ordinal()]        = new double[]{10.0, 19.0};
        DEFAULT_HORAIRES[TypeLieu.BIBLIOTHÈQUE.ordinal()] = new double[]{9.0, 19.0};
        DEFAULT_HORAIRES[TypeLieu.MAIRIE.ordinal()]       = new double[]{9.0, 12.0, 13.30, 17.0};
    }

    private final int    id;
    private final String nom;
    private final TypeLieu type;
    private final Vector2D position;
    private final ZoneManager.ZoneId zone;
    private final int    capaciteMax;
    private final double[] horaires;
    private final boolean summerOnly;
    private volatile int occupants;

    public PlaceInteret(String nom, TypeLieu type, double wx, double wy,
                        ZoneManager.ZoneId zone, int capaciteMax) {
        this(nom, type, wx, wy, zone, capaciteMax, false);
    }

    public PlaceInteret(String nom, TypeLieu type, double wx, double wy,
                        ZoneManager.ZoneId zone, int capaciteMax, boolean summerOnly) {
        this.id          = NEXT_ID.getAndIncrement();
        this.nom         = nom;
        this.type        = type;
        this.position    = new Vector2D(wx, wy);
        this.zone        = zone;
        this.capaciteMax = Math.max(1, capaciteMax);
        this.horaires    = DEFAULT_HORAIRES[type.ordinal()];
        this.summerOnly  = summerOnly;
        this.occupants   = 0;
    }

    /* ── getters ─────────────────────────────────────────────── */

    public int               getId()           { return id; }
    public String            getNom()          { return nom; }
    public TypeLieu          getType()         { return type; }
    public Vector2D          getPosition()     { return position.copy(); }
    public ZoneManager.ZoneId getZone()        { return zone; }
    public int               getCapaciteMax()  { return capaciteMax; }
    public boolean           isSummerOnly()    { return summerOnly; }
    public int               getOccupants()    { return occupants; }
    public void              addOccupant()     { occupants = Math.min(capaciteMax, occupants + 1); }
    public void              removeOccupant()  { occupants = Math.max(0, occupants - 1); }

    /** Returns [0..1] attractiveness at the given hour. */
    public double attractivenessAt(double hour) {
        double h = hour % 24.0;
        if (h < 0) h += 24.0;
        for (int i = 0; i < horaires.length - 1; i += 2) {
            double open  = horaires[i];
            double close = horaires[i + 1];
            // handle wrap-around midnight
            if (open <= close) {
                if (h >= open && h < close) return 1.0;
            } else {
                if (h >= open || h < close) return 1.0;
            }
        }
        return 0.0;
    }

    public boolean isOpenAt(double hour) {
        return attractivenessAt(hour) > 0.0;
    }

    public boolean isFull() {
        return occupants >= capaciteMax;
    }

    /** Direct (non-copy) position reference for rendering (read-only). */
    public Vector2D positionRef() { return position; }

    @Override
    public String toString() {
        return type.label + " \"" + nom + "\"";
    }
}
