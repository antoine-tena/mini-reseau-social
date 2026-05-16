package socialnetwork.front.seirs;

import socialnetwork.back.seirs.ZoneManager;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Renders the static map background: zone fills, textures, roads, labels.
 * Renders once into an offscreen image and re-paints it every frame
 * (no need to re-generate unless the panel is resized).
 */
final class MapRenderer {

    private static final Color COL_VILLE_BASE   = new Color(42,  44,  52);
    private static final Color COL_VILLE_LIGHT  = new Color(56,  58,  70);
    private static final Color COL_PETITE_BASE  = new Color(62,  92,  50);
    private static final Color COL_PETITE_LIGHT = new Color(74, 108,  60);
    private static final Color COL_HAMEAU_BASE  = new Color(30,  58,  30);
    private static final Color COL_HAMEAU_LIGHT = new Color(38,  72,  36);
    private static final Color COL_MER_SAND     = new Color(200, 185, 130);
    private static final Color COL_MER_WATER    = new Color( 58, 118, 175);
    private static final Color COL_MER_SHALLOW  = new Color( 90, 155, 205);
    private static final Color COL_ROAD_MAJOR   = new Color(120, 118, 106);
    private static final Color COL_ROAD_MINOR   = new Color( 85,  82,  72);
    private static final Color COL_ZONE_BORDER  = new Color(180, 180, 160,  70);
    private static final Color COL_LABEL        = new Color(230, 230, 220, 140);

    private static final Font ZONE_LABEL_FONT   = new Font("Segoe UI", Font.BOLD, 11);

    private final ZoneManager zoneManager;
    private BufferedImage cachedMap;
    private int cachedW;
    private int cachedH;

    MapRenderer(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    void invalidate() {
        cachedMap = null;
    }

    /**
     * Returns (or regenerates) the map background image scaled to (w, h).
     */
    BufferedImage getMapImage(int w, int h) {
        if (cachedMap != null && cachedW == w && cachedH == h) {
            return cachedMap;
        }
        cachedW = w;
        cachedH = h;
        cachedMap = generateMapImage(w, h);
        return cachedMap;
    }

    private BufferedImage generateMapImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double ww = ZoneManager.WORLD_WIDTH;
        double wh = ZoneManager.WORLD_HEIGHT;

        g.setColor(new Color(20, 22, 28));
        g.fillRect(0, 0, w, h);

        drawZoneVillePrincipale(g, w, h, ww, wh);
        drawZonePetiteVille(g, w, h, ww, wh);
        drawZoneHameau(g, w, h, ww, wh);
        drawZoneMer(g, w, h, ww, wh);
        drawRoads(g, w, h, ww, wh);
        drawZoneBorders(g, w, h, ww, wh);
        drawZoneLabels(g, w, h, ww, wh);

        g.dispose();
        return img;
    }

    /* -------------------------  zones  ------------------------- */

    private void drawZoneVillePrincipale(Graphics2D g, int w, int h, double ww, double wh) {
        int[] xs = sx(w, ww, 340,1600,1730,1600,450,280);
        int[] ys = sy(h, wh, 410, 410, 860,1510,1540,1000);
        Polygon poly = new Polygon(xs, ys, xs.length);

        GradientPaint gp = new GradientPaint(
            sx1(w, ww, 280), sy1(h, wh, 970),  COL_VILLE_LIGHT,
            sx1(w, ww,1730), sy1(h, wh, 970),  COL_VILLE_BASE);
        g.setPaint(gp);
        g.fill(poly);

        drawCityGrid(g, w, h, ww, wh);

        g.setPaint(null);
        g.setColor(new Color(200, 185, 90, 22));
        g.fill(poly);
    }

    private void drawCityGrid(Graphics2D g, int w, int h, double ww, double wh) {
        g.setColor(new Color(65, 65, 80, 90));
        g.setStroke(new BasicStroke(0.8f));

        double blockW = 115;
        double blockH = 85;
        for (double wx = 320; wx < 1760; wx += blockW) {
            int x = sx1(w, ww, wx);
            g.drawLine(x, sy1(h, wh, 410), x, sy1(h, wh, 1540));
        }
        for (double wy = 420; wy < 1550; wy += blockH) {
            int y = sy1(h, wh, wy);
            g.drawLine(sx1(w, ww, 300), y, sx1(w, ww, 1740), y);
        }

        g.setColor(new Color(75, 80, 100, 120));
        g.setStroke(new BasicStroke(2.0f));
        int[] majX = { 620, 1060, 1430 };
        for (int wx : majX) {
            int x = sx1(w, ww, wx);
            g.drawLine(x, sy1(h, wh, 420), x, sy1(h, wh, 1535));
        }
        int[] majY = { 630, 970, 1240 };
        for (int wy : majY) {
            int y = sy1(h, wh, wy);
            g.drawLine(sx1(w, ww, 310), y, sx1(w, ww, 1740), y);
        }
    }

