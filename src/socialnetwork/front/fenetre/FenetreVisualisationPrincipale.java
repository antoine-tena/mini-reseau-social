package socialnetwork.front.fenetre;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.personnalite.*;
import socialnetwork.back.simulation.MoteurSimulation;
import socialnetwork.back.topologie.GenerateurTopologie;
import socialnetwork.back.topologie.GenerateurTopologie.Topologie;
import socialnetwork.front.canvas.AnimatedNetworkCanvas;
import socialnetwork.front.theme.ThemeVisuel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * Fenêtre principale de visualisation animée du réseau social.
 *
 * Architecture :
 *  ┌────────────────────────────────────────────────────────────┐
 *  │  BARRE D'OUTILS (génération rapide, thème, layout)         │
 *  ├───────────────────────────────┬────────────────────────────┤
 *  │                               │  PANNEAU DE CONTRÔLE       │
 *  │  CANVAS ANIMÉ                 │  ┌──────────────────────┐  │
 *  │  (force-directed graph,       │  │ Onglet : Réseau       │  │
 *  │   particules messages,        │  ├──────────────────────┤  │
 *  │   communautés colorées)       │  │ Onglet : Simulation   │  │
 *  │                               │  ├──────────────────────┤  │
 *  │                               │  │ Onglet : Nœud sél.   │  │
 *  │                               │  └──────────────────────┘  │
 *  ├───────────────────────────────┴────────────────────────────┤
 *  │  BARRE D'ÉTAT                                               │
 *  └────────────────────────────────────────────────────────────┘
 */
@SuppressWarnings("serial")
public class FenetreVisualisationPrincipale extends JFrame {

    // =========================================================
    //  COMPOSANTS PRINCIPAUX
    // =========================================================

    private final AnimatedNetworkCanvas canvas;
    private final MoteurSimulation moteur;
    private Reseau reseau;

    // Panneau de contrôle droit
    private JTabbedPane tabbedPane;

    // Onglet "Réseau"
    private JComboBox<Topologie>     comboTopologie;
    private JSpinner                 spinnerNbBavards;
    private JComboBox<String>        comboPersonnalite;
    private JButton                  btnGenerer;
    private JButton                  btnDetectCommunautes;
    private JButton                  btnCentrer;
    private JButton                  btnAjouterBavard;
    private JTextField               txtNomNouveauBavard;

    // Onglet "Simulation"
    private JButton         btnDemarrer;
    private JButton         btnPause;
    private JButton         btnStop;
    private JButton         btnPasAPas;
    private JSlider         sliderVitesse;
    private JLabel          lblVitesse;
    private JComboBox<String> comboScenario;
    private JLabel          lblTickCount;
    private JTextArea       areaJournal;

    // Onglet "Nœud sélectionné"
    private JLabel          lblNomNode;
    private JLabel          lblPersonnaliteNode;
    private JLabel          lblAmisNode;
    private JLabel          lblMessagesNode;
    private JLabel          lblTransmissionsNode;
    private JProgressBar    barHumeur;
    private JProgressBar    barEnergie;
    private JProgressBar    barStress;
    private JButton         btnEnvoyerMessage;
    private JTextField      txtMessageAEnvoyer;
    private JLabel          lblIconPersonnalite;

    // Barre d'état
    private JLabel  lblStatut;
    private JLabel  lblParticules;
    private JLabel  lblFPS;

    // Timer pour rafraîchir la barre d'état
    private final Timer timerStatut;

    // =========================================================
    //  PERSONNALITÉS DISPONIBLES
    // =========================================================

