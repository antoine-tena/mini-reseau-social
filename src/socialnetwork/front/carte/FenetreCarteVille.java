package socialnetwork.front.carte;

import socialnetwork.back.monde.*;

import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre principale de la carte ville animée.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  STRUCTURE
 * ══════════════════════════════════════════════════════════════════════
 *
 *  ┌─────────────────────────────────────────────────────────┐
 *  │                     Barre d'outils                       │
 *  ├─────────────────────────────────────────────┬───────────┤
 *  │                                             │           │
 *  │            CarteVilleCanvas                 │ Panneau   │
 *  │           (carte interactive)               │  Suivi    │
 *  │                                             │           │
 *  ├─────────────────────────────────────────────┴───────────┤
 *  │                    Barre de statut                       │
 *  └─────────────────────────────────────────────────────────┘
 * ══════════════════════════════════════════════════════════════════════
 */
@SuppressWarnings("serial")
public class FenetreCarteVille extends JFrame {

    // ─── Données ─────────────────────────────────────────────────────────────

    private VilleSociale ville;
    private GestionnaireDeplacements gestionnaire;

    // ─── Composants ──────────────────────────────────────────────────────────

    private final CarteVilleCanvas canvas  = new CarteVilleCanvas();
    private final PanneauSuivi panneauSuivi = new PanneauSuivi();
    private final JLabel statusBar         = new JLabel(" ");

    // Toolbar
    private JButton btnDemarrer;
    private JButton btnPause;
    private JButton btnReset;
    private JButton btn500;
    private JComboBox<String> comboVitesse;
    private JToggleButton btnRelations;
    private JToggleButton btnLabels;
    private JToggleButton btnMinimap;
    private JToggleButton btnBavards;

    // ──────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTEUR
    // ──────────────────────────────────────────────────────────────────────────

