package socialnetwork.back.seirs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public final class SimulationEngine implements Runnable, AutoCloseable {

    public static final double FIXED_STEP_SECONDS = 1.0 / 60.0;
    public static final double SIMULATION_HOURS_PER_REAL_SECOND = 0.40;
    public static final int TARGET_TICKS_PER_SECOND = 60;

    private final ZoneManager zoneManager;
    private final SpatialHashGrid spatialHashGrid;
    private final RumorManager rumorManager;
    private final EventManager eventManager;
    private final PlacesManager placesManager;
    private final RelationManager relationManager;
    private final CopyOnWriteArrayList<Bavard> agents;
    private final ExecutorService workers;
    private final Random random;

    /** Simulation speed multiplier. 1.0 = normal, 0.01 = very slow, 5.0 = very fast. */
    private volatile double timeScale = 1.0;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private volatile Thread simulationThread;
    private volatile long tick;
    private volatile double elapsedRealSeconds;
    private volatile double elapsedSimulationHours;

    public SimulationEngine() {
        this(1200, System.currentTimeMillis());
    }

    public SimulationEngine(int populationSize) {
        this(populationSize, System.currentTimeMillis());
    }

    public SimulationEngine(int populationSize, long seed) {
        this.random = new Random(seed);
        this.zoneManager = new ZoneManager();
        this.spatialHashGrid = new SpatialHashGrid(92.0);
        this.rumorManager = new RumorManager(random);
        this.eventManager = new EventManager(random);
        this.placesManager = new PlacesManager();
        this.relationManager = new RelationManager();
        this.agents = new CopyOnWriteArrayList<>();
        this.workers = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            new EngineThreadFactory()
        );
        this.tick = 0L;
        this.elapsedRealSeconds = 0.0;
        this.elapsedSimulationHours = 8.0;
        bootstrapPopulation(Math.max(1, populationSize));
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            paused.set(false);
            Thread thread = new Thread(this, "seirs-simulation-main");
            thread.setDaemon(true);
            this.simulationThread = thread;
            thread.start();
        }
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }

    public void stop() {
        running.set(false);
        Thread thread = simulationThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1500L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        simulationThread = null;
    }

    @Override
    public void close() {
        stop();
        workers.shutdownNow();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double accumulator = 0.0;

        while (running.get()) {
            if (paused.get()) {
                lastTime = System.nanoTime();
                LockSupport.parkNanos(2_000_000L);
                continue;
            }

            long now = System.nanoTime();
            double frameTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            if (frameTime > 0.25) {
                frameTime = 0.25;
            }

            accumulator += frameTime;
            while (accumulator >= FIXED_STEP_SECONDS) {
                update(FIXED_STEP_SECONDS);
                accumulator -= FIXED_STEP_SECONDS;
            }

            LockSupport.parkNanos(500_000L);
        }
    }

    public void update(double deltaSeconds) {
        elapsedRealSeconds += deltaSeconds;
        elapsedSimulationHours += toSimulationHours(deltaSeconds);
        tick++;

        rumorManager.update(toSimulationHours(deltaSeconds), elapsedSimulationHours);
        for (Rumeur rumor : rumorManager.drainNewActivations()) {
            seedRumorOutbreak(rumor);
        }
        eventManager.update(toSimulationHours(deltaSeconds), this);

        parallelUpdateAgents(deltaSeconds);

        for (Bavard agent : agents) {
            agent.captureSnapshot();
        }
        spatialHashGrid.rebuild(agents);
    }

    public void addAgent(Bavard agent) {
        agent.attachEngine(this);
        agents.add(agent);
        spatialHashGrid.insert(agent);
    }

    public void removeAgent(Bavard agent) {
        agents.remove(agent);
        spatialHashGrid.remove(agent);
    }

    public List<Bavard> getAgentsSnapshot() {
        return new ArrayList<>(agents);
    }

    public EnumMap<ZoneManager.ZoneId, Integer> getPopulationByZone() {
        EnumMap<ZoneManager.ZoneId, Integer> counts = new EnumMap<>(ZoneManager.ZoneId.class);
        for (ZoneManager.ZoneId zoneId : ZoneManager.ZoneId.values()) {
            counts.put(zoneId, 0);
        }
        for (Bavard agent : agents) {
            counts.compute(agent.getZoneActuelle(), (zone, value) -> value == null ? 1 : value + 1);
        }
        return counts;
    }

    public EnumMap<EtatSEIRS, Integer> getPopulationByState() {
        EnumMap<EtatSEIRS, Integer> counts = new EnumMap<>(EtatSEIRS.class);
        for (EtatSEIRS state : EtatSEIRS.values()) {
            counts.put(state, 0);
        }
        for (Bavard agent : agents) {
            counts.compute(agent.getEtatSEIRS(), (state, value) -> value == null ? 1 : value + 1);
        }
        return counts;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public SpatialHashGrid getSpatialHashGrid() {
        return spatialHashGrid;
    }

    public RumorManager getRumorManager() {
        return rumorManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public long getTick() {
        return tick;
    }

    public double getElapsedRealSeconds() {
        return elapsedRealSeconds;
    }

    public double getElapsedSimulationHours() {
        return elapsedSimulationHours;
    }

    public double getHourOfDay() {
        double hour = elapsedSimulationHours % 24.0;
        return hour < 0.0 ? hour + 24.0 : hour;
    }

    public int getDayIndex() {
        return (int) Math.floor(elapsedSimulationHours / 24.0);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    public double toSimulationHours(double deltaSeconds) {
        return deltaSeconds * SIMULATION_HOURS_PER_REAL_SECOND * timeScale;
    }

    public void setTimeScale(double scale) {
        this.timeScale = Math.max(0.0001, Math.min(5.0, scale));
    }

    public double getTimeScale() {
        return timeScale;
    }

    public PlacesManager getPlacesManager() {
        return placesManager;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    private void bootstrapPopulation(int populationSize) {
        agents.clear();
        for (int i = 0; i < populationSize; i++) {
            ZoneManager.ZoneId homeZone = zoneManager.sampleHomeZone(random);
            ZoneManager.ZoneId workZone = zoneManager.pickWorkZone(homeZone, random);
            boolean pendulaire = switch (homeZone) {
                case HAMEAU -> random.nextDouble() < 0.88;
                case PETITE_VILLE -> random.nextDouble() < 0.72;
                case ZONE_VACANCES_MER -> random.nextDouble() < 0.34;
                case VILLE_PRINCIPALE -> random.nextDouble() < 0.16;
            };
            double sociability = clamp(0.20 + random.nextDouble() * 0.75, 0.0, 1.0);
            double travelAffinity = clamp(0.12 + random.nextDouble() * 0.82, 0.0, 1.0);
            double sanitaryCaution = clamp(0.08 + random.nextDouble() * 0.90, 0.0, 1.0);
            Vector2D spawn = zoneManager.randomPointInZone(homeZone, random);

            Bavard agent = new Bavard(
                String.format("Agent-%04d", i + 1),
                spawn,
                homeZone,
                workZone,
                sociability,
                travelAffinity,
                sanitaryCaution,
                pendulaire,
                random.nextLong()
            );
            agent.attachEngine(this);
            agents.add(agent);
        }

        // Assign POIs and build relations AFTER all agents are created
        for (Bavard agent : agents) {
            PlaceInteret domicilePOI = placesManager.nearestInZone(
                    agent.getSnapshotPosition(), agent.getZoneDomicile(), PlaceInteret.TypeLieu.DOMICILE);
            PlaceInteret travailPOI = agent.isTravailleurPendulaire()
                    ? placesManager.randomInZone(agent.getZoneTravail(), PlaceInteret.TypeLieu.TRAVAIL,
                            new java.util.SplittableRandom(random.nextLong()))
                    : null;
            java.util.Map<PlaceInteret.TypeLieu, PlaceInteret> favorites =
                    placesManager.assignFavorites(agent.getZoneDomicile(), agent.getTraitSociabilite(),
                            new java.util.SplittableRandom(random.nextLong()));
            agent.setPOIs(
                    domicilePOI != null ? domicilePOI.positionRef() : null,
                    travailPOI  != null ? travailPOI.positionRef()  : null,
                    favorites,
                    random.nextDouble() // credulite
            );
        }
        relationManager.build(new java.util.ArrayList<>(agents), new java.util.SplittableRandom(random.nextLong()));

        for (Bavard agent : agents) {
            agent.captureSnapshot();
        }
        spatialHashGrid.rebuild(agents);
    }

    private void parallelUpdateAgents(double deltaSeconds) {
        if (agents.isEmpty()) {
            return;
        }
        int workerCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        if (agents.size() < 256 || workerCount == 1) {
            for (Bavard agent : agents) {
                agent.updateLogic(deltaSeconds);
            }
            return;
        }

        int chunkSize = Math.max(48, (agents.size() + workerCount - 1) / workerCount);
        List<Future<?>> futures = new ArrayList<>();
        for (int start = 0; start < agents.size(); start += chunkSize) {
            final int from = start;
            final int to = Math.min(agents.size(), start + chunkSize);
            futures.add(workers.submit(() -> {
                for (int index = from; index < to; index++) {
                    agents.get(index).updateLogic(deltaSeconds);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Simulation interrompue", interruptedException);
            } catch (ExecutionException executionException) {
                throw new IllegalStateException("Erreur pendant la mise à jour parallèle", executionException.getCause());
            }
        }
    }

    private void seedRumorOutbreak(Rumeur rumor) {
        List<Bavard> candidates = new ArrayList<>();
        ZoneManager.ZoneId focusZone = rumor.getZoneAttractive() != null
            ? rumor.getZoneAttractive()
            : rumor.getZoneRepulsive() != null
                ? rumor.getZoneRepulsive()
                : ZoneManager.ZoneId.VILLE_PRINCIPALE;

        for (Bavard agent : agents) {
            if (agent.getZoneActuelle() == focusZone || agent.getZoneDomicile() == focusZone) {
                candidates.add(agent);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(agents);
        }
        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates, random);
        int seeds = Math.max(2, Math.min(candidates.size(), Math.max(3, agents.size() / 180)));
        for (int i = 0; i < seeds; i++) {
            Bavard agent = candidates.get(i);
            if (i == 0 || random.nextDouble() < 0.35) {
                agent.forceInfectious(rumor.mutate(new java.util.SplittableRandom(random.nextLong())));
            } else {
                agent.forceExpose(rumor.mutate(new java.util.SplittableRandom(random.nextLong())));
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class EngineThreadFactory implements ThreadFactory {
        private int index = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "seirs-worker-" + index++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
