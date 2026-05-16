package socialnetwork.front.carte;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Caméra avec zoom et panoramique pour la carte ville.
 *
 * La caméra maintient une transformation affine qui mappe les coordonnées
 * de la carte virtuelle (0–1200 × 0–900) vers les coordonnées d'écran.
 *
 * Zoom : centré sur le curseur (comportement standard de carte).
 * Pan : décalage libre (alt+drag ou clic milieu).
 * Follow : la caméra suit doucement une cible (bavard sélectionné).
 */
public final class CameraVille {

    // ─── Paramètres ──────────────────────────────────────────────────────────

    public static final double ZOOM_MIN  = 0.15;
    public static final double ZOOM_MAX  = 10.0;
    public static final double ZOOM_STEP = 1.15;  // multiplicateur par cran de molette

    /** Facteur de lissage pour le mode "follow" (0 → pas de lissage, 1 → instantané). */
    private static final double SMOOTH = 0.08;

    // ─── État ────────────────────────────────────────────────────────────────

    /** Zoom courant (1.0 = taille naturelle). */
    private double zoom     = 1.0;

    /** Décalage courant en pixels écran. */
    private double panX     = 0;
    private double panY     = 0;

    /** Dimensions du panel d'affichage. */
    private int largeur     = 1200;
    private int hauteur     = 900;

    /** True si on suit une cible. */
    private boolean enSuivi = false;

    /** Position cible à suivre (en coordonnées carte). */
    private double cibleX   = 600;
    private double cibleY   = 450;

    // ─── Mise à jour ─────────────────────────────────────────────────────────

    /**
     * Met à jour les dimensions du panel.
     */
    public void setDimensions(int w, int h) {
        this.largeur  = w;
        this.hauteur  = h;
    }

    /**
     * Zoom centré sur un point écran (xE, yE).
     */
    public void zoomer(double facteur, double xE, double yE) {
        double nouveauZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * facteur));
        double ratio = nouveauZoom / zoom;

        // Déplacer le pan pour que le point sous le curseur reste fixe
        panX = xE - ratio * (xE - panX);
        panY = yE - ratio * (yE - panY);

        zoom = nouveauZoom;
    }

    /** Zoom par molette : facteur ZOOM_STEP si delta > 0, 1/ZOOM_STEP sinon. */
    public void zoomerMolette(int delta, double xE, double yE) {
        double f = (delta > 0) ? ZOOM_STEP : (1.0 / ZOOM_STEP);
        zoomer(f, xE, yE);
    }

    /**
     * Pan relatif (décalage en pixels écran).
     */
    public void deplacer(double dx, double dy) {
        panX += dx;
        panY += dy;
        enSuivi = false;  // interrompre le suivi si on pan manuellement
    }

    /**
     * Centre la caméra sur un point carte (xC, yC).
     */
    public void centrerSur(double xC, double yC) {
        panX = largeur  / 2.0 - xC * zoom;
        panY = hauteur / 2.0 - yC * zoom;
        enSuivi = false;
    }

    /**
     * Centre sur un point carte avec un zoom donné.
     */
    public void centrerSur(double xC, double yC, double nouveauZoom) {
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, nouveauZoom));
        centrerSur(xC, yC);
    }

    /**
     * Active le mode suivi sur une cible (coordonnées carte).
     */
    public void suivre(double xC, double yC) {
        this.cibleX  = xC;
        this.cibleY  = yC;
        this.enSuivi = true;
    }

    /** Met à jour la cible de suivi sans activer le suivi. */
    public void majCible(double xC, double yC) {
        this.cibleX = xC;
        this.cibleY = yC;
    }

    public void arreterSuivi() {
        enSuivi = false;
    }

    /**
     * Avance d'un frame : applique le lissage de caméra si en mode suivi.
     * À appeler à chaque frame (60fps).
     */
    public void tick() {
        if (!enSuivi) return;

        // Position écran actuelle du centre
        double cx = largeur  / 2.0;
        double cy = hauteur / 2.0;

        // Position écran de la cible
        double txE = cibleX * zoom + panX;
        double tyE = cibleY * zoom + panY;

        // Interpolation vers le centre
        panX += (cx - txE) * SMOOTH;
        panY += (cy - tyE) * SMOOTH;
    }

    // ─── Conversions ─────────────────────────────────────────────────────────

    /** Convertit un point carte en pixel écran. */
    public Point2D.Double carteVersEcran(double xC, double yC) {
        return new Point2D.Double(xC * zoom + panX, yC * zoom + panY);
    }

    /** Convertit un pixel écran en coordonnées carte. */
    public Point2D.Double ecranVersCarte(double xE, double yE) {
        return new Point2D.Double((xE - panX) / zoom, (yE - panY) / zoom);
    }

    /**
     * Retourne la transformation AffineTransform à appliquer à Graphics2D
     * avant de dessiner les éléments en coordonnées carte.
     */
    public AffineTransform getTransform() {
        AffineTransform at = new AffineTransform();
        at.translate(panX, panY);
        at.scale(zoom, zoom);
        return at;
    }

    // ─── Bounds carte visible ─────────────────────────────────────────────────

    /** Renvoie xMin de la zone carte visible. */
    public double xMinVisible() { return (0       - panX) / zoom; }
    /** Renvoie xMax de la zone carte visible. */
    public double xMaxVisible() { return (largeur  - panX) / zoom; }
    /** Renvoie yMin de la zone carte visible. */
    public double yMinVisible() { return (0       - panY) / zoom; }
    /** Renvoie yMax de la zone carte visible. */
    public double yMaxVisible() { return (hauteur - panY) / zoom; }

    /** True si le point carte (x, y) est dans la zone visible (plus marge). */
    public boolean estVisible(double x, double y, double marge) {
        return x >= xMinVisible() - marge && x <= xMaxVisible() + marge
            && y >= yMinVisible() - marge && y <= yMaxVisible() + marge;
    }

    // ─── Accesseurs ──────────────────────────────────────────────────────────

    public double  getZoom()     { return zoom; }
    public double  getPanX()     { return panX; }
    public double  getPanY()     { return panY; }
    public boolean isEnSuivi()   { return enSuivi; }
    public double  getCibleX()   { return cibleX; }
    public double  getCibleY()   { return cibleY; }
    public int     getLargeur()  { return largeur; }
    public int     getHauteur()  { return hauteur; }

    /** Rayon d'un cercle en unités carte → rayon en pixels écran. */
    public double rayonEcran(double rayonCarte) {
        return rayonCarte * zoom;
    }

    /** True si zoom suffisamment proche pour afficher les labels. */
    public boolean afficherLabels() { return zoom > 0.5; }

    /** True si zoom suffisamment proche pour afficher les icônes de type. */
    public boolean afficherIcones() { return zoom > 0.8; }
}