    public FenetreCarteVille() {
        super("🗺  Carte de la Ville Sociale");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1400, 880));
        setMinimumSize(new Dimension(900, 600));

        construireUI();
        pack();
        setLocationRelativeTo(null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTION DE L'UI
    // ──────────────────────────────────────────────────────────────────────────

    private void construireUI() {
        // Fond général sombre
        getContentPane().setBackground(new Color(8, 10, 18));
        setLayout(new BorderLayout(0, 0));

        // ── Barre d'outils ────────────────────────────────────────────────
        add(construireToolbar(), BorderLayout.NORTH);

        // ── Zone centrale ─────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, panneauSuivi);
        split.setDividerLocation(0.80);
        split.setResizeWeight(0.80);
        split.setDividerSize(4);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(new Color(8, 10, 18));
        add(split, BorderLayout.CENTER);

        // ── Barre de statut ────────────────────────────────────────────────
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.setForeground(new Color(120, 150, 200));
        statusBar.setBackground(new Color(8, 10, 18));
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 40, 70)),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        add(statusBar, BorderLayout.SOUTH);

        // Callbacks canvas
        canvas.setOnBavardSelectionne(b -> {
            panneauSuivi.setBavardSuivi(b);
            if (b != null) majStatut("Sélectionné : " + b.getNom());
            else           majStatut("Prêt");
        });

        canvas.setOnLieuSelectionne(l -> majStatut("Lieu : " + l.getNom()));
    }

    private JToolBar construireToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBackground(new Color(15, 20, 35));
        tb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 45, 80)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        // ── Contrôles de simulation ───────────────────────────────────────
        btnDemarrer = bouton("▶  Démarrer", new Color(60, 140, 80));
        btnDemarrer.addActionListener(e -> actionDemarrer());

        btnPause = bouton("⏸  Pause", new Color(140, 120, 40));
        btnPause.addActionListener(e -> actionPause());
        btnPause.setEnabled(false);

        btnReset = bouton("↺  Réinitialiser", new Color(100, 50, 50));
        btnReset.addActionListener(e -> actionReset());

        btn500 = bouton("⚡  Simuler 500 personnes", new Color(80, 50, 130));
        btn500.addActionListener(e -> actionSimuler500());

        // ── Vitesse ───────────────────────────────────────────────────────
        JLabel lblVitesse = labelTB("Vitesse :");
        comboVitesse = new JComboBox<>(new String[]{"0.5×","1×","2×","5×","10×","20×","50×"});
        comboVitesse.setSelectedIndex(1); // 1× par défaut
        comboVitesse.setBackground(new Color(20, 28, 50));
        comboVitesse.setForeground(new Color(200, 220, 255));
        comboVitesse.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboVitesse.setMaximumSize(new Dimension(80, 28));
        comboVitesse.addActionListener(e -> appliquerVitesse());

        // ── Options visuelles ─────────────────────────────────────────────
        btnRelations = toggleBtn("🔗 Relations", true);
        btnRelations.addActionListener(e -> canvas.setAfficherRelations(btnRelations.isSelected()));

        btnLabels = toggleBtn("🏷 Labels", true);
        btnLabels.addActionListener(e -> canvas.setAfficherLabels(btnLabels.isSelected()));

        btnMinimap = toggleBtn("🗺 Minimap", true);
        btnMinimap.addActionListener(e -> canvas.setAfficherMinimap(btnMinimap.isSelected()));

        btnBavards = toggleBtn("👤 Bavards", true);
        btnBavards.addActionListener(e -> canvas.setAfficherBavards(btnBavards.isSelected()));

        // ── Bouton centrer ────────────────────────────────────────────────
        JButton btnCentrer = bouton("⊕ Centrer", new Color(40, 60, 100));
        btnCentrer.addActionListener(e -> canvas.centrerVue());

        // Assemblage
        tb.add(btnDemarrer);
        tb.add(btnPause);
        tb.add(btnReset);
        tb.add(new JToolBar.Separator());
        tb.add(btn500);
        tb.add(new JToolBar.Separator());
        tb.add(lblVitesse);
        tb.add(comboVitesse);
        tb.add(new JToolBar.Separator());
        tb.add(btnRelations);
        tb.add(btnLabels);
        tb.add(btnMinimap);
        tb.add(btnBavards);
        tb.add(new JToolBar.Separator());
        tb.add(btnCentrer);

        return tb;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ACTIONS
    // ──────────────────────────────────────────────────────────────────────────

    private void actionDemarrer() {
        if (gestionnaire == null) return;
        gestionnaire.demarrer();
        btnDemarrer.setEnabled(false);
        btnPause.setEnabled(true);
        majStatut("Simulation en cours…");
    }

    private void actionPause() {
        if (gestionnaire == null) return;
        if (gestionnaire.isEnMarche()) {
            gestionnaire.pause();
            btnPause.setText("▶  Reprendre");
            majStatut("Pause");
        } else {
            gestionnaire.reprendre();
            btnPause.setText("⏸  Pause");
            majStatut("Simulation en cours…");
        }
    }

    private void actionReset() {
        if (gestionnaire == null) return;
        gestionnaire.arreter();
        btnDemarrer.setEnabled(true);
        btnPause.setEnabled(false);
        btnPause.setText("⏸  Pause");
        canvas.selectionnerBavard(null);
        majStatut("Réinitialisé");
    }

    private void actionSimuler500() {
        int nbPersonnes = 500;
        Object input = JOptionPane.showInputDialog(this,
            "Nombre de personnes à simuler :",
            "Grande simulation", JOptionPane.QUESTION_MESSAGE,
            null, null, "500");
        if (input == null) return;
        try { nbPersonnes = Integer.parseInt(input.toString().trim()); }
        catch (NumberFormatException ex) { /* garder 500 */ }

        final int nb = Math.max(10, Math.min(2000, nbPersonnes));
        majStatut("Génération de " + nb + " personnes…");

        SwingWorker<VilleSociale, Void> worker = new SwingWorker<>() {
            @Override
            protected VilleSociale doInBackground() {
                return SimulateurGrandPopulation.generer(nb, System.currentTimeMillis());
            }
            @Override
            protected void done() {
                try {
                    VilleSociale nouvelleVille = get();
                    charger(nouvelleVille);
                    majStatut("Simulation prête : " + nb + " personnes");
                } catch (Exception ex) {
                    majStatut("Erreur : " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void appliquerVitesse() {
        if (gestionnaire == null) return;
        String sel = (String) comboVitesse.getSelectedItem();
        if (sel == null) return;
        try {
            double v = Double.parseDouble(sel.replace("×","").replace(",","."));
            gestionnaire.setVitesse(v);
        } catch (NumberFormatException ignored) {}
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Charge une VilleSociale dans la fenêtre.
     * Crée automatiquement un GestionnaireDeplacements.
     */
    public void charger(VilleSociale ville) {
        // Arrêter l'ancien gestionnaire
        if (gestionnaire != null) {
            gestionnaire.arreter();
        }

        this.ville = ville;
        this.gestionnaire = new GestionnaireDeplacements(ville);

        // Appliquer la vitesse sélectionnée
        appliquerVitesse();

        // Configurer les callbacks du gestionnaire
        gestionnaire.ajouterListenerHeure(h -> SwingUtilities.invokeLater(() -> {
            majStatut(gestionnaire.getNomJour() + "  " + gestionnaire.getHeureFmt()
                + "  |  " + gestionnaire.getDeplacementsActifs().size() + " déplacements");
        }));

        // Configurer le canvas
        canvas.setVille(ville, gestionnaire);

        // Configurer le panneau de suivi
        panneauSuivi.setContexte(ville, gestionnaire);

        // Réinitialiser les boutons
        btnDemarrer.setEnabled(true);
        btnPause.setEnabled(false);
        btnPause.setText("⏸  Pause");

        majStatut("Ville chargée : " + ville.getNom()
            + " (" + ville.getNbBavards() + " bavards, "
            + ville.getNbLieux() + " lieux)");

        // Afficher
        if (!isVisible()) {
            setVisible(true);
        }
    }

    /**
     * Charge et démarre automatiquement la ville par défaut (30 bavards).
     */
    public void chargerEtDemarrer() {
        charger(DonneesVille.creer());
        actionDemarrer();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES UI
    // ──────────────────────────────────────────────────────────────────────────

    private JButton bouton(String titre, Color bg) {
        JButton b = new JButton(titre);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return b;
    }

    private JToggleButton toggleBtn(String titre, boolean selected) {
        JToggleButton b = new JToggleButton(titre, selected);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(selected ? new Color(40, 60, 100) : new Color(25, 30, 50));
        b.setForeground(new Color(180, 210, 255));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        b.addChangeListener(e -> b.setBackground(b.isSelected()
            ? new Color(40, 60, 100) : new Color(25, 30, 50)));
        return b;
    }

    private JLabel labelTB(String texte) {
        JLabel l = new JLabel(texte);
        l.setForeground(new Color(150, 180, 230));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    private void majStatut(String msg) {
        SwingUtilities.invokeLater(() -> statusBar.setText(" " + msg));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ACCESSEURS
    // ──────────────────────────────────────────────────────────────────────────

    public CarteVilleCanvas     getCanvas()        { return canvas; }
    public PanneauSuivi         getPanneauSuivi()  { return panneauSuivi; }
    public VilleSociale         getVille()         { return ville; }
    public GestionnaireDeplacements getGestionnaire() { return gestionnaire; }
}
