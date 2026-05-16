package socialnetwork.front.carte;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.monde.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Canvas principal de la carte ville.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  FONCTIONNALITÉS VISUELLES
 * ══════════════════════════════════════════════════════════════════════
 *
 *  🗺  Fond de carte sombre avec grille de rues style "nuit urbaine"
 *  🏢  Lieux rendus comme des cercles lumineux avec icône + label
 *  👤  Bavards comme des points colorés animés entre les lieux
 *  ✈️  Mouvements interpolés avec courbe ease-in-out
 *  🔗  Superposition des relations (lignes colorées par type)
 *  🗺  Minimap en coin inférieur droit
 *  🔍  Zoom molette (centré sur le curseur)
 *  👆  Pan (alt+drag ou clic milieu)
 *  🎯  Follow mode : caméra suit le bavard sélectionné
 *  💬  Tooltip informatif au survol
 *  ⏰  Intégration GestionnaireDeplacements (temps simulé)
 * ══════════════════════════════════════════════════════════════════════
 */
@SuppressWarnings("serial")
public class CarteVilleCanvas extends JPanel {

    // ─── Données ─────────────────────────────────────────────────────────────

    private VilleSociale          ville;
    private GestionnaireDeplacements gestionnaire;

    // ─── Caméra ──────────────────────────────────────────────────────────────

    private final CameraVille camera = new CameraVille();

    // ─── État interactif ─────────────────────────────────────────────────────

    private Bavard bavardSelectionne = null;
    private Bavard bavardSurvole     = null;
    private Lieu   lieuSurvole       = null;

    private boolean afficherRelations = true;
    private boolean afficherLabels    = true;
    private boolean afficherMinimap   = true;
    private boolean afficherBavards   = true;

    // ─── Pan par drag ────────────────────────────────────────────────────────

    private int dragStartX, dragStartY;
    private boolean enDrag = false;

    // ─── Tooltip ─────────────────────────────────────────────────────────────

    private String tooltipTexte = null;
    private int    tooltipX, tooltipY;

    // ─── Animation ───────────────────────────────────────────────────────────

    private final Timer timerAnimation;

    // ─── Callbacks ───────────────────────────────────────────────────────────

    private Consumer<Bavard> onBavardSelectionne;
    private Consumer<Lieu>   onLieuSelectionne;

    // ─── Minimap ─────────────────────────────────────────────────────────────

    private static final int MINIMAP_W = 180;
    private static final int MINIMAP_H = 135;
    private static final int MINIMAP_MARGE = 12;
    private static final double CARTE_W = 1200;
    private static final double CARTE_H = 900;

    // ─── Particules de message ────────────────────────────────────────────────

    private final CopyOnWriteArrayList<ParticuleCarte> particules = new CopyOnWriteArrayList<>();

    /** Animation légère d'un message sur la carte. */
    private static final class ParticuleCarte {
        double sx, sy, dx, dy; // source, destination
        float  prog;           // [0, 1]
        Color  couleur;
        float  alpha = 1f;

        ParticuleCarte(double sx, double sy, double dx, double dy, Color c) {
            this.sx = sx; this.sy = sy;
            this.dx = dx; this.dy = dy;
            this.couleur = c;
        }

        /** @return true si fini */
        boolean avancer() {
            prog = Math.min(1f, prog + 0.025f);
            if (prog >= 1f) { alpha = Math.max(0f, alpha - 0.1f); }
            return alpha <= 0f;
        }

        double getX() { return sx + (dx - sx) * easeInOut(prog); }
        double getY() { return sy + (dy - sy) * easeInOut(prog); }

