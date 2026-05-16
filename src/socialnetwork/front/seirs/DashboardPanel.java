package socialnetwork.front.seirs;

import socialnetwork.back.seirs.EtatSEIRS;
import socialnetwork.back.seirs.SimulationEngine;
import socialnetwork.back.seirs.ZoneManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Right-side dashboard panel showing:
 *   - SEIR pie chart (live)
 *   - Per-zone infection bars
 *   - Global bienveillance gauge
 *   - Day/time indicator
 */
@SuppressWarnings("serial")
public final class DashboardPanel extends JPanel {

    private static final int    PANEL_WIDTH  = 290;
    private static final Color  BG           = new Color(12, 14, 22, 245);
    private static final Color  BG_SECTION   = new Color(20, 22, 36);
    private static final Color  BORDER       = new Color(50, 55, 80);
    private static final Color  TEXT_TITLE   = new Color(180, 190, 220);
    private static final Color  TEXT_LABEL   = new Color(140, 150, 175);
    private static final Color  TEXT_VALUE   = new Color(220, 225, 240);
    private static final Color  ACCENT       = new Color(90, 130, 200);

    private static final Color COL_S = new Color( 70, 150, 240);
    private static final Color COL_E = new Color(240, 150,  40);
    private static final Color COL_I = new Color(220,  40,  40);
    private static final Color COL_R = new Color( 50, 220, 100);

    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_VALUE  = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  9);
    private static final Font FONT_TIME   = new Font("Segoe UI Semibold", Font.PLAIN, 18);

    private volatile SimulationEngine engine;

    // snapshot data (updated on EDT by 30fps timer)
    private EnumMap<EtatSEIRS, Integer>      stateSEIRS = new EnumMap<>(EtatSEIRS.class);
    private EnumMap<ZoneManager.ZoneId, Integer> zoneTotal  = new EnumMap<>(ZoneManager.ZoneId.class);
    private EnumMap<ZoneManager.ZoneId, Integer> zoneInfect = new EnumMap<>(ZoneManager.ZoneId.class);
    private double bienveillance = 0.72;
    private int    totalAgents   = 0;
    private String timeString    = "08:00";
    private int    dayIndex      = 0;

    public DashboardPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, 600));
        setMinimumSize(new Dimension(PANEL_WIDTH, 300));
        setOpaque(false);

        // Refresh data at ~30fps
        Timer refresh = new Timer(33, e -> {
            refreshData();
            repaint();
        });
        refresh.setCoalesce(true);
        refresh.start();
    }

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
    }

    /* ── data refresh ────────────────────────────────────────── */

    private void refreshData() {
        SimulationEngine eng = this.engine;
        if (eng == null) return;

        stateSEIRS = eng.getPopulationByState();
        zoneTotal  = eng.getPopulationByZone();

        // Infectious per zone — we walk agents snapshot
        EnumMap<ZoneManager.ZoneId, Integer> inf = new EnumMap<>(ZoneManager.ZoneId.class);
        for (ZoneManager.ZoneId z : ZoneManager.ZoneId.values()) inf.put(z, 0);
        for (var agent : eng.getAgentsSnapshot()) {
            if (agent.getEtatSEIRS() == EtatSEIRS.INFECTIOUS) {
                inf.merge(agent.getZoneActuelle(), 1, Integer::sum);
            }
        }
        zoneInfect = inf;

        // Bienveillance: based on SEIRS distribution
        int total = stateSEIRS.values().stream().mapToInt(Integer::intValue).sum();
        totalAgents = total;
        if (total > 0) {
            int s = stateSEIRS.getOrDefault(EtatSEIRS.SUSCEPTIBLE, 0);
            int r = stateSEIRS.getOrDefault(EtatSEIRS.RECOVERED, 0);
            bienveillance = (s + r * 0.6) / total;
        }

        // Time
        double h = eng.getHourOfDay();
        int hh = (int) h;
        int mm = (int) ((h - hh) * 60);
        timeString = String.format("%02d:%02d", hh, mm);
        dayIndex = eng.getDayIndex();
    }

    /* ── painting ────────────────────────────────────────────── */

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // Background
        g.setColor(BG);
        g.fillRoundRect(0, 0, w, h, 8, 8);

        int y = 12;

        // === Clock ===
        y = drawClock(g, w, y);
        y += 10;

        // === Pie chart SEIRS ===
        y = drawSEIRSSection(g, w, y);
        y += 8;

        // === Zone bars ===
        y = drawZoneSection(g, w, y);
        y += 8;

        // === Bienveillance gauge ===
        drawBienveillanceGauge(g, w, y);

        g.dispose();
    }

    /* ── clock ───────────────────────────────────────────────── */

    private int drawClock(Graphics2D g, int w, int y) {
        g.setFont(FONT_TIME);
        g.setColor(TEXT_VALUE);
        FontMetrics fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(timeString)) / 2;
        g.drawString(timeString, tx, y + fm.getAscent());

        g.setFont(FONT_SMALL);
        g.setColor(TEXT_LABEL);
        fm = g.getFontMetrics();
        String dayStr = "Jour " + (dayIndex + 1);
        g.drawString(dayStr, (w - fm.stringWidth(dayStr)) / 2, y + 22 + fm.getAscent());

        drawDivider(g, w, y + 34);
        return y + 46;
    }

    /* ── SEIRS section ───────────────────────────────────────── */

    private int drawSEIRSSection(Graphics2D g, int w, int y) {
        drawSectionTitle(g, "ÉTAT ÉPIDÉMIQUE", w, y);
        y += 20;

        int cx = w / 2;
        int cy = y + 68;
        int r  = 56;

        drawPieChart(g, cx, cy, r);
        drawPieLegend(g, w, cy + r + 14);

        return cy + r + 14 + 58;
    }

    private void drawPieChart(Graphics2D g, int cx, int cy, int r) {
        int total = stateSEIRS.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            g.setColor(new Color(50, 55, 70));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            return;
        }

        EtatSEIRS[] order = {EtatSEIRS.SUSCEPTIBLE, EtatSEIRS.EXPOSED, EtatSEIRS.INFECTIOUS, EtatSEIRS.RECOVERED};
        Color[]     colors= {COL_S, COL_E, COL_I, COL_R};

        double startAngle = -90.0;
        int idx = 0;
        for (EtatSEIRS state : order) {
            int count = stateSEIRS.getOrDefault(state, 0);
            if (count == 0) { idx++; continue; }
            double sweep = 360.0 * count / total;

            // Slice shadow
            g.setColor(new Color(0, 0, 0, 60));
            g.fillArc(cx - r + 2, cy - r + 2, r * 2, r * 2, (int)startAngle, (int)sweep);

            g.setColor(colors[idx]);
            g.fillArc(cx - r, cy - r, r * 2, r * 2, (int)startAngle, (int)sweep);

            // Slice border
            g.setColor(BG_SECTION);
            g.setStroke(new BasicStroke(1.2f));
            g.drawArc(cx - r, cy - r, r * 2, r * 2, (int)startAngle, (int)sweep);

            startAngle += sweep;
            idx++;
        }

        // Donut hole
        g.setColor(BG);
        g.fillOval(cx - r/2, cy - r/2, r, r);
        g.setFont(FONT_SMALL);
        g.setColor(TEXT_VALUE);
        FontMetrics fm = g.getFontMetrics();
        String tot = String.valueOf(totalAgents);
        g.drawString(tot, cx - fm.stringWidth(tot)/2, cy + fm.getAscent()/2 - 1);
    }

    private void drawPieLegend(Graphics2D g, int w, int y) {
        String[] labels = {"SUSCEPTIBLE", "EXPOSED", "INFECTIOUS", "RECOVERED"};
        Color[]  colors = {COL_S, COL_E, COL_I, COL_R};
        EtatSEIRS[] order = {EtatSEIRS.SUSCEPTIBLE, EtatSEIRS.EXPOSED, EtatSEIRS.INFECTIOUS, EtatSEIRS.RECOVERED};
        int total = Math.max(1, stateSEIRS.values().stream().mapToInt(Integer::intValue).sum());

        Font legendFont = new Font("Segoe UI", Font.BOLD, 8);
        int colW = (w - 16) / 4;
        for (int i = 0; i < 4; i++) {
            int x = 8 + i * colW;
            g.setColor(colors[i]);
            g.fillRoundRect(x, y, colW - 4, 8, 3, 3);

            g.setFont(legendFont);
            FontMetrics fm = g.getFontMetrics();
            // Truncate if label too wide
            String lbl = labels[i];
            while (fm.stringWidth(lbl) > colW - 2 && lbl.length() > 4) {
                lbl = lbl.substring(0, lbl.length() - 1);
            }
            g.setColor(TEXT_LABEL);
            g.drawString(lbl, x, y + 20 + fm.getAscent());

            int count = stateSEIRS.getOrDefault(order[i], 0);
            String pct = String.format("%d%%", count * 100 / total);
            g.setColor(TEXT_VALUE);
            g.setFont(FONT_VALUE);
            fm = g.getFontMetrics();
            g.drawString(pct, x + 2, y + 34 + fm.getAscent());

            g.setFont(FONT_SMALL);
            g.setColor(TEXT_LABEL);
            fm = g.getFontMetrics();
            g.drawString(String.valueOf(count), x + 2, y + 48 + fm.getAscent());
        }
    }

    /* ── zone bars ───────────────────────────────────────────── */

    private int drawZoneSection(Graphics2D g, int w, int y) {
        drawDivider(g, w, y);
        y += 8;
        drawSectionTitle(g, "PAR ZONE", w, y);
        y += 18;

        ZoneManager.ZoneId[] zones = ZoneManager.ZoneId.values();
        String[] shortNames = {"Ville", "Petite V.", "Hameau", "Mer"};

        for (int i = 0; i < zones.length; i++) {
            ZoneManager.ZoneId zone = zones[i];
            int total = zoneTotal.getOrDefault(zone, 0);
            int infected = zoneInfect.getOrDefault(zone, 0);
            y = drawZoneBar(g, w, y, shortNames[i], total, infected);
            y += 4;
        }
        return y;
    }

    private int drawZoneBar(Graphics2D g, int w, int y, String label, int total, int infected) {
        int barW = w - 80;
        int barH = 10;
        int barX = 8;
        int barY = y + 14;

        g.setFont(FONT_LABEL);
        g.setColor(TEXT_LABEL);
        g.drawString(label, barX, y + 12);

        g.setColor(TEXT_VALUE);
        g.setFont(FONT_SMALL);
        FontMetrics fm = g.getFontMetrics();
        String valStr = "I:" + infected + "/" + total;
        g.drawString(valStr, w - fm.stringWidth(valStr) - 4, y + 12);

        // Background bar
        g.setColor(new Color(40, 45, 60));
        g.fillRoundRect(barX, barY, barW, barH, 4, 4);

        // Total fill
        if (total > 0) {
            int fillTotal = (int) (barW);
            g.setColor(new Color(60, 80, 120));
            g.fillRoundRect(barX, barY, fillTotal, barH, 4, 4);
        }

        // Infected fill
        if (total > 0 && infected > 0) {
            int fillInf = (int) ((double) infected / total * barW);
            g.setColor(COL_I);
            g.fillRoundRect(barX, barY, Math.max(2, fillInf), barH, 4, 4);
        }

        return barY + barH + 2;
    }

    /* ── bienveillance gauge ─────────────────────────────────── */

    private void drawBienveillanceGauge(Graphics2D g, int w, int y) {
        drawDivider(g, w, y);
        y += 8;
        drawSectionTitle(g, "BIENVEILLANCE GLOBALE", w, y);
        y += 18;

        int bx = 8;
        int bw = w - 16;
        int bh = 18;

        // Background
        g.setColor(new Color(30, 32, 48));
        g.fillRoundRect(bx, y, bw, bh, bh, bh);

        // Gradient fill
        float val = (float) Math.max(0.0, Math.min(1.0, bienveillance));
        int fillW = (int) (bw * val);
        if (fillW > 4) {
            GradientPaint gp = new GradientPaint(
                bx, y,           new Color(220, 60, 60),
                bx + bw, y,      new Color(50, 200, 120));
            g.setPaint(gp);
            g.fillRoundRect(bx, y, fillW, bh, bh, bh);
            g.setPaint(null);
        }

        // Border
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRoundRect(bx, y, bw, bh, bh, bh);

        // Percentage label
        g.setFont(FONT_VALUE);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String pct = String.format("%.0f%%", bienveillance * 100);
        g.drawString(pct, bx + (bw - fm.stringWidth(pct)) / 2, y + bh - (bh - fm.getAscent()) / 2 - 1);
    }

    /* ── utilities ───────────────────────────────────────────── */

    private void drawSectionTitle(Graphics2D g, String title, int w, int y) {
        g.setFont(FONT_TITLE);
        g.setColor(TEXT_TITLE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (w - fm.stringWidth(title)) / 2, y + fm.getAscent());
    }

    private void drawDivider(Graphics2D g, int w, int y) {
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.0f));
        g.drawLine(8, y, w - 8, y);
    }
}
