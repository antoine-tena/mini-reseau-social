package socialnetwork.front.seirs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.MultipleGradientPaint;

/**
 * Manages:
 *   - sky colour gradient (day/night cycle)
 *   - ambient darkness overlay
 *   - city light pollution (Ville Principale)
 *   - agent halos at night
 *
 * Driven by SimulationEngine.getHourOfDay().
 */
public final class EnvironmentManager {

    /* ── sky colour keyframes: (hour, R, G, B) ─────────────── */
    private static final float[][] SKY_KEYS = {
        { 0.0f, 4,   4,  16},   // midnight — deep black-blue
        { 4.5f, 6,   8,  26},   // pre-dawn
        { 6.0f,140,  70,  30},   // dawn orange/red
        { 7.5f, 90, 130, 190},   // early morning blue
        { 9.0f, 65, 120, 200},   // day sky
        {18.5f, 65, 120, 200},   // late afternoon — same
        {20.0f,150,  85,  35},   // dusk / crépuscule
        {21.5f, 30,  20,  60},   // twilight deep purple
        {23.0f,  4,   4,  16},   // night again
        {24.0f,  4,   4,  16},   // end sentinel
    };

    /* ── city-glow constants ─────────────────────────────────── */
    private static final float CITY_GLOW_RADIUS_WORLD = 600.0f;

    /* ── state ───────────────────────────────────────────────── */
    private double hourOfDay = 8.0;
    private float nightAlpha = 0.0f;  // 0=day, 1=midnight

    /** Must be called every render frame, before drawBackground(). */
    public void update(double hourOfDay) {
        this.hourOfDay = hourOfDay % 24.0;
        if (this.hourOfDay < 0) this.hourOfDay += 24.0;
        this.nightAlpha = computeNightAlpha((float) this.hourOfDay);
    }

    public float getNightAlpha() { return nightAlpha; }
    public double getHourOfDay() { return hourOfDay; }

    /**
     * Draws the full-screen sky gradient (drawn first, behind map).
     */
    public void drawSky(Graphics2D g, int w, int h) {
        Color top    = interpolateSky((float) hourOfDay, 0.0f);
        Color bottom = interpolateSky((float) hourOfDay, 1.0f);
        GradientPaint sky = new GradientPaint(0, 0, top, 0, h, bottom);
        g.setPaint(sky);
        g.fillRect(0, 0, w, h);
        g.setPaint(null);
    }

