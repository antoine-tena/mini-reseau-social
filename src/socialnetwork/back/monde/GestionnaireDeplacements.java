package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Gestionnaire des déplacements de bavards dans la ville simulée.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  FONCTIONNEMENT
 * ══════════════════════════════════════════════════════════════════════
 *
 * Le gestionnaire maintient une "horloge simulée" dont la vitesse est
 * contrôlable. À chaque tick d'un Timer Swing (60fps), l'horloge avance
 * d'un nombre de millisecondes simulées proportionnel à la vitesse.
 *
 * Une heure simulée dure par défaut 5 secondes réelles (vitesse 1×).
 * Vitesse 10× → 5 secondes réelles = 10 heures simulées.
 *
 * À chaque changement d'heure, le gestionnaire :
 *  1. Calcule le lieu planifié pour chaque bavard (RoutineJournaliere)
 *  2. Si le bavard doit changer de lieu, crée un Deplacement
 *  3. Met à jour les lieux (libèrer, accueillir)
 *  4. Met à jour le StatutPresence
 *
 * La position interpolée de chaque bavard est accessible via
 * {@link #getPositionCarte(Bavard)} pour le rendu du canvas.
 * ══════════════════════════════════════════════════════════════════════
 */
public class GestionnaireDeplacements {

    // ─── Constantes ──────────────────────────────────────────────────────────

    /** Durée d'une heure simulée en millisecondes réelles (à vitesse 1×). */
    private static final long MS_REELS_PAR_HEURE_SIM = 5_000L;

    /** Taux de rafraîchissement du Timer Swing (ms). */
    private static final int TICK_MS = 16; // ~60fps

    /** Nombre d'heures simulées par jour. */
    private static final int HEURES_PAR_JOUR = 24;

    // ─── État de la simulation ────────────────────────────────────────────────

    private final VilleSociale ville;
    private final Random       rng = new Random(42L);

    /** Horloge simulée : ms simulées depuis le début. */
    private long msSim       = 8 * 3_600_000L; // démarre à 8h du matin
    private int  heureSim    = 8;
    private int  jourSim     = 0;
    private boolean estWeekEnd = false;

    /** Vitesse de la simulation (multiplicateur). */
    private double vitesse   = 1.0;

    /** True si la simulation est en cours. */
    private boolean enMarche = false;

    // ─── Déplacements en cours ────────────────────────────────────────────────

    /** Déplacement actif par bavard (null si le bavard est sur place). */
    private final Map<Bavard, Deplacement> deplacementsActifs = new ConcurrentHashMap<>();

    /** Lieu courant logique (destination atteinte) de chaque bavard. */
    private final Map<Bavard, Lieu> lieuCourant = new ConcurrentHashMap<>();

    // ─── Callbacks ────────────────────────────────────────────────────────────

    /** Appelé à chaque tick (fréquence 60fps). */
    private final List<Runnable>            listenersTick      = new CopyOnWriteArrayList<>();

    /** Appelé à chaque changement d'heure simulée. */
    private final List<Consumer<Integer>>   listenersHeure     = new CopyOnWriteArrayList<>();

    /** Appelé quand un bavard arrive à sa destination. */
    private final List<Consumer<Bavard>>    listenersArrivee   = new CopyOnWriteArrayList<>();

    // ─── Timer Swing ─────────────────────────────────────────────────────────

    private final Timer timer;

    /** Instant réel du dernier tick (pour calculer le delta). */
    private long lastTickReel = System.currentTimeMillis();

    // ─── Constructeur ────────────────────────────────────────────────────────

    public GestionnaireDeplacements(VilleSociale ville) {
        this.ville = ville;

        // Placer tous les bavards à leur domicile au départ
        for (Bavard b : ville.getReseau().getBavards()) {
            Lieu domicile = ville.getDomicile(b);
            if (domicile != null) {
                lieuCourant.put(b, domicile);
                domicile.accueillir(b);
            }
        }

        // Créer le Timer Swing
        this.timer = new Timer(TICK_MS, e -> tick());
        this.timer.setCoalesce(true);
    }

    // ─── Contrôle de la simulation ───────────────────────────────────────────

    public void demarrer() {
        if (!enMarche) {
            lastTickReel = System.currentTimeMillis();
            timer.start();
            enMarche = true;
        }
    }

    public void pause() {
        if (enMarche) {
            timer.stop();
            enMarche = false;
        }
    }

    public void reprendre() {
        if (!enMarche) {
            lastTickReel = System.currentTimeMillis();
            timer.start();
            enMarche = true;
        }
    }

    public void arreter() {
        timer.stop();
        enMarche = false;
        msSim    = 8 * 3_600_000L;
        heureSim = 8;
        jourSim  = 0;
        reinitialiserPositions();
    }

    /** Remet tous les bavards à leur domicile. */
    public void reinitialiserPositions() {
        // Vider tous les lieux
        for (Lieu l : ville.getLieux()) {
            l.vider();
        }
        deplacementsActifs.clear();
        lieuCourant.clear();

        // Replacer au domicile
        for (Bavard b : ville.getReseau().getBavards()) {
            Lieu domicile = ville.getDomicile(b);
            if (domicile != null) {
                lieuCourant.put(b, domicile);
                domicile.accueillir(b);
                ville.setStatut(b, StatutPresence.A_DOMICILE);
            }
        }
    }

    // ─── Logique du tick ─────────────────────────────────────────────────────

    private void tick() {
        long maintenant    = System.currentTimeMillis();
        long deltaReel     = maintenant - lastTickReel;
        lastTickReel       = maintenant;

        // Avancer l'horloge simulée
        long deltaSim = (long)(deltaReel * vitesse
                               * (3_600_000.0 / MS_REELS_PAR_HEURE_SIM));
        msSim += deltaSim;

        // Calculer heure et jour courants
        int nouvelleHeure = (int)((msSim / 3_600_000L) % HEURES_PAR_JOUR);
        int nouveauJour   = (int)(msSim / (3_600_000L * HEURES_PAR_JOUR));

        // Changement de jour
        if (nouveauJour > jourSim) {
            jourSim    = nouveauJour;
            estWeekEnd = (jourSim % 7 >= 5); // samedi/dimanche
        }

        // Changement d'heure → mise à jour des destinations
        if (nouvelleHeure != heureSim) {
            int ancienneHeure = heureSim;
            heureSim = nouvelleHeure;
            mettreAJourDestinations(ancienneHeure, heureSim);
            for (Consumer<Integer> l : listenersHeure) {
                l.accept(heureSim);
            }
        }

        // Avancer les déplacements en cours
        List<Bavard> arrives = new ArrayList<>();
        for (Map.Entry<Bavard, Deplacement> entry : deplacementsActifs.entrySet()) {
            Deplacement dep = entry.getValue();
            boolean fini = dep.avancer(msSim);
            if (fini) {
                arrives.add(entry.getKey());
            }
        }

        // Traiter les arrivées
        for (Bavard b : arrives) {
            Deplacement dep = deplacementsActifs.remove(b);
            Lieu destination = dep.getLieuDestination();

            // Quitter l'ancien lieu
            Lieu ancienLieu = dep.getLieuSource();
            ancienLieu.liberer(b);

            // Arriver au nouveau lieu
            destination.accueillir(b);
            lieuCourant.put(b, destination);
            ville.setStatut(b, statutPourLieu(destination));

            // Callbacks
            for (Consumer<Bavard> l : listenersArrivee) {
                l.accept(b);
            }
        }

        // Callbacks tick
        for (Runnable r : listenersTick) {
            r.run();
        }
    }

    // ─── Mise à jour des destinations ────────────────────────────────────────

    /**
     * À chaque changement d'heure simulée, vérifie si chaque bavard doit
     * se déplacer vers un nouveau lieu.
     */
    private void mettreAJourDestinations(int ancienneHeure, int nouvelleHeure) {
        for (Bavard b : ville.getReseau().getBavards()) {
            // Ignorer si déjà en déplacement
            if (deplacementsActifs.containsKey(b)) continue;

            Lieu lieuPrevu = ville.getLieuActuelDe(b, nouvelleHeure,
                                                    jourSim, estWeekEnd, rng);
            Lieu lieuActuel = lieuCourant.get(b);

            if (lieuPrevu == null || lieuPrevu.equals(lieuActuel)) continue;

            // Démarrer un déplacement
            Deplacement.Mode mode = choisirModeTransport(lieuActuel, lieuPrevu, b);
            Deplacement dep = new Deplacement(b, lieuActuel, lieuPrevu, mode, msSim);
            deplacementsActifs.put(b, dep);
            ville.setStatut(b, StatutPresence.EN_DEPLACEMENT);
        }
    }

    /**
     * Choisit un mode de transport adapté à la distance et au profil du bavard.
     */
    private Deplacement.Mode choisirModeTransport(Lieu source, Lieu dest, Bavard b) {
        if (source == null || dest == null) return Deplacement.Mode.PIED;
        double dist = source.distanceCarte(dest);
        if (dist == Double.MAX_VALUE) return Deplacement.Mode.AVION;
        if (dist < 100)  return Deplacement.Mode.PIED;
        if (dist < 300)  return Deplacement.Mode.VELO;
        if (dist < 700)  return Deplacement.Mode.VOITURE;
        return Deplacement.Mode.TRAIN;
    }

    /** Déduit le statut de présence en fonction du type de lieu. */
    private StatutPresence statutPourLieu(Lieu lieu) {
        if (lieu == null) return StatutPresence.A_DOMICILE;
        return switch (lieu.getType().categorie()) {
            case DOMICILE   -> StatutPresence.A_DOMICILE;
            case TRAVAIL    -> StatutPresence.AU_TRAVAIL;
            case VACANCES   -> StatutPresence.EN_VACANCES;
            default         -> StatutPresence.EN_LOISIR;
        };
    }

    // ─── Position de rendu ───────────────────────────────────────────────────

    /**
     * Renvoie la position courante du bavard sur la carte (en coordonnées carte 0–1200 × 0–900).
     * Si le bavard est en déplacement, la position est interpolée.
     * Si le bavard est hors carte (vacances), renvoie null.
     */
    public java.awt.geom.Point2D.Double getPositionCarte(Bavard b) {
        Deplacement dep = deplacementsActifs.get(b);
        if (dep != null) {
            double x = dep.getXCarte();
            double y = dep.getYCarte();
            if (x < 0) return null;
            return new java.awt.geom.Point2D.Double(x, y);
        }

        Lieu lieu = lieuCourant.get(b);
        if (lieu == null || lieu.estHorsCarte()) return null;
        return new java.awt.geom.Point2D.Double(lieu.getXCarte(), lieu.getYCarte());
    }

    /**
     * Renvoie le lieu actuel (non interpolé) d'un bavard.
     * Pendant un déplacement, renvoie le lieu source (d'où il part).
     */
    public Lieu getLieuCourant(Bavard b) {
        Deplacement dep = deplacementsActifs.get(b);
        if (dep != null) return dep.getLieuSource();
        return lieuCourant.get(b);
    }

    /**
     * Renvoie le lieu de destination d'un bavard en déplacement, ou son lieu courant.
     */
    public Lieu getLieuDestination(Bavard b) {
        Deplacement dep = deplacementsActifs.get(b);
        if (dep != null) return dep.getLieuDestination();
        return lieuCourant.get(b);
    }

    // ─── Ajout/suppression de bavards dynamiquement ──────────────────────────

    /**
     * Ajoute un bavard en cours de simulation (pour le bouton "Simuler 500").
     */
    public void ajouterBavard(Bavard b) {
        Lieu domicile = ville.getDomicile(b);
        if (domicile != null) {
            lieuCourant.put(b, domicile);
            domicile.accueillir(b);
            ville.setStatut(b, StatutPresence.A_DOMICILE);
        }
    }

    // ─── Listeners ───────────────────────────────────────────────────────────

    public void ajouterListenerTick(Runnable r)          { listenersTick.add(r); }
    public void ajouterListenerHeure(Consumer<Integer> l){ listenersHeure.add(l); }
    public void ajouterListenerArrivee(Consumer<Bavard> l){ listenersArrivee.add(l); }

    public void retirerListenerTick(Runnable r)          { listenersTick.remove(r); }
    public void retirerListenerHeure(Consumer<Integer> l){ listenersHeure.remove(l); }
    public void retirerListenerArrivee(Consumer<Bavard> l){ listenersArrivee.remove(l); }

    // ─── Accesseurs ─────────────────────────────────────────────────────────

    public int     getHeureSim()       { return heureSim; }
    public int     getJourSim()        { return jourSim; }
    public boolean isEstWeekEnd()      { return estWeekEnd; }
    public double  getVitesse()        { return vitesse; }
    public boolean isEnMarche()        { return enMarche; }
    public long    getMsSimulee()      { return msSim; }

    public VilleSociale getVille()     { return ville; }

    public Map<Bavard, Deplacement> getDeplacementsActifs() {
        return Collections.unmodifiableMap(deplacementsActifs);
    }

    public void setVitesse(double v) {
        this.vitesse = Math.max(0.1, Math.min(100.0, v));
    }

    /** Renvoie un affichage de l'heure simulée : "08:30". */
    public String getHeureFmt() {
        long totalMin = msSim / 60_000L;
        int h = (int)((totalMin / 60) % 24);
        int m = (int)(totalMin % 60);
        return String.format("%02d:%02d", h, m);
    }

    /** Renvoie le nom du jour simulé (Lundi, Mardi, …). */
    public String getNomJour() {
        String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi",
                          "Vendredi", "Samedi", "Dimanche"};
        return jours[jourSim % 7];
    }
}
