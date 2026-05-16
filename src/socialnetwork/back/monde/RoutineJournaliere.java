package socialnetwork.back.monde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Routine journalière d'un bavard.
 *
 * Modélise une journée type (24 heures) sous forme de créneaux planifiés.
 * Le {@link GestionnaireDeplacements} consulte cette routine pour
 * décider où envoyer le bavard à chaque heure simulée.
 *
 * ─────────────────────────────────────────────────────────────────
 * STRUCTURE D'UNE JOURNÉE TYPIQUE :
 *
 *  0h – 7h   : DOMICILE (sommeil)
 *  7h – 8h   : DOMICILE (réveil, petit-déjeuner)
 *  8h – 12h  : TRAVAIL/ÉTUDE
 *  12h – 14h : PAUSE DÉJEUNER (café ou restaurant universitaire)
 *  14h – 18h : TRAVAIL/ÉTUDE
 *  18h – 20h : LOISIR ou DOMICILE
 *  20h – 22h : DOMICILE (dîner, détente)
 *  22h – 24h : DOMICILE (sommeil)
 *
 * Les week-ends modifient cette routine (pas de travail, plus de loisirs).
 * ─────────────────────────────────────────────────────────────────
 */
public final class RoutineJournaliere {

    // ─── Créneau horaire ─────────────────────────────────────────────────────

    /**
     * Un créneau dans la routine : plage horaire + destination.
     */
    public static final class Creneau {

        /** Heure de début inclusive (0–23). */
        public final int heureDebut;

        /** Heure de fin exclusive (1–24). */
        public final int heureFin;

        /** Lieu cible (null = rester au domicile). */
        public final Lieu lieu;

        /** Statut de présence associé. */
        public final StatutPresence statut;

        /** Description courte du créneau. */
        public final String label;

        /** Probabilité de vraiment aller au lieu [0,1] (ex: 0.7 = parfois sauté). */
        public final float probabilite;

        /** True si ce créneau ne s'applique qu'en semaine. */
        public final boolean semaineUniquement;

        /** True si ce créneau ne s'applique qu'en week-end. */
        public final boolean weekEndUniquement;

        public Creneau(int heureDebut, int heureFin, Lieu lieu,
                       StatutPresence statut, String label,
                       float probabilite,
                       boolean semaineUniquement, boolean weekEndUniquement) {
            this.heureDebut         = heureDebut;
            this.heureFin           = heureFin;
            this.lieu               = lieu;
            this.statut             = statut;
            this.label              = label;
            this.probabilite        = probabilite;
            this.semaineUniquement  = semaineUniquement;
            this.weekEndUniquement  = weekEndUniquement;
        }

        /** Constructeur simplifié (toute la semaine, probabilité 1). */
        public Creneau(int heureDebut, int heureFin, Lieu lieu, StatutPresence statut) {
            this(heureDebut, heureFin, lieu, statut, statut.label, 1.0f, false, false);
        }

        /** True si l'heure donnée est dans ce créneau. */
        public boolean contient(int heure) {
            if (heureDebut <= heureFin) {
                return heure >= heureDebut && heure < heureFin;
            } else {
                // Créneau nocturne chevauchant minuit
                return heure >= heureDebut || heure < heureFin;
            }
        }

        /** True si ce créneau s'applique aujourd'hui (compte tenu du jour de semaine). */
        public boolean sApplique(boolean estWeekEnd) {
            if (semaineUniquement && estWeekEnd) return false;
            if (weekEndUniquement && !estWeekEnd) return false;
            return true;
        }

        @Override
        public String toString() {
            return String.format("%02dh–%02dh : %s (%s)",
                heureDebut, heureFin,
                lieu != null ? lieu.getNom() : "Domicile",
                statut.label);
        }
    }

    // ─── Champs de RoutineJournaliere ────────────────────────────────────────

    private final Lieu domicile;
    private final List<Creneau> creneaux = new ArrayList<>();

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public RoutineJournaliere(Lieu domicile) {
        this.domicile = domicile;
        // Créneaux de base : la nuit et les transitions sont au domicile
        ajouterCreneauDomicile(0, 7);   // Nuit
        ajouterCreneauDomicile(22, 24); // Soirée tardive
    }

    // ─── Construction de la routine ──────────────────────────────────────────