        private static double easeInOut(double t) {
            return t < 0.5 ? 2*t*t : -1 + (4 - 2*t)*t;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ══════════════════════════════════════════════════════════════════════════

    public CarteVilleCanvas() {
        setBackground(ThemeCartographie.FOND_CARTE);
        setPreferredSize(new Dimension(1200, 900));
        setFocusable(true);

        // Timer 60fps
        timerAnimation = new Timer(16, e -> frameUpdate());
        timerAnimation.setCoalesce(true);

        enregistrerEvenements();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ──────────────────────────────────────────────────────────────────────────

    public void setVille(VilleSociale ville, GestionnaireDeplacements gestionnaire) {
        this.ville       = ville;
        this.gestionnaire = gestionnaire;
        centrerVue();
        if (!timerAnimation.isRunning()) timerAnimation.start();
    }

    public void centrerVue() {
        camera.setDimensions(getWidth() > 0 ? getWidth() : 1200,
                             getHeight() > 0 ? getHeight() : 900);
        camera.centrerSur(CARTE_W / 2, CARTE_H / 2, 0.9);
    }

    private void frameUpdate() {
        // Mise à jour caméra (suivi smooth)
        if (bavardSelectionne != null && gestionnaire != null) {
            Point2D.Double pos = gestionnaire.getPositionCarte(bavardSelectionne);
            if (pos != null) {
                camera.majCible(pos.x, pos.y);
            }
        }
        camera.tick();

        // Avancer particules
        particules.removeIf(ParticuleCarte::avancer);

        repaint();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  RENDU PRINCIPAL
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        activerAntiAliasing(g2);

        camera.setDimensions(getWidth(), getHeight());

        // ── Fond ──────────────────────────────────────────────────────────
        g2.setColor(ThemeCartographie.FOND_CARTE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // ── Grille de rues ────────────────────────────────────────────────
        dessinerGrille(g2);

        // ── Transformation caméra ─────────────────────────────────────────
        AffineTransform savedTransform = g2.getTransform();
        g2.transform(camera.getTransform());

        // ── Rues stylisées ────────────────────────────────────────────────
        dessinerRues(g2);

        // ── Relations entre bavards ───────────────────────────────────────
        if (afficherRelations && ville != null) {
            dessinerRelations(g2);
        }

        // ── Halos des lieux ───────────────────────────────────────────────
        if (ville != null) {
            dessinerHalosLieux(g2);
        }

        // ── Lieux ─────────────────────────────────────────────────────────
        if (ville != null) {
            dessinerLieux(g2);
        }

        // ── Particules de messages ────────────────────────────────────────
        dessinerParticules(g2);

        // ── Bavards ───────────────────────────────────────────────────────
        if (afficherBavards && ville != null && gestionnaire != null) {
            dessinerBavards(g2);
        }

        // Rétablir la transformation d'origine
        g2.setTransform(savedTransform);

        // ── UI superposée (hors transformation) ──────────────────────────
        if (afficherMinimap) {
            dessinerMinimap(g2);
        }

        dessinerHUD(g2);

        if (tooltipTexte != null) {
            dessinerTooltip(g2, tooltipTexte, tooltipX, tooltipY);
        }

        g2.dispose();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GRILLE DE FOND
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerGrille(Graphics2D g2) {
        // Grille adaptée au zoom (ne dessiner que ce qui est visible)
        double zoom = camera.getZoom();
        int pas = (int) Math.max(20, 80 / zoom);

        double x0 = camera.xMinVisible();
        double x1 = camera.xMaxVisible();
        double y0 = camera.yMinVisible();
        double y1 = camera.yMaxVisible();

        // Lignes secondaires
        g2.setColor(ThemeCartographie.GRILLE);
        g2.setStroke(new BasicStroke(0.5f));

        AffineTransform t = camera.getTransform();
        Graphics2D gt = (Graphics2D) g2.create();
        gt.transform(t);

        int startX = (int)(Math.floor(x0 / pas) * pas);
        int startY = (int)(Math.floor(y0 / pas) * pas);

        for (int x = startX; x <= x1 + pas; x += pas) {
            boolean majeure = (x % (pas * 5)) == 0;
            gt.setColor(majeure ? ThemeCartographie.GRILLE_MAJOR : ThemeCartographie.GRILLE);
            gt.drawLine(x, (int)y0 - pas, x, (int)y1 + pas);
        }
        for (int y = startY; y <= y1 + pas; y += pas) {
            boolean majeure = (y % (pas * 5)) == 0;
            gt.setColor(majeure ? ThemeCartographie.GRILLE_MAJOR : ThemeCartographie.GRILLE);
            gt.drawLine((int)x0 - pas, y, (int)x1 + pas, y);
        }
        gt.dispose();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  RUES STYLISÉES
    // ──────────────────────────────────────────────────────────────────────────

    private static final int[][] RUES_HORIZ = {
        {0, 150, 1200, 150}, {0, 300, 1200, 300}, {0, 450, 1200, 450},
        {0, 600, 1200, 600}, {0, 750, 1200, 750}
    };
    private static final int[][] RUES_VERT = {
        {200, 0, 200, 900}, {400, 0, 400, 900}, {600, 0, 600, 900},
        {800, 0, 800, 900}, {1000, 0, 1000, 900}
    };

    private void dessinerRues(Graphics2D g2) {
        // Rues normales
        g2.setColor(ThemeCartographie.RUE_NORMALE);
        g2.setStroke(new BasicStroke(1.5f));
        for (int[] r : RUES_HORIZ) g2.drawLine(r[0], r[1], r[2], r[3]);
        for (int[] r : RUES_VERT)  g2.drawLine(r[0], r[1], r[2], r[3]);

        // Quelques "boulevards" plus larges
        g2.setColor(ThemeCartographie.RUE_PRINCIPALE);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(0, 300, 1200, 300);
        g2.drawLine(600, 0, 600, 900);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HALOS DES LIEUX
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerHalosLieux(Graphics2D g2) {
        for (Lieu lieu : ville.getLieux()) {
            if (lieu.estHorsCarte()) continue;
            if (!camera.estVisible(lieu.getXCarte(), lieu.getYCarte(), 100)) continue;

            Color c = ThemeCartographie.couleurLieu(lieu.getType());
            int rayon = lieu.getRayon();
            int haloR = (int)(rayon * 2.5);

            RadialGradientPaint halo = ThemeCartographie.haloLieu(
                lieu.getXCarte(), lieu.getYCarte(), haloR, c);
            g2.setPaint(halo);
            g2.fillOval((int)(lieu.getXCarte() - haloR),
                         (int)(lieu.getYCarte() - haloR),
                         haloR * 2, haloR * 2);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  LIEUX
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerLieux(Graphics2D g2) {
        for (Lieu lieu : ville.getLieux()) {
            if (lieu.estHorsCarte()) continue;
            if (!camera.estVisible(lieu.getXCarte(), lieu.getYCarte(), lieu.getRayon() + 20)) continue;

            double cx = lieu.getXCarte();
            double cy = lieu.getYCarte();
            int r     = lieu.getRayon();

            Color cBase = ThemeCartographie.couleurLieu(lieu.getType());
            boolean survole = lieu.equals(lieuSurvole);

            if (survole) cBase = cBase.brighter().brighter();

            // Cercle de fond semi-transparent
            Color fond = new Color(cBase.getRed(), cBase.getGreen(), cBase.getBlue(), 40);
            g2.setColor(fond);
            g2.fillOval((int)(cx - r), (int)(cy - r), r*2, r*2);

            // Contour
            g2.setColor(cBase);
            float epaisseur = survole ? 2.5f : 1.5f;
            g2.setStroke(new BasicStroke(epaisseur));
            g2.drawOval((int)(cx - r), (int)(cy - r), r*2, r*2);

            // Barre de remplissage (taux d'occupation)
            float taux = lieu.tauxRemplissage();
            if (taux > 0.01f) {
                int barW = (int)(r * 2 * taux);
                g2.setColor(new Color(cBase.getRed(), cBase.getGreen(), cBase.getBlue(), 100));
                g2.fillRect((int)(cx - r), (int)(cy - r) - 4, barW, 3);
            }

            // Icône au centre
            if (camera.afficherIcones()) {
                g2.setFont(ThemeCartographie.POLICE_ICONE);
                g2.setColor(cBase);
                FontMetrics fm = g2.getFontMetrics();
                String icone = lieu.getType().icone;
                int iw = fm.stringWidth(icone);
                g2.drawString(icone, (int)(cx - iw/2.0), (int)(cy + 5));
            }

            // Label
            if (afficherLabels && camera.afficherLabels() && camera.getZoom() > 0.6) {
                g2.setFont(ThemeCartographie.POLICE_LIEU);
                dessinerLabel(g2, lieu.getNom(),
                    (int)cx, (int)(cy + r + 14), ThemeCartographie.LIEU_TEXTE);
            }

            // Compteur de bavards
            int nb = lieu.getNbOccupants();
            if (nb > 0 && camera.getZoom() > 0.4) {
                String countStr = nb > 99 ? "99+" : String.valueOf(nb);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                int badgeX = (int)(cx + r - 4);
                int badgeY = (int)(cy - r - 4);
                g2.fillOval(badgeX - 7, badgeY - 9, 16, 14);
                g2.setColor(ThemeCartographie.FOND_CARTE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(countStr, badgeX - 7 + (16 - fm.stringWidth(countStr))/2, badgeY + 1);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  RELATIONS
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerRelations(Graphics2D g2) {
        if (ville == null || gestionnaire == null) return;
        if (camera.getZoom() < 0.4) return; // trop dézoomé

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (RelationSociale rel : ville.getRelations()) {
            if (!rel.isActive()) continue;

            Point2D.Double posA = gestionnaire.getPositionCarte(rel.getSource());
            Point2D.Double posB = gestionnaire.getPositionCarte(rel.getCible());
            if (posA == null || posB == null) continue;

            // Couleur selon type
            Color c = couleurRelation(rel.getType());
            g2.setColor(c);
            g2.setStroke(new BasicStroke(0.8f));

            // Ligne droite simple
            g2.drawLine((int)posA.x, (int)posA.y, (int)posB.x, (int)posB.y);
        }
    }

    private Color couleurRelation(TypeRelation type) {
        return switch (type) {
            case COUPLE, EPOUX -> ThemeCartographie.RELATION_FAMILLE;
            case PARENT_ENFANT, ENFANT_PARENT,
                 FRERE_SOEUR, GRAND_PARENT,
                 PETIT_ENFANT, COUSIN,
                 ONCLE_TANTE -> ThemeCartographie.RELATION_FAMILLE;
            case AMI_PROCHE, AMI -> ThemeCartographie.RELATION_AMI;
            case COLLEGUE, SUPERIEUR,
                 PARTENAIRE_SPORT,
                 PARTENAIRE_CLUB -> ThemeCartographie.RELATION_COLLEGUE;
            default -> ThemeCartographie.RELATION_DEFAULT;
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  BAVARDS
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerBavards(Graphics2D g2) {
        List<Bavard> bavards = ville.getReseau().getBavards();

        for (Bavard b : bavards) {
            Point2D.Double pos = gestionnaire.getPositionCarte(b);
            if (pos == null) continue;
            if (!camera.estVisible(pos.x, pos.y, 20)) continue;

            boolean selectionne = b.equals(bavardSelectionne);
            boolean survole     = b.equals(bavardSurvole);
            StatutPresence statut = ville.getStatut(b);

            Color c = ThemeCartographie.couleurBavard(statut, selectionne);
            int r = selectionne ? ThemeCartographie.BAVARD_RAYON_SELECTED
                                : ThemeCartographie.BAVARD_RAYON;

            double px = pos.x;
            double py = pos.y;

            // Halo pour le sélectionné
            if (selectionne) {
                long t = System.currentTimeMillis();
                float pulse = (float)(0.5 + 0.5 * Math.sin(t / 300.0));
                int haloR = r + 8 + (int)(5 * pulse);
                Color haloC = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(80 * pulse));
                g2.setColor(haloC);
                g2.fillOval((int)(px - haloR), (int)(py - haloR), haloR*2, haloR*2);
            }

            // Ombre
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval((int)(px - r + 1), (int)(py - r + 2), r*2, r*2);

            // Dégradé du bavard
            RadialGradientPaint grad = new RadialGradientPaint(
                new Point2D.Float((float)(px - r*0.3), (float)(py - r*0.3)),
                r,
                new float[]{0f, 1f},
                new Color[]{c.brighter(), c.darker()}
            );
            g2.setPaint(grad);
            g2.fillOval((int)(px - r), (int)(py - r), r*2, r*2);

            // Contour
            g2.setColor(selectionne ? Color.WHITE : c.brighter());
            g2.setStroke(new BasicStroke(selectionne ? 2f : 1f));
            g2.drawOval((int)(px - r), (int)(py - r), r*2, r*2);

            // Indicateur de déplacement (flèche)
            if (statut == StatutPresence.EN_DEPLACEMENT) {
                Deplacement dep = gestionnaire.getDeplacementsActifs().get(b);
                if (dep != null) {
                    dessinerFleche(g2, px, py, dep, c);
                }
            }

            // Label (zoom suffisant)
            if (afficherLabels && (selectionne || survole) && camera.getZoom() > 0.6) {
                g2.setFont(ThemeCartographie.POLICE_BAVARD);
                dessinerLabel(g2, b.getNom(),
                    (int)px, (int)(py - r - 5), Color.WHITE);
            } else if (afficherLabels && camera.getZoom() > 1.2) {
                g2.setFont(ThemeCartographie.POLICE_BAVARD);
                FontMetrics fm = g2.getFontMetrics();
                String nom = b.getNom().split(" ")[0]; // prénom seulement
                g2.setColor(new Color(200, 220, 255, 200));
                g2.drawString(nom,
                    (int)(px - fm.stringWidth(nom)/2.0),
                    (int)(py - r - 3));
            }
        }
    }

    /** Dessine une petite flèche indiquant la direction de déplacement. */
    private void dessinerFleche(Graphics2D g2, double px, double py,
                                  Deplacement dep, Color c) {
        double dx = dep.getLieuDestination().getXCarte() - px;
        double dy = dep.getLieuDestination().getYCarte() - py;
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;

        double nx = dx / dist * 12;
        double ny = dy / dist * 12;

        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine((int)px, (int)py, (int)(px + nx), (int)(py + ny));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PARTICULES
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerParticules(Graphics2D g2) {
        for (ParticuleCarte p : particules) {
            int alpha = (int)(p.alpha * 220);
            Color c = new Color(p.couleur.getRed(), p.couleur.getGreen(),
                                p.couleur.getBlue(), alpha);
            g2.setColor(c);
            double r = 4 * (1 - p.prog * 0.5);
            g2.fillOval((int)(p.getX() - r), (int)(p.getY() - r),
                         (int)(r*2), (int)(r*2));
        }
    }

    /** Déclenche une particule de message entre deux lieux (pour visualisation). */
    public void emettreParticule(Lieu source, Lieu destination, float bienveillance) {
        if (source == null || destination == null) return;
        if (source.estHorsCarte() || destination.estHorsCarte()) return;

        Color c;
        if (bienveillance > 3f)       c = new Color(80, 220, 100);
        else if (bienveillance > 0f)  c = new Color(100, 180, 255);
        else if (bienveillance > -3f) c = new Color(255, 180, 60);
        else                          c = new Color(255, 80, 80);

        particules.add(new ParticuleCarte(
            source.getXCarte(), source.getYCarte(),
            destination.getXCarte(), destination.getYCarte(),
            c));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  MINIMAP
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerMinimap(Graphics2D g2) {
        int mx = getWidth()  - MINIMAP_W - MINIMAP_MARGE;
        int my = getHeight() - MINIMAP_H - MINIMAP_MARGE;

        // Fond
        g2.setColor(ThemeCartographie.FOND_MINIMAP);
        g2.fillRoundRect(mx, my, MINIMAP_W, MINIMAP_H, 8, 8);
        g2.setColor(ThemeCartographie.BORD_MINIMAP);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(mx, my, MINIMAP_W, MINIMAP_H, 8, 8);

        if (ville == null) return;

        double scaleX = MINIMAP_W / CARTE_W;
        double scaleY = MINIMAP_H / CARTE_H;

        // Lieux
        for (Lieu lieu : ville.getLieux()) {
            if (lieu.estHorsCarte()) continue;
            Color c = ThemeCartographie.couleurLieu(lieu.getType());
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 180);
            g2.setColor(c);
            int lx = mx + (int)(lieu.getXCarte() * scaleX);
            int ly = my + (int)(lieu.getYCarte() * scaleY);
            g2.fillOval(lx - 3, ly - 3, 6, 6);
        }

        // Bavards (si pas trop nombreux)
        if (ville.getNbBavards() <= 200 && gestionnaire != null) {
            for (Bavard b : ville.getReseau().getBavards()) {
                Point2D.Double pos = gestionnaire.getPositionCarte(b);
                if (pos == null) continue;
                boolean sel = b.equals(bavardSelectionne);
                g2.setColor(sel ? ThemeCartographie.BAVARD_SELECTIONNE
                                : new Color(100, 180, 255, 150));
                int bx = mx + (int)(pos.x * scaleX);
                int by = my + (int)(pos.y * scaleY);
                g2.fillOval(bx - (sel?2:1), by - (sel?2:1), sel?4:2, sel?4:2);
            }
        }

        // Rectangle viewport
        double vx0 = camera.xMinVisible();
        double vy0 = camera.yMinVisible();
        double vx1 = camera.xMaxVisible();
        double vy1 = camera.yMaxVisible();

        int rx = mx + (int)(vx0 * scaleX);
        int ry = my + (int)(vy0 * scaleY);
        int rw = (int)((vx1 - vx0) * scaleX);
        int rh = (int)((vy1 - vy0) * scaleY);

        g2.setColor(ThemeCartographie.VIEWPORT_RECT);
        g2.fillRect(Math.max(mx, rx), Math.max(my, ry),
                     Math.min(MINIMAP_W, rw), Math.min(MINIMAP_H, rh));
        g2.setColor(ThemeCartographie.VIEWPORT_BORD);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(Math.max(mx, rx), Math.max(my, ry),
                     Math.min(MINIMAP_W, rw), Math.min(MINIMAP_H, rh));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HUD
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerHUD(Graphics2D g2) {
        // Heure simulée
        if (gestionnaire != null) {
            String heure = gestionnaire.getHeureFmt();
            String jour  = gestionnaire.getNomJour();
            String txt   = jour + "  " + heure;

            g2.setFont(ThemeCartographie.POLICE_HEURE);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(txt);

            // Fond
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(getWidth()/2 - tw/2 - 12, 8, tw + 24, 32, 10, 10);

            // Texte
            g2.setColor(new Color(200, 230, 255));
            g2.drawString(txt, getWidth()/2 - tw/2, 30);
        }

        // Zoom indicator
        String zoomStr = String.format("×%.1f", camera.getZoom());
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(120, 150, 200, 180));
        g2.drawString(zoomStr, 8, getHeight() - 10);

        // Ville
        if (ville != null) {
            String info = ville.getNom() + " — " + ville.getNbBavards() + " personnes";
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(120, 150, 200, 200));
            g2.drawString(info, 8, getHeight() - 26);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  TOOLTIP
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerTooltip(Graphics2D g2, String texte, int x, int y) {
        g2.setFont(ThemeCartographie.POLICE_TOOLTIP);
        FontMetrics fm = g2.getFontMetrics();
        String[] lignes = texte.split("\n");
        int maxW = 0;
        for (String l : lignes) maxW = Math.max(maxW, fm.stringWidth(l));
        int h = lignes.length * fm.getHeight();

        int tx = Math.min(x + 14, getWidth() - maxW - 20);
        int ty = y - h - 8;
        if (ty < 10) ty = y + 14;

        g2.setColor(new Color(10, 15, 30, 220));
        g2.fillRoundRect(tx - 8, ty - fm.getAscent() - 4,
                          maxW + 16, h + 12, 8, 8);
        g2.setColor(new Color(80, 120, 200, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(tx - 8, ty - fm.getAscent() - 4,
                          maxW + 16, h + 12, 8, 8);

        g2.setColor(Color.WHITE);
        for (int i = 0; i < lignes.length; i++) {
            g2.drawString(lignes[i], tx, ty + i * fm.getHeight());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UTILITAIRE : label avec ombre
    // ──────────────────────────────────────────────────────────────────────────

    private void dessinerLabel(Graphics2D g2, String texte, int cx, int y, Color c) {
        FontMetrics fm = g2.getFontMetrics();
        int x = cx - fm.stringWidth(texte) / 2;
        g2.setColor(ThemeCartographie.LIEU_TEXTE_OMBRE);
        g2.drawString(texte, x + 1, y + 1);
        g2.setColor(c);
        g2.drawString(texte, x, y);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ÉVÉNEMENTS
    // ──────────────────────────────────────────────────────────────────────────

    private void enregistrerEvenements() {
        // Zoom molette
        addMouseWheelListener(e -> {
            camera.zoomerMolette(e.getWheelRotation() < 0 ? 1 : -1,
                                  e.getX(), e.getY());
        });

        // Pan + sélection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON2
                 || (e.isAltDown() && e.getButton() == MouseEvent.BUTTON1)) {
                    enDrag   = true;
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (enDrag) {
                    enDrag = false;
                    setCursor(Cursor.getDefaultCursor());
                } else if (e.getButton() == MouseEvent.BUTTON1 && !e.isAltDown()) {
                    handleClic(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClic(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (enDrag) {
                    camera.deplacer(e.getX() - dragStartX, e.getY() - dragStartY);
                    dragStartX = e.getX();
                    dragStartY = e.getY();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                detecterSurvol(e.getX(), e.getY());
            }
        });
    }

    private void handleClic(int xE, int yE) {
        if (ville == null || gestionnaire == null) return;

        Point2D.Double posC = camera.ecranVersCarte(xE, yE);
        double seuil = 10.0 / camera.getZoom();

        // Chercher le bavard le plus proche
        Bavard plusProche = null;
        double distMin = seuil;
        for (Bavard b : ville.getReseau().getBavards()) {
            Point2D.Double pos = gestionnaire.getPositionCarte(b);
            if (pos == null) continue;
            double d = posC.distance(pos);
            if (d < distMin) { distMin = d; plusProche = b; }
        }

        if (plusProche != null) {
            selectionnerBavard(plusProche);
            return;
        }

        // Chercher le lieu le plus proche
        double seuilLieu = 20.0 / camera.getZoom();
        Lieu lieuProche = null;
        double dMin = seuilLieu;
        for (Lieu l : ville.getLieux()) {
            if (l.estHorsCarte()) continue;
            double d = Math.hypot(l.getXCarte() - posC.x, l.getYCarte() - posC.y);
            if (d < dMin) { dMin = d; lieuProche = l; }
        }

        if (lieuProche != null) {
            if (onLieuSelectionne != null) onLieuSelectionne.accept(lieuProche);
            return;
        }

        // Désélectionner
        selectionnerBavard(null);
    }

    private void handleDoubleClic(int xE, int yE) {
        if (ville == null) return;
        Point2D.Double posC = camera.ecranVersCarte(xE, yE);

        // Centre sur le lieu le plus proche
        double seuilLieu = 40.0 / camera.getZoom();
        for (Lieu l : ville.getLieux()) {
            if (l.estHorsCarte()) continue;
            double d = Math.hypot(l.getXCarte() - posC.x, l.getYCarte() - posC.y);
            if (d < seuilLieu) {
                camera.centrerSur(l.getXCarte(), l.getYCarte(), 2.0);
                return;
            }
        }

        // Sinon zoom sur le curseur
        camera.zoomer(CameraVille.ZOOM_STEP * CameraVille.ZOOM_STEP, xE, yE);
    }

    private void detecterSurvol(int xE, int yE) {
        if (ville == null || gestionnaire == null) return;

        Point2D.Double posC = camera.ecranVersCarte(xE, yE);
        double seuilBavard = 12.0 / camera.getZoom();
        double seuilLieu   = 20.0 / camera.getZoom();

        Bavard ancienBavard = bavardSurvole;
        Lieu ancienLieu     = lieuSurvole;

        bavardSurvole = null;
        lieuSurvole   = null;
        tooltipTexte  = null;

        // Bavard le plus proche
        double distMin = seuilBavard;
        for (Bavard b : ville.getReseau().getBavards()) {
            Point2D.Double pos = gestionnaire.getPositionCarte(b);
            if (pos == null) continue;
            double d = posC.distance(pos);
            if (d < distMin) { distMin = d; bavardSurvole = b; }
        }

        if (bavardSurvole != null) {
            tooltipTexte = construireTooltipBavard(bavardSurvole);
            tooltipX = xE; tooltipY = yE;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            // Lieu le plus proche
            double dMin = seuilLieu;
            for (Lieu l : ville.getLieux()) {
                if (l.estHorsCarte()) continue;
                double d = Math.hypot(l.getXCarte() - posC.x, l.getYCarte() - posC.y);
                if (d < dMin) { dMin = d; lieuSurvole = l; }
            }
            if (lieuSurvole != null) {
                tooltipTexte = construireTooltipLieu(lieuSurvole);
                tooltipX = xE; tooltipY = yE;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        if (bavardSurvole != ancienBavard || lieuSurvole != ancienLieu) {
            repaint();
        }
    }

    private String construireTooltipBavard(Bavard b) {
        StringBuilder sb = new StringBuilder();
        sb.append(b.getNom()).append("\n");
        sb.append(b.getPersonnalite().getNom()).append("\n");
        sb.append("Statut : ").append(ville.getStatut(b).label).append("\n");
        sb.append("Trait : ").append(ville.getTrait(b).icone).append(" ").append(ville.getTrait(b).nom).append("\n");
        sb.append("Âge : ").append(ville.getAge(b)).append(" ans\n");
        sb.append("Profession : ").append(ville.getProfession(b));
        return sb.toString();
    }

    private String construireTooltipLieu(Lieu l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.getType().icone).append(" ").append(l.getNom()).append("\n");
        sb.append(l.getType().label).append("\n");
        sb.append("Occupants : ").append(l.getNbOccupants()).append("/").append(l.getCapacite()).append("\n");
        sb.append(l.getDescription());
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ──────────────────────────────────────────────────────────────────────────

    private void activerAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    public void selectionnerBavard(Bavard b) {
        this.bavardSelectionne = b;
        if (b != null) {
            camera.suivre(0, 0); // activer le suivi (la cible sera mise à jour dans frameUpdate)
            if (gestionnaire != null) {
                Point2D.Double pos = gestionnaire.getPositionCarte(b);
                if (pos != null) camera.suivre(pos.x, pos.y);
            }
        } else {
            camera.arreterSuivi();
        }
        if (onBavardSelectionne != null) onBavardSelectionne.accept(b);
        repaint();
    }

    public Bavard getBavardSelectionne()     { return bavardSelectionne; }
    public VilleSociale getVille()           { return ville; }
    public GestionnaireDeplacements getGestionnaire() { return gestionnaire; }
    public CameraVille getCamera()           { return camera; }

    public void setAfficherRelations(boolean b) { afficherRelations = b; repaint(); }
    public void setAfficherLabels(boolean b)    { afficherLabels = b;    repaint(); }
    public void setAfficherMinimap(boolean b)   { afficherMinimap = b;   repaint(); }
    public void setAfficherBavards(boolean b)   { afficherBavards = b;   repaint(); }

    public void setOnBavardSelectionne(Consumer<Bavard> cb) { this.onBavardSelectionne = cb; }
    public void setOnLieuSelectionne(Consumer<Lieu> cb)     { this.onLieuSelectionne = cb; }

    public void demarrer()  { if (!timerAnimation.isRunning()) timerAnimation.start(); }
    public void arreter()   { timerAnimation.stop(); }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        camera.setDimensions(w, h);
    }
}
