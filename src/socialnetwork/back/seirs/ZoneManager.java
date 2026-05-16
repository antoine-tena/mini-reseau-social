package socialnetwork.back.seirs;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ZoneManager {

    public static final double WORLD_WIDTH = 4200.0;
    public static final double WORLD_HEIGHT = 2400.0;

    public enum ZoneId {
        VILLE_PRINCIPALE,
        PETITE_VILLE,
        HAMEAU,
        ZONE_VACANCES_MER
    }

    public static final class ZoneDefinition {
        private final ZoneId id;
        private final String nom;
        private final Path2D.Double forme;
        private final Rectangle2D bounds;
        private final Vector2D centre;
        private final double poidsPopulation;
        private final double densiteLocale;
        private final boolean dense;
        private final boolean isolee;
        private final boolean attractiveEnEte;
        private final CopyOnWriteArrayList<Vector2D> pointsTransit;
        private final CopyOnWriteArrayList<Vector2D> pointsInteret;

        private ZoneDefinition(
            ZoneId id,
            String nom,
            Path2D.Double forme,
            Rectangle2D bounds,
            Vector2D centre,
            double poidsPopulation,
            double densiteLocale,
            boolean dense,
            boolean isolee,
            boolean attractiveEnEte,
            List<Vector2D> pointsTransit,
            List<Vector2D> pointsInteret
        ) {
            this.id = id;
            this.nom = nom;
            this.forme = forme;
            this.bounds = bounds;
            this.centre = centre;
            this.poidsPopulation = poidsPopulation;
            this.densiteLocale = densiteLocale;
            this.dense = dense;
            this.isolee = isolee;
            this.attractiveEnEte = attractiveEnEte;
            this.pointsTransit = new CopyOnWriteArrayList<>(pointsTransit);
            this.pointsInteret = new CopyOnWriteArrayList<>(pointsInteret);
        }

        public ZoneId getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public Rectangle2D getBounds() {
            return (Rectangle2D) bounds.clone();
        }

        public Vector2D getCentre() {
            return centre.copy();
        }

        public double getPoidsPopulation() {
            return poidsPopulation;
        }

        public double getDensiteLocale() {
            return densiteLocale;
        }

        public boolean isDense() {
            return dense;
        }

        public boolean isIsolee() {
            return isolee;
        }

        public boolean isAttractiveEnEte() {
            return attractiveEnEte;
        }

        public List<Vector2D> getPointsTransit() {
            List<Vector2D> copy = new ArrayList<>(pointsTransit.size());
            for (Vector2D point : pointsTransit) {
                copy.add(point.copy());
            }
            return Collections.unmodifiableList(copy);
        }

        public List<Vector2D> getPointsInteret() {
            List<Vector2D> copy = new ArrayList<>(pointsInteret.size());
            for (Vector2D point : pointsInteret) {
                copy.add(point.copy());
            }
            return Collections.unmodifiableList(copy);
        }

        public boolean contains(Vector2D position) {
            return forme.contains(position.getX(), position.getY());
        }

        public Vector2D randomTransitPoint(Random random) {
            if (pointsTransit.isEmpty()) {
                return centre.copy();
            }
            return pointsTransit.get(random.nextInt(pointsTransit.size())).copy();
        }

        public Vector2D randomTransitPoint(SplittableRandom random) {
            if (pointsTransit.isEmpty()) {
                return centre.copy();
            }
            return pointsTransit.get(random.nextInt(pointsTransit.size())).copy();
        }

        public Vector2D randomInterestPoint(Random random) {
            if (pointsInteret.isEmpty()) {
                return centre.copy();
            }
            return pointsInteret.get(random.nextInt(pointsInteret.size())).copy();
        }

        public Vector2D randomInterestPoint(SplittableRandom random) {
            if (pointsInteret.isEmpty()) {
                return centre.copy();
            }
            return pointsInteret.get(random.nextInt(pointsInteret.size())).copy();
        }

        public Vector2D randomPoint(Random random) {
            return randomPointInternal(random, null);
        }

        public Vector2D randomPoint(SplittableRandom random) {
            return randomPointInternal(null, random);
        }

        private Vector2D randomPointInternal(Random random, SplittableRandom splitRandom) {
            for (int attempt = 0; attempt < 64; attempt++) {
                double x = nextDouble(random, splitRandom, bounds.getMinX(), bounds.getMaxX());
                double y = nextDouble(random, splitRandom, bounds.getMinY(), bounds.getMaxY());
                if (forme.contains(x, y)) {
                    return new Vector2D(x, y);
                }
            }
            return centre.copy();
        }

        private static double nextDouble(Random random, SplittableRandom splitRandom, double min, double max) {
            if (splitRandom != null) {
                return splitRandom.nextDouble(min, max);
            }
            return min + random.nextDouble() * (max - min);
        }
    }

    private final EnumMap<ZoneId, ZoneDefinition> zones = new EnumMap<>(ZoneId.class);
    /** Multiple alternative transit paths per zone pair: from → to → list-of-alternative-paths. */
    private final EnumMap<ZoneId, EnumMap<ZoneId, List<List<Vector2D>>>> transitPaths = new EnumMap<>(ZoneId.class);
    private static final Random TRANSIT_RNG = new Random();

    public ZoneManager() {
        definirZones();
        definirAxesTransit();
    }

    public Collection<ZoneDefinition> getZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public ZoneDefinition getZone(ZoneId id) {
        ZoneDefinition zone = zones.get(id);
        if (zone == null) {
            throw new IllegalArgumentException("Zone inconnue: " + id);
        }
        return zone;
    }

    public ZoneId findZoneId(Vector2D position) {
        for (ZoneDefinition zone : zones.values()) {
            if (zone.contains(position)) {
                return zone.getId();
            }
        }
        return nearestZone(position).getId();
    }

    public ZoneDefinition nearestZone(Vector2D position) {
        ZoneDefinition nearest = null;
        double best = Double.MAX_VALUE;
        for (ZoneDefinition zone : zones.values()) {
            double dist = zone.getCentre().distanceSquared(position);
            if (dist < best) {
                best = dist;
                nearest = zone;
            }
        }
        return Objects.requireNonNull(nearest);
    }

    public Vector2D randomPointInZone(ZoneId zoneId, Random random) {
        return getZone(zoneId).randomPoint(random);
    }

    public Vector2D randomPointInZone(ZoneId zoneId, SplittableRandom random) {
        return getZone(zoneId).randomPoint(random);
    }

    public Vector2D randomInterestPoint(ZoneId zoneId, SplittableRandom random) {
        return getZone(zoneId).randomInterestPoint(random);
    }

    public ZoneId sampleHomeZone(Random random) {
        double draw = random.nextDouble();
        if (draw < 0.56) {
            return ZoneId.VILLE_PRINCIPALE;
        }
        if (draw < 0.81) {
            return ZoneId.PETITE_VILLE;
        }
        if (draw < 0.91) {
            return ZoneId.HAMEAU;
        }
        return ZoneId.ZONE_VACANCES_MER;
    }

    public ZoneId pickWorkZone(ZoneId zoneDomicile, Random random) {
        return switch (zoneDomicile) {
            case HAMEAU, PETITE_VILLE -> ZoneId.VILLE_PRINCIPALE;
            case ZONE_VACANCES_MER -> random.nextDouble() < 0.35
                ? ZoneId.VILLE_PRINCIPALE
                : ZoneId.ZONE_VACANCES_MER;
            case VILLE_PRINCIPALE -> ZoneId.VILLE_PRINCIPALE;
        };
    }

    public Vector2D worldClamp(Vector2D position) {
        return position.clampToBounds(0.0, 0.0, WORLD_WIDTH, WORLD_HEIGHT);
    }

    public Vector2D containmentForce(ZoneId zoneId, Vector2D position, double margin, double strength) {
        ZoneDefinition zone = getZone(zoneId);
        Rectangle2D bounds = zone.bounds;
        Vector2D center = zone.centre;
        if (!zone.contains(position)) {
            return center.subtract(position).normalized().multiply(strength * 2.8);
        }

        double left = position.getX() - bounds.getMinX();
        double right = bounds.getMaxX() - position.getX();
        double top = position.getY() - bounds.getMinY();
        double bottom = bounds.getMaxY() - position.getY();
        double minEdge = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (minEdge >= margin) {
            return new Vector2D();
        }

        double factor = (margin - minEdge) / Math.max(1.0, margin);
        return center.subtract(position).normalized().multiply(strength * factor);
    }

    public List<Vector2D> buildTransitPath(ZoneId from, ZoneId to) {
        return buildTransitPathInternal(from, to, true);
    }

    /** Always picks the first registered path (used when building composite routes). */
    private List<Vector2D> buildTransitPathFirst(ZoneId from, ZoneId to) {
        return buildTransitPathInternal(from, to, false);
    }

    private List<Vector2D> buildTransitPathInternal(ZoneId from, ZoneId to, boolean random) {
        if (from == to) {
            List<Vector2D> direct = new ArrayList<>(1);
            direct.add(getZone(to).getCentre());
            return direct;
        }
        EnumMap<ZoneId, List<List<Vector2D>>> pathsFrom = transitPaths.get(from);
        if (pathsFrom != null) {
            List<List<Vector2D>> alternatives = pathsFrom.get(to);
            if (alternatives != null && !alternatives.isEmpty()) {
                List<Vector2D> chosen = random && alternatives.size() > 1
                        ? alternatives.get(TRANSIT_RNG.nextInt(alternatives.size()))
                        : alternatives.get(0);
                List<Vector2D> copy = new ArrayList<>(chosen.size());
                for (Vector2D waypoint : chosen) copy.add(waypoint.copy());
                return copy;
            }
        }
        List<Vector2D> fallback = new ArrayList<>(2);
        fallback.add(getZone(from).getCentre());
        fallback.add(getZone(to).getCentre());
        return fallback;
    }

    public double distanceBetweenZones(ZoneId a, ZoneId b) {
        return getZone(a).centre.distance(getZone(b).centre);
    }

    public Vector2D hotspotForFestivalMer() {
        return new Vector2D(3320.0, 2010.0);
    }

    public Vector2D hotspotForCityCenter() {
        return new Vector2D(1100.0, 970.0);
    }

    private void definirZones() {
        ZoneDefinition villePrincipale = new ZoneDefinition(
            ZoneId.VILLE_PRINCIPALE,
            "Ville Principale",
            polygon(
                340.0, 410.0,
                1600.0, 410.0,
                1730.0, 860.0,
                1600.0, 1510.0,
                450.0, 1540.0,
                280.0, 1000.0
            ),
            new Rectangle2D.Double(280.0, 410.0, 1450.0, 1130.0),
            new Vector2D(1060.0, 975.0),
            0.56, 1.0, true, false, false,
            List.of(
                new Vector2D(1620.0, 860.0),
                new Vector2D(1580.0, 1230.0),
                new Vector2D(430.0, 980.0),
                new Vector2D(870.0,  420.0),
                new Vector2D(1500.0, 440.0),
                new Vector2D(340.0,  700.0)
            ),
            // 30 named interest points spread across the whole VP grid
            List.of(
                new Vector2D( 500.0,  520.0), new Vector2D( 680.0,  490.0), new Vector2D( 900.0,  480.0),
                new Vector2D(1150.0,  470.0), new Vector2D(1380.0,  500.0), new Vector2D(1520.0,  640.0),
                new Vector2D(1560.0,  860.0), new Vector2D(1500.0, 1050.0), new Vector2D(1440.0, 1250.0),
                new Vector2D(1300.0, 1420.0), new Vector2D(1080.0, 1490.0), new Vector2D( 820.0, 1520.0),
                new Vector2D( 580.0, 1490.0), new Vector2D( 380.0, 1330.0), new Vector2D( 310.0, 1100.0),
                new Vector2D( 330.0,  850.0), new Vector2D( 400.0,  620.0), new Vector2D( 600.0,  700.0),
                new Vector2D( 800.0,  680.0), new Vector2D(1000.0,  700.0), new Vector2D(1200.0,  680.0),
                new Vector2D(1350.0,  760.0), new Vector2D(1420.0,  960.0), new Vector2D(1350.0, 1150.0),
                new Vector2D(1200.0, 1260.0), new Vector2D(1050.0, 1310.0), new Vector2D( 870.0, 1290.0),
                new Vector2D( 700.0, 1200.0), new Vector2D( 560.0, 1050.0), new Vector2D( 720.0,  920.0),
                // city centre cluster — highly attractive
                new Vector2D(1040.0,  950.0), new Vector2D(1080.0,  990.0), new Vector2D(1020.0, 1020.0),
                new Vector2D( 980.0,  960.0), new Vector2D(1060.0, 1060.0)
            )
        );

        ZoneDefinition petiteVille = new ZoneDefinition(
            ZoneId.PETITE_VILLE,
            "Petite Ville",
            polygon(
                2330.0,  560.0,
                3320.0,  510.0,
                3630.0,  780.0,
                3530.0, 1360.0,
                2740.0, 1480.0,
                2260.0, 1160.0
            ),
            new Rectangle2D.Double(2260.0, 510.0, 1370.0, 970.0),
            new Vector2D(2920.0, 985.0),
            0.25, 0.72, false, false, false,
            List.of(
                new Vector2D(2320.0, 1030.0), new Vector2D(3270.0,  610.0),
                new Vector2D(3360.0, 1340.0), new Vector2D(2600.0,  580.0),
                new Vector2D(3100.0, 1400.0)
            ),
            List.of(
                new Vector2D(2400.0,  620.0), new Vector2D(2600.0,  590.0), new Vector2D(2820.0,  570.0),
                new Vector2D(3050.0,  570.0), new Vector2D(3260.0,  580.0), new Vector2D(3480.0,  720.0),
                new Vector2D(3560.0,  940.0), new Vector2D(3530.0, 1160.0), new Vector2D(3380.0, 1330.0),
                new Vector2D(3100.0, 1420.0), new Vector2D(2840.0, 1450.0), new Vector2D(2580.0, 1350.0),
                new Vector2D(2340.0, 1140.0), new Vector2D(2420.0,  900.0), new Vector2D(2560.0,  780.0),
                new Vector2D(2740.0,  700.0), new Vector2D(2920.0,  700.0), new Vector2D(3100.0,  750.0),
                new Vector2D(3250.0,  900.0), new Vector2D(3300.0, 1080.0), new Vector2D(3180.0, 1220.0),
                new Vector2D(3000.0, 1320.0), new Vector2D(2800.0, 1290.0), new Vector2D(2640.0, 1140.0),
                new Vector2D(2780.0, 1000.0), new Vector2D(2920.0, 1010.0), new Vector2D(3060.0, 1000.0)
            )
        );

        ZoneDefinition hameau = new ZoneDefinition(
            ZoneId.HAMEAU,
            "Hameau",
            polygon(
                3360.0, 130.0,
                3950.0, 150.0,
                4060.0, 490.0,
                3720.0, 710.0,
                3300.0, 560.0
            ),
            new Rectangle2D.Double(3300.0, 130.0, 760.0, 580.0),
            new Vector2D(3680.0, 405.0),
            0.10, 0.36, false, true, false,
            List.of(
                new Vector2D(3360.0, 530.0), new Vector2D(3650.0, 690.0),
                new Vector2D(3900.0, 200.0), new Vector2D(3480.0, 200.0)
            ),
            List.of(
                new Vector2D(3420.0, 200.0), new Vector2D(3600.0, 180.0), new Vector2D(3800.0, 200.0),
                new Vector2D(3950.0, 300.0), new Vector2D(3980.0, 450.0), new Vector2D(3850.0, 580.0),
                new Vector2D(3700.0, 640.0), new Vector2D(3520.0, 560.0), new Vector2D(3360.0, 440.0),
                new Vector2D(3380.0, 290.0), new Vector2D(3540.0, 350.0), new Vector2D(3720.0, 400.0),
                new Vector2D(3880.0, 380.0), new Vector2D(3820.0, 470.0), new Vector2D(3660.0, 500.0),
                new Vector2D(3500.0, 450.0)
            )
        );

        ZoneDefinition mer = new ZoneDefinition(
            ZoneId.ZONE_VACANCES_MER,
            "Zone Vacances / Mer",
            polygon(
                2440.0, 1560.0,
                3900.0, 1500.0,
                4140.0, 1840.0,
                4090.0, 2360.0,
                2780.0, 2330.0,
                2320.0, 1960.0
            ),
            new Rectangle2D.Double(2320.0, 1500.0, 1820.0, 860.0),
            new Vector2D(3230.0, 1960.0),
            0.09, 0.54, false, true, true,
            List.of(
                new Vector2D(2470.0, 1730.0), new Vector2D(2700.0, 1545.0),
                new Vector2D(3940.0, 1820.0), new Vector2D(3600.0, 2300.0),
                new Vector2D(2900.0, 2280.0)
            ),
            List.of(
                new Vector2D(2450.0, 1700.0), new Vector2D(2620.0, 1620.0), new Vector2D(2800.0, 1580.0),
                new Vector2D(3000.0, 1560.0), new Vector2D(3200.0, 1540.0), new Vector2D(3400.0, 1540.0),
                new Vector2D(3600.0, 1560.0), new Vector2D(3800.0, 1560.0), new Vector2D(3960.0, 1640.0),
                new Vector2D(4050.0, 1800.0), new Vector2D(4060.0, 2000.0), new Vector2D(4030.0, 2200.0),
                new Vector2D(3900.0, 2310.0), new Vector2D(3700.0, 2330.0), new Vector2D(3500.0, 2320.0),
                new Vector2D(3300.0, 2300.0), new Vector2D(3100.0, 2280.0), new Vector2D(2900.0, 2260.0),
                new Vector2D(2750.0, 2200.0), new Vector2D(2600.0, 2060.0), new Vector2D(2450.0, 1920.0),
                // beach / festival hotspot cluster
                new Vector2D(3200.0, 2000.0), new Vector2D(3320.0, 2020.0), new Vector2D(3440.0, 1980.0),
                new Vector2D(3560.0, 1900.0), new Vector2D(3100.0, 1900.0)
            )
        );

        zones.put(villePrincipale.getId(), villePrincipale);
        zones.put(petiteVille.getId(), petiteVille);
        zones.put(hameau.getId(), hameau);
        zones.put(mer.getId(), mer);
    }

    private void definirAxesTransit() {

        // =================================================================
        // VILLE PRINCIPALE — 30 internal roads (artériales + quartiers)
        // =================================================================
        // East-west artériales
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(340,700), new Vector2D(560,700), new Vector2D(780,700), new Vector2D(1000,700), new Vector2D(1220,700), new Vector2D(1440,700), new Vector2D(1560,750));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(340,950), new Vector2D(560,950), new Vector2D(780,950), new Vector2D(1000,950), new Vector2D(1200,950), new Vector2D(1420,970), new Vector2D(1560,920));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(360,1200), new Vector2D(600,1200), new Vector2D(840,1200), new Vector2D(1060,1200), new Vector2D(1280,1200), new Vector2D(1460,1150));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(380,1400), new Vector2D(620,1420), new Vector2D(860,1450), new Vector2D(1100,1460), new Vector2D(1320,1420), new Vector2D(1490,1350));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(350,500), new Vector2D(580,470), new Vector2D(820,460), new Vector2D(1060,460), new Vector2D(1300,470), new Vector2D(1500,500));
        // North-south artériales
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(420,430), new Vector2D(420,640), new Vector2D(400,860), new Vector2D(390,1100), new Vector2D(370,1310), new Vector2D(360,1500));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(660,460), new Vector2D(660,680), new Vector2D(660,900), new Vector2D(660,1130), new Vector2D(660,1360), new Vector2D(650,1510));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(900,460), new Vector2D(900,680), new Vector2D(900,910), new Vector2D(900,1140), new Vector2D(890,1380), new Vector2D(870,1510));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(1140,460), new Vector2D(1140,700), new Vector2D(1140,950), new Vector2D(1130,1200), new Vector2D(1100,1440));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(1360,480), new Vector2D(1380,720), new Vector2D(1400,980), new Vector2D(1380,1250), new Vector2D(1320,1470));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(1540,600), new Vector2D(1560,840), new Vector2D(1560,1100), new Vector2D(1520,1360));
        // Diagonal & ring roads
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(450,520), new Vector2D(680,680), new Vector2D(920,820), new Vector2D(1060,975), new Vector2D(1240,860), new Vector2D(1440,700));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(400,1450), new Vector2D(640,1300), new Vector2D(880,1150), new Vector2D(1060,975), new Vector2D(1280,1140), new Vector2D(1500,1280));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(340,800), new Vector2D(580,870), new Vector2D(800,940), new Vector2D(1060,975), new Vector2D(1260,1060), new Vector2D(1480,1060));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(600,600), new Vector2D(830,580), new Vector2D(1060,570), new Vector2D(1300,600), new Vector2D(1500,680));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(500,1150), new Vector2D(720,1100), new Vector2D(940,1080), new Vector2D(1160,1090), new Vector2D(1380,1100));
        // Inner ring around city centre
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE,
            new Vector2D(900,780), new Vector2D(1060,750), new Vector2D(1220,790), new Vector2D(1300,960), new Vector2D(1240,1140), new Vector2D(1060,1220), new Vector2D(880,1160), new Vector2D(800,980));
        // Neighbourhood connectors
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(480,650), new Vector2D(660,720), new Vector2D(820,800));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(820,800), new Vector2D(980,870), new Vector2D(1060,975));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(1060,975), new Vector2D(1180,1080), new Vector2D(1240,1200));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(560,1060), new Vector2D(720,1010), new Vector2D(900,980));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(900,980), new Vector2D(1000,1100), new Vector2D(1060,1220));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(1060,975), new Vector2D(1100,840), new Vector2D(1200,750));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(460,890), new Vector2D(620,900), new Vector2D(780,920), new Vector2D(900,980));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(1320,1350), new Vector2D(1200,1290), new Vector2D(1060,1220), new Vector2D(900,1200));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(700,1340), new Vector2D(840,1280), new Vector2D(980,1230));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(500,750), new Vector2D(680,800), new Vector2D(860,850), new Vector2D(1000,920));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(1200,1350), new Vector2D(1100,1430), new Vector2D(960,1490));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(380,1100), new Vector2D(480,1050), new Vector2D(600,1020), new Vector2D(720,1000));
        registerInternalRoute(ZoneId.VILLE_PRINCIPALE, new Vector2D(740,530), new Vector2D(780,700), new Vector2D(800,900), new Vector2D(820,1100), new Vector2D(840,1300));

        // =================================================================
        // PETITE VILLE — 10 internal roads
        // =================================================================
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2340,820), new Vector2D(2580,760), new Vector2D(2820,720), new Vector2D(3060,720), new Vector2D(3280,740), new Vector2D(3480,800));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2340,1040), new Vector2D(2580,1020), new Vector2D(2820,1000), new Vector2D(3060,1000), new Vector2D(3280,1020), new Vector2D(3500,1060));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2380,1280), new Vector2D(2620,1320), new Vector2D(2900,1380), new Vector2D(3160,1380), new Vector2D(3360,1330));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2440,640), new Vector2D(2440,880), new Vector2D(2440,1100), new Vector2D(2440,1320));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2920,600), new Vector2D(2920,800), new Vector2D(2920,1000), new Vector2D(2920,1220), new Vector2D(2920,1430));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(3360,640), new Vector2D(3380,840), new Vector2D(3420,1060), new Vector2D(3440,1300));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2500,700), new Vector2D(2700,800), new Vector2D(2920,1000), new Vector2D(3120,1180), new Vector2D(3300,1300));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2700,600), new Vector2D(2920,700), new Vector2D(3160,760), new Vector2D(3360,840));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2400,1200), new Vector2D(2640,1100), new Vector2D(2880,1060), new Vector2D(3120,1100), new Vector2D(3360,1180));
        registerInternalRoute(ZoneId.PETITE_VILLE,
            new Vector2D(2920,1000), new Vector2D(3000,1100), new Vector2D(3100,1200), new Vector2D(3200,1300));

        // =================================================================
        // HAMEAU — 10 internal tracks
        // =================================================================
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3400,200), new Vector2D(3600,200), new Vector2D(3800,220), new Vector2D(3940,300));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3400,350), new Vector2D(3600,380), new Vector2D(3820,380), new Vector2D(3960,400), new Vector2D(3980,500));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3360,450), new Vector2D(3500,500), new Vector2D(3680,540), new Vector2D(3820,560), new Vector2D(3900,580));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3380,200), new Vector2D(3380,350), new Vector2D(3380,480), new Vector2D(3360,560));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3680,200), new Vector2D(3680,350), new Vector2D(3680,500), new Vector2D(3680,640));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3900,200), new Vector2D(3900,340), new Vector2D(3880,470), new Vector2D(3820,570));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3400,280), new Vector2D(3540,280), new Vector2D(3680,290), new Vector2D(3820,300));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3440,440), new Vector2D(3560,420), new Vector2D(3680,420), new Vector2D(3800,440), new Vector2D(3920,460));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3500,280), new Vector2D(3580,380), new Vector2D(3680,420), new Vector2D(3780,380), new Vector2D(3880,320));
        registerInternalRoute(ZoneId.HAMEAU,
            new Vector2D(3640,300), new Vector2D(3680,420), new Vector2D(3720,560));

        // =================================================================
        // ZONE MER — 10 beachfront / resort roads
        // =================================================================
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2480,1680), new Vector2D(2680,1640), new Vector2D(2880,1610), new Vector2D(3080,1590), new Vector2D(3280,1580), new Vector2D(3480,1580), new Vector2D(3680,1590), new Vector2D(3880,1620));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2480,1880), new Vector2D(2680,1900), new Vector2D(2880,1920), new Vector2D(3080,1940), new Vector2D(3280,1960), new Vector2D(3480,1980), new Vector2D(3680,1960), new Vector2D(3900,1920));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2700,2100), new Vector2D(2900,2120), new Vector2D(3100,2140), new Vector2D(3300,2140), new Vector2D(3500,2120), new Vector2D(3700,2080), new Vector2D(3900,2020));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2900,2260), new Vector2D(3100,2280), new Vector2D(3300,2280), new Vector2D(3500,2260), new Vector2D(3700,2240), new Vector2D(3900,2200));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2500,1720), new Vector2D(2500,1900), new Vector2D(2500,2080), new Vector2D(2520,2260));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(3230,1580), new Vector2D(3230,1760), new Vector2D(3230,1960), new Vector2D(3230,2160), new Vector2D(3230,2300));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(3900,1560), new Vector2D(3960,1700), new Vector2D(4000,1900), new Vector2D(4020,2100), new Vector2D(4000,2300));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2700,1680), new Vector2D(2900,1780), new Vector2D(3100,1860), new Vector2D(3300,1940), new Vector2D(3500,1980));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(3500,1980), new Vector2D(3640,2060), new Vector2D(3780,2120), new Vector2D(3920,2140));
        registerInternalRoute(ZoneId.ZONE_VACANCES_MER,
            new Vector2D(2600,2000), new Vector2D(2800,2050), new Vector2D(3000,2080), new Vector2D(3200,2100));

        // =================================================================
        // INTER-ZONE — VP ↔ PV (3 routes)
        // =================================================================
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.PETITE_VILLE, List.of(
            new Vector2D(1620,860), new Vector2D(1760,875), new Vector2D(1920,890),
            new Vector2D(2100,910), new Vector2D(2300,960), new Vector2D(2920,985)
        ));
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.PETITE_VILLE, List.of(
            new Vector2D(1500,500), new Vector2D(1700,480), new Vector2D(1960,490),
            new Vector2D(2220,520), new Vector2D(2500,570), new Vector2D(2760,650), new Vector2D(2920,985)
        ));
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.PETITE_VILLE, List.of(
            new Vector2D(1580,1100), new Vector2D(1780,1050), new Vector2D(1980,1020),
            new Vector2D(2200,1000), new Vector2D(2500,990), new Vector2D(2920,985)
        ));

        // =================================================================
        // INTER-ZONE — PV ↔ Hameau (2 routes)
        // =================================================================
        registerBidirectionalPath(ZoneId.PETITE_VILLE, ZoneId.HAMEAU, List.of(
            new Vector2D(3270,610), new Vector2D(3400,620), new Vector2D(3530,640),
            new Vector2D(3640,690), new Vector2D(3680,405)
        ));
        registerBidirectionalPath(ZoneId.PETITE_VILLE, ZoneId.HAMEAU, List.of(
            new Vector2D(3360,900), new Vector2D(3480,780), new Vector2D(3580,680),
            new Vector2D(3660,580), new Vector2D(3680,405)
        ));

        // =================================================================
        // INTER-ZONE — VP ↔ Mer (3 routes)
        // =================================================================
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.ZONE_VACANCES_MER, List.of(
            new Vector2D(1580,1250), new Vector2D(1760,1320), new Vector2D(1960,1390),
            new Vector2D(2200,1460), new Vector2D(2440,1580), new Vector2D(2700,1700),
            new Vector2D(3230,1960)
        ));
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.ZONE_VACANCES_MER, List.of(
            new Vector2D(640,1520), new Vector2D(800,1640), new Vector2D(1060,1720),
            new Vector2D(1380,1790), new Vector2D(1720,1840), new Vector2D(2060,1880),
            new Vector2D(2400,1910), new Vector2D(2820,1940), new Vector2D(3230,1960)
        ));
        registerBidirectionalPath(ZoneId.VILLE_PRINCIPALE, ZoneId.ZONE_VACANCES_MER, List.of(
            new Vector2D(1400,1400), new Vector2D(1800,1450), new Vector2D(2200,1500),
            new Vector2D(2600,1600), new Vector2D(3000,1740), new Vector2D(3230,1960)
        ));

        // =================================================================
        // INTER-ZONE — PV ↔ Mer (2 routes)
        // =================================================================
        registerBidirectionalPath(ZoneId.PETITE_VILLE, ZoneId.ZONE_VACANCES_MER, List.of(
            new Vector2D(2440,1560), new Vector2D(2700,1620),
            new Vector2D(2900,1700), new Vector2D(3230,1960)
        ));
        registerBidirectionalPath(ZoneId.PETITE_VILLE, ZoneId.ZONE_VACANCES_MER, List.of(
            new Vector2D(3360,1340), new Vector2D(3440,1460),
            new Vector2D(3500,1600), new Vector2D(3540,1800), new Vector2D(3230,1960)
        ));

        // =================================================================
        // COMPOSITES — Hameau ↔ VP, Hameau ↔ Mer
        // =================================================================
        registerBidirectionalPath(ZoneId.HAMEAU, ZoneId.VILLE_PRINCIPALE, concat(
            buildTransitPathFirst(ZoneId.HAMEAU, ZoneId.PETITE_VILLE),
            buildTransitPathFirst(ZoneId.PETITE_VILLE, ZoneId.VILLE_PRINCIPALE)
        ));
        registerBidirectionalPath(ZoneId.HAMEAU, ZoneId.ZONE_VACANCES_MER, concat(
            buildTransitPathFirst(ZoneId.HAMEAU, ZoneId.PETITE_VILLE),
            buildTransitPathFirst(ZoneId.PETITE_VILLE, ZoneId.ZONE_VACANCES_MER)
        ));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /**
     * Registers a bidirectional internal route within a single zone.
     * Both forward and reversed paths are stored so the scheduler can
     * pick them for intra-zone navigation.
     */
    private void registerInternalRoute(ZoneId zone, Vector2D... waypoints) {
        List<Vector2D> fwd = new ArrayList<>(waypoints.length);
        for (Vector2D w : waypoints) fwd.add(w.copy());
        appendPath(zone, zone, fwd);
        List<Vector2D> rev = new ArrayList<>(waypoints.length);
        for (int i = waypoints.length - 1; i >= 0; i--) rev.add(waypoints[i].copy());
        appendPath(zone, zone, rev);
    }

    private void registerBidirectionalPath(ZoneId from, ZoneId to, List<Vector2D> waypoints) {
        appendPath(from, to, waypoints);
        List<Vector2D> reversed = new ArrayList<>(waypoints.size());
        for (int i = waypoints.size() - 1; i >= 0; i--) {
            reversed.add(waypoints.get(i).copy());
        }
        appendPath(to, from, reversed);
    }

    private void appendPath(ZoneId from, ZoneId to, List<Vector2D> path) {
        transitPaths.computeIfAbsent(from, k -> new EnumMap<>(ZoneId.class))
                    .computeIfAbsent(to, k -> new ArrayList<>())
                    .add(immutableCopy(path));
    }

    private static List<Vector2D> immutableCopy(List<Vector2D> points) {
        List<Vector2D> copy = new ArrayList<>(points.size());
        for (Vector2D point : points) {
            copy.add(point.copy());
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<Vector2D> concat(List<Vector2D> first, List<Vector2D> second) {
        List<Vector2D> out = new ArrayList<>(first.size() + second.size());
        for (Vector2D point : first) {
            out.add(point.copy());
        }
        for (int i = 0; i < second.size(); i++) {
            Vector2D point = second.get(i);
            if (!out.isEmpty() && out.get(out.size() - 1).epsilonEquals(point, 0.001)) {
                continue;
            }
            out.add(point.copy());
        }
        return out;
    }

    private static Path2D.Double polygon(double... coords) {
        if (coords.length < 6 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Un polygone requiert au moins 3 points");
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(coords[0], coords[1]);
        for (int i = 2; i < coords.length; i += 2) {
            path.lineTo(coords[i], coords[i + 1]);
        }
        path.closePath();
        return path;
    }
}
