package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;
import java.util.*;

/**
 * Lieu physique dans la ville virtuelle.
 *
 * Un lieu possède :
 *  - Un identifiant unique, un nom et un type
 *  - Une adresse physique et des coordonnées sur la carte (0–1200 × 0–900)
 *  - Une liste des bavards actuellement présents
 *  - Une capacité d'accueil
 *  - Des horaires d'ouverture (optionnellement surchargés par rapport au type)
 *  - Un état courant (ouvert / fermé)
 *  - Un historique de présence (pour les statistiques)
 *
 * Les coordonnées (xCarte, yCarte) sont dans un espace virtuel 0–1200 × 0–900
 * qui correspond à l'espace de rendu du {@code CarteVilleCanvas}.
 */
public class Lieu {

    // ─── Champs invariants ───────────────────────────────────────────────────

    private static int prochainId = 1;

    private final int      id;
    private final String   nom;
    private final TypeLieu type;
    private final Adresse  adresse;

    /** Position sur la carte virtuelle. */
    private double xCarte;
    private double yCarte;

    /** Rayon visuel sur la carte (en unités carte). */
    private int rayon;

    // ─── Champs dynamiques ───────────────────────────────────────────────────

    /** Bavards actuellement présents dans ce lieu. */
    private final List<Bavard> occupantsActuels = new ArrayList<>();

    /** Capacité maximale d'accueil. */
    private final int capacite;

    /** Horaires personnalisés (null = utilise ceux du TypeLieu). */
    private Integer heureOuverture = null;
    private Integer heureFermeture = null;
    private boolean ouvert24h      = false;

    /** Description humaine étendue. */
    private String descriptionPersonnalisee = "";

    /** True si le lieu existe en mode "destination de vacances" (hors carte). */
    private final boolean estHorsCarte;

    /** Sous-lieux associés (ex: campus avec plusieurs bâtiments). */
    private final List<Lieu> sousLieux = new ArrayList<>();

    /** Catégorie visuelle pour le regroupement. */
    private String groupe = "";

    // Statistiques
    private int totalVisites        = 0;
    private int picFrequentation    = 0;

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public Lieu(String nom, TypeLieu type, Adresse adresse,
                double xCarte, double yCarte) {
        this.id         = prochainId++;
        this.nom        = nom;
        this.type       = type;
        this.adresse    = adresse;
        this.xCarte     = xCarte;
        this.yCarte     = yCarte;
        this.rayon      = type.rayonDefaut;
        this.capacite   = type.capacite;
        this.estHorsCarte = false;
    }

    /** Constructeur pour une destination de vacances (hors ville). */
    public Lieu(String nom, TypeLieu type, String descriptionCourte) {
        this.id         = prochainId++;
        this.nom        = nom;
        this.type       = type;
        this.adresse    = new Adresse(0, "Hors ville", "00000", nom);
        this.xCarte     = -1;   // hors carte
        this.yCarte     = -1;
        this.rayon      = type.rayonDefaut;
        this.capacite   = type.capacite;
        this.estHorsCarte = true;
        this.descriptionPersonnalisee = descriptionCourte;
    }

    // ─── Gestion des occupants ───────────────────────────────────────────────

    /** Ajoute un bavard comme présent dans ce lieu. */
    public synchronized void accueillir(Bavard b) {
        if (b == null || occupantsActuels.contains(b)) return;
        occupantsActuels.add(b);
        totalVisites++;
        if (occupantsActuels.size() > picFrequentation) {
            picFrequentation = occupantsActuels.size();
        }
    }

    /** Retire un bavard de ce lieu. */
    public synchronized void liberer(Bavard b) {
        occupantsActuels.remove(b);
    }

    /** Retire tous les bavards (réinitialisation de la simulation). */
    public synchronized void vider() {
        occupantsActuels.clear();
    }

    /** Snapshot des occupants actuels (copie défensive). */
    public List<Bavard> getOccupants() {
        return new ArrayList<>(occupantsActuels);
    }

    /** Nombre d'occupants actuels. */
    public int getNbOccupants() {
        return occupantsActuels.size();
    }

    /** True si le lieu peut accueillir au moins un bavard supplémentaire. */
    public boolean aDelaPlace() {
        return occupantsActuels.size() < capacite;
    }

    /** Taux de remplissage [0, 1]. */
    public float tauxRemplissage() {
        if (capacite <= 0) return 0f;
        return Math.min(1f, (float) occupantsActuels.size() / capacite);
    }

    // ─── Horaires ────────────────────────────────────────────────────────────

    /** True si le lieu est ouvert à l'heure donnée (0–23). */
    public boolean estOuvertA(int heure) {
        if (ouvert24h) return true;
        int ouv = (heureOuverture != null) ? heureOuverture : type.heureOuverture;
        int fer = (heureFermeture != null) ? heureFermeture : type.heureFermeture;
        if (ouv <= fer) {
            return heure >= ouv && heure < fer;
        } else {
            return heure >= ouv || heure < fer;
        }
    }

    /** Surcharge les horaires d'ouverture. */
    public void setHoraires(int ouverture, int fermeture) {
        this.heureOuverture = ouverture;
        this.heureFermeture = fermeture;
        this.ouvert24h = false;
    }

    public void setOuvert24h() {
        this.ouvert24h = true;
    }

    // ─── Sous-lieux ──────────────────────────────────────────────────────────

    public void ajouterSousLieu(Lieu sous) {
        sousLieux.add(sous);
    }

    public List<Lieu> getSousLieux() {
        return Collections.unmodifiableList(sousLieux);
    }

    // ─── Setters ─────────────────────────────────────────────────────────────

    public void setDescription(String desc) { this.descriptionPersonnalisee = desc; }
    public void setRayon(int r)             { this.rayon = r; }
    public void setGroupe(String g)         { this.groupe = g; }
    public void setPosition(double x, double y) { this.xCarte = x; this.yCarte = y; }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public int      getId()             { return id; }
    public String   getNom()            { return nom; }
    public TypeLieu getType()           { return type; }
    public Adresse  getAdresse()        { return adresse; }
    public double   getXCarte()         { return xCarte; }
    public double   getYCarte()         { return yCarte; }
    public int      getRayon()          { return rayon; }
    public int      getCapacite()       { return capacite; }
    public boolean  estHorsCarte()      { return estHorsCarte; }
    public String   getGroupe()         { return groupe; }
    public int      getTotalVisites()   { return totalVisites; }
    public int      getPicFrequentation(){ return picFrequentation; }

    public String getDescription() {
        if (!descriptionPersonnalisee.isEmpty()) return descriptionPersonnalisee;
        return type.description;
    }

    /** Distance cartographique jusqu'à un autre lieu (unités carte). */
    public double distanceCarte(Lieu autre) {
        if (estHorsCarte || autre.estHorsCarte) return Double.MAX_VALUE;
        double dx = xCarte - autre.xCarte;
        double dy = yCarte - autre.yCarte;
        return Math.hypot(dx, dy);
    }

    @Override
    public String toString() {
        return "[" + type.icone + " " + nom + " (" + occupantsActuels.size() + " pers.)]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Lieu l)) return false;
        return id == l.id;
    }

    @Override
    public int hashCode() { return id; }
}