    /** Ajoute un créneau au domicile (sommeil ou repos). */
    public RoutineJournaliere ajouterCreneauDomicile(int debut, int fin) {
        return ajouter(new Creneau(debut, fin, domicile, StatutPresence.A_DOMICILE));
    }

    /** Ajoute un créneau de travail/étude (jours de semaine uniquement). */
    public RoutineJournaliere ajouterTravail(Lieu lieu, int debut, int fin) {
        creneaux.add(new Creneau(debut, fin, lieu, StatutPresence.AU_TRAVAIL,
            "Travail – " + lieu.getNom(), 0.95f, true, false));
        return this;
    }

    /** Ajoute un créneau de loisir (avec probabilité réduite — pas tous les jours). */
    public RoutineJournaliere ajouterLoisir(Lieu lieu, int debut, int fin, float proba) {
        creneaux.add(new Creneau(debut, fin, lieu, StatutPresence.EN_LOISIR,
            "Loisir – " + lieu.getNom(), proba, false, false));
        return this;
    }

    /** Ajoute un créneau de loisir week-end uniquement. */
    public RoutineJournaliere ajouterLoisirWeekEnd(Lieu lieu, int debut, int fin) {
        creneaux.add(new Creneau(debut, fin, lieu, StatutPresence.EN_LOISIR,
            "Loisir W-E – " + lieu.getNom(), 0.85f, false, true));
        return this;
    }

    /** Ajoute un créneau de pause déjeuner. */
    public RoutineJournaliere ajouterPauses(Lieu lieuDejeuner) {
        // Pause déjeuner 12h-14h en semaine
        creneaux.add(new Creneau(12, 14, lieuDejeuner, StatutPresence.EN_PAUSE,
            "Déjeuner", 0.80f, true, false));
        return this;
    }

    /** Ajoute un créneau générique. */
    public RoutineJournaliere ajouter(Creneau c) {
        creneaux.add(c);
        return this;
    }

    // ─── Consultation de la routine ──────────────────────────────────────────

    /**
     * Renvoie le créneau actif à l'heure donnée et selon le jour de semaine.
     * Si plusieurs créneaux correspondent, le plus spécifique (probabilité max) l'emporte.
     * Si aucun créneau n'est trouvé, renvoie un créneau "domicile" par défaut.
     */
    public Creneau getCreneauActif(int heure, boolean estWeekEnd) {
        Creneau meilleur = null;
        float meilleureProba = -1f;

        for (Creneau c : creneaux) {
            if (!c.sApplique(estWeekEnd)) continue;
            if (!c.contient(heure)) continue;
            if (c.probabilite > meilleureProba) {
                meilleureProba = c.probabilite;
                meilleur = c;
            }
        }

        if (meilleur == null) {
            // Créneau par défaut : domicile
            return new Creneau(heure, heure + 1, domicile, StatutPresence.A_DOMICILE);
        }
        return meilleur;
    }

    /**
     * Renvoie le lieu prévu pour l'heure et le jour donnés.
     * La décision probabiliste est prise ici (un tirage au sort détermine
     * si le créneau est vraiment suivi).
     */
    public Lieu getLieuPlanifie(int heure, boolean estWeekEnd, java.util.Random rng) {
        Creneau c = getCreneauActif(heure, estWeekEnd);
        if (rng.nextFloat() <= c.probabilite) {
            return c.lieu;
        }
        // Si le créneau n'est pas suivi, le bavard reste chez lui
        return domicile;
    }

    /**
     * Renvoie tous les lieux fréquentés dans la semaine (pour initialiser les amis).
     */
    public List<Lieu> getTousLieuxFréquentés() {
        List<Lieu> lieux = new ArrayList<>();
        for (Creneau c : creneaux) {
            if (c.lieu != null && !lieux.contains(c.lieu)) {
                lieux.add(c.lieu);
            }
        }
        return lieux;
    }

    /** Liste des créneaux de la routine. */
    public List<Creneau> getCreneaux() {
        return Collections.unmodifiableList(creneaux);
    }

    public Lieu getDomicile() { return domicile; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Routine journalière :\n");
        List<Creneau> sorted = new ArrayList<>(creneaux);
        sorted.sort(java.util.Comparator.comparingInt(c -> c.heureDebut));
        for (Creneau c : sorted) sb.append("  ").append(c).append("\n");
        return sb.toString();
    }
}
