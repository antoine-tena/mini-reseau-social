package socialnetwork.front.seirs;

import socialnetwork.back.seirs.Rumeur;
import socialnetwork.back.seirs.RumorManager;
import socialnetwork.back.seirs.SimulationEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 8 (entry point) — the main JFrame for the SEIRS simulation.
 *
 * Layout:
 *   CENTER  → RenderPanel (map + agents)
 *   EAST    → DashboardPanel (SEIR pie + zone bars + bienveillance)
 *   SOUTH   → NewsTickerPanel (scrolling rumour headlines)
 *
 * Launch via main() or via ApplicationGraphique.
 */
@SuppressWarnings("serial")
public final class FenetreSimulationSEIRS extends JFrame {

    private static final int DEFAULT_POPULATION = 1200;
    private static final int WINDOW_W           = 1480;
    private static final int WINDOW_H           = 860;

    private final SimulationEngine engine;
    private final RenderPanel      renderPanel;
    private final DashboardPanel   dashboardPanel;
    private final NewsTickerPanel  tickerPanel;
    private final AgentInfoPanel   agentInfoPanel;
    private JSlider                speedSlider;
    private JLabel                 speedLabel;
    private final List<JToggleButton> rumorButtons = new ArrayList<>();

    public FenetreSimulationSEIRS() {
        this(DEFAULT_POPULATION, 1.0);
    }

    public FenetreSimulationSEIRS(int population) {
        this(population, 1.0);
    }

    public FenetreSimulationSEIRS(int population, double initialTimeScale) {
        super("SEIRS Crisis Center — Simulation Multi-Agents");

        this.engine = new SimulationEngine(Math.max(1, population));
        engine.setTimeScale(initialTimeScale);

        this.renderPanel    = new RenderPanel();
        this.dashboardPanel = new DashboardPanel();
        this.tickerPanel    = new NewsTickerPanel();
        this.agentInfoPanel = new AgentInfoPanel(renderPanel);

        renderPanel.setEngine(engine);
        dashboardPanel.setEngine(engine);
        tickerPanel.setRumorManager(engine.getRumorManager());
        agentInfoPanel.setEngine(engine);

        buildUI();
        wireShortcuts();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
    }

    /* ── public helpers ──────────────────────────────────────── */

    public void chargerEtDemarrer() {
        setLocationRelativeTo(null);
        setVisible(true);
        // Start engine AFTER window is visible so Swing EDT is running
        SwingUtilities.invokeLater(engine::start);
    }

    /* ── UI construction ─────────────────────────────────────── */

    private void buildUI() {
        setSize(WINDOW_W, WINDOW_H);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.BLACK);

