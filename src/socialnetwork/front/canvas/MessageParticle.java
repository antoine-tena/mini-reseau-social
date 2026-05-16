package socialnetwork.front.canvas;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Particule visuelle représentant un message en transit entre deux bavards.
 *
 * La particule voyage le long d'une courbe de Bézier quadratique de la source
 * vers la destination. Sa couleur reflète la bienveillance du message.
 * Un effet de traînée (trail) mémorise les dernières positions pour un rendu
 * de type "queue de comète".
 *
 * Cycle de vie :
 *  1. Création : vitesse, couleur, taille basées sur la bienveillance
 *  2. Avancement : progress avance de 0.0 → 1.0
 *  3. Fin : quand progress ≥ 1.0, {@link #estFinie()} retourne true
 *     → le nœud destination pulse alors
 */
public class MessageParticle {

    // =========================================================
    //  CONSTANTES
    // =========================================================

    /** Taille de la traînée. */
    private static final int TAILLE_TRAIL = 10;

    /** Vitesse par défaut (fraction du chemin par milliseconde). */
    private static final float VITESSE_BASE = 0.0012f;

    // =========================================================
    //  IDENTITÉ DU MESSAGE
    // =========================================================

    /** Nom du bavard source. */
    public final String nomSource;

    /** Nom du bavard destination. */
    public final String nomDestination;

    /** Contenu tronqué du message (pour l'affichage tooltip). */
    public final String contenu;

    /** Valeur de bienveillance du message [-10, +10]. */
    public final float bienveillance;

    // =========================================================
    //  ÉTAT D'ANIMATION
    // =========================================================

    /** Progression dans le chemin [0.0, 1.0]. */
    private float progress = 0f;

    /** Vitesse de déplacement (fraction par ms). */
    private final float vitesse;

    /** Couleur principale de la particule. */
    private final Color couleur;

    /** Couleur du halo de glow. */
    private final Color couleurGlow;

    /** Rayon visuel de la particule (pixels). */
    private final float rayon;

    /** Timestamp de création (ms). */
    private final long creationMs;

    /** Dernier timestamp de mise à jour. */
    private long dernierUpdateMs;

    /** Traînée : liste des positions récentes (du plus vieux au plus récent). */
    private final List<Point2D.Float> trail = new ArrayList<>(TAILLE_TRAIL);

    /** Point de contrôle Bézier calculé au premier positionnement. */
    private Point2D.Float controlPoint = null;

    /** Vrai si cette particule vient d'arriver à destination. */
    private boolean arrivee = false;

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    /**
     * Crée une particule de message.
     *
     * @param nomSource      nom du bavard émetteur
     * @param nomDestination nom du bavard récepteur
     * @param bienveillance  valeur de bienveillance du message [-10, +10]
     * @param contenu        contenu textuel du message
     * @param couleur        couleur déjà calculée selon la bienveillance
     */
    public MessageParticle(String nomSource, String nomDestination,
                            float bienveillance, String contenu, Color couleur) {
        this.nomSource      = nomSource;
        this.nomDestination = nomDestination;
        this.bienveillance  = bienveillance;
        this.contenu        = contenu != null && contenu.length() > 40
                              ? contenu.substring(0, 40) + "…"
                              : (contenu != null ? contenu : "");
        this.couleur        = couleur;
        this.couleurGlow    = new Color(
                couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 60
        );
        this.creationMs     = System.currentTimeMillis();
        this.dernierUpdateMs = creationMs;

        // Rayon et vitesse selon l'intensité de la bienveillance
        float intensite = Math.abs(bienveillance) / 10f; // [0, 1]
        this.rayon  = 4f + intensite * 6f;   // [4, 10] pixels
        this.vitesse = VITESSE_BASE * (0.8f + intensite * 0.6f); // [0.0010, 0.0019] par ms
    }

    // =========================================================
    //  ANIMATION
    // =========================================================

    /**
     * Fait avancer la particule selon le temps écoulé.
     * Doit être appelé à chaque frame d'animation.
     *
     * @return true si la particule vient d'arriver (première fois que progress ≥ 1)
     */
    public boolean avancer() {
        long maintenant = System.currentTimeMillis();
        long deltaMs    = maintenant - dernierUpdateMs;
        dernierUpdateMs = maintenant;

        if (progress >= 1f) return false;

        progress = Math.min(1f, progress + vitesse * deltaMs);

        return progress >= 1f && !arrivee;
    }

    /**
     * Marque la particule comme arrivée.
     */
    public void marquerArrivee() {
        arrivee = true;
    }

    /**
     * Calcule la position actuelle sur la courbe de Bézier quadratique.
     *
     * @param src position du nœud source
     * @param dst position du nœud destination
     * @return position interpolée [x, y]
     */
    public Point2D.Float getPosition(Point2D src, Point2D dst) {
        if (src == null || dst == null) return new Point2D.Float(0, 0);

        // Calcul du point de contrôle (courbure de la courbe)
        if (controlPoint == null) {
            controlPoint = calculerPointControle(src, dst);
        }

        // Formule de Bézier quadratique : B(t) = (1-t)²P0 + 2(1-t)t·P1 + t²P2
        float t  = progress;
        float t1 = 1f - t;
        float x  = t1 * t1 * (float) src.getX()
                 + 2f * t1 * t * controlPoint.x
                 + t  * t  * (float) dst.getX();
        float y  = t1 * t1 * (float) src.getY()
                 + 2f * t1 * t * controlPoint.y
                 + t  * t  * (float) dst.getY();

        Point2D.Float pos = new Point2D.Float(x, y);

        // Mise à jour de la traînée
        trail.add(new Point2D.Float(x, y));
        if (trail.size() > TAILLE_TRAIL) {
            trail.remove(0);
        }

        return pos;
    }

    /**
     * Calcule un point de contrôle décalé latéralement pour courber la trajectoire.
     * Évite que toutes les particules entre deux nœuds suivent exactement la même ligne.
     */
    private Point2D.Float calculerPointControle(Point2D src, Point2D dst) {
        float mx = (float)(src.getX() + dst.getX()) / 2f;
        float my = (float)(src.getY() + dst.getY()) / 2f;

        // Vecteur perpendiculaire normalisé
        float dx = (float)(dst.getX() - src.getX());
        float dy = (float)(dst.getY() - src.getY());
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len < 1f) return new Point2D.Float(mx, my);

        float nx = -dy / len;
        float ny =  dx / len;

        // Courbure proportionnelle à la distance (max 60px de déviation)
        float courbure = Math.min(60f, len * 0.2f);

        // Alternance gauche/droite selon le hashCode des noms
        int signe = ((nomSource.hashCode() ^ nomDestination.hashCode()) % 2 == 0) ? 1 : -1;

        return new Point2D.Float(mx + nx * courbure * signe, my + ny * courbure * signe);
    }

    // =========================================================
    //  GETTERS
    // =========================================================

    public boolean estFinie()          { return progress >= 1f; }
    public boolean estArrivee()        { return arrivee; }
    public float   getProgress()       { return progress; }
    public Color   getCouleur()        { return couleur; }
    public Color   getCouleurGlow()    { return couleurGlow; }
    public float   getRayon()          { return rayon; }
    public List<Point2D.Float> getTrail() { return trail; }

    /** Alpha de la particule (commence plein, finit en fondu). */
    public int getAlpha() {
        // Légère apparition au début, fondu à la fin
        if (progress < 0.1f) {
            return (int)(progress * 10f * 255);
        } else if (progress > 0.85f) {
            return (int)((1f - (progress - 0.85f) / 0.15f) * 255);
        }
        return 255;
    }

    /** Alpha d'un point de la traînée selon son index (0 = le plus vieux). */
    public int getAlphaTrail(int index) {
        float ratio = (float) index / TAILLE_TRAIL;
        return (int)(ratio * ratio * 180 * (getAlpha() / 255f));
    }

    /** Retourne le rayon du halo de glow (plus grand que la particule). */
    public float getRayonGlow() {
        return rayon * 2.5f;
    }

    /** Durée de vie en ms depuis la création. */
    public long getDureeMs() {
        return System.currentTimeMillis() - creationMs;
    }

    @Override
    public String toString() {
        return String.format("Particle[%s→%s | b=%.1f | %.0f%%]",
                nomSource, nomDestination, bienveillance, progress * 100);
    }
}
