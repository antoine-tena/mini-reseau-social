package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;

/**
 * Relation sociale entre deux bavards.
 *
 * Une RelationSociale relie deux bavards avec un type de relation et
 * un niveau de confiance (potentiellement différent de la valeur par défaut
 * du type). Elle peut évoluer dans le temps (le niveau de confiance varie
 * selon les interactions).
 */
public final class RelationSociale {

    // ─── Champs ─────────────────────────────────────────────────────────────

    private final Bavard        source;
    private final Bavard        cible;
    private final TypeRelation  type;

    /** Confiance effective [0,1], peut dériver de la valeur de base du type. */
    private float confiance;

    /** Date de création de la relation (en ticks simulés). */
    private final long tickCreation;

    /** Nombre d'interactions passées (messages échangés via ce lien). */
    private int nbInteractions;

    /** True si la relation est actuellement "active" (non gelée / brisée). */
    private boolean active;

    /** Description optionnelle (anecdote, histoire de la relation). */
    private String note;

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public RelationSociale(Bavard source, Bavard cible,
                           TypeRelation type, long tickCreation) {
        this.source        = source;
        this.cible         = cible;
        this.type          = type;
        this.confiance     = type.confianceBase;
        this.tickCreation  = tickCreation;
        this.nbInteractions = 0;
        this.active        = true;
        this.note          = "";
    }

    public RelationSociale(Bavard source, Bavard cible, TypeRelation type) {
        this(source, cible, type, 0L);
    }

    // ─── Évolution de la relation ────────────────────────────────────────────

    /**
     * Enregistre une interaction positive (message bien reçu, échange chaleureux).
     * Augmente légèrement la confiance.
     */
    public void interactionPositive() {
        nbInteractions++;
        confiance = Math.min(1.0f, confiance + 0.005f);
    }

    /**
     * Enregistre une interaction négative (conflit, message toxique).
     * Réduit la confiance.
     */
    public void interactionNegative() {
        nbInteractions++;
        confiance = Math.max(0.0f, confiance - 0.02f);
    }

    /**
     * Enregistre une interaction neutre.
     */
    public void interaction() {
        nbInteractions++;
    }

    /**
     * Dégrade légèrement la confiance si le lien n'est pas entretenu
     * (appelée périodiquement par le gestionnaire).
     */
    public void decroissanceSansContact() {
        // Seulement pour les relations non familiales
        if (type != TypeRelation.PARENT_ENFANT
         && type != TypeRelation.ENFANT_PARENT
         && type != TypeRelation.FRERE_SOEUR
         && type != TypeRelation.EPOUX
         && type != TypeRelation.COUPLE) {
            confiance = Math.max(type.confianceBase * 0.5f, confiance - 0.001f);
        }
    }

    // ─── Accesseurs ─────────────────────────────────────────────────────────

    public Bavard       getSource()          { return source; }
    public Bavard       getCible()           { return cible; }
    public TypeRelation getType()            { return type; }
    public float        getConfiance()       { return confiance; }
    public long         getTickCreation()    { return tickCreation; }
    public int          getNbInteractions()  { return nbInteractions; }
    public boolean      isActive()           { return active; }
    public String       getNote()            { return note; }

    public void setActive(boolean active)    { this.active = active; }
    public void setNote(String note)         { this.note  = note; }
    public void setConfiance(float c)        { this.confiance = Math.max(0, Math.min(1, c)); }

    /** Modificateur de bienveillance lors d'un message sur ce lien. */
    public float modificateurBienveillance() {
        return type.modificateurBienveillance() * confiance;
    }

    /** True si cette relation implique la cohabitation. */
    public boolean estCohabitation() {
        return type.adresseCommune;
    }

    /** Vrai si source == a ET cible == b, ou vice-versa si bidirectionnel. */
    public boolean concerne(Bavard a, Bavard b) {
        if (source.equals(a) && cible.equals(b)) return true;
        if (type.bidirectionnel && source.equals(b) && cible.equals(a)) return true;
        return false;
    }

    /** Renvoie le bavard "autre" dans la relation (celui qui n'est pas le bavard fourni). */
    public Bavard getAutre(Bavard b) {
        if (source.equals(b)) return cible;
        if (cible.equals(b))  return source;
        return null;
    }

    @Override
    public String toString() {
        return source.getNom() + " " + type.icone + " " + cible.getNom()
             + " (confiance=" + String.format("%.2f", confiance) + ")";
    }
}
