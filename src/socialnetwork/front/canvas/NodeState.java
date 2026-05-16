package socialnetwork.front.canvas;

import java.awt.Color;

/**
 * État d'animation d'un nœud (bavard) dans le canvas animé.
 *
 * Encapsule toutes les données d'animation associées à un nœud :
 *  - Pulse : animation d'expansion quand le nœud reçoit un message
 *  - Glow  : halo ambiant, renforcé après une activité récente
 *  - Sélection et survol : mise en évidence interactive
 *  - Activité récente : compteur qui décroît progressivement
 *  - Bienveillance reçue : mémorise la dernière pour colorier le nœud
 *
 * Les valeurs évoluent à chaque frame via {@link #mettreAJour(double)}.
 */
public class NodeState {

    // =========================================================
    //  CONSTANTES D'ANIMATION
    // =========================================================

    /** Durée d'une pulsation complète en ms. */
    private static final float DUREE_PULSE_MS       = 600f;

    /** Durée de la décroissance du glow d'activité en ms. */
    private static final float DUREE_GLOW_ACTIVITE  = 3_000f;

    /** Seuil d'activité à partir duquel le nœud est rendu "actif". */
    private static final int SEUIL_ACTIVITE         = 3;

    // =========================================================
    //  ÉTAT DE PULSE
    // =========================================================

    /** Progress du pulse [0.0, 1.0]. 0 = au repos, 1 = fin de pulse. */
    private float pulseProgress  = 1f; // commence "fini" (pas de pulse initial)

    /** Couleur du pulse (couleur du dernier message reçu). */
    private Color couleurPulse   = null;

    /** Rayon supplémentaire ajouté par le pulse (pixels). */
    private float rayonPulseExtra = 0f;

    /** Alpha du pulse [0, 255]. */
    private int alphaPulse        = 0;

    // =========================================================
    //  GLOW AMBIANT
    // =========================================================

    /** Intensité du glow ambiant [0, 1]. */
    private float glowIntensity  = 0f;

    /** Couleur de base du glow (définie par la communauté). */
    private Color couleurGlow    = null;

    // =========================================================
    //  ACTIVITÉ RÉCENTE
    // =========================================================

    /** Nombre de messages reçus récemment (décroît avec le temps). */
    private float activiteRecente = 0f;

    /** Timestamp du dernier message reçu. */
    private long dernierMessageMs = 0L;

    // =========================================================
    //  INTERACTION
    // =========================================================

    /** Vrai si ce nœud est sélectionné par un clic. */
    private boolean selectionne = false;

    /** Vrai si la souris survole ce nœud. */
    private boolean survole     = false;

    // =========================================================
    //  BIENVEILLANCE MÉMORISÉE
    // =========================================================

    /** Dernière bienveillance reçue (pour coloration). */
    private float derniereBienveillance = 0f;

    /** Bienveillance moyennée (décroît vers 0 avec le temps). */
    private float bienveillanceMoyenne  = 0f;

    // =========================================================
    //  COMMUNAUTÉ
    // =========================================================

    /** Index de la communauté détectée. */
    private int indexCommunaute = -1;

    /** Couleur principale (dérivée de la personnalité ou communauté). */
    private Color couleurBase = new Color(80, 120, 220);

    // =========================================================
    //  SCALE
    // =========================================================

    /** Facteur d'échelle courant (1.0 = normal, >1 = agrandi pendant le pulse). */
    private float scale = 1f;

    // =========================================================
    //  MISE À JOUR
    // =========================================================

