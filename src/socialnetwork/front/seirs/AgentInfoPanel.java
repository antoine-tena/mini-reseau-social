package socialnetwork.front.seirs;

import socialnetwork.back.seirs.Bavard;
import socialnetwork.back.seirs.EtatSEIRS;
import socialnetwork.back.seirs.Relation;
import socialnetwork.back.seirs.RelationManager;
import socialnetwork.back.seirs.SimulationEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Floating inspector panel that appears when the user clicks an agent.
 * Shows name, SEIRS state, zone, traits, and social relations.
 * Also provides "Follow on map" / "Stop following" buttons.
 */
public final class AgentInfoPanel extends JPanel {

    private static final Color BG_COLOR  = new Color(18, 22, 38, 220);
    private static final Color HDR_COLOR = new Color(220, 225, 255);
    private static final Color SEP_COLOR = new Color(60, 70, 110);
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  BODY_FONT  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font  REL_FONT   = new Font("Segoe UI", Font.ITALIC, 10);

    /* ── references ─────────────────────────────────────── */
    private final RenderPanel renderPanel;
    private volatile SimulationEngine engine;

    /* ── UI widgets ──────────────────────────────────────── */
    private final JLabel  titleLabel     = new JLabel();
    private final JLabel  stateLabel     = new JLabel();
    private final JLabel  zoneLabel      = new JLabel();
    private final JLabel  activityLabel  = new JLabel();
    private final JLabel  credLabel      = new JLabel();
    private final JLabel  sociabilLabel  = new JLabel();
    private final JLabel  voyageLabel    = new JLabel();
    private final JTextArea relationsArea = new JTextArea(5, 22);
    private final JButton followBtn      = new JButton("Suivre sur la carte");
    private final JButton closeBtn       = new JButton("Fermer");

    /* ── timer for live updates ──────────────────────────── */
    private final Timer refreshTimer;
    private volatile Bavard trackedAgent = null;

    public AgentInfoPanel(RenderPanel renderPanel) {
        this.renderPanel = renderPanel;

        setLayout(new BorderLayout(0, 4));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 160), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        setOpaque(true);

        // Header
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(HDR_COLOR);

        // Body info
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

        for (JLabel lbl : new JLabel[]{stateLabel, zoneLabel, activityLabel,
                                        credLabel, sociabilLabel, voyageLabel}) {
            lbl.setFont(BODY_FONT);
            lbl.setForeground(new Color(190, 200, 230));
            bodyPanel.add(lbl);
            bodyPanel.add(Box.createVerticalStrut(2));
        }

        // Relations section
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(SEP_COLOR);
        bodyPanel.add(sep);
        bodyPanel.add(Box.createVerticalStrut(3));
        JLabel relHeader = new JLabel("Relations :");
        relHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        relHeader.setForeground(new Color(160, 180, 220));
        bodyPanel.add(relHeader);

        relationsArea.setFont(REL_FONT);
        relationsArea.setForeground(new Color(175, 190, 215));
        relationsArea.setBackground(new Color(24, 28, 48, 180));
        relationsArea.setEditable(false);
        relationsArea.setBorder(new EmptyBorder(2, 4, 2, 4));
        relationsArea.setOpaque(true);
        JScrollPane relScroll = new JScrollPane(relationsArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        relScroll.setBorder(BorderFactory.createLineBorder(SEP_COLOR, 1));
        relScroll.setPreferredSize(new Dimension(220, 90));
        bodyPanel.add(relScroll);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);
        followBtn.setFont(BODY_FONT);
        closeBtn.setFont(BODY_FONT);
        followBtn.setPreferredSize(new Dimension(130, 24));
        closeBtn.setPreferredSize(new Dimension(65, 24));

        followBtn.addActionListener(e -> toggleFollow());
        closeBtn.addActionListener(e -> {
            renderPanel.setSelectedAgent(null);
            renderPanel.setFollowingAgent(false);
            trackedAgent = null;
            setVisible(false);
        });
        btnPanel.add(followBtn);
        btnPanel.add(closeBtn);

