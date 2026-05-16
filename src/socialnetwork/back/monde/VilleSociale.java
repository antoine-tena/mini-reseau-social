package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;

import java.util.*;

/**
 * Conteneur principal de la simulation de ville.
 *
 * VilleSociale réunit :
 *  - Un réseau de bavards (Reseau)
 *  - Des lieux physiques (bureaux, cafés, parcs…)
 *  - Des relations sociales enrichies (TypeRelation + confiance)
 *  - Les routines journalières de chaque bavard
 *  - L'état de vacances de chaque bavard
 *  - Le statut de présence courant
 *  - Les traits de caractère
 *
 * La ville est le point d'entrée central lu par :
 *  - {@link GestionnaireDeplacements} (pour les déplacements)
 *  - {@link socialnetwork.front.carte.CarteVilleCanvas} (pour le rendu)
 */
public class VilleSociale {

    // ─── Données de base ─────────────────────────────────────────────────────

    private final String nom;
    private final Reseau reseau;

    // ─── Lieux ───────────────────────────────────────────────────────────────

    /** Tous les lieux de la ville. */
    private final List<Lieu> lieux = new ArrayList<>();

    /** Index nom → Lieu pour accès rapide. */
    private final Map<String, Lieu> lieuxParNom = new LinkedHashMap<>();

    // ─── Relations ───────────────────────────────────────────────────────────

    /** Relations sociales entre bavards. */
    private final List<RelationSociale> relations = new ArrayList<>();

    // ─── Données par bavard ───────────────────────────────────────────────────

    private final Map<Bavard, RoutineJournaliere> routines    = new LinkedHashMap<>();
    private final Map<Bavard, Vacances>           vacancesMap = new LinkedHashMap<>();
    private final Map<Bavard, StatutPresence>     statutsMap  = new LinkedHashMap<>();
    private final Map<Bavard, TraitCaractere>     traitsMap   = new LinkedHashMap<>();

    /** Domicile de chaque bavard (pour accès rapide). */
    private final Map<Bavard, Lieu> domiciles = new LinkedHashMap<>();

    /** Âge de chaque bavard. */
    private final Map<Bavard, Integer> ages = new LinkedHashMap<>();

    /** Profession / description courte de chaque bavard. */
    private final Map<Bavard, String> professions = new LinkedHashMap<>();

    // ─── Constructeur ────────────────────────────────────────────────────────

    public VilleSociale(String nom) {
        this.nom    = nom;
        this.reseau = new Reseau();
    }

    public VilleSociale(String nom, Reseau reseau) {
        this.nom    = nom;
        this.reseau = reseau;
    }

    // ─── Gestion des bavards ─────────────────────────────────────────────────

    /**
     * Ajoute un bavard dans la ville et initialise ses données de base.
     */
    public void ajouterBavard(Bavard bavard, Lieu domicile, int age,
                               String profession, TraitCaractere trait) {
        reseau.ajouterBavard(bavard);
        domiciles.put(bavard, domicile);
        ages.put(bavard, age);
        professions.put(bavard, profession);
        traitsMap.put(bavard, trait);
        statutsMap.put(bavard, StatutPresence.A_DOMICILE);
    }

    /** Version simplifiée. */
    public void ajouterBavard(Bavard bavard, Lieu domicile) {
        ajouterBavard(bavard, domicile, 30, "Citoyen", TraitCaractere.CONVENTIONNEL);
    }

    // ─── Gestion des lieux ───────────────────────────────────────────────────

    public void ajouterLieu(Lieu lieu) {
        lieux.add(lieu);
        lieuxParNom.put(lieu.getNom(), lieu);
    }

    public Optional<Lieu> getLieuParNom(String nom) {
        return Optional.ofNullable(lieuxParNom.get(nom));
    }

    public List<Lieu> getLieux() {
        return Collections.unmodifiableList(lieux);
    }

    public List<Lieu> getLieuxDeType(TypeLieu type) {
        List<Lieu> resultat = new ArrayList<>();
        for (Lieu l : lieux) {
            if (l.getType() == type) resultat.add(l);
        }
        return resultat;
    }

    // ─── Gestion des relations ───────────────────────────────────────────────

    /**
     * Crée une relation sociale et connecte automatiquement les deux bavards dans le réseau.
     */
    public RelationSociale ajouterRelation(Bavard a, Bavard b, TypeRelation type) {
        RelationSociale rel = new RelationSociale(a, b, type);
        relations.add(rel);
        reseau.connecter(a, b);  // connexion dans le réseau de messages
        return rel;
    }