        // Centre: render surface + AgentInfoPanel overlay
        JLayeredPane centerLayer = new JLayeredPane();
        renderPanel.setBounds(0, 0, WINDOW_W - 330, WINDOW_H - 36);
        centerLayer.add(renderPanel, JLayeredPane.DEFAULT_LAYER);
        agentInfoPanel.setBounds(10, 60, 260, 320);
        centerLayer.add(agentInfoPanel, JLayeredPane.POPUP_LAYER);
        // Agent click callback
        renderPanel.setOnAgentSelected(agent -> {
            if (agent == null) agentInfoPanel.clearSelection();
            else agentInfoPanel.showAgent(agent);
        });
        // Resize listener to keep panels sized properly
        centerLayer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = centerLayer.getSize();
                renderPanel.setBounds(0, 0, d.width, d.height);
            }
        });
        root.add(centerLayer, BorderLayout.CENTER);

        // East: dashboard + rumor chooser
        JPanel eastPanel = new JPanel(new BorderLayout(0, 4));
        eastPanel.setBackground(new Color(12, 14, 22));
        eastPanel.setPreferredSize(new Dimension(330, WINDOW_H));

        JScrollPane dashScroll = new JScrollPane(dashboardPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dashScroll.setBorder(null);
        dashScroll.setBackground(new Color(12, 14, 22));
        dashScroll.getViewport().setBackground(new Color(12, 14, 22));
        eastPanel.add(dashScroll, BorderLayout.CENTER);

        JPanel rumorPanel = buildRumorPanel();
        JScrollPane rumorScroll = new JScrollPane(rumorPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rumorScroll.setBorder(null);
        rumorScroll.setBackground(new Color(12, 14, 22));
        rumorScroll.getViewport().setBackground(new Color(12, 14, 22));
        rumorScroll.setPreferredSize(new Dimension(330, 320));
        eastPanel.add(rumorScroll, BorderLayout.SOUTH);

        root.add(eastPanel, BorderLayout.EAST);

        // South: news ticker
        root.add(tickerPanel, BorderLayout.SOUTH);

        // Top toolbar
        root.add(buildToolbar(), BorderLayout.NORTH);

        setContentPane(root);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(18, 20, 30));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 55, 80)));

        JLabel title = new JLabel("● SEIRS CRISIS CENTER");
        title.setForeground(new Color(220, 60, 60));
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bar.add(title);

        bar.add(Box.createHorizontalStrut(20));

        JButton btnPause = makeButton("⏸ Pause");
        btnPause.addActionListener(e -> {
            if (engine.isPaused()) {
                engine.resume();
                btnPause.setText("⏸ Pause");
            } else {
                engine.pause();
                btnPause.setText("▶ Resume");
            }
        });
        bar.add(btnPause);

        JButton btnHeat = makeButton("🌡 Heatmap");
        btnHeat.addActionListener(e -> {
            renderPanel.getHeatmapOverlay().setVisible(!renderPanel.getHeatmapOverlay().isVisible());
        });
        bar.add(btnHeat);

        JButton btnPois = makeButton("♦ POIs");
        btnPois.addActionListener(e -> renderPanel.setShowPois(!renderPanel.isShowPois()));
        bar.add(btnPois);

        // Speed control
        bar.add(Box.createHorizontalStrut(12));
        JLabel speedTitleLabel = new JLabel("Vitesse:");
        speedTitleLabel.setForeground(new Color(140, 160, 200));
        speedTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bar.add(speedTitleLabel);

        int initSlider = LaunchDialog.speedToSlider(engine.getTimeScale());
        speedSlider = new JSlider(0, 100, initSlider);
        speedSlider.setPreferredSize(new Dimension(120, 22));
        speedSlider.setBackground(new Color(18, 20, 30));
        speedSlider.setForeground(new Color(140, 160, 200));
        speedLabel = new JLabel(speedText(engine.getTimeScale()));
        speedLabel.setForeground(new Color(200, 215, 240));
        speedLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        speedLabel.setPreferredSize(new Dimension(52, 22));
        speedSlider.addChangeListener(e -> {
            double scale = LaunchDialog.sliderToSpeed(speedSlider.getValue());
            engine.setTimeScale(scale);
            speedLabel.setText(speedText(scale));
        });
        bar.add(speedSlider);
        bar.add(speedLabel);

        // Population info label (updated by timer)
        JLabel popLabel = new JLabel("  Pop: —");
        popLabel.setForeground(new Color(140, 160, 200));
        popLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bar.add(popLabel);

        Timer popTimer = new Timer(1000, e -> {
            int size = engine.getAgentsSnapshot().size();
            popLabel.setText(String.format("  Pop: %,d agents", size));
        });
        popTimer.start();

        return bar;
    }

    private static String speedText(double scale) {
        if (scale < 0.01) return String.format("%.2f%%", scale * 100);
        if (scale < 1.0)  return String.format("%.0f%%",  scale * 100);
        return String.format("x%.1f", scale);
    }

    /** Build the rumour chooser panel. */
    private JPanel buildRumorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(14, 16, 28));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 55, 80)),
            BorderFactory.createEmptyBorder(8, 6, 8, 6)
        ));

        JLabel rumorTitle = new JLabel("💥 SCÉNARIOS DE CRISE");
        rumorTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rumorTitle.setForeground(new Color(220, 180, 60));
        rumorTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rumorTitle);
        panel.add(Box.createVerticalStrut(4));

        JLabel rumorNote = new JLabel(
            "<html><small style='color:#8090b0'>Chaque personne peut croire ou ignorer<br>une rumeur selon sa crédulité.</small></html>");
        rumorNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(rumorNote);
        panel.add(Box.createVerticalStrut(6));

        RumorManager rm = engine.getRumorManager();
        java.util.List<Rumeur> library = new java.util.ArrayList<>(rm.getLibraryAsList());
        for (int i = 0; i < library.size(); i++) {
            final int idx = i;
            Rumeur r = library.get(i);
            JToggleButton btn = new JToggleButton();
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(318, 36));
            btn.setPreferredSize(new Dimension(318, 36));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Build label
            String impact = r.getImpactBienveillance() >= 0 ? "↑" : "↓";
            String title = r.getTitre();
            if (title.length() > 55) title = title.substring(0, 52) + "...";
            btn.setText("<html>" + impact + " " + title + "</html>");
            Color btnBgNormal  = new Color(25, 28, 45);
            Color btnBgActive  = r.getImpactBienveillance() >= 0
                    ? new Color(20, 60, 30) : new Color(60, 20, 20);
            btn.setBackground(btnBgNormal);
            btn.setForeground(new Color(175, 185, 210));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 55, 80), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            btn.setFocusPainted(false);

            btn.addItemListener(e -> {
                if (btn.isSelected()) {
                    rm.manualActivate(idx);
                    btn.setBackground(btnBgActive);
                    btn.setForeground(Color.WHITE);
                } else {
                    rm.manualDeactivate(r.getId());
                    btn.setBackground(btnBgNormal);
                    btn.setForeground(new Color(175, 185, 210));
                }
            });

            rumorButtons.add(btn);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(3));
        }

        // Sync button states with active rumors timer
        Timer syncTimer = new Timer(800, e -> {
            for (int i = 0; i < rumorButtons.size() && i < library.size(); i++) {
                boolean active = rm.isRumorActive(library.get(i).getId());
                JToggleButton btn = rumorButtons.get(i);
                if (btn.isSelected() != active) {
                    btn.setSelected(active);
                }
            }
        });
        syncTimer.start();

        return panel;
    }

    private static JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setForeground(new Color(200, 210, 235));
        b.setBackground(new Color(30, 35, 52));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 65, 90)),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /* ── keyboard shortcuts ──────────────────────────────────── */

    private void wireShortcuts() {
        JRootPane root = getRootPane();
        // SPACE → pause/resume
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePause");
        root.getActionMap().put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (engine.isPaused()) engine.resume(); else engine.pause();
            }
        });

        // H → toggle heatmap
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "toggleHeat");
        root.getActionMap().put("toggleHeat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HeatmapOverlay ho = renderPanel.getHeatmapOverlay();
                ho.setVisible(!ho.isVisible());
            }
        });
    }

    /* ── shutdown ────────────────────────────────────────────── */

    private void onClose() {
        engine.close();
        dispose();
    }

    /* ── standalone entry point ──────────────────────────────── */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Show launch dialog first
            LaunchDialog dialog = new LaunchDialog(null);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) return;
            int pop   = dialog.getPopulation();
            double spd = dialog.getInitialSpeed();
            FenetreSimulationSEIRS fenetre = new FenetreSimulationSEIRS(pop, spd);
            fenetre.chargerEtDemarrer();
        });
    }
}
