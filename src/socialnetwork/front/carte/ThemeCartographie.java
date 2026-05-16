package socialnetwork.front.carte;

import socialnetwork.back.monde.TypeLieu;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Thème visuel de la carte ville.
 *
 * Couleurs, polices et paramètres graphiques pour le rendu dark-city.
 */
public final class ThemeCartographie {

    private ThemeCartographie() {}

    // ─── Fond et grille ──────────────────────────────────────────────────────

    public static final Color FOND_CARTE      = new Color(10, 12, 20);
    public static final Color GRILLE          = new Color(25, 30, 50);
    public static final Color GRILLE_MAJOR    = new Color(35, 42, 70);
    public static final Color FOND_MINIMAP    = new Color(0, 0, 0, 200);
    public static final Color BORD_MINIMAP    = new Color(80, 120, 200, 180);

    // ─── Rues ────────────────────────────────────────────────────────────────

    public static final Color RUE_NORMALE     = new Color(40, 50, 75);
    public static final Color RUE_PRINCIPALE  = new Color(60, 75, 110);
    public static final Color RUE_SURLIGNEE   = new Color(100, 140, 220);

    // ─── Lieux ───────────────────────────────────────────────────────────────

    public static final Color LIEU_HALO_ALPHA   = new Color(255, 255, 255, 18);
    public static final Color LIEU_TEXTE        = new Color(220, 230, 255);
    public static final Color LIEU_TEXTE_OMBRE  = new Color(0, 0, 0, 160);
    public static final float LIEU_HALO_RAYON   = 1.8f;   // multiplicateur du rayon

    // ─── Bavards ─────────────────────────────────────────────────────────────

    public static final Color BAVARD_DEFAULT    = new Color(100, 180, 255);
    public static final Color BAVARD_SELECTIONNE= new Color(255, 220, 50);
    public static final Color BAVARD_SURVOLE    = new Color(200, 255, 180);
    public static final Color BAVARD_EN_VACANCES= new Color(255, 160, 60);
    public static final Color BAVARD_DORMANT    = new Color(60, 80, 120);

    public static final int BAVARD_RAYON        = 6;
    public static final int BAVARD_RAYON_SELECTED = 9;

    // ─── Trajectoires ─────────────────────────────────────────────────────────

    public static final Color TRAJET_COULEUR    = new Color(80, 180, 255, 80);
    public static final Stroke TRAJET_STROKE    = new BasicStroke(1.2f,
        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
        new float[]{5f, 4f}, 0f);

    // ─── Relations ───────────────────────────────────────────────────────────

    public static final Color RELATION_FAMILLE  = new Color(255, 120, 120, 70);
    public static final Color RELATION_AMI      = new Color(100, 200, 255, 60);
    public static final Color RELATION_COLLEGUE = new Color(150, 255, 150, 50);
    public static final Color RELATION_DEFAULT  = new Color(120, 120, 180, 40);

    // ─── Viewport minimap ────────────────────────────────────────────────────

    public static final Color VIEWPORT_RECT    = new Color(100, 180, 255, 100);
    public static final Color VIEWPORT_BORD    = new Color(100, 180, 255, 200);

    // ─── Polices ─────────────────────────────────────────────────────────────

    public static final Font POLICE_LIEU       = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font POLICE_BAVARD     = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font POLICE_TOOLTIP    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font POLICE_HEURE      = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font POLICE_ICONE      = new Font("Segoe UI Emoji", Font.PLAIN, 14);

    // ─── Rendu par type de lieu ───────────────────────────────────────────────

    /**
     * Renvoie la couleur de rendu d'un lieu selon son type.
     * Légèrement modifiée par rapport à TypeLieu.couleur pour un rendu nocturne.
     */
    public static Color couleurLieu(TypeLieu type) {
        return switch (type.categorie()) {
            case EDUCATION     -> new Color(120, 180, 255);
            case TRAVAIL       -> new Color(255, 160, 80);
            case CULTURE       -> new Color(200, 130, 255);
            case SPORT_LOISIR  -> new Color(100, 220, 160);
            case COMMERCE      -> new Color(255, 200, 80);
            case INFRASTRUCTURE-> new Color(200, 200, 200);
            case DOMICILE      -> new Color(160, 200, 255);
            case VACANCES      -> new Color(255, 220, 100);
        };
    }

    /**
     * Crée un RadialGradientPaint pour le halo lumineux d'un lieu.
     */
    public static RadialGradientPaint haloLieu(double cx, double cy,
                                                int rayon, Color couleur) {
        float[] fractions = {0f, 0.4f, 1f};
        Color transparent = new Color(couleur.getRed(), couleur.getGreen(),
                                       couleur.getBlue(), 0);
        Color semi = new Color(couleur.getRed(), couleur.getGreen(),
                                couleur.getBlue(), 60);
        Color[] colors = {semi, semi, transparent};
        return new RadialGradientPaint(
            new Point2D.Float((float)cx, (float)cy),
            rayon * LIEU_HALO_RAYON,
            fractions, colors
        );
    }

    /**
     * Couleur d'un bavard selon son statut.
     */
    public static Color couleurBavard(socialnetwork.back.monde.StatutPresence statut,
                                       boolean selectionne) {
        if (selectionne) return BAVARD_SELECTIONNE;
        return switch (statut) {
            case A_DOMICILE    -> new Color(140, 180, 255);
            case AU_TRAVAIL    -> new Color(255, 180, 80);
            case EN_LOISIR     -> new Color(100, 220, 160);
            case EN_VACANCES   -> BAVARD_EN_VACANCES;
            case EN_DEPLACEMENT-> new Color(180, 220, 255);
            case EN_PAUSE      -> new Color(255, 220, 120);
            case EN_SOMMEIL    -> BAVARD_DORMANT;
            case EN_EVENEMENT  -> new Color(255, 130, 200);
        };
    }
}
