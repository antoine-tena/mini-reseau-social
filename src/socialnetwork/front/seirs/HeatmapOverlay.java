package socialnetwork.front.seirs;

import socialnetwork.back.seirs.Bavard;
import socialnetwork.back.seirs.EtatSEIRS;
import socialnetwork.back.seirs.SpatialHashGrid;
import socialnetwork.back.seirs.ZoneManager;
import socialnetwork.back.seirs.Vector2D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overlays a thermal grid that highlights zones of high infectious-agent density.
 * Grid resolution: CELL_SIZE in world units.
 * Call update() once per frame from the render thread.
 */
public final class HeatmapOverlay {

    private static final double CELL_SIZE_WORLD = 120.0;
    private static final float  MAX_ALPHA       = 0.64f;
    private static final int    HOT_THRESHOLD   = 3;    // agents per cell to start warming

    private final int cols;
    private final int rows;
    private final float[] heat;      // [rows][cols] flattened, range 0..1
    private final float[] smoothed;  // low-pass smoothed version for rendering

    private float globalAlpha = 0.9f;
    private boolean visible   = true;

    public HeatmapOverlay() {
        this.cols = (int) Math.ceil(ZoneManager.WORLD_WIDTH  / CELL_SIZE_WORLD);
        this.rows = (int) Math.ceil(ZoneManager.WORLD_HEIGHT / CELL_SIZE_WORLD);
        this.heat     = new float[rows * cols];
        this.smoothed = new float[rows * cols];
    }

    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible()              { return visible; }

    /**
     * Recompute heatmap from current snapshot of agents.
     * Call this from the simulation-side or just before rendering.
     */
    public void update(List<Bavard> agents) {
        java.util.Arrays.fill(heat, 0f);

        for (Bavard agent : agents) {
            if (agent.getEtatSEIRS() != EtatSEIRS.INFECTIOUS) continue;
            Vector2D pos = agent.getSnapshotPosition();
            int col = (int) (pos.getX() / CELL_SIZE_WORLD);
            int row = (int) (pos.getY() / CELL_SIZE_WORLD);
            if (col >= 0 && col < cols && row >= 0 && row < rows) {
                heat[row * cols + col] += 1.0f;
            }
        }

        int maxCount = HOT_THRESHOLD * 8;
        for (int i = 0; i < heat.length; i++) {
            heat[i] = Math.min(1.0f, heat[i] / maxCount);
        }

        // Spatial low-pass blur (3×3 box)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float sum = 0; int n = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr; int nc = c + dc;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                            sum += heat[nr * cols + nc]; n++;
                        }
                    }
                }
                smoothed[r * cols + c] = sum / n;
            }
        }
    }

    /**
     * Render the overlay onto the given Graphics2D (already in panel pixel space).
     */
    public void draw(Graphics2D g, int panelW, int panelH) {
        if (!visible) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Composite old = g.getComposite();

        double cellW = (double) panelW / cols;
        double cellH = (double) panelH / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float v = smoothed[r * cols + c];
                if (v < 0.02f) continue;

                float alpha = v * MAX_ALPHA * globalAlpha;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
                g.setColor(heatColor(v));

                int x = (int) Math.round(c * cellW);
                int y = (int) Math.round(r * cellH);
                int cw = (int) Math.ceil(cellW) + 1;
                int ch = (int) Math.ceil(cellH) + 1;
                g.fillRect(x, y, cw, ch);
            }
        }

        g.setComposite(old);
    }

    /* ── helpers ─────────────────────────────────────────────── */

    /** Maps value [0..1] to a colour: blue → yellow → red → white-hot. */
    private static Color heatColor(float t) {
        // classic thermal gradient
        if (t < 0.25f) {
            float s = t / 0.25f;
            return new Color(0, (int)(s * 120), 200);
        } else if (t < 0.5f) {
            float s = (t - 0.25f) / 0.25f;
            return new Color((int)(s * 255), 120 + (int)(s * 80), (int)((1-s) * 200));
        } else if (t < 0.75f) {
            float s = (t - 0.5f) / 0.25f;
            return new Color(255, (int)((1-s) * 200), 0);
        } else {
            float s = (t - 0.75f) / 0.25f;
            return new Color(255, (int)(s * 255), (int)(s * 200));
        }
    }
}
