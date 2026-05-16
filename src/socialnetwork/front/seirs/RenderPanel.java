package socialnetwork.front.seirs;

import socialnetwork.back.seirs.Bavard;
import socialnetwork.back.seirs.EtatSEIRS;
import socialnetwork.back.seirs.PlaceInteret;
import socialnetwork.back.seirs.PlacesManager;
import socialnetwork.back.seirs.SimulationEngine;
import socialnetwork.back.seirs.Vector2D;
import socialnetwork.back.seirs.ZoneManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module 5 — primary simulation render surface.
 *
 * Render pipeline (each frame):
 *   1. Sky gradient (EnvironmentManager)
 *   2. Static map background (MapRenderer — cached BufferedImage)
 *   3. Heatmap overlay (HeatmapOverlay)
 *   4. Stars / clouds / rain (ParticleSystem)
 *   5. Night darkness overlay (EnvironmentManager)
 *   6. City glow (EnvironmentManager)
 *   7. Agent halos at night (EnvironmentManager)
 *   8. Agents (tiny 2–3 px dots, colour-coded by SEIRS state)
 *   9. Floating Combat Text above infectious agents
 *  10. UI chrome (FPS counter, time badge)
 */
@SuppressWarnings("serial")
public final class RenderPanel extends JPanel {

    /* ── agent dot constants ─────────────────────────────────── */
    private static final int    DOT_RADIUS_S   = 2;
    private static final int    DOT_RADIUS_E   = 2;
    private static final int    DOT_RADIUS_I   = 2;
    private static final int    DOT_RADIUS_R   = 2;
    private static final int    AURA_RADIUS_I  = 8;   // infectious glow radius
    private static final Color  COL_S          = new Color( 80, 165, 255);
    private static final Color  COL_E          = new Color(245, 145,  35);
    private static final Color  COL_I          = new Color(235,  30,  30);
    private static final Color  COL_I_AURA     = new Color(235,  30,  30,  28);
    private static final Color  COL_R          = new Color( 50, 240, 100);

    /* ── FCT constants ───────────────────────────────────────── */
    private static final Font   FCT_FONT       = new Font("Segoe UI", Font.BOLD, 8);
    private static final int    FCT_LIFETIME   = 90;  // frames
    private static final String[] FCT_MESSAGES = {
        "contaminé!", "toussote…", "INFECTIEUX", "fiévreux",
        "symptômes!", "propagation", "virus actif", "alerte!"
    };

    /* ── HUD constants ───────────────────────────────────────── */
    private static final Font  HUD_FONT        = new Font("Segoe UI", Font.BOLD, 10);
    private static final Color HUD_COLOR       = new Color(200, 210, 240, 200);

    /* ── engine & subsystems ─────────────────────────────────── */
    private volatile SimulationEngine engine;
    private final EnvironmentManager  envManager;
    private final ParticleSystem      particles;
    private final HeatmapOverlay      heatmap;
    private MapRenderer               mapRenderer;  // created lazily after setEngine()

    /* ── render state ────────────────────────────────────────── */
    private BufferedImage  backBuffer;
    private int            backW = -1;
    private int            backH = -1;
    private long           lastFrameNanos = System.nanoTime();
    private double         realDelta = 0.0;
    private int            fps = 0;
    private int            fpsCounter = 0;
    private long           fpsLastTime = System.nanoTime();

    /* ── FCT list ────────────────────────────────────────────── */
    private static final class FctEntry {
        float screenX, screenY;
        String text;
        int lifetime;
        float alpha;

        FctEntry(float x, float y, String text) {
            this.screenX = x; this.screenY = y;
            this.text = text;
            this.lifetime = FCT_LIFETIME;
            this.alpha = 1.0f;
        }
    }

    private final List<FctEntry> fctEntries = new ArrayList<>(64);
    private int fctSpawnCooldown = 0;
    private final java.util.Random fctRng = new java.util.Random();

    /* ── selection / follow ──────────────────────────────────── */
    private volatile Bavard   selectedAgent  = null;
    private volatile boolean  followingAgent = false;
    private static final Color SEL_COLOR = new Color(255, 255,  80, 200);
    private static final Color FOL_COLOR = new Color( 80, 255, 255, 200);

    /** Callback fired when user clicks an agent (null if no agent near click). */
    private Consumer<Bavard> onAgentSelected;

