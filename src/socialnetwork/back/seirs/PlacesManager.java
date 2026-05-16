package socialnetwork.back.seirs;

import java.util.*;
import static socialnetwork.back.seirs.PlaceInteret.TypeLieu.*;
import static socialnetwork.back.seirs.ZoneManager.ZoneId.*;

/**
 * Manages all Points of Interest (POIs) across the simulation world.
 *
 * Contains 350+ POIs distributed across 4 zones with realistic density and
 * type balance. The daily schedule logic drives agent POI targets based on
 * hour, traits and personal favourites, producing genuine city-life rhythms:
 *   22:00–07:00  home / sleep
 *   07:00–08:30  morning (café, gym, parc)
 *   08:30–12:30  work / daytime activity
 *   12:30–13:30  lunch break
 *   13:30–17:00  work / afternoon activity
 *   17:00–18:30  errands (supermarché, pharmacie, marché)
 *   18:30–21:00  evening (restaurant, bar, cinéma, gym, concert)
 *   21:00–23:00  late night (bar, discothèque for sociable agents)
 *
 * ZONE GEOMETRY (world units 4200 × 2400):
 *   VILLE_PRINCIPALE  ~340–1730 × 410–1540,  centre (1060, 975)
 *   PETITE_VILLE      ~2260–3630 × 510–1480, centre (2920, 985)
 *   HAMEAU            ~3300–4060 × 130–710,  centre (3680, 405)
 *   ZONE_VACANCES_MER ~2320–4140 × 1500–2360, centre (3230, 1960)
 */
public final class PlacesManager {

    // ── data structures ───────────────────────────────────────────────────────
    private final List<PlaceInteret> allPlaces    = new ArrayList<>();
    private final Map<Integer, PlaceInteret> byId = new HashMap<>();
    private final Map<PlaceInteret.TypeLieu, List<PlaceInteret>> byType
            = new EnumMap<>(PlaceInteret.TypeLieu.class);
    private final Map<ZoneManager.ZoneId, Map<PlaceInteret.TypeLieu, List<PlaceInteret>>> byZoneAndType
            = new EnumMap<>(ZoneManager.ZoneId.class);

    public PlacesManager() {
        for (PlaceInteret.TypeLieu t : PlaceInteret.TypeLieu.values()) {
            byType.put(t, new ArrayList<>());
        }
        for (ZoneManager.ZoneId z : ZoneManager.ZoneId.values()) {
            EnumMap<PlaceInteret.TypeLieu, List<PlaceInteret>> m = new EnumMap<>(PlaceInteret.TypeLieu.class);
            for (PlaceInteret.TypeLieu t : PlaceInteret.TypeLieu.values()) m.put(t, new ArrayList<>());
            byZoneAndType.put(z, m);
        }
        buildAllPlaces();
    }

