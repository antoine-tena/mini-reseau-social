package socialnetwork.front.canvas;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.front.theme.ThemeVisuel;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Algorithme de layout force-directed pour positionner les nœuds du graphe.
 *
 * Implémente l'algorithme de Fruchterman-Reingold (1991) :
 *  - Forces répulsives entre TOUTES les paires de nœuds : F_r = k²/d
 *  - Forces attractives le long des arêtes : F_a = d²/k
 *  - Refroidissement progressif (réduction de la température)
 *  - Gravité vers le centre pour éviter la dispersion infinie
 *
 * Ce layout donne un aspect de "réseau neuronal" :
 * les nœuds fortement connectés (même communauté) se groupent naturellement,
 * les nœuds peu connectés restent à la périphérie.
 *
 * Le layout est exécuté de manière incrémentale : quelques itérations par
 * frame d'animation pour une transition fluide et en temps réel.
 */
public class ForceDirectedLayout {

    // =========================================================
    //  CONSTANTES DE L'ALGORITHME
    // =========================================================

    /** Distance idéale entre nœuds (calculée automatiquement selon la taille). */
    private float k;

    /** Température initiale (contrôle le déplacement maximal par itération). */
    private float temperature;

    /** Facteur de refroidissement par itération [0.9, 0.999]. */
    private static final float FACTEUR_REFROIDISSEMENT = 0.95f;

    /** Température minimale (en dessous : le layout est "figé"). */
    private static final float TEMPERATURE_MIN = 0.5f;

    /** Force de gravité vers le centre. */
    private static final float FORCE_GRAVITE = 0.03f;

    /** Distance minimale entre deux nœuds (évite la division par zéro). */
    private static final float DIST_MIN = 1f;

    /** Multiplicateur de la force répulsive. */
    private static final float COEFF_REPULSION = 0.85f;

    /** Multiplicateur de la force attractive. */
    private static final float COEFF_ATTRACTION = 0.3f;

    // =========================================================
    //  ÉTAT DU LAYOUT
    // =========================================================

    /** Positions actuelles des nœuds. */
    private final Map<String, Point2D.Double> positions  = new HashMap<>();

    /** Vecteurs de vitesse/déplacement. */
    private final Map<String, Point2D.Double> velocities = new HashMap<>();

    /** Forces accumulées pour l'itération courante. */
    private final Map<String, Point2D.Double> forces     = new HashMap<>();

    /** Dimensions du canvas. */
    private int largeur = 800;
    private int hauteur = 600;

    /** Marges internes (les nœuds restent dans [marge, largeur-marge]). */
    private static final int MARGE = 60;

    /** Vrai si le layout a convergé. */
    private boolean converge = false;

    /** Nombre total d'itérations effectuées. */
    private int nbIterations = 0;

    /** Générateur aléatoire pour les positions initiales. */
    private final Random rng = new Random(42L);

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    /**
     * Crée le moteur de layout.
     *
     * @param largeur largeur du canvas en pixels
     * @param hauteur hauteur du canvas en pixels
     */
    public ForceDirectedLayout(int largeur, int hauteur) {
        setDimensions(largeur, hauteur);
    }

    // =========================================================
    //  INITIALISATION
    // =========================================================

    /**
     * Initialise les positions des bavards du réseau.
     * Place les nœuds sur un cercle centré + perturbation aléatoire.
     * Cette disposition initiale aide l'algorithme à converger rapidement.
     *
     * @param reseau le réseau à positionner
     */
    public void initialiser(Reseau reseau) {
        if (reseau == null) return;
        positions.clear();
        velocities.clear();
        forces.clear();
        converge    = false;
        nbIterations = 0;

        List<Bavard> bavards = reseau.getBavards();
        int n = bavards.size();
        if (n == 0) return;

        double cx = largeur / 2.0;
        double cy = hauteur / 2.0;
        double rayon = Math.min(largeur, hauteur) * 0.35;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            double px = cx + rayon * Math.cos(angle) + (rng.nextDouble() - 0.5) * rayon * 0.3;
            double py = cy + rayon * Math.sin(angle) + (rng.nextDouble() - 0.5) * rayon * 0.3;

            // Borne dans le canvas
            px = Math.max(MARGE, Math.min(largeur - MARGE, px));
            py = Math.max(MARGE, Math.min(hauteur - MARGE, py));

            String nom = bavards.get(i).getNom();
            positions.put(nom, new Point2D.Double(px, py));
            velocities.put(nom, new Point2D.Double(0, 0));
            forces.put(nom, new Point2D.Double(0, 0));
        }

