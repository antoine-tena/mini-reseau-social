package socialnetwork.back.personnalite;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstraite de base pour toutes les personnalités à comportement humain.
 *
 * Modélise plusieurs dimensions psychologiques réalistes :
 *  - humeur courante [-1, +1] : varie au fil des interactions
 *  - énergie sociale [0, 1]   : se fatigue, récupère avec le temps
 *  - stress [0, 1]            : monte sous les messages négatifs
 *  - biais de confirmation    : amplifie les messages cohérents avec sa vision
 *  - mémoire courte           : les dernières interactions colorent la perception
 *
 * Chaque sous-classe implémente {@link #calculerReponse(float, String)} pour
 * définir comment la personnalité transforme la bienveillance d'un message
 * en fonction de son état interne du moment.
 *
 * La dynamique temporelle est gérée via {@link #mettreAJourEtat(float, String)}
 * appelé avant chaque réponse. Sans message pendant un moment, la personnalité
 * récupère graduellement vers son état d'équilibre naturel.
 */
public abstract class PersonnaliteHumaine extends Personnalite {

    // =========================================================
    //  ÉTATS INTERNES (dynamiques, évoluent au fil du temps)
    // =========================================================

    /** Humeur courante [-1 = très négatif, +1 = très positif]. */
    protected float humeur;

    /** Énergie sociale disponible [0 = épuisé, 1 = plein d'énergie]. */
    protected float energie;

    /** Niveau de stress accumulé [0 = calme, 1 = très stressé]. */
    protected float stress;

    /** Bienveillance de base, recalculée dynamiquement. */
    protected float bienveillanceBase;

    // =========================================================
    //  PARAMÈTRES FIXES (définissent la personnalité profonde)
    // =========================================================

    /** Tendance naturelle (valeur d'équilibre de l'humeur). */
    protected final float tendance;

    /** Sensibilité aux messages reçus [0 = insensible, 2 = hypersensible]. */
    protected final float sensibilite;

    /** Vitesse de récupération vers l'état d'équilibre [0.01 = lent, 1 = rapide]. */
    protected final float resilience;

    /** Biais de confirmation [0 = neutre, 1 = fort biais]. */
    protected final float biais;

    /** Capacité à influencer les autres (amplification émission). */
    protected final float charisme;

    // =========================================================
    //  MÉMOIRE COURTE
    // =========================================================

    /** Taille de la mémoire courte (nb messages récents conservés). */
    protected static final int TAILLE_MEMOIRE = 10;

    /** Historique des bienveillances reçues récemment. */
    protected final List<Float> historiqueRecent = new ArrayList<>();

    /** Nombre total de messages reçus (pour détecter la fatigue sociale). */
    protected int nbMessagesRecus = 0;

    /** Timestamp du dernier message reçu (pour la récupération temporelle). */
    protected long dernierInteraction = System.currentTimeMillis();

    // =========================================================
    //  MOTS-CLÉS ÉMOTIONNELS
    // =========================================================

    protected static final String[] MOTS_POSITIFS = {
        "amour", "joie", "bonheur", "super", "génial", "excellent", "merci",
        "bravo", "félicitations", "heureux", "formidable", "parfait", "incroyable",
        "fantastique", "magnifique", "réussite", "succès", "victoire", "espoir",
        "confiance", "paix", "solidarité", "partage", "ensemble", "aide"
    };

    protected static final String[] MOTS_NEGATIFS = {
        "haine", "colère", "tristesse", "terrible", "horrible", "catastrophe",
        "mensonge", "tromperie", "danger", "peur", "anxiété", "désastre",
        "échec", "nul", "mauvais", "problème", "crise", "conflit", "violence",
        "manipulation", "arnaque", "scandal", "corruption", "mort", "guerre"
    };

    protected static final String[] MOTS_URGENCE = {
        "urgent", "vite", "maintenant", "alerte", "attention", "important",
        "critique", "immédiat", "breaking", "flash", "exclusif"
    };

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    /**
     * Crée une personnalité humaine avec tous ses paramètres.
     *
     * @param nom          identifiant affiché
     * @param tendance     humeur naturelle d'équilibre [-1, +1]
     * @param sensibilite  réactivité aux messages [0, 2]
     * @param resilience   vitesse de récupération [0.01, 1]
     * @param biais        force du biais de confirmation [0, 1]
     * @param charisme     capacité d'influence sur l'émission [0, 2]
     */
    protected PersonnaliteHumaine(String nom, float tendance,
                                   float sensibilite, float resilience,
                                   float biais, float charisme) {
        super(nom, tendance);
        this.tendance    = Math.max(-1f, Math.min(1f, tendance));
        this.sensibilite = Math.max(0f,  Math.min(2f, sensibilite));
        this.resilience  = Math.max(0.01f, Math.min(1f, resilience));
        this.biais       = Math.max(0f,  Math.min(1f, biais));
        this.charisme    = Math.max(0f,  Math.min(2f, charisme));

        // Initialisation de l'état
        this.humeur            = tendance * 0.5f;
        this.energie           = 0.8f;
        this.stress            = 0.1f;
        this.bienveillanceBase = tendance;
    }

    // =========================================================
    //  INTERFACE PERSONNALITE (surcharge)
    // =========================================================

    /** Point d'entrée principal : met à jour l'état interne puis calcule la réponse. */
    @Override
    public final float appliquer(float bienveillance, String contenu) {
        mettreAJourEtat(bienveillance, contenu);
        float resultat = calculerReponse(bienveillance, contenu);
        return borner(resultat);
    }

    @Override
    public final float appliquer(float bienveillance) {
        return appliquer(bienveillance, "");
    }

    // =========================================================
    //  DYNAMIQUE INTERNE
    // =========================================================

    /**
     * Met à jour l'état psychologique après réception d'un message.
     * Modélise : fatigue sociale, impact émotionnel, récupération temporelle.
     */
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        nbMessagesRecus++;
        long maintenant = System.currentTimeMillis();

        // --- Récupération temporelle (si silence récent) ---
        long silence = maintenant - dernierInteraction;
        if (silence > 500) { // plus de 0.5s sans message
            float secEcoulees = silence / 1000f;
            float tauxRec = resilience * 0.05f;
            energie  = Math.min(1f, energie + secEcoulees * tauxRec);
            humeur  += (tendance - humeur) * secEcoulees * tauxRec * 0.5f;
            stress   = Math.max(0f, stress - secEcoulees * tauxRec * 0.3f);
        }
        dernierInteraction = maintenant;

        // --- Impact émotionnel du message ---
        float impact = bienveillance * sensibilite * 0.08f;
        humeur = borner11(humeur + impact);

        // Stress : monte avec les messages très négatifs
        if (bienveillance < -3f) {
            stress = Math.min(1f, stress + 0.04f * sensibilite);
        } else if (bienveillance > 3f) {
            // Les bons messages réduisent légèrement le stress
            stress = Math.max(0f, stress - 0.02f);
        }

        // Fatigue sociale : chaque message consomme un peu d'énergie
        float fatigueParMessage = 0.015f / (1f + resilience);
        energie = Math.max(0f, energie - fatigueParMessage);

        // Mémoire courte
        historiqueRecent.add(bienveillance);
        if (historiqueRecent.size() > TAILLE_MEMOIRE) {
            historiqueRecent.remove(0);
        }
    }

    /**
     * Calcule comment cette personnalité transforme la bienveillance d'un message.
     * Doit être implémenté par chaque sous-classe avec sa logique propre.
     *
     * @param bienveillance valeur d'entrée du message [-10, +10]
     * @param contenu       texte brut du message (peut être null ou vide)
     * @return bienveillance transformée AVANT bornage (le bornage est fait dans appliquer)
     */
    protected abstract float calculerReponse(float bienveillance, String contenu);

    // =========================================================
    //  MÉTHODES ABSTRAITES D'IDENTIFICATION
    // =========================================================

    /** Retourne le type humain (ex: "Émotif", "Introverti", "Influenceur"). */
    public abstract String typePersonnalite();

    /** Description courte pour l'interface (2-3 mots max). */
    public abstract String descriptionCourte();

    /** Description longue pour le tooltip (peut être multi-phrases). */
    public String descriptionLongue() {
        return descriptionCourte() + ". Sensibilité=" + String.format("%.1f", sensibilite)
               + ", Résilience=" + String.format("%.1f", resilience);
    }

    // =========================================================
    //  UTILITAIRES PARTAGÉS
    // =========================================================

    /**
     * Calcule le score de mots positifs/négatifs dans le contenu.
     * Positif = +1 par mot positif trouvé, négatif = -1 par mot négatif.
     */
    protected float scoreContenu(String contenu) {
        if (contenu == null || contenu.isBlank()) return 0f;
        String low = contenu.toLowerCase();
        float score = 0f;
        for (String mot : MOTS_POSITIFS) if (low.contains(mot)) score += 1f;
        for (String mot : MOTS_NEGATIFS) if (low.contains(mot)) score -= 1f;
        return score;
    }

    /**
     * Détecte si le message exprime une urgence.
     */
    protected boolean estUrgent(String contenu) {
        if (contenu == null) return false;
        String low = contenu.toLowerCase();
        for (String mot : MOTS_URGENCE) if (low.contains(mot)) return true;
        return false;
    }

    /**
     * Moyenne des bienveillances récentes (mémoire courte).
     */
    protected float moyenneRecente() {
        if (historiqueRecent.isEmpty()) return 0f;
        float somme = 0f;
        for (float v : historiqueRecent) somme += v;
        return somme / historiqueRecent.size();
    }

    /**
     * Variance des bienveillances récentes (indique la volatilité).
     */
    protected float varianceRecente() {
        if (historiqueRecent.size() < 2) return 0f;
        float moy = moyenneRecente();
        float variance = 0f;
        for (float v : historiqueRecent) variance += (v - moy) * (v - moy);
        return variance / historiqueRecent.size();
    }

    /** Ratio de messages positifs dans la mémoire [0, 1]. */
    protected float ratioPositifsRecents() {
        if (historiqueRecent.isEmpty()) return 0.5f;
        long positifs = historiqueRecent.stream().filter(v -> v > 0).count();
        return (float) positifs / historiqueRecent.size();
    }

    // =========================================================
    //  UTILITAIRES DE BORNAGE
    // =========================================================

    /** Borne dans [-10, +10] pour la bienveillance globale. */
    protected static float borner(float v) {
        return Math.max(-10f, Math.min(10f, v));
    }

    /** Borne dans [-1, +1] pour l'humeur. */
    protected static float borner11(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    /** Borne dans [0, 1] pour l'énergie et le stress. */
    protected static float borner01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // =========================================================
    //  GETTERS D'ÉTAT INTERNE
    // =========================================================

    public float getHumeur()            { return humeur; }
    public float getEnergie()           { return energie; }
    public float getStress()            { return stress; }
    public float getTendance()          { return tendance; }
    public float getSensibilite()       { return sensibilite; }
    public float getResilience()        { return resilience; }
    public float getBiais()             { return biais; }
    public float getCharisme()          { return charisme; }
    public int   getNbMessagesRecus()   { return nbMessagesRecus; }

    /** Indice de bien-être global (combinaison humeur + énergie - stress). */
    public float getBienEtre() {
        return borner11((humeur + energie * 0.5f - stress * 0.5f) / 1.5f);
    }

    // =========================================================
    //  AFFICHAGE
    // =========================================================

    @Override
    public String toString() {
        return String.format("%s[%s | humeur=%.1f | energie=%.1f | stress=%.1f]",
                getNom(), typePersonnalite(), humeur, energie, stress);
    }
}
