package socialnetwork.back.seirs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and stores the social graph for all agents.
 * Relations are created once at bootstrap and never modified.
 */
public final class RelationManager {

    /** agentId → list of relations that include this agent */
    private final ConcurrentHashMap<Long, List<Relation>> graph = new ConcurrentHashMap<>();

    /** Reverse-lookup: agentId → list of (partnerId, Relation) */
    private final ConcurrentHashMap<Long, List<Relation>> graph2 = new ConcurrentHashMap<>();

    /**
     * Build the social graph from the bootstrapped agent list.
     * Called once from SimulationEngine after all agents are created.
     */
    public void build(List<Bavard> agents, SplittableRandom rng) {
        for (Bavard b : agents) {
            graph.put(b.getId(), new ArrayList<>());
        }

        buildFamilyClusters(agents, rng);
        buildWorkClusters(agents, rng);
        buildFriendships(agents, rng);
        buildRomanticPairs(agents, rng);
    }

    // ── FAMILY CLUSTERS ──────────────────────────────────────────────────────
    // Agents living in the same home zone and close in space become family.
    private void buildFamilyClusters(List<Bavard> agents, SplittableRandom rng) {
        // group by zone
        Map<ZoneManager.ZoneId, List<Bavard>> byZone = new EnumMap<>(ZoneManager.ZoneId.class);
        for (Bavard b : agents) {
            byZone.computeIfAbsent(b.getZoneDomicile(), k -> new ArrayList<>()).add(b);
        }
        for (List<Bavard> zoneAgents : byZone.values()) {
            List<Bavard> pool = new ArrayList<>(zoneAgents);
            Collections.shuffle(pool, new Random(rng.nextLong()));
            int idx = 0;
            while (idx < pool.size()) {
                int size = 2 + rng.nextInt(4); // family of 2-5
                List<Bavard> family = pool.subList(idx, Math.min(idx + size, pool.size()));
                for (int i = 0; i < family.size(); i++) {
                    for (int j = i + 1; j < family.size(); j++) {
                        link(family.get(i), family.get(j), Relation.TypeRelation.FAMILLE,
                             0.7f + rng.nextFloat() * 0.3f);
                    }
                }
                idx += size;
            }
        }
    }

    // ── WORK CLUSTERS ────────────────────────────────────────────────────────
    // Agents sharing the same work zone (commuters) become colleagues.
    private void buildWorkClusters(List<Bavard> agents, SplittableRandom rng) {
        Map<ZoneManager.ZoneId, List<Bavard>> byWorkZone = new EnumMap<>(ZoneManager.ZoneId.class);
        for (Bavard b : agents) {
            if (b.isTravailleurPendulaire() && b.getZoneTravail() != null) {
                byWorkZone.computeIfAbsent(b.getZoneTravail(), k -> new ArrayList<>()).add(b);
            }
        }
        for (List<Bavard> workers : byWorkZone.values()) {
            // form work groups of 5-15
            List<Bavard> pool = new ArrayList<>(workers);
            Collections.shuffle(pool, new Random(rng.nextLong()));
            int idx = 0;
            while (idx < pool.size()) {
                int size = 5 + rng.nextInt(11);
                List<Bavard> team = pool.subList(idx, Math.min(idx + size, pool.size()));
                for (int i = 0; i < team.size(); i++) {
                    // each worker linked to ~3 colleagues
                    int conns = Math.min(3, team.size() - 1);
                    for (int j = i + 1; j <= i + conns && j < team.size(); j++) {
                        link(team.get(i), team.get(j), Relation.TypeRelation.COLLÈGUE,
                             0.4f + rng.nextFloat() * 0.4f);
                    }
                }
                idx += size;
            }
        }
    }

    // ── FRIENDSHIPS ──────────────────────────────────────────────────────────
    // Random friendships weighted by sociability trait.
    private void buildFriendships(List<Bavard> agents, SplittableRandom rng) {
        for (Bavard b : agents) {
            int targetFriends = 1 + (int)(b.getTraitSociabilite() * 5.0);
            List<Bavard> candidates = new ArrayList<>(agents);
            Collections.shuffle(candidates, new Random(rng.nextLong()));
            int formed = 0;
            for (Bavard other : candidates) {
                if (other.getId() == b.getId()) continue;
                if (formed >= targetFriends) break;
                if (hasRelation(b.getId(), other.getId())) continue;
                float strength = 0.3f + rng.nextFloat() * 0.5f;
                link(b, other, Relation.TypeRelation.AMI, strength);
                formed++;
            }
        }
    }

    // ── ROMANTIC PAIRS ───────────────────────────────────────────────────────
    // ~15% of agents have a romantic partner.
    private void buildRomanticPairs(List<Bavard> agents, SplittableRandom rng) {
        Set<Long> paired = new HashSet<>();
        List<Bavard> pool = new ArrayList<>(agents);
        Collections.shuffle(pool, new Random(rng.nextLong()));
        for (Bavard b : pool) {
            if (paired.contains(b.getId())) continue;
            if (rng.nextDouble() > 0.15) continue;
            // find a partner
            for (Bavard other : pool) {
                if (other.getId() == b.getId()) continue;
                if (paired.contains(other.getId())) continue;
                paired.add(b.getId());
                paired.add(other.getId());
                link(b, other, Relation.TypeRelation.AMOUREUX, 0.8f + rng.nextFloat() * 0.2f);
                break;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void link(Bavard a, Bavard b, Relation.TypeRelation type, float strength) {
        Relation r = new Relation(a.getId(), b.getId(), type, strength);
        graph.get(a.getId()).add(r);
        graph.get(b.getId()).add(r);
    }

    private boolean hasRelation(long idA, long idB) {
        List<Relation> rels = graph.get(idA);
        if (rels == null) return false;
        for (Relation r : rels) {
            if (r.getOtherId(idA) == idB) return true;
        }
        return false;
    }

    /** Returns all relations for the given agent id (never null). */
    public List<Relation> getRelations(long agentId) {
        List<Relation> rels = graph.get(agentId);
        return rels != null ? Collections.unmodifiableList(rels) : Collections.emptyList();
    }

    /** Returns ids of all agents directly linked to this agent. */
    public List<Long> getRelatedIds(long agentId) {
        List<Relation> rels = graph.get(agentId);
        if (rels == null || rels.isEmpty()) return Collections.emptyList();
        List<Long> ids = new ArrayList<>(rels.size());
        for (Relation r : rels) ids.add(r.getOtherId(agentId));
        return ids;
    }
}
