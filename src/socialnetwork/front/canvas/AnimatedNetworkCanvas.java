package socialnetwork.front.canvas;

import socialnetwork.back.analyse.DetecteurCommunautes;
import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.observer.BavardObserver;
import socialnetwork.back.personnalite.Personnalite;
import socialnetwork.back.personnalite.PersonnaliteHumaine;
import socialnetwork.front.theme.ThemeVisuel;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Canvas animé de visualisation du réseau social.
 *
 * ===========================================================
 *  CONCEPT VISUEL : RÉSEAU NEURONAL VIVANT
 * ===========================================================
 * Le réseau est affiché comme un graphe force-directed avec un
 * esthétique "neuroscience" :
 *
 *  • Fond sombre avec gradient (espace profond)
 *  • Nœuds = "neurones" : cercles lumineux avec halo radial
 *    - Couleur selon la communauté détectée
 *    - Taille proportionnelle au degré (connexions)
 *    - Pulse/expansion quand un message est reçu
 *  • Arêtes = "synapses" : lignes avec gradient bicolore
 *    - S'illuminent brièvement lors du passage d'un message
 *  • Particules = "impulsions nerveuses" : points lumineux
 *    - Voyagent le long des arêtes en courbes de Bézier
 *    - Couleur : vert = positif, rouge = négatif, bleu = neutre
 *    - Queue de comète pour effet de mouvement
 *  • Halos de communauté : zones colorées translucides groupant
 *    les bavards de la même communauté détectée
 *
 * ===========================================================
 *  INTERACTION
 * ===========================================================
 *  • Souris : zoom par molette, pan par drag, sélection par clic
 *  • Nœud sélectionné : info-bulle avec détails du bavard
 *  • Double-clic : force un recalcul du layout
 *
 * ===========================================================
 *  OBSERVATION
 * ===========================================================
 * Implémente BavardObserver pour être notifié en temps réel
 * de l'arrivée de chaque message → crée une particule animée.
 */
@SuppressWarnings("serial")
public class AnimatedNetworkCanvas extends JPanel implements BavardObserver {

    // =========================================================
    //  CONSTANTES VISUELLES
    // =========================================================

    private static final int RAYON_BASE      = 20;   // rayon de base des nœuds
    private static final int RAYON_MIN       = 14;   // rayon minimal (nœud isolé)
    private static final int RAYON_MAX       = 32;   // rayon maximal (hub)
    private static final int FPS             = 60;   // frames par seconde
    private static final int ITER_PAR_FRAME  = 3;    // itérations de layout par frame
    private static final int MAX_PARTICULES  = 400;  // particules simultanées max
    private static final int EPAISSEUR_MAX   = 5;    // épaisseur max des arêtes

    // =========================================================
    //  ÉTAT DU RÉSEAU
    // =========================================================

    private Reseau reseau;

    /** États d'animation per-nœud : nom → NodeState. */
    private final Map<String, NodeState> nodeStates = new HashMap<>();

    /** Activité des arêtes : "A|B" → intensité [0, 1]. */
    private final Map<String, Float> areteActivite = new HashMap<>();

    /** Compteur de messages par arête : "A|B" → int. */
    private final Map<String, Integer> areteCompteur = new HashMap<>();

    /** Mapping communauté : nom du bavard → index de communauté. */
    private final Map<String, Integer> communautesIndex = new HashMap<>();

    /** Thème visuel actif. */
    private ThemeVisuel theme = ThemeVisuel.get();

    // =========================================================
    //  PARTICULES ANIMÉES
    // =========================================================

    /** File des particules actives (thread-safe pour les callbacks observer). */
    private final CopyOnWriteArrayList<MessageParticle> particules = new CopyOnWriteArrayList<>();

    // =========================================================
    //  LAYOUT FORCE-DIRECTED
    // =========================================================

    private ForceDirectedLayout layout;

    /** Vrai si le layout a été initialisé pour le réseau courant. */
    private boolean layoutInitialise = false;

    // =========================================================
    //  ANIMATION TIMER
    // =========================================================

    private final Timer animTimer;
    private long dernierFrameMs = System.currentTimeMillis();

    // =========================================================
    //  ZOOM & PAN
    // =========================================================

    private double scale       = 1.0;
    private double translateX  = 0.0;
    private double translateY  = 0.0;
    private boolean dragging   = false;
    private Point   dragStart  = null;
    private double  panStartX  = 0;
    private double  panStartY  = 0;

    // =========================================================
    //  SÉLECTION / SURVOL
    // =========================================================

    private String  nomSurvol     = null;   // nœud survolé par la souris
    private String  nomSelectionne = null;  // nœud sélectionné par clic

    /** Callback déclenché quand l'utilisateur sélectionne un nœud. */
    private Consumer<Bavard> onBavardSelectionne = null;

    // =========================================================
    //  OVERLAY D'INFORMATION
    // =========================================================

    private boolean afficherCommunautes = true;
    private boolean afficherStats       = true;
    private boolean afficherLegende     = true;
    private boolean afficherEtiquettes  = true;

    // =========================================================
    //  STATISTIQUES LIVE
    // =========================================================

    private int totalMessagesReçus = 0;
    private float bienveillanceMoyenne = 0f;
    private long frameCount = 0;

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    public AnimatedNetworkCanvas() {
        setBackground(theme.fond);
        setPreferredSize(new Dimension(900, 700));
        setDoubleBuffered(true);
        setFocusable(true);

        layout = new ForceDirectedLayout(900, 700);

        // Timer d'animation à ~60fps
        animTimer = new Timer(1000 / FPS, e -> {
            long maintenant   = System.currentTimeMillis();
            double deltaMs    = maintenant - dernierFrameMs;
            dernierFrameMs    = maintenant;
            onAnimFrame(deltaMs);
            repaint();
        });
        animTimer.setCoalesce(true);

        installMouseListeners();
        installKeyListeners();
    }

