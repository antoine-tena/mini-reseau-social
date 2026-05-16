package socialnetwork.back.monde;

/**
 * Adresse physique d'un bavard ou d'un lieu.
 */
public final class Adresse {

    // ─── Champs ─────────────────────────────────────────────────────────────

    private final int    numero;
    private final String voie;      // type + nom de la voie
    private final String complement; // Apt, Bât, etc. (peut être vide)
    private final String codePostal;
    private final String ville;
    private final String pays;

    /** Coordonnées sur la carte de la ville (0–1200 × 0–900). */
    private final double xCarte;
    private final double yCarte;

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public Adresse(int numero, String voie, String complement,
                   String codePostal, String ville, String pays,
                   double xCarte, double yCarte) {
        this.numero      = numero;
        this.voie        = voie;
        this.complement  = complement == null ? "" : complement;
        this.codePostal  = codePostal;
        this.ville       = ville;
        this.pays        = pays;
        this.xCarte      = xCarte;
        this.yCarte      = yCarte;
    }

    /** Constructeur simplifié sans coordonnées (sera dérivé du lieu). */
    public Adresse(int numero, String voie, String codePostal, String ville) {
        this(numero, voie, "", codePostal, ville, "France", 0, 0);
    }

    /** Constructeur complet sans complément. */
    public Adresse(int numero, String voie, String codePostal, String ville,
                   double xCarte, double yCarte) {
        this(numero, voie, "", codePostal, ville, "France", xCarte, yCarte);
    }

    // ─── Accesseurs ─────────────────────────────────────────────────────────

    public int    getNumero()      { return numero; }
    public String getVoie()        { return voie; }
    public String getComplement()  { return complement; }
    public String getCodePostal()  { return codePostal; }
    public String getVille()       { return ville; }
    public String getPays()        { return pays; }
    public double getXCarte()      { return xCarte; }
    public double getYCarte()      { return yCarte; }

    /** Ligne d'adresse complète formatée. */
    public String ligneComplete() {
        StringBuilder sb = new StringBuilder();
        sb.append(numero).append(" ").append(voie);
        if (!complement.isEmpty()) sb.append(", ").append(complement);
        sb.append(", ").append(codePostal).append(" ").append(ville);
        if (!"France".equals(pays)) sb.append(", ").append(pays);
        return sb.toString();
    }

    /** Ligne courte (numéro + voie). */
    public String ligneCourte() {
        return numero + " " + voie;
    }

    @Override
    public String toString() { return ligneComplete(); }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Adresse other)) return false;
        return numero == other.numero
            && voie.equals(other.voie)
            && codePostal.equals(other.codePostal);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * numero + voie.hashCode()) + codePostal.hashCode();
    }
}