    /** POI rendering toggle. */
    private volatile boolean showPois = true;

    /* ── constructor ─────────────────────────────────────────── */

    public RenderPanel() {
        setBackground(Color.BLACK);
        setDoubleBuffered(false); // we manage our own back-buffer

        this.mapRenderer = null;  // created in setEngine()
        this.envManager  = new EnvironmentManager();
        this.particles   = new ParticleSystem();
        this.heatmap     = new HeatmapOverlay();

        // Main 60fps render timer
        Timer renderTimer = new Timer(16, e -> {
            long now = System.nanoTime();
            realDelta = Math.min(0.1, (now - lastFrameNanos) / 1_000_000_000.0);
            lastFrameNanos = now;
            tick();
            repaint();
        });
        renderTimer.setCoalesce(true);
        renderTimer.start();

        // Click-to-inspect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    /* ── public API ──────────────────────────────────────────── */

    public void setOnAgentSelected(Consumer<Bavard> callback) { this.onAgentSelected = callback; }
    public void setShowPois(boolean show)                     { this.showPois = show; }
    public boolean isShowPois()                               { return showPois; }
    public Bavard  getSelectedAgent()                         { return selectedAgent; }
    public void    setSelectedAgent(Bavard b)                 { this.selectedAgent = b; }
    public boolean isFollowingAgent()                         { return followingAgent; }
    public void    setFollowingAgent(boolean f)               { this.followingAgent = f; }

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
        this.mapRenderer = new MapRenderer(engine.getZoneManager());
    }

    public EnvironmentManager getEnvironmentManager() { return envManager; }
    public HeatmapOverlay     getHeatmapOverlay()      { return heatmap;   }
    public ParticleSystem     getParticleSystem()       { return particles; }

    /* ── tick (called on EDT every frame) ────────────────────── */

    private void tick() {
        SimulationEngine eng = engine;
        if (eng == null) return;

        double hour = eng.getHourOfDay();
        envManager.update(hour);

        boolean rain = (eng.getTick() / 720) % 5 == 0;  // pseudo-weather cycle
        float rainI = rain ? 0.6f + 0.4f * (float) Math.sin(eng.getElapsedRealSeconds() * 0.3) : 0f;
        particles.update(realDelta, rain, rainI, eng.getElapsedRealSeconds());

        List<Bavard> agents = eng.getAgentsSnapshot();
        heatmap.update(agents);

        tickFCT(agents);
        tickFPS();
    }

    private void tickFPS() {
        fpsCounter++;
        long now = System.nanoTime();
        if (now - fpsLastTime >= 1_000_000_000L) {
            fps = fpsCounter;
            fpsCounter = 0;
            fpsLastTime = now;
        }
    }

    private void tickFCT(List<Bavard> agents) {
        // Advance existing entries
        Iterator<FctEntry> it = fctEntries.iterator();
        while (it.hasNext()) {
            FctEntry e = it.next();
            e.lifetime--;
            e.screenY -= 0.4f;
            e.alpha = Math.max(0f, (float) e.lifetime / FCT_LIFETIME);
            if (e.lifetime <= 0) it.remove();
        }

        // Spawn new FCT for infectious agents
        if (fctSpawnCooldown > 0) { fctSpawnCooldown--; return; }
        if (fctEntries.size() >= 80) return;

        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());