    /**
     * Met à jour l'état d'animation selon le temps écoulé.
     *
     * @param deltaMs millisecondes écoulées depuis la dernière frame
     */
    public void mettreAJour(double deltaMs) {
        float dt = (float) deltaMs;

        // --- Progression du pulse ---
        if (pulseProgress < 1f) {
            pulseProgress += dt / DUREE_PULSE_MS;
            if (pulseProgress >= 1f) {
                pulseProgress  = 1f;
                rayonPulseExtra = 0f;
                alphaPulse      = 0;
                scale           = 1f;
            } else {
                // Courbe ease-out : décélère en fin de pulse
                float eased     = 1f - (1f - pulseProgress) * (1f - pulseProgress);
                rayonPulseExtra = (1f - eased) * 30f;  // de 30px → 0px
                alphaPulse      = (int)((1f - eased) * 200);
                // Scale : légère expansion puis retour
                float scalePhase = Math.min(1f, pulseProgress / 0.3f);
                scale = 1f + 0.2f * (float) Math.sin(scalePhase * Math.PI);
            }
        }

        // --- Décroissance du glow d'activité ---
        if (activiteRecente > 0f) {
            activiteRecente = Math.max(0f, activiteRecente - dt / DUREE_GLOW_ACTIVITE);
        }

        // --- Décroissance de la bienveillance mémorisée ---
        if (Math.abs(bienveillanceMoyenne) > 0.01f) {
            bienveillanceMoyenne *= (1f - dt / 10_000f);
        }

        // --- Calcul de l'intensité du glow ---
        float actNorm = Math.min(1f, activiteRecente / SEUIL_ACTIVITE);
        float pulseGlow = (pulseProgress < 1f) ? (1f - pulseProgress) * 0.8f : 0f;
        glowIntensity = Math.min(1f, actNorm * 0.6f + pulseGlow);
    }

    // =========================================================
    //  DÉCLENCHEMENT DU PULSE
    // =========================================================

    /**
     * Déclenche une pulsation sur ce nœud (quand il reçoit un message).
     *
     * @param couleur      couleur du message reçu
     * @param bienveillance valeur de bienveillance du message
     */
    public void declencher(Color couleur, float bienveillance) {
        this.couleurPulse          = couleur;
        this.pulseProgress         = 0f;
        this.activiteRecente       = Math.min(activiteRecente + 1.5f, 10f);
        this.derniereBienveillance = bienveillance;
        this.dernierMessageMs      = System.currentTimeMillis();

        // Moyenne glissante de bienveillance
        bienveillanceMoyenne = bienveillanceMoyenne * 0.7f + bienveillance * 0.3f;
    }

    // =========================================================
    //  GETTERS ÉTAT D'ANIMATION
    // =========================================================

    public boolean estEnPulse()          { return pulseProgress < 1f; }
    public float   getPulseProgress()    { return pulseProgress; }
    public Color   getCouleurPulse()     { return couleurPulse; }
    public float   getRayonPulseExtra()  { return rayonPulseExtra; }
    public int     getAlphaPulse()       { return alphaPulse; }
    public float   getScale()            { return scale; }
    public float   getGlowIntensity()    { return glowIntensity; }
    public Color   getCouleurGlow()      { return couleurGlow; }
    public float   getActiviteRecente()  { return activiteRecente; }
    public boolean estActif()            { return activiteRecente > 0.1f; }

    // =========================================================
    //  GETTERS / SETTERS GÉNÉRAUX
    // =========================================================

    public boolean isSelectionne()       { return selectionne; }
    public void    setSelectionne(boolean s) { this.selectionne = s; }
    public boolean isSurvole()           { return survole; }
    public void    setSurvole(boolean s) { this.survole = s; }

    public float   getDerniereBienveillance()  { return derniereBienveillance; }
    public float   getBienveillanceMoyenne()   { return bienveillanceMoyenne; }
    public long    getDernierMessageMs()        { return dernierMessageMs; }

    public int     getIndexCommunaute()    { return indexCommunaute; }
    public void    setIndexCommunaute(int idx) { this.indexCommunaute = idx; }

    public Color   getCouleurBase()        { return couleurBase; }
    public void    setCouleurBase(Color c) { this.couleurBase = c; }
    public void    setCouleurGlow(Color c) { this.couleurGlow = c; }

    /** Nombre de secondes depuis le dernier message reçu. */
    public float getSecondedepuisMessage() {
        if (dernierMessageMs == 0) return Float.MAX_VALUE;
        return (System.currentTimeMillis() - dernierMessageMs) / 1000f;
    }

    @Override
    public String toString() {
        return String.format("NodeState[pulse=%.1f%% | glow=%.2f | activite=%.1f | selected=%b]",
                pulseProgress * 100, glowIntensity, activiteRecente, selectionne);
    }
}
