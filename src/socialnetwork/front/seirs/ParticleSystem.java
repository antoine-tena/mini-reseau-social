package socialnetwork.front.seirs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.RenderingHints;
import java.awt.BasicStroke;

/**
 * Manages all particle effects: clouds, rain, stars.
 * Ticked on the Swing EDT at render time (delta from EnvironmentManager).
 */
public final class ParticleSystem {

    /* ═══  CLOUD  ══════════════════════════════════════════════ */
    private static final int CLOUD_COUNT   = 18;
    private static final double CLOUD_SPEED_MIN  = 12.0;
    private static final double CLOUD_SPEED_MAX  = 30.0;

    private static final class Cloud {
        double x, y, w, h;
        float alpha;
        double speed;
        int layer;  // 0 = back, 1 = mid, 2 = front

        Cloud(double x, double y, double w, double h, float alpha, double speed, int layer) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.alpha = alpha; this.speed = speed; this.layer = layer;
        }
    }

    /* ═══  RAIN  ═══════════════════════════════════════════════ */
    private static final int RAIN_PARTICLE_COUNT = 1800;

    private static final class RainDrop {
        float x, y;
        float length;
        float speedY;
        float speedX;
        float alpha;

        RainDrop(float x, float y, float length, float speedY, float speedX, float alpha) {
            this.x = x; this.y = y; this.length = length;
            this.speedY = speedY; this.speedX = speedX; this.alpha = alpha;
        }
    }

    /* ═══  STAR  ═══════════════════════════════════════════════ */
    private static final int STAR_COUNT = 380;

    private static final class Star {
        float x, y, radius;
        float twinklePhase;
        float twinkleSpeed;
        float baseBrightness;
    }

    /* ═══  FIELDS  ══════════════════════════════════════════════ */
    private final List<Cloud>    clouds   = new ArrayList<>(CLOUD_COUNT);
    private final List<RainDrop> rainDops = new ArrayList<>(RAIN_PARTICLE_COUNT);
    private final List<Star>     stars    = new ArrayList<>(STAR_COUNT);
    private final Random         rng      = new Random(0xC10D5EEL);

    private int   panelW = 1200;
    private int   panelH = 800;
    private boolean rainActive = false;
    private float  rainIntensity = 0f;  // 0..1
    private double simTime = 0.0;

    public ParticleSystem() {
        initClouds();
        initRain();
        initStars();
    }

    /* ══  public API  ══════════════════════════════════════════ */

    public void resize(int w, int h) {
        panelW = w;
        panelH = h;
    }

    /** Call from render loop: advance particles by realDeltaSeconds. */
    public void update(double delta, boolean raining, float rainI, double simTime) {
        this.simTime = simTime;
        this.rainActive = raining;
        this.rainIntensity = Math.max(0f, Math.min(1f, rainI));
        updateClouds(delta);
        if (rainActive) updateRain(delta);
    }

    /** Draw clouds (before agents). */
    public void drawClouds(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Cloud c : clouds) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, c.alpha));
            g.setColor(new Color(220, 220, 225));
            drawCloudShape(g, c);
            g.setComposite(old);
        }
    }

    /** Draw rain (after clouds, before agents). */
    public void drawRain(Graphics2D g) {
        if (!rainActive || rainIntensity < 0.01f) return;
        Composite old = g.getComposite();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(0.8f));
        for (RainDrop drop : rainDops) {
            float alpha = drop.alpha * rainIntensity * 0.75f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(180, 200, 240));
            int x1 = (int) drop.x;
            int y1 = (int) drop.y;
            int x2 = (int) (drop.x + drop.speedX * 0.12f);
            int y2 = (int) (drop.y + drop.length);
            g.drawLine(x1, y1, x2, y2);
        }
        g.setComposite(old);
    }

    /** Draw stars (night only, call only when nightAlpha > 0). */
    public void drawStars(Graphics2D g, float nightAlpha) {
        if (nightAlpha < 0.05f) return;
        Composite old = g.getComposite();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Star star : stars) {
            double twinkle = 0.55 + 0.45 * Math.sin(simTime * star.twinkleSpeed + star.twinklePhase);
            float alpha = nightAlpha * star.baseBrightness * (float) twinkle;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
            g.setColor(Color.WHITE);
            int r = (int) Math.ceil(star.radius);
            int sx = (int) star.x;
            int sy = (int) star.y;
            g.fillOval(sx - r, sy - r, r * 2 + 1, r * 2 + 1);
        }
        g.setComposite(old);
    }

    /* ══  init helpers  ════════════════════════════════════════ */

    private void initClouds() {
        clouds.clear();
        for (int i = 0; i < CLOUD_COUNT; i++) {
            clouds.add(makeCloud((float) (rng.nextDouble() * panelW)));
        }
    }

    private void initRain() {
        rainDops.clear();
        for (int i = 0; i < RAIN_PARTICLE_COUNT; i++) {
            rainDops.add(makeRainDrop());
        }
    }

    private void initStars() {
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            Star s = new Star();
            s.x = rng.nextInt(Math.max(1, panelW));
            s.y = rng.nextInt(Math.max(1, panelH / 2));
            s.radius = 0.5f + rng.nextFloat() * 1.2f;
            s.twinklePhase = (float) (rng.nextDouble() * Math.PI * 2);
            s.twinkleSpeed = 0.3f + rng.nextFloat() * 1.8f;
            s.baseBrightness = 0.25f + rng.nextFloat() * 0.75f;
            stars.add(s);
        }
    }

    /* ══  update helpers  ══════════════════════════════════════ */

    private void updateClouds(double delta) {
        for (Cloud c : clouds) {
            c.x += c.speed * delta;
            if (c.x > panelW + c.w) {
                reset(c);
            }
        }
    }

    private void updateRain(double delta) {
        for (RainDrop d : rainDops) {
            d.y += d.speedY * (float) delta;
            d.x += d.speedX * (float) delta;
            if (d.y > panelH || d.x > panelW) {
                RainDrop fresh = makeRainDrop();
                d.x = fresh.x; d.y = fresh.y; d.length = fresh.length;
                d.speedX = fresh.speedX; d.speedY = fresh.speedY; d.alpha = fresh.alpha;
            }
        }
    }

    /* ══  factory helpers  ═════════════════════════════════════ */

    private Cloud makeCloud(float startX) {
        int layer = rng.nextInt(3);
        double w = 120 + rng.nextDouble() * 280;
        double h = 30 + rng.nextDouble() * 70;
        double y = 20 + rng.nextDouble() * (panelH * 0.28);
        float alpha = 0.18f + rng.nextFloat() * 0.30f;
        double speed = CLOUD_SPEED_MIN + rng.nextDouble() * (CLOUD_SPEED_MAX - CLOUD_SPEED_MIN);
        speed *= switch (layer) { case 0 -> 0.45; case 1 -> 0.75; default -> 1.0; };
        return new Cloud(startX, y, w, h, alpha, speed, layer);
    }

    private void reset(Cloud c) {
        double w = 120 + rng.nextDouble() * 280;
        double h = 30 + rng.nextDouble() * 70;
        c.x = -w;
        c.y = 20 + rng.nextDouble() * (panelH * 0.28);
        c.w = w; c.h = h;
        c.alpha = 0.18f + rng.nextFloat() * 0.30f;
    }

    private RainDrop makeRainDrop() {
        float x = rng.nextFloat() * panelW;
        float y = rng.nextFloat() * panelH;
        float length = 6 + rng.nextFloat() * 10;
        float speedY = 300 + rng.nextFloat() * 200;
        float speedX = -20 - rng.nextFloat() * 40;
        float alpha = 0.3f + rng.nextFloat() * 0.4f;
        return new RainDrop(x, y, length, speedY, speedX, alpha);
    }

    private static void drawCloudShape(Graphics2D g, Cloud c) {
        int n = Math.max(3, (int) (c.w / 28));
        double stepX = c.w / Math.max(1, n - 1);
        for (int i = 0; i < n; i++) {
            double bx = c.x + i * stepX;
            double by = c.y + c.h * 0.5;
            double br = c.h * (0.35 + 0.3 * Math.abs(Math.sin(i * 1.1)));
            g.fillOval((int)(bx - br), (int)(by - br), (int)(br * 2), (int)(br * 2));
        }
    }
}