    private void drawZonePetiteVille(Graphics2D g, int w, int h, double ww, double wh) {
        int[] xs = sx(w, ww, 2330,3320,3630,3530,2740,2260);
        int[] ys = sy(h, wh,  560, 510, 780,1360,1480,1160);
        Polygon poly = new Polygon(xs, ys, xs.length);

        GradientPaint gp = new GradientPaint(
            sx1(w, ww,2260), sy1(h, wh,1480), COL_PETITE_BASE,
            sx1(w, ww,3630), sy1(h, wh, 510), COL_PETITE_LIGHT);
        g.setPaint(gp);
        g.fill(poly);

        g.setColor(new Color(55, 85, 40, 80));
        g.setStroke(new BasicStroke(0.7f));
        for (double wx = 2270; wx < 3640; wx += 70) {
            int x = sx1(w, ww, wx);
            g.drawLine(x, sy1(h, wh, 510), x, sy1(h, wh, 1480));
        }
        for (double wy = 515; wy < 1490; wy += 55) {
            int y = sy1(h, wh, wy);
            g.drawLine(sx1(w, ww,2260), y, sx1(w, ww,3640), y);
        }

        g.setColor(new Color(110, 160, 80, 50));
        g.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < 120; i++) {
            int bx = sx1(w, ww, 2310 + i * 22 % 1280);
            int by = sy1(h, wh,  530 + i * 17 % 900);
            g.fillOval(bx - 2, by - 2, 4, 4);
        }
    }

    private void drawZoneHameau(Graphics2D g, int w, int h, double ww, double wh) {
        int[] xs = sx(w, ww, 3360,3950,4060,3720,3300);
        int[] ys = sy(h, wh,  130, 150, 490, 710, 560);
        Polygon poly = new Polygon(xs, ys, xs.length);

        GradientPaint gp = new GradientPaint(
            sx1(w, ww,3300), sy1(h, wh, 710), COL_HAMEAU_BASE,
            sx1(w, ww,4060), sy1(h, wh, 130), COL_HAMEAU_LIGHT);
        g.setPaint(gp);
        g.fill(poly);

        g.setColor(new Color(35, 65, 28, 60));
        drawTrees(g, w, h, ww, wh, 3310, 140, 740, 560, 320);
    }

    private void drawTrees(Graphics2D g, int w, int h, double ww, double wh,
                           int areaX, int areaY, int areaW, int areaH, int count) {
        java.util.Random rng = new java.util.Random(42L);
        for (int i = 0; i < count; i++) {
            double wx = areaX + rng.nextDouble() * areaW;
            double wy = areaY + rng.nextDouble() * areaH;
            int px = sx1(w, ww, wx);
            int py = sy1(h, wh, wy);
            int r = 2 + rng.nextInt(3);
            g.setColor(new Color(30 + rng.nextInt(20), 65 + rng.nextInt(20), 25 + rng.nextInt(15), 130));
            g.fillOval(px - r, py - r, r * 2, r * 2);
        }
    }

    private void drawZoneMer(Graphics2D g, int w, int h, double ww, double wh) {
        int[] xSand = sx(w, ww, 2440,3900,4140,4090,2780,2320);
        int[] ySand = sy(h, wh, 1560,1500,1840,2360,2330,1960);
        Polygon sand = new Polygon(xSand, ySand, xSand.length);
        g.setColor(COL_MER_SAND);
        g.fill(sand);

        int[] xWater = sx(w, ww, 2640,3900,4140,4090,2780,2520);
        int[] yWater = sy(h, wh, 1720,1620,1840,2360,2330,2060);
        Polygon water = new Polygon(xWater, yWater, xWater.length);
        GradientPaint gpW = new GradientPaint(
            sx1(w, ww, 2640), sy1(h, wh, 1720), COL_MER_SHALLOW,
            sx1(w, ww, 4140), sy1(h, wh, 2360), COL_MER_WATER);
        g.setPaint(gpW);
        g.fill(water);

        g.setPaint(null);
        g.setColor(new Color(255,255,255,25));
        g.setStroke(new BasicStroke(1.8f));
        for (int i = 0; i < 12; i++) {
            int y = sy1(h, wh, 1650 + i * 58);
            g.drawLine(sx1(w, ww, 2550), y, sx1(w, ww, 4130), y);
        }

        drawBeachDots(g, w, h, ww, wh);
    }

    private void drawBeachDots(Graphics2D g, int w, int h, double ww, double wh) {
        java.util.Random rng = new java.util.Random(77L);
        g.setColor(new Color(180, 165, 110, 90));
        for (int i = 0; i < 200; i++) {
            double wx = 2440 + rng.nextDouble() * 1700;
            double wy = 1510 + rng.nextDouble() * 250;
            g.fillOval(sx1(w, ww, wx), sy1(h, wh, wy), 2, 2);
        }
    }

    /* -------------------------  roads  ------------------------- */

    private void drawRoads(Graphics2D g, int w, int h, double ww, double wh) {
        drawMajorRoad(g, w, h, ww, wh,
            1620, 860, 1880, 885, 2140, 920, 2320, 1030, 2920, 985);
        drawMajorRoad(g, w, h, ww, wh,
            3270, 610, 3410, 610, 3530, 635, 3650, 690, 3680, 405);
        drawMajorRoad(g, w, h, ww, wh,
            1580,1230, 1940,1370, 2320,1510, 2470,1730, 3230,1960);
        drawMinorRoad(g, w, h, ww, wh,
            1060, 970, 1260, 730, 890,1220, 640, 830);
        drawMinorRoad(g, w, h, ww, wh,
            2930,1010, 3150,1160, 2740, 830, 2540,1160);
    }

    private void drawMajorRoad(Graphics2D g, int w, int h, double ww, double wh,
                               double... coords) {
        g.setColor(new Color(COL_ROAD_MAJOR.getRed(), COL_ROAD_MAJOR.getGreen(),
                             COL_ROAD_MAJOR.getBlue(), 210));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawPolyline(g, w, h, ww, wh, coords);

        g.setColor(new Color(240, 235, 210, 40));
        g.setStroke(new BasicStroke(1.0f));
        drawPolyline(g, w, h, ww, wh, coords);
    }

    private void drawMinorRoad(Graphics2D g, int w, int h, double ww, double wh,
                               double... coords) {
        g.setColor(new Color(COL_ROAD_MINOR.getRed(), COL_ROAD_MINOR.getGreen(),
                             COL_ROAD_MINOR.getBlue(), 160));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawPolyline(g, w, h, ww, wh, coords);
    }

    private void drawPolyline(Graphics2D g, int w, int h, double ww, double wh, double[] coords) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(sx1(w, ww, coords[0]), sy1(h, wh, coords[1]));
        for (int i = 2; i < coords.length; i += 2) {
            path.lineTo(sx1(w, ww, coords[i]), sy1(h, wh, coords[i + 1]));
        }
        g.draw(path);
    }

    /* -------------------------  borders & labels  ------------------------- */

    private void drawZoneBorders(Graphics2D g, int w, int h, double ww, double wh) {
        g.setColor(COL_ZONE_BORDER);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                    0, new float[]{6, 5}, 0));
        drawPolygonBorder(g, w, h, ww, wh, 340,410, 1600,410, 1730,860, 1600,1510, 450,1540, 280,1000);
        drawPolygonBorder(g, w, h, ww, wh, 2330,560, 3320,510, 3630,780, 3530,1360, 2740,1480, 2260,1160);
        drawPolygonBorder(g, w, h, ww, wh, 3360,130, 3950,150, 4060,490, 3720,710, 3300,560);
        drawPolygonBorder(g, w, h, ww, wh, 2440,1560, 3900,1500, 4140,1840, 4090,2360, 2780,2330, 2320,1960);
    }

    private void drawPolygonBorder(Graphics2D g, int w, int h, double ww, double wh, double... coords) {
        Polygon poly = new Polygon(sx(w, ww, coords), sy(h, wh, coords), coords.length / 2);
        g.draw(poly);
    }

    private void drawZoneLabels(Graphics2D g, int w, int h, double ww, double wh) {
        g.setFont(ZONE_LABEL_FONT);
        g.setColor(COL_LABEL);
        drawLabel(g, w, h, ww, wh, "VILLE PRINCIPALE",    1060, 975);
        drawLabel(g, w, h, ww, wh, "PETITE VILLE",        2920, 985);
        drawLabel(g, w, h, ww, wh, "HAMEAU",              3680, 405);
        drawLabel(g, w, h, ww, wh, "ZONE MER / VACANCES", 3230,1960);
    }

    private void drawLabel(Graphics2D g, int w, int h, double ww, double wh,
                           String text, double wx, double wy) {
        FontMetrics fm = g.getFontMetrics();
        int px = sx1(w, ww, wx) - fm.stringWidth(text) / 2;
        int py = sy1(h, wh, wy) + fm.getAscent() / 2;
        g.setColor(new Color(0, 0, 0, 90));
        g.drawString(text, px + 1, py + 1);
        g.setColor(COL_LABEL);
        g.drawString(text, px, py);
    }

    /* -------------------------  coord helpers  ------------------------- */

    private static int sx1(int w, double ww, double wx) {
        return (int) Math.round(wx / ww * w);
    }

    private static int sy1(int h, double wh, double wy) {
        return (int) Math.round(wy / wh * h);
    }

    private static int[] sx(int w, double ww, double... coords) {
        int n = coords.length / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = sx1(w, ww, coords[i * 2]);
        return out;
    }

    private static int[] sy(int h, double wh, double... coords) {
        int n = coords.length / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = sy1(h, wh, coords[i * 2 + 1]);
        return out;
    }

    private static int[] sx(int w, double ww, int... coords) {
        int n = coords.length / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = sx1(w, ww, coords[i * 2]);
        return out;
    }

    private static int[] sy(int h, double wh, int... coords) {
        int n = coords.length / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = sy1(h, wh, coords[i * 2 + 1]);
        return out;
    }
}