    // =========================================================
    //  LIAISON AU RÉSEAU
    // =========================================================

    /**
     * Lie ce canvas à un réseau social.
     * Réinitialise tout et observe tous les bavards existants.
     */
    public void setReseau(Reseau reseau) {
        this.reseau = reseau;
        nodeStates.clear();
        areteActivite.clear();
        areteCompteur.clear();
        communautesIndex.clear();
        particules.clear();
        layoutInitialise = false;
        totalMessagesReçus = 0;
        bienveillanceMoyenne = 0f;
        nomSelectionne = null;
        nomSurvol = null;

        if (reseau == null) return;

        // Observation de tous les bavards existants
        observerTousBavards();

        // Initialisation du layout
        Dimension taille = getSize();
        if (taille.width > 0 && taille.height > 0) {
            layout.setDimensions(taille.width, taille.height);
            layout.initialiser(reseau);
            layoutInitialise = true;
        }

        // Détection des communautés
        actualiserCommunautes();

        // Démarrage de l'animation
        if (!animTimer.isRunning()) {
            animTimer.start();
        }
    }

    /** Enregistre ce canvas comme observer sur tous les bavards du réseau. */
    private void observerTousBavards() {
        if (reseau == null) return;
        for (Bavard b : reseau.getBavards()) {
            b.ajouterObservateur(this);
            if (!nodeStates.containsKey(b.getNom())) {
                NodeState ns = new NodeState();
                ns.setCouleurBase(couleurPersonnalite(b.getPersonnalite()));
                nodeStates.put(b.getNom(), ns);
            }
        }
    }

    /**
     * Rafraîchit la détection des communautés et met à jour les couleurs des nœuds.
     */
    public void actualiserCommunautes() {
        if (reseau == null) return;
        communautesIndex.clear();

        try {
            Map<String, List<Bavard>> communautes =
                    DetecteurCommunautes.detecter(reseau);

            int index = 0;
            for (var entry : communautes.entrySet()) {
                for (Bavard b : entry.getValue()) {
                    communautesIndex.put(b.getNom(), index);
                    NodeState ns = nodeStates.get(b.getNom());
                    if (ns != null) {
                        ns.setIndexCommunaute(index);
                        Color couleurComm = theme.couleurCommunaute(index);
                        ns.setCouleurBase(couleurComm);
                        ns.setCouleurGlow(ThemeVisuel.avecAlpha(couleurComm, 80));
                    }
                }
                index++;
            }
        } catch (Exception ignored) {
            // Si la détection échoue, les nœuds gardent leur couleur de personnalité
        }
    }

    // =========================================================
    //  BOUCLE D'ANIMATION
    // =========================================================

    /**
     * Appelé à chaque frame : met à jour le layout, les particules, les états.
     */
    private void onAnimFrame(double deltaMs) {
        frameCount++;

        // --- Vérification des nouveaux bavards non encore observés ---
        if (reseau != null && frameCount % 60 == 0) {
            observerTousBavards();
            if (!layoutInitialise) {
                Dimension taille = getSize();
                layout.setDimensions(taille.width, taille.height);
                layout.initialiser(reseau);
                layoutInitialise = true;
            } else {
                // Ajoute les nouveaux nœuds au layout
                for (Bavard b : reseau.getBavards()) {
                    if (layout.getPosition(b.getNom()) == null) {
                        layout.ajouterNoeud(b.getNom());
                        layout.rechauffer();
                    }
                }
            }
        }

        // --- Itérations du layout force-directed ---
        if (reseau != null && layoutInitialise) {
            layout.iterer(reseau, ITER_PAR_FRAME);
        }

        // --- Mise à jour des états de nœuds ---
        for (NodeState ns : nodeStates.values()) {
            ns.mettreAJour(deltaMs);
        }

        // --- Décroissance de l'activité des arêtes ---
        areteActivite.replaceAll((k, v) -> Math.max(0f, v - (float)(deltaMs / 2000.0)));

        // --- Avancement des particules ---
        List<MessageParticle> aSupprimer = new ArrayList<>();
        for (MessageParticle p : particules) {
            boolean vientDArriver = p.avancer();
            if (vientDArriver) {
                p.marquerArrivee();
                // Déclenche le pulse sur le nœud destinataire
                NodeState ns = nodeStates.get(p.nomDestination);
                if (ns != null) {
                    ns.declencher(p.getCouleur(), p.bienveillance);
                }
            }
            if (p.estFinie() && p.getDureeMs() > 200) {
                aSupprimer.add(p);
            }
        }
        particules.removeAll(aSupprimer);
    }

    // =========================================================
    //  BAVARD OBSERVER
    // =========================================================