    // =========================================================================
    // POI DEFINITIONS  (~350 total entries)
    // =========================================================================
    private void buildAllPlaces() {
        buildVillePrincipale();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VILLE PRINCIPALE  (~175 POIs)
    // ─────────────────────────────────────────────────────────────────────────
    private void buildVillePrincipale() {

        // Civic
        add("Mairie",                      MAIRIE,       1055, 870,  VILLE_PRINCIPALE, 60);
        add("Palais de Justice",           MAIRIE,        960, 820,  VILLE_PRINCIPALE, 40);
        add("Préfecture",                  MAIRIE,       1120, 900,  VILLE_PRINCIPALE, 35);
        add("Office du Tourisme",          MAIRIE,       1080, 840,  VILLE_PRINCIPALE, 25);

        // Bibliothèques
        add("Bibliothèque Centrale",       BIBLIOTHÈQUE, 1170, 905,  VILLE_PRINCIPALE, 100);
        add("Médiathèque Nord",            BIBLIOTHÈQUE,  880, 560,  VILLE_PRINCIPALE,  80);
        add("Bibliothèque Universitaire",  BIBLIOTHÈQUE, 1280, 600,  VILLE_PRINCIPALE, 120);
        add("Médiathèque Ouest",           BIBLIOTHÈQUE,  450, 1000, VILLE_PRINCIPALE,  60);
        add("Point Lecture Quartier Sud",  BIBLIOTHÈQUE,  800, 1380, VILLE_PRINCIPALE,  30);

        // Logements résidentiels (DOMICILE clusters - multiple per quartier)
        add("Résidence Voltaire",   DOMICILE,      640, 520,  VILLE_PRINCIPALE, 200);
        add("Résidence Hugo",       DOMICILE,      780, 610,  VILLE_PRINCIPALE, 180);
        add("Résidence Zola",       DOMICILE,      920, 680,  VILLE_PRINCIPALE, 200);
        add("Résidence Balzac",     DOMICILE,      580, 720,  VILLE_PRINCIPALE, 150);
        add("Résidence Flaubert",   DOMICILE,      700, 820,  VILLE_PRINCIPALE, 160);
        add("Résidence Baudelaire", DOMICILE,      480, 960,  VILLE_PRINCIPALE, 140);
        add("Résidence Proust",     DOMICILE,      610, 1050, VILLE_PRINCIPALE, 170);
        add("Résidence Dumas",      DOMICILE,      750, 1150, VILLE_PRINCIPALE, 150);
        add("Résidence Molière",    DOMICILE,      900, 1200, VILLE_PRINCIPALE, 130);
        add("Résidence Racine",     DOMICILE,     1080, 1300, VILLE_PRINCIPALE, 160);
        add("Résidence Corneille",  DOMICILE,     1240, 1380, VILLE_PRINCIPALE, 140);
        add("Résidence Stendhal",   DOMICILE,     1420, 1200, VILLE_PRINCIPALE, 120);
        add("Résidence Verne",      DOMICILE,     1550, 1050, VILLE_PRINCIPALE, 130);
        add("Résidence Saint-Ex",   DOMICILE,     1380, 900,  VILLE_PRINCIPALE, 145);
        add("Résidence Camus",      DOMICILE,     1260, 700,  VILLE_PRINCIPALE, 155);
        add("Résidence Sartre",     DOMICILE,      850, 510,  VILLE_PRINCIPALE, 120);

        // Bureaux & industries (TRAVAIL)
        add("Zone industrielle Nord", TRAVAIL,    1480, 510,  VILLE_PRINCIPALE, 150);
        add("Parc d'affaires Est",    TRAVAIL,    1600, 750,  VILLE_PRINCIPALE, 200);
        add("Centre commercial VP",   TRAVAIL,    1350, 660,  VILLE_PRINCIPALE, 180);
        add("Bureaux centre",         TRAVAIL,    1050, 960,  VILLE_PRINCIPALE, 120);
        add("Usine Ouest",            TRAVAIL,     480, 500,  VILLE_PRINCIPALE, 100);
        add("Entrepôts Sud",          TRAVAIL,     620, 1450, VILLE_PRINCIPALE, 80);
        add("Clinique VP",            TRAVAIL,     980, 820,  VILLE_PRINCIPALE, 60);
        add("Lycée professionnel",    TRAVAIL,    1180, 550,  VILLE_PRINCIPALE, 90);

        // Cafés
        add("Café de la Paix",      CAFÉ,          900, 870,  VILLE_PRINCIPALE, 30);
        add("Café du Commerce",     CAFÉ,         1140, 830,  VILLE_PRINCIPALE, 25);
        add("Café Voltaire",        CAFÉ,          660, 570,  VILLE_PRINCIPALE, 20);
        add("L'Éclair",             CAFÉ,         1420, 780,  VILLE_PRINCIPALE, 22);
        add("Café des Artistes",    CAFÉ,          810, 1120, VILLE_PRINCIPALE, 28);

        // Restaurants
        add("La Brasserie",         RESTAURANT,    990, 950,  VILLE_PRINCIPALE, 60);
        add("Le Bistrot du Marché", RESTAURANT,    870, 900,  VILLE_PRINCIPALE, 45);
        add("Chez Marcel",          RESTAURANT,    740, 1050, VILLE_PRINCIPALE, 40);
        add("Pizza Napoli",         RESTAURANT,   1250, 850,  VILLE_PRINCIPALE, 50);
        add("Le Jardin",            RESTAURANT,    660, 780,  VILLE_PRINCIPALE, 55);
        add("Wok & Go",             RESTAURANT,   1500, 920,  VILLE_PRINCIPALE, 35);
        add("La Table Ronde",       RESTAURANT,   1080, 1100, VILLE_PRINCIPALE, 50);

        // Bars
        add("Le Comptoir",          BAR,          1000, 1000, VILLE_PRINCIPALE, 50);
        add("Bar de la Gare",       BAR,           750, 920,  VILLE_PRINCIPALE, 40);
        add("La Cave",              BAR,          1300, 960,  VILLE_PRINCIPALE, 45);
        add("Le Zinc",              BAR,           580, 1000, VILLE_PRINCIPALE, 35);
        add("Nuit Blanche",         BAR,          1450, 1050, VILLE_PRINCIPALE, 60);

        // Supermarchés
        add("Supermarché Atlas",    SUPERMARCHÉ,   720, 780,  VILLE_PRINCIPALE, 120);
        add("Carrefour VP",         SUPERMARCHÉ,  1380, 1050, VILLE_PRINCIPALE, 200);
        add("Bio Marché",           SUPERMARCHÉ,   960, 1240, VILLE_PRINCIPALE, 80);

        // Pharmacies
        add("Pharmacie Centrale",   PHARMACIE,    1010, 890,  VILLE_PRINCIPALE, 20);
        add("Pharmacie du Parc",    PHARMACIE,     700, 1180, VILLE_PRINCIPALE, 15);

        // Écoles
        add("École Primaire Jules Ferry", ÉCOLE,   840, 640,  VILLE_PRINCIPALE, 120);
        add("Collège Molière",            ÉCOLE,  1180, 720,  VILLE_PRINCIPALE, 200);
        add("Lycée Victor Hugo",          ÉCOLE,   950, 550,  VILLE_PRINCIPALE, 250);

        // Salles de sport
        add("FitClub VP",           SALLE_SPORT,  1320, 640,  VILLE_PRINCIPALE, 80);
        add("Stade municipal",      SALLE_SPORT,   560, 860,  VILLE_PRINCIPALE, 150);
        add("Piscine olympique",    SALLE_SPORT,  1200, 1180, VILLE_PRINCIPALE, 100);

        // Cinémas
        add("Ciné VP Grand",        CINÉMA,        880, 850,  VILLE_PRINCIPALE, 120);
        add("Multiplexe Nord",      CINÉMA,       1500, 620,  VILLE_PRINCIPALE, 200);

        // Parcs
        add("Parc de la République",PARC,          780, 960,  VILLE_PRINCIPALE, 300);
        add("Jardin Municipal",     PARC,         1100, 1200, VILLE_PRINCIPALE, 200);
        add("Square Liberté",       PARC,          640, 700,  VILLE_PRINCIPALE, 100);
        add("Bois de la Ville",     PARC,          430, 1100, VILLE_PRINCIPALE, 250);

        // Marché & concert
        add("Marché couvert VP",    MARCHÉ,        940, 810,  VILLE_PRINCIPALE, 200);
        add("Salle de concert VP",  CONCERT,      1040, 1050, VILLE_PRINCIPALE, 400);
        add("Discothèque Le Beat",  DISCOTHÈQUE,  1320, 1120, VILLE_PRINCIPALE, 300);

        // ── PETITE VILLE ──────────────────────────────────────────────────────

        add("Mairie PV",             MAIRIE,       2900, 900,  PETITE_VILLE, 30);
        add("Bibliothèque PV",       BIBLIOTHÈQUE, 2860, 960,  PETITE_VILLE, 40);

        // DOMICILE clusters
        add("Lotissement des Prés",  DOMICILE,    2410, 680,  PETITE_VILLE, 120);
        add("Résidence des Chênes",  DOMICILE,    2570, 780,  PETITE_VILLE, 100);
        add("Hameau des Lilas",      DOMICILE,    2730, 590,  PETITE_VILLE, 90);
        add("Quartier de la Croix",  DOMICILE,    2950, 680,  PETITE_VILLE, 110);
        add("Les Glycines",          DOMICILE,    3120, 720,  PETITE_VILLE, 100);
        add("Résidence du Moulin",   DOMICILE,    3300, 600,  PETITE_VILLE, 80);
        add("Cité Fleurie",          DOMICILE,    2680, 1100, PETITE_VILLE, 90);
        add("Les Tilleuls",          DOMICILE,    2860, 1200, PETITE_VILLE, 100);
        add("Résidence du Lac",      DOMICILE,    3050, 1100, PETITE_VILLE, 85);
        add("Villa des Pins",        DOMICILE,    3250, 1200, PETITE_VILLE, 75);

        // Travail
        add("Zone artisanale PV",    TRAVAIL,     2500, 580,  PETITE_VILLE, 80);
        add("Commerce de détail PV", TRAVAIL,     2940, 880,  PETITE_VILLE, 60);
        add("Petit hôpital PV",      TRAVAIL,     3150, 830,  PETITE_VILLE, 50);
        add("Entrepôt logistique",   TRAVAIL,     3400, 640,  PETITE_VILLE, 70);

        // Social
        add("Café de la Place",      CAFÉ,         2870, 870,  PETITE_VILLE, 20);
        add("Café du Coin",          CAFÉ,         2700, 1050, PETITE_VILLE, 15);
        add("Bistrot des Vignes",    CAFÉ,         3200, 950,  PETITE_VILLE, 18);
        add("Restaurant La Ferme",   RESTAURANT,   2900, 1000, PETITE_VILLE, 40);
        add("Pizzeria PV",           RESTAURANT,   2750, 850,  PETITE_VILLE, 30);
        add("Le Pressoir",           RESTAURANT,   3100, 1050, PETITE_VILLE, 35);
        add("Bar du Football",       BAR,          2960, 1100, PETITE_VILLE, 40);
        add("Le Repaire",            BAR,          2780, 980,  PETITE_VILLE, 30);
        add("Super U PV",            SUPERMARCHÉ,  3200, 760,  PETITE_VILLE, 100);
        add("Épicerie du Village",   SUPERMARCHÉ,  2830, 920,  PETITE_VILLE, 40);
        add("Pharmacie du Bourg",    PHARMACIE,    2890, 890,  PETITE_VILLE, 10);
        add("École du Bourg",        ÉCOLE,        2820, 750,  PETITE_VILLE, 100);
        add("Gym PV",                SALLE_SPORT,  3080, 880,  PETITE_VILLE, 40);
        add("Cinéma Rex",            CINÉMA,       2990, 1020, PETITE_VILLE, 80);
        add("Parc du Château",       PARC,         2750, 1150, PETITE_VILLE, 120);
        add("Jardin des Fleurs",     PARC,         3050, 1200, PETITE_VILLE, 100);
        add("Marché du Dimanche",    MARCHÉ,       2910, 810,  PETITE_VILLE, 100);
        add("Salle polyvalente",     CONCERT,      3180, 1100, PETITE_VILLE, 200);

        // ── HAMEAU ────────────────────────────────────────────────────────────

        add("Mairie Hameau",         MAIRIE,       3680, 420,  HAMEAU, 15);

        // DOMICILE clusters
        add("Fermes de l'Ouest",     DOMICILE,    3340, 280,  HAMEAU, 60);
        add("Vieux bourg Hameau",    DOMICILE,    3510, 370,  HAMEAU, 50);
        add("Résidence du Moulin H.",DOMICILE,    3680, 260,  HAMEAU, 45);
        add("Les Hameaux clairs",    DOMICILE,    3840, 420,  HAMEAU, 55);
        add("Ferme des Prés verts",  DOMICILE,    3960, 330,  HAMEAU, 40);
        add("Mas des Collines",      DOMICILE,    3590, 580,  HAMEAU, 45);
        add("Chalet du Bois",        DOMICILE,    3780, 570,  HAMEAU, 35);

        // Travail (agriculture, artisanat)
        add("Coopérative agricole",  TRAVAIL,     3470, 310,  HAMEAU, 30);
        add("Atelier menuiserie",    TRAVAIL,     3860, 370,  HAMEAU, 20);
        add("Marché fermier H.",     TRAVAIL,     3700, 480,  HAMEAU, 25);

        // Social (very limited)
        add("L'Auberge du Hameau",   CAFÉ,         3660, 400,  HAMEAU, 15);
        add("Épicerie du Hameau",    SUPERMARCHÉ,  3640, 440,  HAMEAU, 20);
        add("Pharmacie rurale",      PHARMACIE,    3670, 420,  HAMEAU, 8);
        add("École Hameau",          ÉCOLE,        3700, 360,  HAMEAU, 50);
        add("Parc naturel",          PARC,         3500, 500,  HAMEAU, 200);
        add("Forêt du Hameau",       PARC,         3850, 500,  HAMEAU, 200);
        add("Bar-Tabac le Tillac",   BAR,          3680, 440,  HAMEAU, 20);

        // ── ZONE MER (vacances) ───────────────────────────────────────────────

        // DOMICILE (résidences balnéaires)
        add("Résidence Mer Bleue",   DOMICILE,    2560, 1820, ZONE_VACANCES_MER, 80);
        add("Villa Côte d'Azur",     DOMICILE,    2780, 1900, ZONE_VACANCES_MER, 70);
        add("Camping du Soleil",     DOMICILE,    3100, 1760, ZONE_VACANCES_MER, 100);
        add("Hôtel La Vague",        DOMICILE,    3350, 1850, ZONE_VACANCES_MER, 90);
        add("Résidence Les Flots",   DOMICILE,    3580, 1950, ZONE_VACANCES_MER, 75);
        add("Bungalows du Port",     DOMICILE,    3850, 2050, ZONE_VACANCES_MER, 60);
        add("Villas des Pins",       DOMICILE,    4000, 1880, ZONE_VACANCES_MER, 50);

        // Travail (saisonnier)
        add("Hôtel La Plage",        TRAVAIL,     3200, 1920, ZONE_VACANCES_MER, 50);
        add("Restaurant du Port",    TRAVAIL,     3580, 1990, ZONE_VACANCES_MER, 40);

        // Plages
        add("Grande Plage",          PLAGE,       2700, 2250, ZONE_VACANCES_MER, 400, true);
        add("Plage du Port",         PLAGE,       3150, 2280, ZONE_VACANCES_MER, 300, true);
        add("Calanque Secrète",      PLAGE,       3700, 2240, ZONE_VACANCES_MER, 150, true);
        add("Plage Nord",            PLAGE,       3950, 2150, ZONE_VACANCES_MER, 200, true);

        // Bars & restaurants mer
        add("La Paillote",           BAR,         2820, 2200, ZONE_VACANCES_MER, 60, true);
        add("Cocktail Sunset",       BAR,         3280, 2230, ZONE_VACANCES_MER, 50, true);
        add("Les Pieds dans l'eau",  BAR,         3680, 2210, ZONE_VACANCES_MER, 55, true);
        add("Café de la Mer",        CAFÉ,        3100, 1980, ZONE_VACANCES_MER, 25);
        add("Snack de la Plage",     CAFÉ,        2940, 2150, ZONE_VACANCES_MER, 20, true);
        add("Le Poisson d'Or",       RESTAURANT,  3050, 2020, ZONE_VACANCES_MER, 55, true);
        add("La Cabane du Pêcheur",  RESTAURANT,  3500, 2100, ZONE_VACANCES_MER, 45, true);
        add("Brasserie du Port",     RESTAURANT,  3700, 2050, ZONE_VACANCES_MER, 60, true);

        // Entertainment
        add("Casino Mer",            DISCOTHÈQUE, 3200, 2000, ZONE_VACANCES_MER, 200, true);
        add("Scène du Festival",     CONCERT,     3500, 2150, ZONE_VACANCES_MER, 1000, true);
        add("Club Tropical",         DISCOTHÈQUE, 3800, 2100, ZONE_VACANCES_MER, 250, true);
        add("Miniature Parc",        PARC,        2620, 1900, ZONE_VACANCES_MER, 150);
        add("Marché Provençal",      MARCHÉ,      3100, 1900, ZONE_VACANCES_MER, 100);
    }

    private void add(String nom, PlaceInteret.TypeLieu type, double wx, double wy,
                     ZoneManager.ZoneId zone, int cap) {
        register(new PlaceInteret(nom, type, wx, wy, zone, cap));
    }

    private void add(String nom, PlaceInteret.TypeLieu type, double wx, double wy,
                     ZoneManager.ZoneId zone, int cap, boolean summerOnly) {
        register(new PlaceInteret(nom, type, wx, wy, zone, cap, summerOnly));
    }

    private void register(PlaceInteret p) {
        allPlaces.add(p);
        byId.put(p.getId(), p);
        byType.get(p.getType()).add(p);
        byZoneAndType.get(p.getZone()).get(p.getType()).add(p);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // QUERY
    // ──────────────────────────────────────────────────────────────────────────

    public List<PlaceInteret> getAllPlaces()                          { return Collections.unmodifiableList(allPlaces); }
    public PlaceInteret       getById(int id)                         { return byId.get(id); }
    public List<PlaceInteret> getByType(PlaceInteret.TypeLieu t)      { return Collections.unmodifiableList(byType.get(t)); }

    public List<PlaceInteret> getByZoneAndType(ZoneManager.ZoneId zone, PlaceInteret.TypeLieu type) {
        return Collections.unmodifiableList(byZoneAndType.get(zone).get(type));
    }

    /** Returns the nearest POI of the given type within the zone (or null). */
    public PlaceInteret nearestInZone(Vector2D pos, ZoneManager.ZoneId zone, PlaceInteret.TypeLieu type) {
        List<PlaceInteret> candidates = byZoneAndType.get(zone).get(type);
        PlaceInteret best = null;
        double bestDist2 = Double.MAX_VALUE;
        for (PlaceInteret p : candidates) {
            double d2 = pos.distanceSquared(p.positionRef());
            if (d2 < bestDist2) { bestDist2 = d2; best = p; }
        }
        return best;
    }

    /** Returns a random POI of type in zone, using provided rng. */
    public PlaceInteret randomInZone(ZoneManager.ZoneId zone, PlaceInteret.TypeLieu type, SplittableRandom rng) {
        List<PlaceInteret> candidates = byZoneAndType.get(zone).get(type);
        if (candidates.isEmpty()) return null;
        return candidates.get(rng.nextInt(candidates.size()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SCHEDULED TARGET (core daily life)
    // Returns the world-space position an agent should head to given hour and traits.
    // Caller: Bavard.evaluateStrategicState()
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Given an agent's current context and the simulated hour, picks the best
     * POI target position to set as localTarget.
     *
     * @param agentZone      zone where the agent currently is
     * @param agentDomicile  home position (may be null)
     * @param agentTravail   work position (may be null)
     * @param favoritePois   map of TypeLieu -> PlaceInteret (agent's favorites)
     * @param pendulaire     true if the agent is a commuter
     * @param sociabilite    [0..1]
     * @param hour           current simulation hour of day
     * @param rng            agent's own rng
     * @return target position, or null if no change needed
     */
    public Vector2D getScheduledTarget(
            ZoneManager.ZoneId agentZone,
            Vector2D agentDomicile,
            Vector2D agentTravail,
            Map<PlaceInteret.TypeLieu, PlaceInteret> favoritePois,
            boolean pendulaire,
            double sociabilite,
            double hour,
            SplittableRandom rng
    ) {
        double h = hour % 24.0;

        // Night-time: go home
        if (h < 6.5 || h >= 23.0) {
            return agentDomicile;
        }

        // Morning: café / breakfast
        if (h >= 6.5 && h < 8.5) {
            if (sociabilite > 0.4 && rng.nextDouble() < sociabilite * 0.7) {
                PlaceInteret café = getFavorite(favoritePois, CAFÉ, agentZone, rng);
                if (café != null) return café.positionRef();
            }
            return agentDomicile;
        }

        // Work hours (non-commuters stay local)
        if (h >= 8.5 && h < 12.0) {
            if (pendulaire && agentTravail != null) return agentTravail;
            // non-workers: parc, bibliothèque, supermarché
            return pickDaytimeLocal(favoritePois, agentZone, h, sociabilite, rng);
        }

        // Lunch
        if (h >= 12.0 && h < 13.5) {
            if (rng.nextDouble() < 0.6) {
                PlaceInteret resto = getFavorite(favoritePois, RESTAURANT, agentZone, rng);
                if (resto != null) return resto.positionRef();
            }
            PlaceInteret café = getFavorite(favoritePois, CAFÉ, agentZone, rng);
            if (café != null) return café.positionRef();
            return agentDomicile;
        }

        // Afternoon work
        if (h >= 13.5 && h < 17.0) {
            if (pendulaire && agentTravail != null) return agentTravail;
            return pickDaytimeLocal(favoritePois, agentZone, h, sociabilite, rng);
        }

        // Post-work / errands
        if (h >= 17.0 && h < 18.5) {
            double roll = rng.nextDouble();
            if (roll < 0.3) {
                PlaceInteret super_ = getFavorite(favoritePois, SUPERMARCHÉ, agentZone, rng);
                if (super_ != null) return super_.positionRef();
            } else if (roll < 0.5) {
                PlaceInteret gym = getFavorite(favoritePois, SALLE_SPORT, agentZone, rng);
                if (gym != null) return gym.positionRef();
            } else if (roll < 0.65) {
                PlaceInteret parc = getFavorite(favoritePois, PARC, agentZone, rng);
                if (parc != null) return parc.positionRef();
            }
            return agentDomicile;
        }

        // Evening activities
        if (h >= 18.5 && h < 21.0) {
            double roll = rng.nextDouble();
            if (roll < sociabilite * 0.5) {
                PlaceInteret bar = getFavorite(favoritePois, BAR, agentZone, rng);
                if (bar != null) return bar.positionRef();
            } else if (roll < 0.45) {
                PlaceInteret cinema = getFavorite(favoritePois, CINÉMA, agentZone, rng);
                if (cinema != null) return cinema.positionRef();
            } else if (roll < 0.6) {
                PlaceInteret gym = getFavorite(favoritePois, SALLE_SPORT, agentZone, rng);
                if (gym != null) return gym.positionRef();
            } else if (roll < 0.75) {
                PlaceInteret resto = getFavorite(favoritePois, RESTAURANT, agentZone, rng);
                if (resto != null) return resto.positionRef();
            }
            return agentDomicile;
        }

        // Late night
        if (h >= 21.0 && h < 23.0) {
            if (sociabilite > 0.7 && rng.nextDouble() < sociabilite * 0.5) {
                // Nightlife types go to bar/disco
                PlaceInteret venue = rng.nextDouble() < 0.4
                        ? getFavorite(favoritePois, DISCOTHÈQUE, agentZone, rng)
                        : getFavorite(favoritePois, BAR, agentZone, rng);
                if (venue != null) return venue.positionRef();
            }
            return agentDomicile;
        }

        return agentDomicile;
    }

    private Vector2D pickDaytimeLocal(
            Map<PlaceInteret.TypeLieu, PlaceInteret> favorites,
            ZoneManager.ZoneId zone, double h, double sociabilite, SplittableRandom rng) {
        double roll = rng.nextDouble();
        if (roll < 0.30) {
            PlaceInteret parc = getFavorite(favorites, PARC, zone, rng);
            if (parc != null) return parc.positionRef();
        }
        if (roll < 0.45) {
            PlaceInteret lib = getFavorite(favorites, BIBLIOTHÈQUE, zone, rng);
            if (lib != null) return lib.positionRef();
        }
        if (roll < 0.60) {
            PlaceInteret super_ = getFavorite(favorites, SUPERMARCHÉ, zone, rng);
            if (super_ != null) return super_.positionRef();
        }
        if (h >= 8.0 && h < 13.0) {
            PlaceInteret marché = getFavorite(favorites, MARCHÉ, zone, rng);
            if (marché != null) return marché.positionRef();
        }
        return null;
    }

    /** Returns the agent's personal favorite (pre-assigned or random fallback). */
    private PlaceInteret getFavorite(
            Map<PlaceInteret.TypeLieu, PlaceInteret> favorites,
            PlaceInteret.TypeLieu type,
            ZoneManager.ZoneId zone,
            SplittableRandom rng) {
        PlaceInteret fav = favorites.get(type);
        if (fav != null) return fav;
        // Fallback: pick random in zone or globally
        List<PlaceInteret> zoneList = byZoneAndType.get(zone).get(type);
        if (!zoneList.isEmpty()) return zoneList.get(rng.nextInt(zoneList.size()));
        List<PlaceInteret> global = byType.get(type);
        if (!global.isEmpty()) return global.get(rng.nextInt(global.size()));
        return null;
    }

    /**
     * Assigns one favorite per relevant TypeLieu to an agent at bootstrap.
     * Uses the agent's home zone and traits as seeding criteria.
     */
    public Map<PlaceInteret.TypeLieu, PlaceInteret> assignFavorites(
            ZoneManager.ZoneId homeZone,
            double sociabilite,
            SplittableRandom rng) {
        Map<PlaceInteret.TypeLieu, PlaceInteret> favs = new EnumMap<>(PlaceInteret.TypeLieu.class);
        assign(favs, CAFÉ,         homeZone, rng);
        assign(favs, RESTAURANT,   homeZone, rng);
        assign(favs, BAR,          homeZone, rng);
        assign(favs, SUPERMARCHÉ,  homeZone, rng);
        assign(favs, PHARMACIE,    homeZone, rng);
        assign(favs, PARC,         homeZone, rng);
        assign(favs, BIBLIOTHÈQUE, homeZone, rng);
        assign(favs, CINÉMA,       homeZone, rng);
        assign(favs, SALLE_SPORT,  homeZone, rng);
        assign(favs, MARCHÉ,       homeZone, rng);
        if (sociabilite > 0.6) assign(favs, DISCOTHÈQUE, homeZone, rng);
        return favs;
    }

    private void assign(Map<PlaceInteret.TypeLieu, PlaceInteret> favs,
                        PlaceInteret.TypeLieu type, ZoneManager.ZoneId zone, SplittableRandom rng) {
        PlaceInteret p = randomInZone(zone, type, rng);
        if (p != null) favs.put(type, p);
    }

    public static final PlaceInteret.TypeLieu CAFÉ        = PlaceInteret.TypeLieu.CAFÉ;
    public static final PlaceInteret.TypeLieu RESTAURANT  = PlaceInteret.TypeLieu.RESTAURANT;
    public static final PlaceInteret.TypeLieu BAR         = PlaceInteret.TypeLieu.BAR;
    public static final PlaceInteret.TypeLieu SUPERMARCHÉ = PlaceInteret.TypeLieu.SUPERMARCHÉ;
    public static final PlaceInteret.TypeLieu PHARMACIE   = PlaceInteret.TypeLieu.PHARMACIE;
    public static final PlaceInteret.TypeLieu PARC        = PlaceInteret.TypeLieu.PARC;
    public static final PlaceInteret.TypeLieu BIBLIOTHÈQUE= PlaceInteret.TypeLieu.BIBLIOTHÈQUE;
    public static final PlaceInteret.TypeLieu CINÉMA      = PlaceInteret.TypeLieu.CINÉMA;
    public static final PlaceInteret.TypeLieu SALLE_SPORT = PlaceInteret.TypeLieu.SALLE_SPORT;
    public static final PlaceInteret.TypeLieu MARCHÉ      = PlaceInteret.TypeLieu.MARCHÉ;
    public static final PlaceInteret.TypeLieu DISCOTHÈQUE = PlaceInteret.TypeLieu.DISCOTHÈQUE;
    public static final PlaceInteret.TypeLieu CONCERT     = PlaceInteret.TypeLieu.CONCERT;
    public static final PlaceInteret.TypeLieu PLAGE       = PlaceInteret.TypeLieu.PLAGE;
}