        for (Bavard agent : agents) {
            if (agent.getEtatSEIRS() != EtatSEIRS.INFECTIOUS) continue;
            if (fctRng.nextInt(100) > 1) continue;  // 1% chance per infectious agent per frame

            Vector2D pos = agent.getSnapshotPosition();
            float sx = worldToScreenX(pos.getX(), w);
            float sy = worldToScreenY(pos.getY(), h);
            String msg = FCT_MESSAGES[fctRng.nextInt(FCT_MESSAGES.length)];
            fctEntries.add(new FctEntry(sx, sy - 6, msg));
            fctSpawnCooldown = 4;
            break;
        }
    }

    /* ── paintComponent ──────────────────────────────────────── */

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Ensure back-buffer is the right size
        if (backBuffer == null || backW != w || backH != h) {
            backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            backW = w; backH = h;
            particles.resize(w, h);
            mapRenderer.invalidate();
        }

        Graphics2D bg = backBuffer.createGraphics();
        renderFrame(bg, w, h);
        bg.dispose();

        graphics.drawImage(backBuffer, 0, 0, null);
    }

    /* ── render pipeline ─────────────────────────────────────── */

    private void renderFrame(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // 1. Sky
        envManager.drawSky(g, w, h);

        // 2. Map background (cached bitmap)
        if (mapRenderer != null) {
            BufferedImage mapImg = mapRenderer.getMapImage(w, h);
            g.drawImage(mapImg, 0, 0, null);
        }

        // 2b. Sky tint over map (fixes sky invisibility — applies after opaque map)
        envManager.drawSkyTintOverMap(g, w, h);

        // 3. Heatmap overlay (under agents)
        heatmap.draw(g, w, h);

        // 3b. POI icons (if available)
        if (showPois) renderPOIs(g, w, h);

        // 4. Particles: clouds
        particles.drawClouds(g);

        // 5. Night overlay
        envManager.drawNightOverlay(g, w, h);

        // 6. Stars (night only)
        float nightA = envManager.getNightAlpha();
        if (nightA > 0.05f) {
            particles.drawStars(g, nightA);
        }

        // 7. City glow
        envManager.drawCityGlow(g, w, h,
            1100.0, 970.0,
            ZoneManager.WORLD_WIDTH, ZoneManager.WORLD_HEIGHT);

        // 8. Rain (above everything except agents/UI)
        particles.drawRain(g);

        // 9. Agents
        renderAgents(g, w, h, nightA);

        // 10. FCT
        renderFCT(g);

        // 11. HUD chrome
        renderHUD(g, w, h);
    }

    /* ── POI rendering ───────────────────────────────────────── */

    private void renderPOIs(Graphics2D g, int w, int h) {
        SimulationEngine eng = engine;
        if (eng == null) return;
        PlacesManager pm = eng.getPlacesManager();
        if (pm == null) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (PlaceInteret poi : pm.getAllPlaces()) {
            PlaceInteret.TypeLieu type = poi.getType();
            if (type == PlaceInteret.TypeLieu.DOMICILE) continue; // too many, clutters display
            Vector2D pos = poi.positionRef();
            int px = (int) worldToScreenX(pos.getX(), w);
            int py = (int) worldToScreenY(pos.getY(), h);
            Color c = parseHex(type.hexColor);
            g.setColor(c);
            // Draw a small 3px diamond
            int[] xs = {px, px+3, px, px-3};
            int[] ys = {py-3, py, py+3, py};
            g.fillPolygon(xs, ys, 4);
        }
        g.setComposite(old);
    }

    private static Color parseHex(String hex) {
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    /* ── agent rendering ─────────────────────────────────────── */

    private void renderAgents(Graphics2D g, int w, int h, float nightAlpha) {
        SimulationEngine eng = engine;
        if (eng == null) return;

        List<Bavard> agents = eng.getAgentsSnapshot();

        // First pass: auras for INFECTIOUS (drawn behind dots)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Bavard agent : agents) {
            if (agent.getEtatSEIRS() != EtatSEIRS.INFECTIOUS) continue;
            Vector2D pos = agent.getSnapshotPosition();
            int px = (int) worldToScreenX(pos.getX(), w);
            int py = (int) worldToScreenY(pos.getY(), h);
            g.setColor(COL_I_AURA);
            int r2 = AURA_RADIUS_I * 2;
            g.fillOval(px - AURA_RADIUS_I, py - AURA_RADIUS_I, r2, r2);
        }

        // Second pass: halos at night + dots
        boolean drawHalos = nightAlpha > 0.22f;
        for (Bavard agent : agents) {
            Vector2D pos = agent.getSnapshotPosition();
            double wx = pos.getX();
            double wy = pos.getY();
            int px = (int) worldToScreenX(wx, w);
            int py = (int) worldToScreenY(wy, h);

            if (drawHalos) {
                envManager.drawAgentHalo(g, px, py);
            }

            EtatSEIRS state = agent.getEtatSEIRS();
            int r = switch (state) {
                case SUSCEPTIBLE -> DOT_RADIUS_S;
                case EXPOSED     -> DOT_RADIUS_E;
                case INFECTIOUS  -> DOT_RADIUS_I;
                case RECOVERED   -> DOT_RADIUS_R;
            };
            Color col = switch (state) {
                case SUSCEPTIBLE -> COL_S;
                case EXPOSED     -> COL_E;
                case INFECTIOUS  -> COL_I;
                case RECOVERED   -> COL_R;
            };

            g.setColor(col);
            g.fillOval(px - r, py - r, r * 2, r * 2);
        }

        // Selection ring
        Bavard sel = selectedAgent;
        if (sel != null) {
            Vector2D selPos = sel.getSnapshotPosition();
            int spx = (int) worldToScreenX(selPos.getX(), w);
            int spy = (int) worldToScreenY(selPos.getY(), h);
            Color ringColor = followingAgent ? FOL_COLOR : SEL_COLOR;
            g.setColor(ringColor);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 3f}, 0f));
            int ringR = 7;
            g.drawOval(spx - ringR, spy - ringR, ringR * 2, ringR * 2);
            g.setStroke(new BasicStroke(1f));
        }
    }

    /* ── FCT rendering ───────────────────────────────────────── */

    private void renderFCT(Graphics2D g) {
        if (fctEntries.isEmpty()) return;
        g.setFont(FCT_FONT);
        for (FctEntry e : fctEntries) {
            if (e.alpha < 0.02f) continue;
            g.setColor(new Color(255, 80, 80, (int)(e.alpha * 200)));
            g.drawString(e.text, (int) e.screenX, (int) e.screenY);
        }
    }

    /* ── HUD ─────────────────────────────────────────────────── */

    private void renderHUD(Graphics2D g, int w, int h) {
        g.setFont(HUD_FONT);
        g.setColor(HUD_COLOR);
        g.drawString("FPS: " + fps, 8, 14);

        SimulationEngine eng = engine;
        if (eng != null) {
            double hour = eng.getHourOfDay();
            int hh = (int) hour;
            int mm = (int) ((hour - hh) * 60);
            String time = String.format("J%d  %02d:%02d", eng.getDayIndex() + 1, hh, mm);
            g.drawString(time, 8, 26);

            int total = eng.getAgentsSnapshot().size();
            g.drawString("Agents: " + total, 8, 38);

            String speedLabel = String.format("x%.2f", eng.getTimeScale());
            g.drawString("Vitesse: " + speedLabel, 8, 50);
        }

        Bavard sel = selectedAgent;
        if (sel != null) {
            String label = (followingAgent ? "\u25BA SUIVANT: " : "Sél.: ") + sel.getNom();
            g.setColor(followingAgent ? FOL_COLOR : SEL_COLOR);
            g.drawString(label, 8, h - 10);
        }
    }

    /* ── coordinate helpers ──────────────────────────────────── */

    public static float worldToScreenX(double wx, int panelW) {
        return (float) (wx / ZoneManager.WORLD_WIDTH * panelW);
    }

    public static float worldToScreenY(double wy, int panelH) {
        return (float) (wy / ZoneManager.WORLD_HEIGHT * panelH);
    }

    private static double screenToWorldX(int sx, int panelW) {
        return sx * ZoneManager.WORLD_WIDTH / Math.max(1, panelW);
    }

    private static double screenToWorldY(int sy, int panelH) {
        return sy * ZoneManager.WORLD_HEIGHT / Math.max(1, panelH);
    }

    /* ── click-to-inspect ───────────────────────────────────────── */

    private void handleClick(int mx, int my) {
        SimulationEngine eng = engine;
        if (eng == null) return;
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        double clickWx = screenToWorldX(mx, w);
        double clickWy = screenToWorldY(my, h);

        // pixel threshold converted to world units (approx 12 pixels)
        double threshPx = 12.0;
        double threshWx = threshPx * ZoneManager.WORLD_WIDTH  / w;
        double threshWy = threshPx * ZoneManager.WORLD_HEIGHT / h;
        double thresh2  = threshWx * threshWy * 4.0; // rough ellipse

        List<Bavard> agents = eng.getAgentsSnapshot();
        Bavard closest = null;
        double closestDist2 = thresh2;
        for (Bavard agent : agents) {
            Vector2D pos = agent.getSnapshotPosition();
            double dx = pos.getX() - clickWx;
            double dy = pos.getY() - clickWy;
            double d2 = dx * dx + dy * dy;
            if (d2 < closestDist2) {
                closestDist2 = d2;
                closest = agent;
            }
        }
        selectedAgent  = closest;
        followingAgent = false;
        if (onAgentSelected != null) {
            onAgentSelected.accept(closest); // null if nothing clicked
        }
    }
}