        // Température initiale = 20% de la dimension minimale
        temperature = Math.min(largeur, hauteur) * 0.2f;

        // Recalcule k (longueur idéale des arêtes)
        double aire = (double)(largeur - 2 * MARGE) * (hauteur - 2 * MARGE);
        k = (float) Math.sqrt(aire / Math.max(1, n));
    }

    /**
     * Ajoute un nouveau nœud au layout (sans réinitialiser tout).
     * Place le nœud à une position aléatoire proche du centre.
     */
    public void ajouterNoeud(String nom) {
        if (positions.containsKey(nom)) return;
        double px = largeur / 2.0 + (rng.nextDouble() - 0.5) * 200;
        double py = hauteur / 2.0 + (rng.nextDouble() - 0.5) * 200;
        px = Math.max(MARGE, Math.min(largeur - MARGE, px));
        py = Math.max(MARGE, Math.min(hauteur - MARGE, py));
        positions.put(nom, new Point2D.Double(px, py));
        velocities.put(nom, new Point2D.Double(0, 0));
        forces.put(nom, new Point2D.Double(0, 0));
        converge = false;
    }

    // =========================================================
    //  ITÉRATION DE LAYOUT
    // =========================================================

    /**
     * Exécute un certain nombre d'itérations de l'algorithme.
     * Appelé à chaque frame d'animation.
     *
     * @param reseau       le réseau (pour les arêtes)
     * @param nbIterations nombre d'itérations à faire ce frame
     */
    public void iterer(Reseau reseau, int nbIterations) {
        if (reseau == null || positions.isEmpty()) return;
        for (int i = 0; i < nbIterations; i++) {
            if (temperature < TEMPERATURE_MIN) {
                converge = true;
                break;
            }
            uneIteration(reseau);
        }
    }

    /**
     * Une itération complète de Fruchterman-Reingold.
     */
    private void uneIteration(Reseau reseau) {
        List<Bavard> bavards = reseau.getBavards();

        // --- Réinitialisation des forces ---
        for (var entry : forces.entrySet()) {
            entry.getValue().setLocation(0, 0);
        }

        // -------------------------------------------------------
        // 1. Forces répulsives entre toutes les paires
        // -------------------------------------------------------
        for (int i = 0; i < bavards.size(); i++) {
            String nomI = bavards.get(i).getNom();
            Point2D.Double pi = positions.get(nomI);
            if (pi == null) continue;

            for (int j = i + 1; j < bavards.size(); j++) {
                String nomJ = bavards.get(j).getNom();
                Point2D.Double pj = positions.get(nomJ);
                if (pj == null) continue;

                double dx  = pi.x - pj.x;
                double dy  = pi.y - pj.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < DIST_MIN) dist = DIST_MIN;

                // F_r = k² / dist
                double fr = COEFF_REPULSION * k * k / dist;
                double fx = fr * dx / dist;
                double fy = fr * dy / dist;

                Point2D.Double fi = forces.get(nomI);
                Point2D.Double fj = forces.get(nomJ);
                if (fi != null) fi.setLocation(fi.x + fx, fi.y + fy);
                if (fj != null) fj.setLocation(fj.x - fx, fj.y - fy);
            }
        }

        // -------------------------------------------------------
        // 2. Forces attractives le long des arêtes
        // -------------------------------------------------------
        for (Bavard u : bavards) {
            Point2D.Double pu = positions.get(u.getNom());
            if (pu == null) continue;

            for (Bavard v : u.getAmis()) {
                Point2D.Double pv = positions.get(v.getNom());
                if (pv == null) continue;

                double dx   = pv.x - pu.x;
                double dy   = pv.y - pu.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < DIST_MIN) dist = DIST_MIN;

                // F_a = dist² / k
                double fa = COEFF_ATTRACTION * dist * dist / k;
                double fx = fa * dx / dist;
                double fy = fa * dy / dist;

                Point2D.Double fu = forces.get(u.getNom());
                if (fu != null) fu.setLocation(fu.x + fx, fu.y + fy);

                // La force attractive est bidirectionnelle (on ne la compte qu'une fois par nœud)
                // mais pour les amis on l'applique aussi dans l'autre sens
            }
        }

        // -------------------------------------------------------
        // 3. Gravité vers le centre
        // -------------------------------------------------------
        double cx = largeur / 2.0;
        double cy = hauteur / 2.0;

        for (Bavard b : bavards) {
            String nom = b.getNom();
            Point2D.Double pos = positions.get(nom);
            Point2D.Double frc = forces.get(nom);
            if (pos == null || frc == null) continue;

            double dx = cx - pos.x;
            double dy = cy - pos.y;
            frc.setLocation(frc.x + dx * FORCE_GRAVITE, frc.y + dy * FORCE_GRAVITE);
        }

        // -------------------------------------------------------
        // 4. Application des forces avec limitation par la température
        // -------------------------------------------------------
        for (Bavard b : bavards) {
            String nom = b.getNom();
            Point2D.Double pos = positions.get(nom);
            Point2D.Double frc = forces.get(nom);
            if (pos == null || frc == null) continue;

            double fx   = frc.x;
            double fy   = frc.y;
            double len  = Math.sqrt(fx * fx + fy * fy);

            if (len > 0) {
                // Limite le déplacement à la température courante
                double depl = Math.min(len, temperature);
                double nx   = pos.x + (fx / len) * depl;
                double ny   = pos.y + (fy / len) * depl;

                // Borne dans le canvas
                nx = Math.max(MARGE, Math.min(largeur - MARGE, nx));
                ny = Math.max(MARGE, Math.min(hauteur - MARGE, ny));
                pos.setLocation(nx, ny);
            }
        }

        // -------------------------------------------------------
        // 5. Refroidissement
        // -------------------------------------------------------
        temperature *= FACTEUR_REFROIDISSEMENT;
        nbIterations++;
    }

    // =========================================================
    //  RÉCHAUFFEMENT (pour forcer un re-layout après changement)
    // =========================================================

    /**
     * Réchauffe le système (déclenche un re-layout).
     * Utile quand le réseau change (ajout/suppression de bavards).
     */
    public void rechauffer() {
        temperature = Math.min(largeur, hauteur) * 0.1f;
        converge    = false;
    }

    /**
     * Réchauffe fortement (recalcul complet du layout).
     */
    public void recalculerComplet(Reseau reseau) {
        initialiser(reseau);
    }

    // =========================================================
    //  ACCESSEURS
    // =========================================================

    /**
     * Retourne la position d'un nœud par son nom.
     * Retourne null si le nœud est inconnu.
     */
    public Point2D.Double getPosition(String nomBavard) {
        return positions.get(nomBavard);
    }

    /**
     * Retourne une copie de toutes les positions courantes.
     */
    public Map<String, Point2D.Double> getPositions() {
        return new HashMap<>(positions);
    }

    /** Déplace manuellement un nœud (drag & drop). */
    public void setPosition(String nom, double x, double y) {
        Point2D.Double p = positions.get(nom);
        if (p != null) {
            p.setLocation(
                Math.max(MARGE, Math.min(largeur - MARGE, x)),
                Math.max(MARGE, Math.min(hauteur - MARGE, y))
            );
            // Réchauffe légèrement après un déplacement manuel
            rechauffer();
        }
    }

    /** Retourne vrai si le layout a convergé. */
    public boolean estConverge()       { return converge; }
    public float   getTemperature()    { return temperature; }
    public int     getNbIterations()   { return nbIterations; }

    /** Met à jour les dimensions du canvas. */
    public void setDimensions(int largeur, int hauteur) {
        this.largeur = Math.max(100, largeur);
        this.hauteur = Math.max(100, hauteur);
    }

    /**
     * Trouve le nœud le plus proche d'un point donné (en coordonnées canvas).
     * Utilisé pour le survol / sélection à la souris.
     *
     * @param x      position x
     * @param y      position y
     * @param rayon  rayon de détection max
     * @return nom du bavard trouvé, ou null si aucun dans le rayon
     */
    public String trouverNoeudProche(double x, double y, double rayon) {
        String meilleur = null;
        double meilleureDistance = rayon * rayon;

        for (var entry : positions.entrySet()) {
            Point2D.Double p = entry.getValue();
            double dx   = p.x - x;
            double dy   = p.y - y;
            double dist2 = dx * dx + dy * dy;
            if (dist2 < meilleureDistance) {
                meilleureDistance = dist2;
                meilleur = entry.getKey();
            }
        }

        return meilleur;
    }
}
