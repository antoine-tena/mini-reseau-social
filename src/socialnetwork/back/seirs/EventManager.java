package socialnetwork.back.seirs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EventManager {

    private static final String FESTIVAL_MER = "Festival de la Mer";

    private static final class LiveEvent {
        private final String name;
        private final ZoneManager.ZoneId targetZone;
        private final Vector2D hotspot;
        private final double populationRatio;
        private final Set<Long> participants;
        private double remainingHours;

        private LiveEvent(
            String name,
            ZoneManager.ZoneId targetZone,
            Vector2D hotspot,
            double populationRatio,
            double remainingHours,
            Set<Long> participants
        ) {
            this.name = name;
            this.targetZone = targetZone;
            this.hotspot = hotspot;
            this.populationRatio = populationRatio;
            this.remainingHours = remainingHours;
            this.participants = participants;
        }
    }

    private final ConcurrentHashMap<String, LiveEvent> activeEvents = new ConcurrentHashMap<>();
    private final Random random;

    private double festivalCooldownHours = 8.0;

    public EventManager(Random random) {
        this.random = random;
    }

    public void update(double deltaHours, SimulationEngine engine) {
        festivalCooldownHours = Math.max(0.0, festivalCooldownHours - deltaHours);

        List<String> finishedEvents = new ArrayList<>();
        for (LiveEvent event : activeEvents.values()) {
            event.remainingHours -= deltaHours;
            if (event.remainingHours <= 0.0) {
                finishedEvents.add(event.name);
            }
        }

        for (String eventName : finishedEvents) {
            LiveEvent finished = activeEvents.remove(eventName);
            if (finished == null) {
                continue;
            }
            for (Bavard agent : engine.getAgentsSnapshot()) {
                if (finished.participants.contains(agent.getId())) {
                    agent.clearEventMigration(finished.name);
                }
            }
            if (FESTIVAL_MER.equals(eventName)) {
                festivalCooldownHours = 18.0 + random.nextDouble(16.0);
            }
        }

        if (shouldStartSeaFestival(engine)) {
            activateSeaFestival(engine);
        }
    }

    public Vector2D computeEventForce(Bavard agent, ZoneManager zoneManager) {
        Vector2D force = new Vector2D();
        for (LiveEvent event : activeEvents.values()) {
            if (!event.participants.contains(agent.getId())) {
                continue;
            }
            if (agent.getZoneActuelle() == event.targetZone) {
                Vector2D direction = event.hotspot.subtract(agent.snapshotPositionRef());
                double distance = Math.max(35.0, direction.length());
                if (distance > 1.0e-6) {
                    double intensity = (0.9 + event.populationRatio) * 180.0 / distance;
                    force.addLocal(direction.normalized().multiply(intensity));
                }
            } else if (agent.getEtatFSM() != Bavard.EtatFSM.EN_TRANSIT) {
                Vector2D zoneCenter = zoneManager.getZone(event.targetZone).getCentre();
                force.addLocal(zoneCenter.subtract(agent.snapshotPositionRef()).normalized().multiply(1.2));
            }
        }
        return force;
    }

    public List<String> getActiveEventNames() {
        return new ArrayList<>(activeEvents.keySet());
    }

    public boolean hasActiveEvent(String name) {
        return activeEvents.containsKey(name);
    }

    private boolean shouldStartSeaFestival(SimulationEngine engine) {
        if (activeEvents.containsKey(FESTIVAL_MER)) {
            return false;
        }
        if (festivalCooldownHours > 0.0) {
            return false;
        }
        double summerLevel = engine.getRumorManager().getSummerLevel();
        double hour = engine.getHourOfDay();
        double hourBias = (hour >= 11.0 && hour <= 20.0) ? 1.35 : 0.55;
        double probability = (0.04 + summerLevel * 0.16) * hourBias;
        return random.nextDouble() < probability;
    }

    private void activateSeaFestival(SimulationEngine engine) {
        List<Bavard> agents = new ArrayList<>(engine.getAgentsSnapshot());
        if (agents.isEmpty()) {
            return;
        }

        Collections.shuffle(agents, random);
        int targetCount = Math.max(1, (int) Math.ceil(agents.size() * 0.30));
        Set<Long> participants = ConcurrentHashMap.newKeySet(targetCount);
        double durationHours = 5.0 + random.nextDouble(9.0);

        for (int i = 0; i < targetCount && i < agents.size(); i++) {
            Bavard agent = agents.get(i);
            participants.add(agent.getId());
            agent.assignEventMigration(FESTIVAL_MER, ZoneManager.ZoneId.ZONE_VACANCES_MER, durationHours);
        }

        LiveEvent event = new LiveEvent(
            FESTIVAL_MER,
            ZoneManager.ZoneId.ZONE_VACANCES_MER,
            engine.getZoneManager().hotspotForFestivalMer(),
            0.30,
            durationHours,
            participants
        );
        activeEvents.put(FESTIVAL_MER, event);
        festivalCooldownHours = 20.0 + random.nextDouble(10.0);
    }
}