    private static final Map<String, Personnalite> PERSONNALITES;
    static {
        PERSONNALITES = new LinkedHashMap<>();
        PERSONNALITES.put("Positif",         Personnalite.POSITIF);
        PERSONNALITES.put("Négatif",         Personnalite.NEGATIF);
        PERSONNALITES.put("Neutre",          Personnalite.NEUTRE);
        PERSONNALITES.put("Émotif",          PersonnaliteEmotive.EMOTIF);
        PERSONNALITES.put("Très émotif",     PersonnaliteEmotive.TRES_EMOTIF);
        PERSONNALITES.put("Introverti",      PersonnaliteIntrovert.INTROVERTI);
        PERSONNALITES.put("Extraverti",      PersonnaliteExtrovert.EXTRAVERTI);
        PERSONNALITES.put("Rationnel",       PersonnaliteRationnelle.RATIONNEL);
        PERSONNALITES.put("Médiateur",       PersonnaliteMediateur.MEDIATEUR);
        PERSONNALITES.put("Influenceur",     PersonnaliteInfluenceur.INFLUENCEUR);
        PERSONNALITES.put("Mega-influenceur",PersonnaliteInfluenceur.MEGA_INFLUENCEUR);
        PERSONNALITES.put("Dépressif",       PersonnaliteDepressive.DEPRESSIF);
        PERSONNALITES.put("Complotiste",     PersonnaliteComplotiste.COMPLOTISTE);
        PERSONNALITES.put("Bienveillant",    PersonnaliteBienveillant.BIENVEILLANT);
        PERSONNALITES.put("Cynique",         PersonnaliteCynique.CYNIQUE);
        PERSONNALITES.put("Anxieux",         PersonnaliteAnxieux.ANXIEUX);
    }

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    public FenetreVisualisationPrincipale() {
        super("Réseau Social – Visualisation Animée");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        canvas = new AnimatedNetworkCanvas();
        reseau = new Reseau();
        moteur = new MoteurSimulation(reseau);

        construireUI();
        configurerCallbacks();

        pack();
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        // Timer de rafraîchissement de la barre d'état (1fps)
        timerStatut = new Timer(1000, e -> rafraichirStatut());
        timerStatut.start();
    }

    // =========================================================
    //  CONSTRUCTION DE L'INTERFACE
    // =========================================================

    private void construireUI() {
        setLayout(new BorderLayout(0, 0));

        // Barre d'outils
        add(construireToolBar(), BorderLayout.NORTH);

        // Contenu principal : canvas + panneau de contrôle
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                true,
                canvas,
                construirePanneauControle()
        );
        splitPane.setDividerLocation(820);
        splitPane.setDividerSize(6);
        splitPane.setResizeWeight(0.85);
        add(splitPane, BorderLayout.CENTER);

        // Barre d'état
        add(construireBarreStatut(), BorderLayout.SOUTH);

