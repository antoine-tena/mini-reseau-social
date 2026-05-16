package socialnetwork.back.seirs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RumorManager {

    private static final int MAX_ACTIVE_RUMORS = 4;

    private static final class ActiveRumor {
        private final Rumeur rumor;
        private final double remainingHours;

        private ActiveRumor(Rumeur rumor, double remainingHours) {
            this.rumor = rumor;
            this.remainingHours = remainingHours;
        }

        private ActiveRumor tick(double deltaHours) {
            return new ActiveRumor(rumor, remainingHours - deltaHours);
        }
    }

    private final List<Rumeur> library;
    private final ConcurrentHashMap<String, ActiveRumor> activeRumors = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Rumeur> pendingActivations = new ConcurrentLinkedQueue<>();
    private final Random random;

    private double activationCooldownHours = 0.15;
    private double summerLevel = 0.0;
    /** When true, rumors are never auto-activated; the UI drives activation. */
    private volatile boolean autoActivation = false;

    public RumorManager(Random random) {
        this.random = random;
        this.library = buildLibrary();
    }

    public void update(double deltaHours, double totalSimulationHours) {
        summerLevel = computeSummerLevel(totalSimulationHours);

        List<String> expiredIds = new ArrayList<>();
        for (var entry : activeRumors.entrySet()) {
            ActiveRumor updated = entry.getValue().tick(deltaHours);
            if (updated.remainingHours <= 0.0) {
                expiredIds.add(entry.getKey());
            } else {
                activeRumors.put(entry.getKey(), updated);
            }
        }
        for (String expiredId : expiredIds) {
            activeRumors.remove(expiredId);
        }

        if (autoActivation) {
            activationCooldownHours -= deltaHours;
            while (shouldActivateMoreRumors()) {
                Rumeur candidate = chooseTemplate(totalSimulationHours);
                if (candidate == null) {
                    activationCooldownHours = nextCooldownHours();
                    return;
                }
                activate(candidate, lifespanFor(candidate));
                activationCooldownHours = nextCooldownHours();
            }
        }
    }

    public List<Rumeur> drainNewActivations() {
        List<Rumeur> out = new ArrayList<>();
        Rumeur rumor;
        while ((rumor = pendingActivations.poll()) != null) {
            out.add(rumor);
        }
        return out;
    }

    public List<Rumeur> getActiveRumorsSnapshot() {
        List<Rumeur> snapshot = new ArrayList<>(activeRumors.size());
        for (ActiveRumor rumor : activeRumors.values()) {
            snapshot.add(rumor.rumor.withLifetime(rumor.remainingHours));
        }
        return snapshot;
    }

    public Collection<Rumeur> getLibrary() {
        return Collections.unmodifiableList(library);
    }

    public List<Rumeur> getLibraryAsList() {
        return Collections.unmodifiableList(library);
    }

    // ── Manual activation API ────────────────────────────────────────────────

    public boolean isAutoActivation() { return autoActivation; }
    public void setAutoActivation(boolean auto) { this.autoActivation = auto; }

    /**
     * Manually activate the rumor at the given index in the library.
     * @return true if activated, false if already active or index out of range.
     */
    public boolean manualActivate(int libraryIndex) {
        if (libraryIndex < 0 || libraryIndex >= library.size()) return false;
        Rumeur r = library.get(libraryIndex);
        if (activeRumors.containsKey(r.getId())) return false;
        activate(r, lifespanFor(r) * 1.5); // manual activations last longer
        return true;
    }

    /**
     * Manually deactivate a rumor by its id.
     */
    public void manualDeactivate(String rumorId) {
        activeRumors.remove(rumorId);
    }

    public boolean isRumorActive(String rumorId) {
        return activeRumors.containsKey(rumorId);
    }

    public double getSummerLevel() {
        return summerLevel;
    }

    public double repulsionPressure(ZoneManager.ZoneId currentZone) {
        double pressure = 0.0;
        for (ActiveRumor active : activeRumors.values()) {
            if (active.rumor.repousseDepuis(currentZone)) {
                pressure += active.rumor.getIntensiteRepulsion() * active.rumor.getContagiosite();
            }
        }
        return Math.min(2.0, pressure);
    }

    public ZoneManager.ZoneId pickMigrationTarget(
        ZoneManager.ZoneId currentZone,
        double sociability,
        double vacationAffinity,
        SplittableRandom rng
    ) {
        Rumeur best = null;
        double bestScore = 0.0;
        for (ActiveRumor active : activeRumors.values()) {
            Rumeur rumor = active.rumor;
            ZoneManager.ZoneId attractive = rumor.getZoneAttractive();
            if (attractive == null || attractive == currentZone) {
                continue;
            }
            double score = rumor.getContagiosite() * rumor.getIntensiteAttraction() * (0.55 + sociability * 0.45);
            if (attractive == ZoneManager.ZoneId.ZONE_VACANCES_MER) {
                score *= 1.0 + vacationAffinity * 0.9 + summerLevel * 0.7;
            }
            if (score > bestScore) {
                bestScore = score;
                best = rumor;
            }
        }
        if (best != null && rng.nextDouble() < Math.min(0.85, bestScore * 0.18)) {
            return best.getZoneAttractive();
        }
        return null;
    }

    public Vector2D computeSpatialInfluence(
        ZoneManager zoneManager,
        ZoneManager.ZoneId currentZone,
        Vector2D position,
        double vacationAffinity
    ) {
        Vector2D force = new Vector2D();
        for (ActiveRumor active : activeRumors.values()) {
            Rumeur rumor = active.rumor;
            if (rumor.getZoneAttractive() != null) {
                Vector2D target = zoneManager.getZone(rumor.getZoneAttractive()).getCentre();
                Vector2D direction = target.subtract(position);
                double distance = Math.max(40.0, direction.length());
                if (distance > 1.0e-6) {
                    double intensity = rumor.getIntensiteAttraction() * rumor.getContagiosite() * 120.0 / distance;
                    if (rumor.getZoneAttractive() == ZoneManager.ZoneId.ZONE_VACANCES_MER) {
                        intensity *= 1.0 + vacationAffinity * 0.75 + summerLevel * 0.85;
                    }
                    force.addLocal(direction.normalized().multiply(intensity));
                }
            }
            if (rumor.repousseDepuis(currentZone)) {
                Vector2D center = zoneManager.getZone(currentZone).getCentre();
                Vector2D direction = position.subtract(center);
                if (direction.lengthSquared() <= 1.0e-6) {
                    direction = Vector2D.randomUnit(new Random(Double.doubleToLongBits(position.getX() + position.getY())));
                }
                double intensity = rumor.getIntensiteRepulsion() * rumor.getContagiosite() * 1.4;
                force.addLocal(direction.normalized().multiply(intensity));
            }
        }
        return force;
    }

    public Rumeur mutateForTransmission(Rumeur base, SplittableRandom rng) {
        return base.mutate(rng);
    }

    private boolean shouldActivateMoreRumors() {
        if (activeRumors.size() >= MAX_ACTIVE_RUMORS) {
            return false;
        }
        if (activeRumors.isEmpty()) {
            return true;
        }
        if (activationCooldownHours > 0.0) {
            return false;
        }
        return random.nextDouble() < 0.62;
    }

    private void activate(Rumeur rumor, double hours) {
        if (activeRumors.putIfAbsent(rumor.getId(), new ActiveRumor(rumor, hours)) == null) {
            pendingActivations.add(rumor.withLifetime(hours));
        }
    }

    private Rumeur chooseTemplate(double totalSimulationHours) {
        List<Rumeur> inactive = new ArrayList<>();
        for (Rumeur rumor : library) {
            if (!activeRumors.containsKey(rumor.getId())) {
                inactive.add(rumor);
            }
        }
        if (inactive.isEmpty()) {
            return null;
        }

        if (summerLevel > 0.65) {
            for (Rumeur rumor : inactive) {
                if ("RUMEUR_5".equals(rumor.getId()) && random.nextDouble() < 0.55) {
                    return rumor;
                }
            }
        }

        if ((hourOfDay(totalSimulationHours) > 17.0 || hourOfDay(totalSimulationHours) < 7.0)) {
            for (Rumeur rumor : inactive) {
                if ("RUMEUR_9".equals(rumor.getId()) && random.nextDouble() < 0.35) {
                    return rumor;
                }
            }
        }

        return inactive.get(random.nextInt(inactive.size()));
    }

    private double lifespanFor(Rumeur rumor) {
        double bonus = 0.0;
        if (rumor.getZoneAttractive() == ZoneManager.ZoneId.ZONE_VACANCES_MER) {
            bonus = summerLevel * 5.0;
        }
        return rumor.getDureeDeVie() + bonus + random.nextDouble(0.4, 2.4);
    }

    private double nextCooldownHours() {
        return random.nextDouble(0.18, 0.95);
    }

    private double hourOfDay(double totalSimulationHours) {
        double hour = totalSimulationHours % 24.0;
        return hour < 0.0 ? hour + 24.0 : hour;
    }

    private double computeSummerLevel(double totalSimulationHours) {
        double dayOfYear = (totalSimulationHours / 24.0) % 360.0;
        double radians = ((dayOfYear - 90.0) / 360.0) * Math.PI * 2.0;
        return 0.5 + 0.5 * Math.sin(radians);
    }

    private static List<Rumeur> buildLibrary() {
        List<Rumeur> rumors = new ArrayList<>(10);
        rumors.add(new Rumeur(
            "RUMEUR_1",
            "Le Maire distribue des billets de 100€ au parc municipal !",
            0.55,
            0.78,
            6.0,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            null,
            1.0,
            0.0,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_2",
            "Quelqu'un a volé toutes les échelles de la piscine municipale...",
            -0.28,
            0.54,
            4.5,
            null,
            null,
            0.0,
            0.0,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_3",
            "Des extraterrestres enlèvent des citoyens près de l'observatoire",
            -0.42,
            0.67,
            5.5,
            null,
            ZoneManager.ZoneId.HAMEAU,
            0.0,
            0.75,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_4",
            "Le taux d'imposition local va augmenter de 300% demain",
            -0.60,
            0.74,
            7.5,
            null,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            0.0,
            1.0,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_5",
            "Une star de la Pop mondiale bronze sur la plage !",
            0.72,
            0.89,
            9.5,
            ZoneManager.ZoneId.ZONE_VACANCES_MER,
            null,
            1.25,
            0.0,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_6",
            "Un lama géant a été aperçu en train de bloquer le centre-ville",
            -0.18,
            0.58,
            4.0,
            null,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            0.0,
            0.35,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_7",
            "La boulangerie centrale offre des pâtisseries gratuites",
            0.48,
            0.69,
            5.5,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            null,
            0.72,
            0.0,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_8",
            "L'usine locale déverse du liquide vert fluorescent",
            -0.73,
            0.80,
            8.0,
            null,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            0.0,
            1.35,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_9",
            "Les éboueurs sont en grève illimitée à partir de ce soir",
            -0.36,
            0.62,
            10.0,
            null,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            0.0,
            0.62,
            0
        ));
        rumors.add(new Rumeur(
            "RUMEUR_10",
            "Un trésor archéologique vient d'être déterré sous le stade",
            0.61,
            0.76,
            6.5,
            ZoneManager.ZoneId.VILLE_PRINCIPALE,
            null,
            0.88,
            0.0,
            0
        ));
        return Collections.unmodifiableList(rumors);
    }
}
