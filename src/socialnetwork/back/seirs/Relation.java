package socialnetwork.back.seirs;

/**
 * An oriented social link between two agents.
 */
public final class Relation {

    public enum TypeRelation {
        AMI       ("Ami(e)"),
        FAMILLE   ("Famille"),
        AMOUREUX  ("Amoureux/se"),
        COLLÈGUE  ("Collègue");

        public final String label;
        TypeRelation(String label) { this.label = label; }
    }

    private final long agentIdA;
    private final long agentIdB;
    private final TypeRelation type;
    /** Strength of the bond, [0..1]. Affects communication probability. */
    private final float strength;

    public Relation(long agentIdA, long agentIdB, TypeRelation type, float strength) {
        this.agentIdA = agentIdA;
        this.agentIdB = agentIdB;
        this.type     = type;
        this.strength = Math.max(0f, Math.min(1f, strength));
    }

    public long          getAgentIdA()  { return agentIdA; }
    public long          getAgentIdB()  { return agentIdB; }
    public TypeRelation  getType()      { return type; }
    public float         getStrength()  { return strength; }

    /** Returns the partner's id given one endpoint's id. */
    public long getOtherId(long myId) {
        return myId == agentIdA ? agentIdB : agentIdA;
    }

    @Override
    public String toString() {
        return type.label + " (" + String.format("%.0f%%", strength * 100) + ")";
    }
}