    /**
     * Draws a translucent sky-colour tint OVER the map image so the time of day
     * is always visible. Draw this AFTER the map, BEFORE the night overlay.
     * During full daylight (9-18h) the alpha is ~0.18; during transitions it
     * fades toward 0 since the night overlay takes over.
     */
    public void drawSkyTintOverMap(Graphics2D g, int w, int h) {
        // Tint is strongest in daytime, fades out as night overlay takes over
        float tintAlpha = (1.0f - nightAlpha) * 0.18f;
        if (tintAlpha < 0.005f) return;
        Color top    = interpolateSky((float) hourOfDay, 0.0f);
        Color bottom = interpolateSky((float) hourOfDay, 1.0f);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tintAlpha));
        GradientPaint tint = new GradientPaint(0, 0, top, 0, h, bottom);
        g.setPaint(tint);
        g.fillRect(0, 0, w, h);
        g.setPaint(null);
        g.setComposite(old);
    }

    /**
     * Draws a semi-transparent night overlay.
     * Darkness is zero at noon, max (0.82) at midnight.
     */
    public void drawNightOverlay(Graphics2D g, int w, int h) {
        if (nightAlpha < 0.01f) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nightAlpha * 0.82f));
        g.setColor(new Color(4, 5, 18));
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
    }

    /**
     * Draws city light-pollution glow centred on VILLE_PRINCIPALE.
     * worldToPanelX/Y: converter lambdas from world coords to panel pixels.
     * @param cityWorldX  centre X in world units (≈ 1100)
     * @param cityWorldY  centre Y in world units (≈ 970)
     */
    public void drawCityGlow(Graphics2D g, int w, int h,
                             double cityWorldX, double cityWorldY,
                             double worldWidth, double worldHeight) {
        if (nightAlpha < 0.05f) return;

        float cx = (float) (cityWorldX / worldWidth  * w);
        float cy = (float) (cityWorldY / worldHeight * h);
        float glowR = (float) (CITY_GLOW_RADIUS_WORLD / worldWidth * w);

        float glowAlpha = nightAlpha * 0.38f;

        float[] dist   = {0.0f, 0.55f, 1.0f};
        Color[] colors = {
            new Color(255, 195,  60, (int)(255 * glowAlpha)),
            new Color(220, 140,  40, (int)(255 * glowAlpha * 0.45f)),
            new Color(0, 0, 0, 0)
        };
        RadialGradientPaint glow = new RadialGradientPaint(
            cx, cy, glowR, dist, colors,
            MultipleGradientPaint.CycleMethod.NO_CYCLE);

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g.setPaint(glow);
        g.fillOval((int)(cx - glowR), (int)(cy - glowR), (int)(glowR * 2), (int)(glowR * 2));
        g.setPaint(null);
        g.setComposite(old);
    }

    /**
     * Draws a small white halo around a single agent position (screen coords).
     * Only call when nightAlpha > 0.2.
     */
    public void drawAgentHalo(Graphics2D g, int px, int py) {
        float r = 8.0f;
        float[] dist   = {0.0f, 1.0f};
        Color[] colors = {
            new Color(255, 255, 255, (int)(255 * nightAlpha * 0.55f)),
            new Color(255, 255, 255, 0)
        };
        try {
            RadialGradientPaint halo = new RadialGradientPaint(px, py, r, dist, colors);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g.setPaint(halo);
            g.fillOval(px - (int)r, py - (int)r, (int)(r * 2), (int)(r * 2));
            g.setPaint(null);
            g.setComposite(old);
        } catch (IllegalArgumentException ignored) {
            // degenerate paint — skip
        }
    }

    /* ── helpers ─────────────────────────────────────────────── */

    private static float computeNightAlpha(float hour) {
        // 0 at 9h–18h, ramps up to 1 at midnight
        if (hour >= 9.0f && hour <= 18.0f) return 0.0f;
        if (hour > 18.0f && hour < 21.0f) {
            return (hour - 18.0f) / 3.0f;
        }
        if (hour >= 21.0f) {
            float midnightProgress = (hour - 21.0f) / 3.0f;
            return Math.min(1.0f, midnightProgress + 0.6f);
        }
        // 0..9 — after midnight
        if (hour < 6.0f) return 1.0f;
        return 1.0f - (hour - 6.0f) / 3.0f;
    }

    /** Interpolates the sky colour at a given vertical ratio (0=top, 1=bottom). */
    private static Color interpolateSky(float hour, float vRatio) {
        float h = hour + vRatio * 2.5f; // subtle gradient offset top/bottom
        if (h >= 24.0f) h -= 24.0f;

        int i = 0;
        while (i < SKY_KEYS.length - 2 && SKY_KEYS[i + 1][0] < h) i++;

        float h0 = SKY_KEYS[i][0];
        float h1 = SKY_KEYS[i + 1][0];
        float t  = (h1 > h0) ? (h - h0) / (h1 - h0) : 0.0f;
        t = Math.max(0f, Math.min(1f, t));

        int r = lerp((int)SKY_KEYS[i][1], (int)SKY_KEYS[i+1][1], t);
        int gC= lerp((int)SKY_KEYS[i][2], (int)SKY_KEYS[i+1][2], t);
        int b = lerp((int)SKY_KEYS[i][3], (int)SKY_KEYS[i+1][3], t);
        return new Color(clamp(r), clamp(gC), clamp(b));
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