    /**
     * Renvoie la relation (ou null) entre deux bavards.
     */
    public RelationSociale getRelationEntre(Bavard a, Bavard b) {
        for (RelationSociale r : relations) {
            if (r.concerne(a, b)) return r;
        }
        return null;
    }

    /** Toutes les relations impliquant un bavard donné. */
    public List<RelationSociale> getRelationsDe(Bavard bavard) {
        List<RelationSociale> res = new ArrayList<>();
        for (RelationSociale r : relations) {
            if (r.getSource().equals(bavard) || r.getCible().equals(bavard)) res.add(r);
        }
        return res;
    }

    public List<RelationSociale> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    // ─── Routines ────────────────────────────────────────────────────────────

    public void setRoutine(Bavard b, RoutineJournaliere routine) {
        routines.put(b, routine);
    }

    public RoutineJournaliere getRoutine(Bavard b) {
        return routines.get(b);
    }

    // ─── Vacances ────────────────────────────────────────────────────────────

    public void setVacances(Bavard b, Vacances v) {
        vacancesMap.put(b, v);
    }

    public Vacances getVacances(Bavard b) {
        return vacancesMap.get(b);
    }

    public boolean estEnVacances(Bavard b, int jourSimule) {
        Vacances v = vacancesMap.get(b);
        return v != null && v.estActifAuJour(jourSimule);
    }

    // ─── Statuts ─────────────────────────────────────────────────────────────

    public void setStatut(Bavard b, StatutPresence statut) {
        statutsMap.put(b, statut);
    }

    public StatutPresence getStatut(Bavard b) {
        return statutsMap.getOrDefault(b, StatutPresence.A_DOMICILE);
    }

    // ─── Traits ──────────────────────────────────────────────────────────────

    public void setTrait(Bavard b, TraitCaractere trait) {
        traitsMap.put(b, trait);
    }

    public TraitCaractere getTrait(Bavard b) {
        return traitsMap.getOrDefault(b, TraitCaractere.CONVENTIONNEL);
    }

    // ─── Lieu actuel ─────────────────────────────────────────────────────────

    /**
     * Renvoie où se trouve le bavard à l'heure simulée donnée.
     * Tient compte des vacances (le bavard est à sa destination si en vacances).
     *
     * @param bavard       le bavard à localiser
     * @param heureCourante heure simulée 0–23
     * @param jourSimule   numéro du jour simulé (pour les vacances)
     * @param estWeekEnd   vrai si c'est un week-end
     * @param rng          générateur aléatoire (pour la probabilité des créneaux)
     * @return le Lieu prévu, ou le domicile par défaut
     */
    public Lieu getLieuActuelDe(Bavard bavard, int heureCourante,
                                 int jourSimule, boolean estWeekEnd,
                                 java.util.Random rng) {
        // En vacances ?
        if (estEnVacances(bavard, jourSimule)) {
            Vacances v = getVacances(bavard);
            return v.getDestination();
        }

        RoutineJournaliere routine = routines.get(bavard);
        if (routine == null) {
            return getDomicile(bavard);
        }
        return routine.getLieuPlanifie(heureCourante, estWeekEnd, rng);
    }

    // ─── Accesseurs divers ────────────────────────────────────────────────────

    public Lieu getDomicile(Bavard b) {
        return domiciles.getOrDefault(b, null);
    }

    public int getAge(Bavard b) {
        return ages.getOrDefault(b, 0);
    }

    public String getProfession(Bavard b) {
        return professions.getOrDefault(b, "");
    }

    public Reseau getReseau() { return reseau; }

    public String getNom() { return nom; }

    public int getNbBavards() { return reseau.getBavards().size(); }

    public int getNbLieux() { return lieux.size(); }

    public int getNbRelations() { return relations.size(); }

    // ─── Statistiques ────────────────────────────────────────────────────────

    /** Bavards actuellement dans un lieu donné (selon le statut simulé). */
    public List<Bavard> getBavardsDansLieu(Lieu lieu) {
        return lieu.getOccupants();
    }

    /** Lieu avec le plus d'occupants actuels. */
    public Lieu getLieuLePlusFrequente() {
        return lieux.stream()
            .max(Comparator.comparingInt(Lieu::getNbOccupants))
            .orElse(null);
    }

    // ─── Debug ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "VilleSociale{" + nom + ", "
             + getNbBavards() + " bavards, "
             + getNbLieux() + " lieux, "
             + getNbRelations() + " relations}";
    }
}