        // Apparence sombre globale
        appliquerThemeSombre();
    }

    // =========================================================
    //  BARRE D'OUTILS
    // =========================================================

    private JToolBar construireToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 50, 100)));
        tb.setBackground(new Color(15, 18, 45));

        // Génération rapide
        tb.add(boutonToolbar("★ Réseau aléatoire", e -> genererRapide(Topologie.ALEATOIRE)));
        tb.add(boutonToolbar("⬡ Petit monde",      e -> genererRapide(Topologie.PETIT_MONDE)));
        tb.add(boutonToolbar("✦ Etoile",           e -> genererRapide(Topologie.ETOILE)));
        tb.add(boutonToolbar("⬭ Anneau",           e -> genererRapide(Topologie.ANNEAU)));
        tb.add(boutonToolbar("▦ Grille",           e -> genererRapide(Topologie.GRILLE)));
        tb.addSeparator();

        // Layout
        tb.add(boutonToolbar("⊙ Centrer",     e -> canvas.centrerVue()));
        tb.add(boutonToolbar("↻ Recharger",   e -> {
            if (reseau != null) canvas.getForceLayout().recalculerComplet(reseau);
        }));
        tb.add(boutonToolbar("⬡ Communautés", e -> canvas.actualiserCommunautes()));
        tb.addSeparator();

        // Thèmes
        JLabel lblTheme = new JLabel("  Thème : ");
        lblTheme.setForeground(new Color(150, 160, 200));
        tb.add(lblTheme);

        JComboBox<String> comboTheme = new JComboBox<>(new String[]{"Neural (sombre)", "Dark", "Clair"});
        comboTheme.setMaximumSize(new Dimension(160, 28));
        comboTheme.setBackground(new Color(25, 30, 65));
        comboTheme.setForeground(new Color(180, 190, 230));
        comboTheme.addActionListener(e -> {
            int idx = comboTheme.getSelectedIndex();
            ThemeVisuel.Theme t = switch (idx) {
                case 1  -> ThemeVisuel.Theme.DARK;
                case 2  -> ThemeVisuel.Theme.LIGHT;
                default -> ThemeVisuel.Theme.NEURAL;
            };
            canvas.setTheme(t);
        });
        tb.add(comboTheme);

        return tb;
    }

    private JButton boutonToolbar(String texte, ActionListener action) {
        JButton btn = new JButton(texte);
        btn.setBackground(new Color(25, 32, 70));
        btn.setForeground(new Color(180, 200, 255));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(50, 65, 140));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(25, 32, 70));
            }
        });
        return btn;
    }

    // =========================================================
    //  PANNEAU DE CONTRÔLE DROIT
    // =========================================================

    private JComponent construirePanneauControle() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setPreferredSize(new Dimension(310, 600));
        tabbedPane.setBackground(new Color(12, 15, 38));
        tabbedPane.setForeground(new Color(180, 200, 255));

        tabbedPane.addTab("Réseau",     construireOngletReseau());
        tabbedPane.addTab("Simulation", construireOngletSimulation());
        tabbedPane.addTab("Nœud",       construireOngletNoeud());

        // Bordure
        JPanel panContent = new JPanel(new BorderLayout());
        panContent.setBackground(new Color(12, 15, 38));
        panContent.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(40, 50, 100)));
        panContent.add(tabbedPane, BorderLayout.CENTER);
        return panContent;
    }

    // =========================================================
    //  ONGLET RÉSEAU
    // =========================================================

    private JPanel construireOngletReseau() {
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
        pan.setBackground(new Color(12, 15, 38));
        pan.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // --- Section : Générer un réseau ---
        pan.add(titreSec("Générer un réseau"));

        comboTopologie = new JComboBox<>(Topologie.values());
        styleCombo(comboTopologie);
        pan.add(labelField("Topologie :", comboTopologie));

        spinnerNbBavards = new JSpinner(new SpinnerNumberModel(12, 3, 50, 1));
        styleSpinner(spinnerNbBavards);
        pan.add(labelField("Nb bavards :", spinnerNbBavards));

        comboPersonnalite = new JComboBox<>(PERSONNALITES.keySet().toArray(new String[0]));
        styleCombo(comboPersonnalite);
        pan.add(labelField("Personnalité :", comboPersonnalite));

        btnGenerer = boutonPrimaire("✦ Générer", new Color(40, 100, 200));
        btnGenerer.addActionListener(e -> genererReseau());
        pan.add(Box.createRigidArea(new Dimension(0, 6)));
        pan.add(btnGenerer);

        pan.add(separateur());

        // --- Section : Ajouter un bavard manuellement ---
        pan.add(titreSec("Ajouter un bavard"));
        txtNomNouveauBavard = new JTextField("Nouveau");
        styleTextField(txtNomNouveauBavard);
        pan.add(labelField("Nom :", txtNomNouveauBavard));

        btnAjouterBavard = boutonPrimaire("+ Ajouter", new Color(30, 120, 60));
        btnAjouterBavard.addActionListener(e -> ajouterNouveauBavard());
        pan.add(Box.createRigidArea(new Dimension(0, 4)));
        pan.add(btnAjouterBavard);

        pan.add(separateur());

        // --- Section : Outils ---
        pan.add(titreSec("Outils visuels"));

        btnDetectCommunautes = boutonSecondaire("⬡ Détecter communautés");
        btnDetectCommunautes.addActionListener(e -> {
            canvas.actualiserCommunautes();
            journaliser("Communautés recalculées.");
        });
        pan.add(Box.createRigidArea(new Dimension(0, 4)));
        pan.add(btnDetectCommunautes);

        btnCentrer = boutonSecondaire("⊙ Centrer la vue");
        btnCentrer.addActionListener(e -> canvas.centrerVue());
        pan.add(Box.createRigidArea(new Dimension(0, 4)));
        pan.add(btnCentrer);

        // Checkboxes d'affichage
        pan.add(separateur());
        pan.add(titreSec("Affichage"));
        pan.add(checkBox("Zones de communauté", true,  canvas::setAfficherCommunautes));
        pan.add(checkBox("Étiquettes nœuds",    true,  canvas::setAfficherEtiquettes));
        pan.add(checkBox("Statistiques",        true,  canvas::setAfficherStats));
        pan.add(checkBox("Légende communautés", true,  canvas::setAfficherLegende));

        pan.add(Box.createVerticalGlue());
        return pan;
    }

    // =========================================================
    //  ONGLET SIMULATION
    // =========================================================

    private JPanel construireOngletSimulation() {
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
        pan.setBackground(new Color(12, 15, 38));
        pan.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        pan.add(titreSec("Contrôle de la simulation"));

        // Boutons de contrôle
        JPanel panBoutons = new JPanel(new GridLayout(1, 4, 4, 0));
        panBoutons.setBackground(new Color(12, 15, 38));
        panBoutons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        btnDemarrer = boutonIcone("▶", new Color(30, 140, 70));
        btnPause    = boutonIcone("⏸", new Color(160, 130, 30));
        btnStop     = boutonIcone("⏹", new Color(160, 40, 40));
        btnPasAPas  = boutonIcone("▶|", new Color(50, 90, 170));

        btnDemarrer.addActionListener(e -> {
            moteur.setReseau(reseau);
            moteur.demarrer();
            journaliser("Simulation démarrée.");
        });
        btnPause.addActionListener(e -> {
            moteur.pause();
            journaliser("Simulation en pause.");
        });
        btnStop.addActionListener(e -> {
            moteur.arreter();
            lblTickCount.setText("Tick : 0");
            journaliser("Simulation arrêtée.");
        });
        btnPasAPas.addActionListener(e -> {
            moteur.setReseau(reseau);
            moteur.pasAPas();
        });

        panBoutons.add(btnDemarrer);
        panBoutons.add(btnPause);
        panBoutons.add(btnStop);
        panBoutons.add(btnPasAPas);
        pan.add(panBoutons);

        pan.add(Box.createRigidArea(new Dimension(0, 8)));

        // Vitesse
        JLabel lbl = new JLabel("Vitesse (ticks/s) :");
        lbl.setForeground(new Color(160, 175, 220));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        pan.add(lbl);

        sliderVitesse = new JSlider(1, 15, 3);
        sliderVitesse.setMajorTickSpacing(3);
        sliderVitesse.setPaintTicks(true);
        sliderVitesse.setOpaque(false);
        sliderVitesse.setForeground(new Color(130, 150, 220));
        sliderVitesse.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sliderVitesse.setAlignmentX(Component.LEFT_ALIGNMENT);
        sliderVitesse.addChangeListener(e -> {
            int v = sliderVitesse.getValue();
            moteur.setVitesse(v);
            lblVitesse.setText(v + " t/s");
        });
        pan.add(sliderVitesse);

        lblVitesse = new JLabel("3 t/s");
        lblVitesse.setForeground(new Color(100, 180, 255));
        lblVitesse.setAlignmentX(Component.LEFT_ALIGNMENT);
        pan.add(lblVitesse);

        pan.add(separateur());

        // Scénarios
        pan.add(titreSec("Scénarios prédéfinis"));

        comboScenario = new JComboBox<>(new String[]{
            "Neutre (équilibré)",
            "Fake News (désinformation)",
            "Contagion positive",
            "Polarisation d'opinion",
            "Influenceur dominant"
        });
        styleCombo(comboScenario);
        pan.add(comboScenario);
        pan.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton btnAppliquerScenario = boutonSecondaire("Appliquer le scénario");
        btnAppliquerScenario.addActionListener(e -> appliquerScenario());
        pan.add(btnAppliquerScenario);

        pan.add(separateur());

        // Tick counter
        lblTickCount = new JLabel("Tick : 0");
        lblTickCount.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTickCount.setForeground(new Color(100, 200, 255));
        lblTickCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        pan.add(lblTickCount);

        moteur.ajouterAuditeurTick(tick -> SwingUtilities.invokeLater(() -> {
            lblTickCount.setText("Tick : " + tick);
        }));

        pan.add(separateur());

        // Journal
        pan.add(titreSec("Journal de simulation"));
        areaJournal = new JTextArea(8, 25);
        areaJournal.setEditable(false);
        areaJournal.setFont(new Font("Consolas", Font.PLAIN, 10));
        areaJournal.setBackground(new Color(8, 10, 25));
        areaJournal.setForeground(new Color(140, 180, 255));
        areaJournal.setLineWrap(true);
        areaJournal.setWrapStyleWord(true);

        JScrollPane scrollJournal = new JScrollPane(areaJournal);
        scrollJournal.setBorder(BorderFactory.createLineBorder(new Color(40, 50, 120)));
        scrollJournal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        scrollJournal.setAlignmentX(Component.LEFT_ALIGNMENT);
        pan.add(scrollJournal);

        pan.add(Box.createVerticalGlue());
        return pan;
    }

    // =========================================================
    //  ONGLET NŒUD SÉLECTIONNÉ
    // =========================================================

    private JPanel construireOngletNoeud() {
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
        pan.setBackground(new Color(12, 15, 38));
        pan.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        pan.add(titreSec("Nœud sélectionné"));

        // Infos de base
        lblNomNode = nouveauLabel("(aucun sélectionné)");
        lblNomNode.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNomNode.setForeground(new Color(200, 220, 255));
        pan.add(lblNomNode);

        pan.add(Box.createRigidArea(new Dimension(0, 4)));
        lblPersonnaliteNode  = infoLabel("Personnalité :", "—");
        lblAmisNode          = infoLabel("Amis :", "—");
        lblMessagesNode      = infoLabel("Messages reçus :", "—");
        lblTransmissionsNode = infoLabel("Transmissions :", "—");

        pan.add(lignePairedLabels(lblPersonnaliteNode));
        pan.add(lignePairedLabels(lblAmisNode));
        pan.add(lignePairedLabels(lblMessagesNode));
        pan.add(lignePairedLabels(lblTransmissionsNode));

        pan.add(separateur());
        pan.add(titreSec("État psychologique"));

        // Barres d'humeur / énergie / stress (uniquement pour PersonnaliteHumaine)
        barHumeur  = creerBarreEtat("Humeur",  new Color(80, 200, 120));
        barEnergie = creerBarreEtat("Énergie", new Color(80, 160, 255));
        barStress  = creerBarreEtat("Stress",  new Color(255, 100, 80));

        pan.add(ligneAvecLabel("Humeur",  barHumeur));
        pan.add(ligneAvecLabel("Énergie", barEnergie));
        pan.add(ligneAvecLabel("Stress",  barStress));

        pan.add(separateur());
        pan.add(titreSec("Envoyer un message"));

        txtMessageAEnvoyer = new JTextField("Hello réseau !");
        styleTextField(txtMessageAEnvoyer);
        pan.add(txtMessageAEnvoyer);
        pan.add(Box.createRigidArea(new Dimension(0, 6)));

        btnEnvoyerMessage = boutonPrimaire("✉ Envoyer", new Color(40, 100, 200));
        btnEnvoyerMessage.setEnabled(false);
        btnEnvoyerMessage.addActionListener(e -> envoyerMessageDepuisNoeud());
        pan.add(btnEnvoyerMessage);

        pan.add(Box.createVerticalGlue());
        return pan;
    }

    // =========================================================
    //  BARRE D'ÉTAT
    // =========================================================

    private JPanel construireBarreStatut() {
        JPanel pan = new JPanel(new BorderLayout());
        pan.setBackground(new Color(10, 12, 32));
        pan.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 50, 100)));
        pan.setPreferredSize(new Dimension(0, 22));

        lblStatut     = new JLabel("  Réseau vide. Générez un réseau pour commencer.");
        lblParticules = new JLabel("");
        lblFPS        = new JLabel("");

        lblStatut.setForeground(new Color(130, 150, 200));
        lblParticules.setForeground(new Color(80, 180, 120));
        lblFPS.setForeground(new Color(80, 140, 180));

        Font fontStat = new Font("Consolas", Font.PLAIN, 11);
        lblStatut.setFont(fontStat);
        lblParticules.setFont(fontStat);
        lblFPS.setFont(fontStat);

        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        droite.setOpaque(false);
        droite.add(lblParticules);
        droite.add(new JLabel(" | "));
        droite.add(lblFPS);
        droite.add(Box.createRigidArea(new Dimension(8, 0)));

        pan.add(lblStatut, BorderLayout.WEST);
        pan.add(droite, BorderLayout.EAST);
        return pan;
    }

    // =========================================================
    //  CALLBACKS CANVAS
    // =========================================================

    private void configurerCallbacks() {
        // Quand un nœud est sélectionné dans le canvas
        canvas.setOnBavardSelectionne(bavard -> {
            mettreAJourOngletNoeud(bavard);
            if (bavard != null) {
                tabbedPane.setSelectedIndex(2); // Basculer sur l'onglet "Nœud"
            }
        });
    }

    // =========================================================
    //  ACTIONS
    // =========================================================

    /** Génère un réseau avec les paramètres des contrôles. */
    private void genererReseau() {
        moteur.arreter();
        canvas.pause();

        reseau = new Reseau();
        Topologie topo   = (Topologie) comboTopologie.getSelectedItem();
        int n            = (int) spinnerNbBavards.getValue();
        String persoNom  = (String) comboPersonnalite.getSelectedItem();
        Personnalite p   = PERSONNALITES.getOrDefault(persoNom, Personnalite.NEUTRE);

        // Crée un mélange de personnalités pour un réseau plus réaliste
        Personnalite[] persos = creerMelangePersonnalites(n);

        switch (topo) {
            case ANNEAU      -> GenerateurTopologie.anneau(reseau, n, persos);
            case ETOILE      -> GenerateurTopologie.etoile(reseau, n, persos);
            case GRILLE      -> GenerateurTopologie.grille(reseau, (int)Math.sqrt(n)+1, Math.max(1,(int)Math.sqrt(n)), persos);
            case PETIT_MONDE -> GenerateurTopologie.petitMonde(reseau, n, 4, 0.3, persos, 42L);
            case ALEATOIRE   -> GenerateurTopologie.aleatoire(reseau, n, 0.30, persos, 42L);
            case COMPLET     -> GenerateurTopologie.complet(reseau, Math.min(n, 15), persos);
        }

        canvas.setReseau(reseau);
        moteur.setReseau(reseau);

        journaliser("Réseau '" + topo + "' généré avec " + reseau.getBavards().size() + " bavards.");
        lblStatut.setText("  Réseau '" + topo + "' — " + reseau.getBavards().size() + " bavards.");
        canvas.centrerVue();
        canvas.demarrer();
    }

    /** Génération rapide via la barre d'outils (12 bavards, personnalités mélangées). */
    private void genererRapide(Topologie topo) {
        comboTopologie.setSelectedItem(topo);
        genererReseau();
    }

    /** Crée un mélange varié de personnalités pour un réseau plus intéressant. */
    private Personnalite[] creerMelangePersonnalites(int n) {
        Personnalite[] disponibles = PERSONNALITES.values().toArray(new Personnalite[0]);
        Personnalite[] resultat    = new Personnalite[n];
        Random rng = new Random();
        for (int i = 0; i < n; i++) {
            resultat[i] = disponibles[rng.nextInt(disponibles.length)];
        }
        return resultat;
    }

    /** Ajoute un nouveau bavard manuellement. */
    private void ajouterNouveauBavard() {
        if (reseau == null) { reseau = new Reseau(); }
        String nom = txtNomNouveauBavard.getText().trim();
        if (nom.isEmpty()) nom = "Bavard" + (reseau.getBavards().size() + 1);

        String persoNom = (String) comboPersonnalite.getSelectedItem();
        Personnalite p  = PERSONNALITES.getOrDefault(persoNom, Personnalite.NEUTRE);
        Bavard nouveau  = new Bavard(nom, p);
        reseau.ajouterBavard(nouveau);

        canvas.setReseau(reseau);
        moteur.setReseau(reseau);
        canvas.getForceLayout().ajouterNoeud(nom);
        journaliser("Bavard '" + nom + "' ajouté (" + persoNom + ").");
    }

    /** Applique le scénario de simulation sélectionné. */
    private void appliquerScenario() {
        int idx = comboScenario.getSelectedIndex();
        switch (idx) {
            case 0 -> moteur.configurerScenarioNeutre();
            case 1 -> moteur.configurerScenarioFakeNews();
            case 2 -> moteur.configurerScenarioContagionPositive();
            case 3 -> moteur.configurerScenarioPolarisation();
            case 4 -> {
                // Choisit le bavard le plus connecté comme influenceur
                if (reseau != null) {
                    Optional<Bavard> hub = reseau.getBavards().stream()
                            .max(Comparator.comparingInt(b -> b.getAmis().size()));
                    hub.ifPresent(moteur::configurerScenarioInfluenceur);
                }
            }
        }
        journaliser("Scénario appliqué : " + comboScenario.getSelectedItem());
    }

    /** Envoie un message depuis le nœud sélectionné. */
    private void envoyerMessageDepuisNoeud() {
        String nomNode = canvas.getNomSelectionne();
        if (nomNode == null || reseau == null) return;
        Bavard b = reseau.getBavards().stream()
                .filter(bav -> bav.getNom().equals(nomNode))
                .findFirst().orElse(null);
        if (b == null) return;

        String contenu = txtMessageAEnvoyer.getText().trim();
        if (contenu.isEmpty()) return;

        b.envoyerMessage(contenu);
        journaliser("Message envoyé par " + nomNode + " : \"" + contenu + "\"");
    }

    // =========================================================
    //  MISE À JOUR ONGLET NŒUD
    // =========================================================

    private void mettreAJourOngletNoeud(Bavard b) {
        if (b == null) {
            lblNomNode.setText("(aucun sélectionné)");
            lblPersonnaliteNode.setText("—");
            lblAmisNode.setText("—");
            lblMessagesNode.setText("—");
            lblTransmissionsNode.setText("—");
            barHumeur.setEnabled(false);
            barEnergie.setEnabled(false);
            barStress.setEnabled(false);
            btnEnvoyerMessage.setEnabled(false);
            return;
        }

        lblNomNode.setText(b.getNom());
        lblPersonnaliteNode.setText(b.getPersonnalite().getNom());
        lblAmisNode.setText(String.valueOf(b.getAmis().size()));
        lblMessagesNode.setText(String.valueOf(b.getMessagesRecus().size()));
        lblTransmissionsNode.setText(String.valueOf(b.getCompteurTransmissions()));
        btnEnvoyerMessage.setEnabled(true);

        if (b.getPersonnalite() instanceof PersonnaliteHumaine ph) {
            barHumeur.setEnabled(true);
            barEnergie.setEnabled(true);
            barStress.setEnabled(true);

            // Humeur [-1, +1] → [0, 100]
            barHumeur.setValue((int)((ph.getHumeur() + 1f) / 2f * 100));
            barEnergie.setValue((int)(ph.getEnergie() * 100));
            barStress.setValue((int)(ph.getStress() * 100));

            // Coloration dynamique des barres
            float h = ph.getHumeur();
            barHumeur.setForeground(h > 0 ? new Color(60, 200, 100) : new Color(220, 80, 80));
        } else {
            barHumeur.setEnabled(false);
            barEnergie.setEnabled(false);
            barStress.setEnabled(false);
            barHumeur.setValue(50);
            barEnergie.setValue(80);
            barStress.setValue(10);
        }
    }

    // =========================================================
    //  RAFRAÎCHISSEMENT DE LA BARRE D'ÉTAT
    // =========================================================

    private void rafraichirStatut() {
        if (reseau != null) {
            int nb = reseau.getBavards().size();
            String etat = moteur.isEnCours() ? "▶ Simulation" : "⏸ Arrêtée";
            lblStatut.setText("  " + nb + " bavards | " + etat + " | Tick " + moteur.getTick());
        }
        if (canvas != null) {
            lblParticules.setText("Particules : " + canvas.getTotalMessagesReçus());
        }

        // Mise à jour de l'onglet nœud si visible
        if (tabbedPane.getSelectedIndex() == 2) {
            String nomSel = canvas.getNomSelectionne();
            if (nomSel != null && reseau != null) {
                Bavard b = reseau.getBavards().stream()
                        .filter(bav -> bav.getNom().equals(nomSel))
                        .findFirst().orElse(null);
                mettreAJourOngletNoeud(b);
            }
        }
    }

    private void journaliser(String msg) {
        if (areaJournal == null) return;
        String heure = String.format("%tT", new Date());
        areaJournal.append("[" + heure + "] " + msg + "\n");
        // Scroll automatique vers le bas
        areaJournal.setCaretPosition(areaJournal.getDocument().getLength());
    }

    // =========================================================
    //  UTILITAIRES DE COMPOSANTS
    // =========================================================

    private JLabel titreSec(String texte) {
        JLabel lbl = new JLabel(texte);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(120, 160, 255));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        return lbl;
    }

    private JPanel labelField(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(new Color(12, 15, 38));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(150, 160, 200));
        lbl.setPreferredSize(new Dimension(100, 26));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(new Color(20, 25, 55));
        combo.setForeground(new Color(180, 195, 240));
        combo.setBorder(BorderFactory.createLineBorder(new Color(50, 60, 130)));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(20, 25, 55));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setBackground(new Color(20, 25, 55));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setForeground(new Color(180, 195, 240));
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleTextField(JTextField txt) {
        txt.setBackground(new Color(18, 22, 50));
        txt.setForeground(new Color(180, 200, 255));
        txt.setCaretColor(new Color(180, 200, 255));
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 70, 150)),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        txt.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton boutonPrimaire(String texte, Color couleur) {
        JButton btn = new JButton(texte);
        btn.setBackground(couleur);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton boutonSecondaire(String texte) {
        JButton btn = new JButton(texte);
        btn.setBackground(new Color(25, 35, 80));
        btn.setForeground(new Color(160, 185, 255));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 70, 150)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton boutonIcone(String texte, Color couleur) {
        JButton btn = new JButton(texte);
        btn.setBackground(couleur);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator separateur() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 50, 100));
        sep.setBackground(new Color(40, 50, 100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JCheckBox checkBox(String label, boolean initial, java.util.function.Consumer<Boolean> action) {
        JCheckBox cb = new JCheckBox(label, initial);
        cb.setBackground(new Color(12, 15, 38));
        cb.setForeground(new Color(150, 160, 200));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.addActionListener(e -> action.accept(cb.isSelected()));
        return cb;
    }

    private JLabel nouveauLabel(String texte) {
        JLabel lbl = new JLabel(texte);
        lbl.setForeground(new Color(180, 200, 255));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel infoLabel(String etiquette, String valeur) {
        JLabel lbl = new JLabel(etiquette + " " + valeur);
        lbl.setForeground(new Color(140, 160, 210));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel lignePairedLabels(JLabel lbl) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 1));
        row.setBackground(new Color(12, 15, 38));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        return row;
    }

    private JProgressBar creerBarreEtat(String nom, Color couleur) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(50);
        bar.setForeground(couleur);
        bar.setBackground(new Color(20, 25, 55));
        bar.setBorderPainted(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        return bar;
    }

    private JPanel ligneAvecLabel(String nom, JProgressBar bar) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(new Color(12, 15, 38));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(nom);
        lbl.setForeground(new Color(130, 145, 195));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setPreferredSize(new Dimension(55, 18));
        row.add(lbl, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        return row;
    }

    // =========================================================
    //  THÈME SOMBRE GLOBAL
    // =========================================================

    private void appliquerThemeSombre() {
        getContentPane().setBackground(new Color(10, 12, 32));
        UIManager.put("TabbedPane.background",         new Color(12, 15, 38));
        UIManager.put("TabbedPane.foreground",         new Color(160, 180, 240));
        UIManager.put("TabbedPane.selected",           new Color(25, 35, 85));
        UIManager.put("TabbedPane.contentAreaColor",   new Color(12, 15, 38));
        UIManager.put("ScrollPane.background",         new Color(12, 15, 38));
    }

    // =========================================================
    //  POINT D'ENTRÉE AUTONOME
    // =========================================================

    /** Ouvre la fenêtre directement (sans passer par l'application principale). */
    public static void ouvrir() {
        SwingUtilities.invokeLater(() -> {
            FenetreVisualisationPrincipale f = new FenetreVisualisationPrincipale();
            f.setVisible(true);
        });
    }

    @Override
    public void dispose() {
        timerStatut.stop();
        moteur.arreter();
        canvas.arreter();
        super.dispose();
    }
}