    @Override
    public void messageAjoute(Bavard bavard, MessageEvent event) {
        // Appelé depuis le thread Swing (via Timer ou EDT) — thread-safe
        if (bavard == null || event == null) return;

        totalMessagesReçus++;
        bienveillanceMoyenne = bienveillanceMoyenne * 0.9f + event.getBienveillance() * 0.1f;

        // Trouver le nœud source (avant-dernier dans le chemin)
        List<String> chemin = event.getChemin();
        String nomSource = null;
        if (chemin.size() >= 2) {
            nomSource = chemin.get(chemin.size() - 2);
        } else if (!chemin.isEmpty()) {
            nomSource = chemin.get(0);
        }

        String nomDest = bavard.getNom();

        // Vérifier que les deux nœuds existent dans le layout
        if (nomSource != null && layout.getPosition(nomSource) != null
                && layout.getPosition(nomDest) != null) {

            // Couleur de la particule selon la bienveillance
            Color couleurParticule = theme.couleurBienveillance(event.getBienveillance());

            // Créer la particule si la limite n'est pas atteinte
            if (particules.size() < MAX_PARTICULES) {
                MessageParticle particle = new MessageParticle(
                        nomSource, nomDest,
                        event.getBienveillance(),
                        event.getContenu(),
                        couleurParticule
                );
                particules.add(particle);
            }

            // Illuminer l'arête correspondante
            String cleArete = clefArete(nomSource, nomDest);
            areteActivite.put(cleArete, 1.0f);
            areteCompteur.merge(cleArete, 1, Integer::sum);
        }

        // Pulse sur le nœud destinataire (même sans particule visible)
        NodeState ns = nodeStates.computeIfAbsent(nomDest, k -> {
            NodeState n = new NodeState();
            n.setCouleurBase(couleurPersonnalite(bavard.getPersonnalite()));
            return n;
        });
        ns.declencher(theme.couleurBienveillance(event.getBienveillance()), event.getBienveillance());
    }

    @Override
    public void amisMisAJour(Bavard bavard) {
        // Un bavard a de nouveaux amis : recalcule le layout
        if (layout != null) {
            layout.rechauffer();
        }
        actualiserCommunautes();
    }

    // =========================================================
    //  RENDU PRINCIPAL
    // =========================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Antialiasing de qualité
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,     RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int W = getWidth();
        int H = getHeight();

        // -------------------------------------------------------
        // COUCHE 1 : Fond avec gradient
        // -------------------------------------------------------
        dessFond(g2, W, H);

        if (reseau == null || reseau.getBavards().isEmpty()) {
            dessEtatVide(g2, W, H);
            g2.dispose();
            return;
        }

        // Mise à jour du layout selon la taille courante
        Dimension taille = getSize();
        layout.setDimensions(taille.width, taille.height);

        // Application du zoom/pan
        AffineTransform at = AffineTransform.getTranslateInstance(translateX, translateY);
        at.scale(scale, scale);

        // -------------------------------------------------------
        // COUCHE 2 : Halos de communautés (avant les arêtes)
        // -------------------------------------------------------
        if (afficherCommunautes) {
            g2.setTransform(at);
            dessCommunautes(g2);
        }

        // -------------------------------------------------------
        // COUCHE 3 : Arêtes (avant les nœuds pour être sous eux)
        // -------------------------------------------------------
        g2.setTransform(at);
        dessAretes(g2);

        // -------------------------------------------------------
        // COUCHE 4 : Particules (messages en transit)
        // -------------------------------------------------------
        g2.setTransform(at);
        dessParticules(g2);

        // -------------------------------------------------------
        // COUCHE 5 : Nœuds (bavards)
        // -------------------------------------------------------
        g2.setTransform(at);
        dessNoeuds(g2);

        // -------------------------------------------------------
        // COUCHE 6 : Overlays UI (en coordonnées écran, sans transform)
        // -------------------------------------------------------
        g2.setTransform(new AffineTransform());
        if (afficherStats)   dessStatsOverlay(g2, W, H);
        if (afficherLegende) dessLegende(g2, W, H);
        if (nomSurvol != null) dessTooltip(g2, W, H);

