package socialnetwork.front.seirs;

import socialnetwork.back.seirs.Rumeur;
import socialnetwork.back.seirs.RumorManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Bottom scrolling news-ticker banner displaying live rumour headlines.
 * Styled like a cable news lower-third.
 */
@SuppressWarnings("serial")
public final class NewsTickerPanel extends JPanel {

    private static final int    PANEL_HEIGHT   = 36;
    private static final Color  BG_DARK        = new Color(18, 18, 28, 230);
    private static final Color  BG_FLASH       = new Color(180, 20, 20);
    private static final Color  TEXT_MAIN       = new Color(240, 235, 220);
    private static final Color  TEXT_FLASH      = Color.WHITE;
    private static final Color  SEPARATOR       = new Color(220, 60, 40);
    private static final Font   FONT_LABEL      = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font   FONT_NEWS       = new Font("Segoe UI", Font.PLAIN, 12);
    private static final int    SCROLL_SPEED    = 90;  // pixels/second
    private static final int    MAX_HISTORY     = 40;
    private static final int    PADDING_BETWEEN = 140; // px gap between headlines

    private final Deque<String> headlines       = new ArrayDeque<>();
    private final List<String>  visibleItems    = new ArrayList<>();
    private volatile double     scrollX         = 0.0;
    private int                 totalTextWidth  = 0;
    private long                lastUpdateNanos = System.nanoTime();
    private RumorManager        rumorManager;
    private long                lastRumorCheck  = 0;

    public NewsTickerPanel() {
        setPreferredSize(new Dimension(100, PANEL_HEIGHT));
        setMinimumSize(new Dimension(100, PANEL_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, PANEL_HEIGHT));
        setOpaque(false);

        addDefaultHeadlines();

        // Drive ticker via Swing Timer at ~60fps
        Timer timer = new Timer(16, e -> {
            advanceScroll();
            repaint();
        });
        timer.setCoalesce(true);
        timer.start();
    }

    public void setRumorManager(RumorManager rumorManager) {
        this.rumorManager = rumorManager;
    }

    /** Call this from your main window or renderer to push a new headline. */
    public void pushHeadline(String text) {
        synchronized (headlines) {
            headlines.addLast("FLASH : " + text);
            if (headlines.size() > MAX_HISTORY) headlines.pollFirst();
            rebuildVisible();
        }
    }

    /** Drain newly activated rumors from the engine and add them as headlines. */
    public void pollRumors() {
        if (rumorManager == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRumorCheck < 500) return;  // check every 500 ms
        lastRumorCheck = now;

        List<Rumeur> active = rumorManager.getActiveRumorsSnapshot();
        for (Rumeur r : active) {
            String headline = buildHeadline(r);
            synchronized (headlines) {
                if (!headlineAlreadyPresent(headline)) {
                    headlines.addLast(headline);
                    if (headlines.size() > MAX_HISTORY) headlines.pollFirst();
                }
            }
        }
        rebuildVisible();
    }

    /* ── rendering ───────────────────────────────────────────── */

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        int w = getWidth();
        int h = getHeight();

        // Background bar
        g.setColor(BG_DARK);
        g.fillRect(0, 0, w, h);

        // Flash label
        g.setColor(BG_FLASH);
        g.fillRect(0, 0, 80, h);
        g.setFont(FONT_LABEL);
        g.setColor(TEXT_FLASH);
        FontMetrics fm = g.getFontMetrics();
        String label = "EN DIRECT";
        g.drawString(label, 4, h / 2 + fm.getAscent() / 2 - 2);

        // Separator line
        g.setColor(SEPARATOR);
        g.fillRect(80, 0, 3, h);

        // Clip scroll area
        g.setClip(84, 0, w - 84, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setFont(FONT_NEWS);
        fm = g.getFontMetrics();

        synchronized (headlines) {
            int x = (int) (w - scrollX) + 84;
            for (String item : visibleItems) {
                boolean isFlash = item.startsWith("FLASH");
                g.setColor(isFlash ? TEXT_FLASH : TEXT_MAIN);
                g.drawString(item, x, h / 2 + fm.getAscent() / 2 - 1);
                x += fm.stringWidth(item) + PADDING_BETWEEN;

                g.setColor(SEPARATOR);
                g.drawString("  ◆  ", x - PADDING_BETWEEN + fm.stringWidth(item) + 6, h / 2 + fm.getAscent() / 2 - 1);
            }
        }

        g.dispose();
    }

    /* ── helpers ─────────────────────────────────────────────── */

    private void advanceScroll() {
        long now = System.nanoTime();
        double delta = (now - lastUpdateNanos) / 1_000_000_000.0;
        lastUpdateNanos = now;

        pollRumors();

        synchronized (headlines) {
            if (visibleItems.isEmpty()) return;
            scrollX += SCROLL_SPEED * delta;
            if (scrollX > totalTextWidth + getWidth()) {
                scrollX = 0;
            }
        }
    }

    private void rebuildVisible() {
        synchronized (headlines) {
            visibleItems.clear();
            visibleItems.addAll(headlines);
            if (visibleItems.isEmpty()) addDefaultToVisible();

            // Compute total text width (approx; recalculated on paint if needed)
            totalTextWidth = visibleItems.stream().mapToInt(s -> {
                FontMetrics fm = getFontMetrics(FONT_NEWS);
                return fm == null ? s.length() * 8 : fm.stringWidth(s) + PADDING_BETWEEN;
            }).sum();
        }
    }

    private void addDefaultToVisible() {
        visibleItems.add("Simulation SEIRS en cours — aucune rumeur active");
    }

    private boolean headlineAlreadyPresent(String headline) {
        for (String h : headlines) {
            if (h.equals(headline)) return true;
        }
        return false;
    }

    private static String buildHeadline(Rumeur r) {
        String prefix = r.getTitre().startsWith("Le ") || r.getTitre().startsWith("Un ") ? "" : "";
        double impact = r.getImpactBienveillance();
        String tone = impact < -0.3 ? "ALERTE" : impact > 0.3 ? "INFO" : "FLASH";
        return tone + " : " + r.getTitre();
    }

    private void addDefaultHeadlines() {
        headlines.addLast("Simulation SEIRS démarrée — 4 zones en surveillance");
        headlines.addLast("FLASH : Activité épidémique détectée — suivez les indicateurs");
        rebuildVisible();
    }
}
