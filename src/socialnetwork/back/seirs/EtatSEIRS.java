package socialnetwork.back.seirs;

public enum EtatSEIRS {
    SUSCEPTIBLE,
    EXPOSED,
    INFECTIOUS,
    RECOVERED;

    public boolean isContagieux() {
        return this == INFECTIOUS;
    }

    public boolean peutRecevoir() {
        return this == SUSCEPTIBLE;
    }

    public boolean estImmunise() {
        return this == RECOVERED;
    }
}