        add(titleLabel, BorderLayout.NORTH);
        add(bodyPanel,  BorderLayout.CENTER);
        add(btnPanel,   BorderLayout.SOUTH);

        // Live refresh every 500ms
        refreshTimer = new Timer(500, e -> refreshDisplay());
        refreshTimer.start();

        setVisible(false);
    }

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    /** Call when a user clicks an agent on the map. */
    public void showAgent(Bavard agent) {
        trackedAgent = agent;
        renderPanel.setSelectedAgent(agent);
        renderPanel.setFollowingAgent(false);
        refreshDisplay();
        setVisible(true);
    }

    /** Call when user clicks empty space. */
    public void clearSelection() {
        trackedAgent = null;
        renderPanel.setSelectedAgent(null);
        renderPanel.setFollowingAgent(false);
        setVisible(false);
    }

    private void toggleFollow() {
        if (trackedAgent == null) return;
        boolean newFollow = !renderPanel.isFollowingAgent();
        renderPanel.setFollowingAgent(newFollow);
        followBtn.setText(newFollow ? "Arrêter de suivre" : "Suivre sur la carte");
    }

    private void refreshDisplay() {
        Bavard agent = trackedAgent;
        if (agent == null || !isVisible()) return;

        titleLabel.setText("\u2666 " + agent.getNom());

        EtatSEIRS state = agent.getEtatSEIRS();
        String stateText = switch (state) {
            case SUSCEPTIBLE -> "SUSCEPTIBLE";
            case EXPOSED     -> "EXPOSED";
            case INFECTIOUS  -> "INFECTIOUS";
            case RECOVERED   -> "RECOVERED";
        };
        Color stateColor = switch (state) {
            case SUSCEPTIBLE -> new Color( 80, 165, 255);
            case EXPOSED     -> new Color(245, 145,  35);
            case INFECTIOUS  -> new Color(235,  30,  30);
            case RECOVERED   -> new Color( 50, 240, 100);
        };
        stateLabel.setForeground(stateColor);
        stateLabel.setText("État : " + stateText);

        String zoneName = agent.getZoneActuelle() != null ? agent.getZoneActuelle().name() : "?";
        zoneLabel.setText("Zone : " + zoneName.replace("_", " "));

        String activity = agent.getActiviteActuelle();
        activityLabel.setText("Activité : " + (activity.isEmpty() ? "—" : activity));

        credLabel.setText(String.format("Crédulité : %.0f%%", agent.getCredulite() * 100));
        sociabilLabel.setText(String.format("Sociabilité : %.0f%%", agent.getTraitSociabilite() * 100));
        voyageLabel.setText(String.format("Propension voyage : %.0f%%", agent.getPropensionVoyage() * 100));

        // Relations
        SimulationEngine eng = engine;
        if (eng != null) {
            RelationManager rm = eng.getRelationManager();
            List<Relation> rels = rm.getRelations(agent.getId());
            if (rels.isEmpty()) {
                relationsArea.setText("Aucune relation connue.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Relation r : rels) {
                    // find the partner name from agents list
                    long otherId = r.getOtherId(agent.getId());
                    String partnerName = findAgentName(eng, otherId);
                    sb.append(r.getType().label)
                      .append(": ")
                      .append(partnerName)
                      .append(" (")
                      .append(String.format("%.0f%%", r.getStrength() * 100))
                      .append(")\n");
                }
                relationsArea.setText(sb.toString().trim());
            }
        }

        followBtn.setText(renderPanel.isFollowingAgent() ? "Arrêter de suivre" : "Suivre sur la carte");
        revalidate();
        repaint();
    }

    private String findAgentName(SimulationEngine eng, long agentId) {
        // Linear scan — acceptable since relations are displayed rarely
        for (Bavard b : eng.getAgentsSnapshot()) {
            if (b.getId() == agentId) return b.getNom();
        }
        return "Agent-" + agentId;
    }

    /** Lightweight paint for the semi-transparent background. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG_COLOR);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}