        g2.dispose();
    }

    // =========================================================
    //  COUCHE 1 : FOND
    // =========================================================

    private void dessFond(Graphics2D g2, int W, int H) {
        GradientPaint gp = new GradientPaint(
                0, 0, theme.fondGradientHaut,
                W, H, theme.fondGradientBas
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, W, H);

        // Grille de fond subtile
        g2.setColor(theme.grille);
        g2.setStroke(new BasicStroke(0.5f));
        int pas = 50;
        for (int x = 0; x < W; x += pas) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += pas) g2.drawLine(0, y, W, y);
    }

    // =========================================================
    //  COUCHE 2 : HALOS DE COMMUNAUTÉS
    // =========================================================

    private void dessCommunautes(Graphics2D g2) {
        if (reseau == null || communautesIndex.isEmpty()) return;

        // Grouper les positions par communauté
        Map<Integer, List<Point2D.Double>> groupes = new HashMap<>();
        for (Bavard b : reseau.getBavards()) {
            Integer idx = communautesIndex.get(b.getNom());
            if (idx == null) continue;
            Point2D.Double pos = layout.getPosition(b.getNom());
            if (pos == null) continue;
            groupes.computeIfAbsent(idx, k -> new ArrayList<>()).add(pos);
        }

        for (var entry : groupes.entrySet()) {
            int   idx   = entry.getKey();
            List<Point2D.Double> pts = entry.getValue();
            if (pts.isEmpty()) continue;

            // Calcul du centroïde
            double cx = pts.stream().mapToDouble(p -> p.x).average().orElse(0);
            double cy = pts.stream().mapToDouble(p -> p.y).average().orElse(0);

            // Rayon du halo : demi-diagonale du bounding-box des nœuds + padding
            double maxDist = pts.stream()
                    .mapToDouble(p -> Math.hypot(p.x - cx, p.y - cy))
                    .max().orElse(50);
            double rHalo = Math.max(60, maxDist + 55);

            Color couleurCom = theme.couleurCommunaute(idx);

            // Halo radial avec alpha très bas
            float alpha = theme.communauteHaloAlpha;
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point2D.Double(cx, cy),
                    (float) rHalo,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{
                        ThemeVisuel.avecAlpha(couleurCom, (int)(alpha * 255 * 2.5f)),
                        ThemeVisuel.avecAlpha(couleurCom, (int)(alpha * 255)),
                        ThemeVisuel.avecAlpha(couleurCom, 0)
                    }
            );
            g2.setPaint(rgp);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.fill(new Ellipse2D.Double(cx - rHalo, cy - rHalo, 2 * rHalo, 2 * rHalo));
        }
    }

    // =========================================================
    //  COUCHE 3 : ARÊTES
    // =========================================================

    private void dessAretes(Graphics2D g2) {
        if (reseau == null) return;

        Set<String> traitees = new HashSet<>();

        for (Bavard u : reseau.getBavards()) {
            Point2D.Double pu = layout.getPosition(u.getNom());
            if (pu == null) continue;

            for (Bavard v : u.getAmis()) {
                String cle = clefArete(u.getNom(), v.getNom());
                if (traitees.contains(cle)) continue;
                traitees.add(cle);

                Point2D.Double pv = layout.getPosition(v.getNom());
                if (pv == null) continue;

                float activite  = areteActivite.getOrDefault(cle, 0f);
                int   compteur  = areteCompteur.getOrDefault(cle, 0);

                // Épaisseur selon le trafic (messages passés)
                float epaisseur = Math.min(EPAISSEUR_MAX, 1.2f + compteur * 0.05f);

                // Couleurs des deux extrémités
                Color cu = couleurNoeud(u);
                Color cv = couleurNoeud(v);

                // Gradient le long de l'arête
                GradientPaint gp;
                if (activite > 0.1f) {
                    // Arête active : plus lumineuse
                    Color cuBright = ThemeVisuel.eclaircir(cu, activite * 0.6f);
                    Color cvBright = ThemeVisuel.eclaircir(cv, activite * 0.6f);
                    gp = new GradientPaint(
                            (float)pu.x, (float)pu.y, ThemeVisuel.avecAlpha(cuBright, (int)(150 + activite * 105)),
                            (float)pv.x, (float)pv.y, ThemeVisuel.avecAlpha(cvBright, (int)(150 + activite * 105))
                    );
                    epaisseur *= (1f + activite * 0.8f);
                } else {
                    // Arête inactive : translucide
                    gp = new GradientPaint(
                            (float)pu.x, (float)pu.y, ThemeVisuel.avecAlpha(cu, 80),
                            (float)pv.x, (float)pv.y, ThemeVisuel.avecAlpha(cv, 80)
                    );
                }

                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                g2.drawLine((int)pu.x, (int)pu.y, (int)pv.x, (int)pv.y);

                // Glow de l'arête active (halo supplémentaire)
                if (activite > 0.3f) {
                    Color glowColor = ThemeVisuel.avecAlpha(
                            ThemeVisuel.interpoler(cu, cv, 0.5f), (int)(activite * 60)
                    );
                    g2.setPaint(new GradientPaint(
                            (float)pu.x, (float)pu.y, glowColor,
                            (float)pv.x, (float)pv.y, glowColor
                    ));
                    g2.setStroke(new BasicStroke(epaisseur * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int)pu.x, (int)pu.y, (int)pv.x, (int)pv.y);
                }
            }
        }
    }

    // =========================================================
    //  COUCHE 4 : PARTICULES
    // =========================================================

    private void dessParticules(Graphics2D g2) {
        for (MessageParticle p : particules) {
            Point2D.Double src = layout.getPosition(p.nomSource);
            Point2D.Double dst = layout.getPosition(p.nomDestination);
            if (src == null || dst == null) continue;

            Point2D.Float pos = p.getPosition(src, dst);
            if (pos == null) continue;

            int alpha = p.getAlpha();
            if (alpha <= 0) continue;

            // --- Traînée ---
            List<Point2D.Float> trail = p.getTrail();
            for (int i = 0; i < trail.size() - 1; i++) {
                Point2D.Float tp = trail.get(i);
                int trailAlpha   = p.getAlphaTrail(i);
                if (trailAlpha <= 0) continue;

                float trailRadius = p.getRayon() * (float)(i + 1) / trail.size() * 0.6f;
                Color trailColor  = ThemeVisuel.avecAlpha(p.getCouleur(), trailAlpha);
                g2.setColor(trailColor);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trailAlpha / 255f));
                int r = Math.max(1, (int) trailRadius);
                g2.fillOval((int)(tp.x - r), (int)(tp.y - r), 2 * r, 2 * r);
            }

            // --- Halo de glow ---
            float rayonGlow = p.getRayonGlow();
            Color glowColor = ThemeVisuel.avecAlpha(p.getCouleur(), (int)(alpha * 0.25f));
            RadialGradientPaint glowPaint = new RadialGradientPaint(
                    pos,
                    rayonGlow,
                    new float[]{0f, 1f},
                    new Color[]{glowColor, ThemeVisuel.avecAlpha(p.getCouleur(), 0)}
            );
            g2.setPaint(glowPaint);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2.fill(new Ellipse2D.Float(
                    pos.x - rayonGlow, pos.y - rayonGlow,
                    2 * rayonGlow, 2 * rayonGlow
            ));

            // --- Noyau brillant ---
            float r = p.getRayon();
            Color particuleColor = ThemeVisuel.avecAlpha(p.getCouleur(), alpha);
            Color centreBlanc    = ThemeVisuel.avecAlpha(
                    ThemeVisuel.eclaircir(p.getCouleur(), 0.7f), alpha
            );
            RadialGradientPaint nodePaint = new RadialGradientPaint(
                    new Point2D.Float(pos.x - r * 0.3f, pos.y - r * 0.3f),
                    r,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{centreBlanc, particuleColor, ThemeVisuel.assombrir(particuleColor, 0.3f)}
            );
            g2.setPaint(nodePaint);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2.fill(new Ellipse2D.Float(pos.x - r, pos.y - r, 2 * r, 2 * r));
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    }

    // =========================================================
    //  COUCHE 5 : NŒUDS
    // =========================================================

    private void dessNoeuds(Graphics2D g2) {
        if (reseau == null) return;

        FontMetrics fm = g2.getFontMetrics(theme.policeEtiquette);
        g2.setFont(theme.policeEtiquette);

        for (Bavard b : reseau.getBavards()) {
            Point2D.Double pos = layout.getPosition(b.getNom());
            if (pos == null) continue;

            NodeState ns = nodeStates.computeIfAbsent(b.getNom(), k -> {
                NodeState n = new NodeState();
                n.setCouleurBase(couleurPersonnalite(b.getPersonnalite()));
                return n;
            });

            int degre = b.getAmis().size();
            int rayon = Math.max(RAYON_MIN, Math.min(RAYON_MAX, RAYON_BASE + degre * 2));

            float scale = ns.getScale();
            int r = (int)(rayon * scale);

            int cx = (int) pos.x;
            int cy = (int) pos.y;

            Color couleurBase = ns.getCouleurBase();
            boolean survole   = b.getNom().equals(nomSurvol);
            boolean selection = b.getNom().equals(nomSelectionne);

            // -------------------------------------------------------
            // A. Halo externe (glow ambiant)
            // -------------------------------------------------------
            float glowIntensity = ns.getGlowIntensity();
            if (glowIntensity > 0.01f) {
                int numCouches = 5;
                for (int i = numCouches; i >= 1; i--) {
                    float alpha   = glowIntensity * 0.12f * i;
                    int   rGlow   = r + i * 7;
                    g2.setColor(ThemeVisuel.avecAlpha(couleurBase, (int)(alpha * 255)));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                    g2.fillOval(cx - rGlow, cy - rGlow, 2 * rGlow, 2 * rGlow);
                }
            }

            // -------------------------------------------------------
            // B. Anneau de pulse
            // -------------------------------------------------------
            if (ns.estEnPulse()) {
                float rPulse    = r + ns.getRayonPulseExtra();
                int   alphaPulse = ns.getAlphaPulse();
                Color pulseCoul = ns.getCouleurPulse() != null ? ns.getCouleurPulse() : couleurBase;
                g2.setColor(ThemeVisuel.avecAlpha(pulseCoul, alphaPulse));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(
                        (int)(cx - rPulse), (int)(cy - rPulse),
                        (int)(2 * rPulse),  (int)(2 * rPulse)
                );

                // Deuxième anneau plus large
                float rPulse2 = r + ns.getRayonPulseExtra() * 0.6f;
                g2.setColor(ThemeVisuel.avecAlpha(pulseCoul, alphaPulse / 2));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(
                        (int)(cx - rPulse2), (int)(cy - rPulse2),
                        (int)(2 * rPulse2),  (int)(2 * rPulse2)
                );
            }

            // -------------------------------------------------------
            // C. Cercle de sélection
            // -------------------------------------------------------
            if (selection) {
                g2.setColor(theme.noeudSelectionne);
                g2.setStroke(new BasicStroke(3f));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                int rSel = r + 6;
                g2.drawOval(cx - rSel, cy - rSel, 2 * rSel, 2 * rSel);
            } else if (survole) {
                g2.setColor(ThemeVisuel.avecAlpha(theme.noeudSurvol, 200));
                g2.setStroke(new BasicStroke(2f));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                int rSurv = r + 4;
                g2.drawOval(cx - rSurv, cy - rSurv, 2 * rSurv, 2 * rSurv);
            }

            // -------------------------------------------------------
            // D. Corps du nœud (RadialGradientPaint)
            // -------------------------------------------------------
            Color couleurCentre = ThemeVisuel.eclaircir(couleurBase, 0.5f);
            Color couleurBord   = ThemeVisuel.assombrir(couleurBase, 0.2f);

            // Teinte selon la bienveillance moyenne reçue
            float bienveillanceMoy = ns.getBienveillanceMoyenne();
            if (Math.abs(bienveillanceMoy) > 0.5f) {
                Color teinte = theme.couleurBienveillance(bienveillanceMoy * 5f);
                couleurCentre = ThemeVisuel.interpoler(couleurCentre, teinte, 0.3f);
            }

            RadialGradientPaint noeudPaint = new RadialGradientPaint(
                    new Point2D.Float(cx - r * 0.3f, cy - r * 0.3f),
                    r,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{couleurCentre, couleurBase, couleurBord}
            );
            g2.setPaint(noeudPaint);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);

            // -------------------------------------------------------
            // E. Bordure brillante
            // -------------------------------------------------------
            g2.setColor(ThemeVisuel.avecAlpha(
                    ThemeVisuel.eclaircir(couleurBase, 0.5f),
                    selection ? 255 : (survole ? 200 : 140)
            ));
            g2.setStroke(new BasicStroke(selection ? 2.5f : 1.5f));
            g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);

            // -------------------------------------------------------
            // F. Indicateur de personnalité humaine (mini-barre)
            // -------------------------------------------------------
            dessIndicateurHumeur(g2, b, cx, cy, r);

            // -------------------------------------------------------
            // G. Étiquette du nom
            // -------------------------------------------------------
            if (afficherEtiquettes || survole || selection) {
                dessEtiquette(g2, fm, b.getNom(), cx, cy + r + 4,
                              selection || survole);
            }
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.setStroke(new BasicStroke(1f));
    }

    // =========================================================
    //  INDICATEUR D'HUMEUR (mini-barre sous le nœud)
    // =========================================================

    private void dessIndicateurHumeur(Graphics2D g2, Bavard b, int cx, int cy, int r) {
        Personnalite p = b.getPersonnalite();
        if (!(p instanceof PersonnaliteHumaine ph)) return;

        // Mini-barre d'humeur [-1, +1] → position
        float humeur = ph.getHumeur();
        int barreW   = r;
        int barreH   = 3;
        int barreX   = cx - barreW / 2;
        int barreY   = cy + r - 3; // à l'intérieur du bas du nœud

        // Fond sombre
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(barreX, barreY, barreW, barreH, 2, 2);

        // Remplissage de la barre d'humeur
        int fill     = (int)((humeur + 1f) / 2f * barreW);
        Color c      = theme.couleurBienveillance(humeur * 10f);
        g2.setColor(c);
        g2.fillRoundRect(barreX, barreY, fill, barreH, 2, 2);
    }

    // =========================================================
    //  ÉTIQUETTE DES NŒUDS
    // =========================================================

    private void dessEtiquette(Graphics2D g2, FontMetrics fm,
                                 String nom, int cx, int y, boolean emphase) {
        int w = fm.stringWidth(nom);
        int h = fm.getHeight();

        // Fond semi-transparent
        g2.setColor(ThemeVisuel.avecAlpha(theme.panneauFond, emphase ? 220 : 160));
        g2.fillRoundRect(cx - w/2 - 4, y - 1, w + 8, h, 6, 6);

        // Texte
        g2.setColor(emphase ? theme.noeudEtiquette : ThemeVisuel.avecAlpha(theme.noeudEtiquette, 200));
        g2.drawString(nom, cx - w/2, y + fm.getAscent() - 1);
    }

    // =========================================================
    //  OVERLAY STATS (coin supérieur gauche)
    // =========================================================

    private void dessStatsOverlay(Graphics2D g2, int W, int H) {
        int x = 12, y = 12;
        int lh = 18; // line height

        String[] lignes = {
            "Réseau : " + (reseau != null ? reseau.getBavards().size() : 0) + " bavards",
            "Particules : " + particules.size(),
            "Messages : " + totalMessagesReçus,
            "Bienveillance moy : " + String.format("%.1f", bienveillanceMoyenne),
            "Communautés : " + communautesIndex.values().stream().mapToInt(i->i).max().orElse(-1) + 1,
            "Layout T° : " + String.format("%.1f", layout.getTemperature()),
        };

        int panW = 220;
        int panH = lignes.length * lh + 16;

        // Fond translucide
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(theme.panneauFond);
        g2.fillRoundRect(x - 4, y - 4, panW, panH, 10, 10);
        g2.setColor(theme.panneauBordure);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x - 4, y - 4, panW, panH, 10, 10);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        // Textes
        g2.setFont(theme.policeStats);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < lignes.length; i++) {
            // Colore la ligne de bienveillance selon sa valeur
            if (i == 3) {
                g2.setColor(theme.couleurBienveillance(bienveillanceMoyenne));
            } else {
                g2.setColor(theme.texteUI);
            }
            g2.drawString(lignes[i], x, y + i * lh + fm.getAscent() + 4);
        }
    }

    // =========================================================
    //  LÉGENDE DES COMMUNAUTÉS (coin inférieur droit)
    // =========================================================

    private void dessLegende(Graphics2D g2, int W, int H) {
        if (communautesIndex.isEmpty()) return;

        // Trouver les noms de communautés
        Map<Integer, String> nomCommunautes = new HashMap<>();
        if (reseau != null) {
            for (Bavard b : reseau.getBavards()) {
                Integer idx = communautesIndex.get(b.getNom());
                if (idx != null) {
                    nomCommunautes.put(idx, "Communauté " + (idx + 1));
                }
            }
        }

        if (nomCommunautes.isEmpty()) return;

        int lh = 20;
        int panW = 160;
        int panH = nomCommunautes.size() * lh + 30;
        int px = W - panW - 10;
        int py = H - panH - 10;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(theme.panneauFond);
        g2.fillRoundRect(px, py, panW, panH, 10, 10);
        g2.setColor(theme.panneauBordure);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px, py, panW, panH, 10, 10);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        // Titre
        g2.setFont(theme.policeTitre);
        g2.setColor(theme.texteUI);
        g2.drawString("Communautés", px + 8, py + 18);

        // Entrées
        g2.setFont(theme.policeStats);
        FontMetrics fm = g2.getFontMetrics();
        int y = py + 32;
        for (var entry : nomCommunautes.entrySet()) {
            Color c = theme.couleurCommunaute(entry.getKey());
            // Cercle coloré
            g2.setColor(c);
            g2.fillOval(px + 8, y - 10, 14, 14);
            g2.setColor(ThemeVisuel.avecAlpha(c, 200));
            g2.drawOval(px + 8, y - 10, 14, 14);
            // Nom
            g2.setColor(theme.texteUI);
            g2.drawString(entry.getValue(), px + 26, y + fm.getAscent() - 10);
            y += lh;
        }
    }

    // =========================================================
    //  TOOLTIP (nœud survolé)
    // =========================================================

    private void dessTooltip(Graphics2D g2, int W, int H) {
        if (nomSurvol == null || reseau == null) return;

        Bavard b = reseau.getBavards().stream()
                .filter(bav -> bav.getNom().equals(nomSurvol))
                .findFirst().orElse(null);
        if (b == null) return;

        Point2D.Double pos = layout.getPosition(b.getNom());
        if (pos == null) return;

        // Conversion position monde → écran
        double sx = pos.x * scale + translateX;
        double sy = pos.y * scale + translateY;

        // Contenu du tooltip
        List<String> lignes = new ArrayList<>();
        lignes.add("▶ " + b.getNom());
        lignes.add("Personnalité : " + b.getPersonnalite().getNom());
        lignes.add("Amis : " + b.getAmis().size());
        lignes.add("Messages reçus : " + b.getMessagesRecus().size());
        lignes.add("Transmissions : " + b.getCompteurTransmissions());

        if (b.getPersonnalite() instanceof PersonnaliteHumaine ph) {
            lignes.add("Humeur : " + String.format("%.2f", ph.getHumeur()));
            lignes.add("Energie : " + String.format("%.0f%%", ph.getEnergie() * 100));
            lignes.add("Stress : " + String.format("%.0f%%", ph.getStress() * 100));
        }

        // Disposition
        g2.setFont(theme.policeTooltip);
        FontMetrics fm = g2.getFontMetrics();
        int lh   = fm.getHeight() + 2;
        int panW = lignes.stream().mapToInt(fm::stringWidth).max().orElse(100) + 20;
        int panH = lignes.size() * lh + 14;

        // Positionnement à droite du nœud si possible
        int tx = (int)sx + 20;
        int ty = (int)sy - panH / 2;
        if (tx + panW > W) tx = (int)sx - panW - 20;
        if (ty < 5) ty = 5;
        if (ty + panH > H) ty = H - panH - 5;

        // Fond
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f));
        g2.setColor(theme.panneauFond);
        g2.fillRoundRect(tx, ty, panW, panH, 12, 12);
        g2.setColor(couleurNoeud(b));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(tx, ty, panW, panH, 12, 12);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        // Textes
        for (int i = 0; i < lignes.size(); i++) {
            if (i == 0) {
                g2.setFont(theme.policeTitre);
                g2.setColor(couleurNoeud(b));
            } else {
                g2.setFont(theme.policeTooltip);
                g2.setColor(theme.texteUI);
            }
            g2.drawString(lignes.get(i), tx + 10, ty + 12 + i * lh + fm.getAscent());
        }
    }

    // =========================================================
    //  ÉTAT VIDE (aucun bavard)
    // =========================================================

    private void dessEtatVide(Graphics2D g2, int W, int H) {
        String msg1 = "Réseau vide";
        String msg2 = "Générez un réseau dans l'onglet 'Réseau' pour commencer";

        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(ThemeVisuel.avecAlpha(theme.texteUI, 120));
        g2.drawString(msg1, (W - fm.stringWidth(msg1)) / 2, H / 2 - 20);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fm = g2.getFontMetrics();
        g2.setColor(ThemeVisuel.avecAlpha(theme.texteSombre, 150));
        g2.drawString(msg2, (W - fm.stringWidth(msg2)) / 2, H / 2 + 15);
    }

    // =========================================================
    //  INTERACTION SOURIS
    // =========================================================

    private void installMouseListeners() {
        // Zoom molette
        addMouseWheelListener(e -> {
            double facteur = (e.getWheelRotation() < 0) ? 1.12 : (1.0 / 1.12);
            double ancienScale = scale;
            scale = Math.max(0.2, Math.min(5.0, scale * facteur));

            // Zoom centré sur la position de la souris
            double mx = e.getX();
            double my = e.getY();
            translateX = mx - (mx - translateX) * (scale / ancienScale);
            translateY = my - (my - translateY) * (scale / ancienScale);
            repaint();
        });

        // Pan & clic
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e) || e.isAltDown()) {
                    dragging  = true;
                    dragStart = e.getPoint();
                    panStartX = translateX;
                    panStartY = translateY;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Convertit les coords écran en coords monde
                    double wx = (e.getX() - translateX) / scale;
                    double wy = (e.getY() - translateY) / scale;

                    String trouve = layout.trouverNoeudProche(wx, wy, RAYON_MAX + 5);

                    // Désélection de l'ancien
                    if (nomSelectionne != null) {
                        NodeState old = nodeStates.get(nomSelectionne);
                        if (old != null) old.setSelectionne(false);
                    }

                    if (trouve != null && !trouve.equals(nomSelectionne)) {
                        nomSelectionne = trouve;
                        NodeState ns = nodeStates.get(trouve);
                        if (ns != null) ns.setSelectionne(true);

                        // Notification
                        if (onBavardSelectionne != null && reseau != null) {
                            Bavard b = reseau.getBavards().stream()
                                    .filter(bav -> bav.getNom().equals(trouve))
                                    .findFirst().orElse(null);
                            if (b != null) onBavardSelectionne.accept(b);
                        }
                    } else {
                        nomSelectionne = null;
                        if (onBavardSelectionne != null) onBavardSelectionne.accept(null);
                    }
                    repaint();
                }

                if (e.getClickCount() == 2) {
                    // Double-clic : recalcule le layout
                    if (reseau != null) {
                        layout.recalculerComplet(reseau);
                    }
                }
            }
        });

        // Survol
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double wx = (e.getX() - translateX) / scale;
                double wy = (e.getY() - translateY) / scale;
                String trouve = layout.trouverNoeudProche(wx, wy, RAYON_MAX + 8);

                // Mise à jour état survol
                if (nomSurvol != null && !nomSurvol.equals(trouve)) {
                    NodeState old = nodeStates.get(nomSurvol);
                    if (old != null) old.setSurvole(false);
                }
                nomSurvol = trouve;
                if (trouve != null) {
                    NodeState ns = nodeStates.get(trouve);
                    if (ns != null) ns.setSurvole(true);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Drag gauche sans Alt : déplace un nœud
                if (SwingUtilities.isLeftMouseButton(e) && !e.isAltDown() && nomSurvol != null) {
                    double wx = (e.getX() - translateX) / scale;
                    double wy = (e.getY() - translateY) / scale;
                    layout.setPosition(nomSurvol, wx, wy);
                    return;
                }
                // Pan
                if (dragging && dragStart != null) {
                    translateX = panStartX + (e.getX() - dragStart.x);
                    translateY = panStartY + (e.getY() - dragStart.y);
                    repaint();
                }
            }
        });
    }

    private void installKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> {
                        // Recentre la vue
                        centrerVue();
                    }
                    case KeyEvent.VK_R -> {
                        // Recalcule le layout
                        if (reseau != null) layout.recalculerComplet(reseau);
                    }
                    case KeyEvent.VK_C -> {
                        // Relance la détection de communautés
                        actualiserCommunautes();
                    }
                }
            }
        });
    }

    // =========================================================
    //  UTILITAIRES
    // =========================================================

    /** Centre et adapte la vue à tous les nœuds visibles. */
    public void centrerVue() {
        if (reseau == null || reseau.getBavards().isEmpty()) return;

        Map<String, Point2D.Double> positions = layout.getPositions();
        if (positions.isEmpty()) return;

        double minX = positions.values().stream().mapToDouble(p -> p.x).min().orElse(0);
        double maxX = positions.values().stream().mapToDouble(p -> p.x).max().orElse(getWidth());
        double minY = positions.values().stream().mapToDouble(p -> p.y).min().orElse(0);
        double maxY = positions.values().stream().mapToDouble(p -> p.y).max().orElse(getHeight());

        double graphW = maxX - minX + 100;
        double graphH = maxY - minY + 100;
        double scaleX = getWidth()  / graphW;
        double scaleY = getHeight() / graphH;
        scale = Math.max(0.3, Math.min(2.0, Math.min(scaleX, scaleY)));

        translateX = getWidth()  / 2.0 - (minX + maxX) / 2.0 * scale;
        translateY = getHeight() / 2.0 - (minY + maxY) / 2.0 * scale;
        repaint();
    }

    /** Clef unique pour une arête non-orientée. */
    private String clefArete(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Couleur d'un nœud selon son état (sélectionné, survolé, communauté). */
    private Color couleurNoeud(Bavard b) {
        NodeState ns = nodeStates.get(b.getNom());
        if (ns != null) return ns.getCouleurBase();
        return couleurPersonnalite(b.getPersonnalite());
    }

    /**
     * Dérive une couleur à partir de la personnalité.
     * Utilisé comme fallback si la communauté n'est pas encore détectée.
     */
    public static Color couleurPersonnalite(Personnalite p) {
        if (p == null) return new Color(80, 120, 200);
        float v = p.getVariationBienveillance();
        if (v > 0.5f) return new Color(60, 200, 100);      // positif : vert
        if (v < -0.5f) return new Color(220, 70, 70);      // négatif : rouge
        return new Color(80, 120, 200);                      // neutre : bleu
    }

    // =========================================================
    //  CONTRÔLES DE L'ANIMATION
    // =========================================================

    /** Démarre l'animation si elle n'est pas déjà en cours. */
    public void demarrer() {
        if (!animTimer.isRunning()) {
            dernierFrameMs = System.currentTimeMillis();
            animTimer.start();
        }
    }

    /** Met en pause l'animation. */
    public void pause() {
        animTimer.stop();
    }

    /** Remet l'animation en route. */
    public void reprendre() {
        demarrer();
    }

    /** Arrête définitivement le canvas (libère les ressources). */
    public void arreter() {
        animTimer.stop();
        particules.clear();
    }

    // =========================================================
    //  CONFIGURATION DE L'AFFICHAGE
    // =========================================================

    public void setAfficherCommunautes(boolean b) { this.afficherCommunautes = b; repaint(); }
    public void setAfficherStats(boolean b)        { this.afficherStats = b;       repaint(); }
    public void setAfficherLegende(boolean b)      { this.afficherLegende = b;     repaint(); }
    public void setAfficherEtiquettes(boolean b)   { this.afficherEtiquettes = b;  repaint(); }
    public void setTheme(ThemeVisuel.Theme t)       { ThemeVisuel.setTheme(t); this.theme = ThemeVisuel.get(); repaint(); }

    public void setOnBavardSelectionne(Consumer<Bavard> cb) { this.onBavardSelectionne = cb; }

    public String getNomSelectionne() { return nomSelectionne; }
    public Reseau getReseau()         { return reseau; }
    public ForceDirectedLayout getForceLayout() { return layout; }
    public int getTotalMessagesReçus() { return totalMessagesReçus; }
    public float getBienveillanceMoyenne() { return bienveillanceMoyenne; }

    @Override
    public void addNotify() {
        super.addNotify();
        // Relance l'animation si on était arrêté pendant un setVisible(false)
        if (reseau != null) demarrer();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        animTimer.stop();
    }
}
