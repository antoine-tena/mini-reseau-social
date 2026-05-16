package socialnetwork.back.personnalite;

/**
 * Personnalité extravertie : se nourrit de l'énergie sociale.
 *
 * Profil psychologique :
 *  - Recharge via les interactions (l'inverse de l'introverti)
 *  - Amplifie et diffuse tous les messages avec enthousiasme
 *  - Réseau étendu : forte propension à retransmettre
 *  - Peu de filtre : passe tout, même le négatif, avec conviction
 *  - Charisme naturel : sa bonne humeur est contagieuse
 *  - Légèrement superficiel : analyse peu, réagit vite
 *
 * Analogie humaine : la personne qui anime toute la pièce, qui partage
 * tout sur les réseaux sociaux, qui fait des "stories" à n'importe quelle
 * heure et qui est galvanisée par les fêtes.
 */
public class PersonnaliteExtrovert extends PersonnaliteHumaine {

    /** Facteur d'amplification de l'enthousiasme [1.0, 2.5]. */
    private final float facteurEnthousiasme;

    /** Seuil d'isolation au-dessous duquel l'extraverti déprime (manque de contact). */
    private final float seuilIsolation;

    /** Compteur de silence (pas de message depuis longtemps = déprime). */
    private long dernierMessageRecu = System.currentTimeMillis();

    // Constantes prédéfinies
    public static final PersonnaliteExtrovert TRES_EXTRAVERTI = new PersonnaliteExtrovert("Très extraverti", 2.0f, 10_000L);
    public static final PersonnaliteExtrovert EXTRAVERTI      = new PersonnaliteExtrovert("Extraverti",      1.6f, 20_000L);
    public static final PersonnaliteExtrovert SOCIABLE        = new PersonnaliteExtrovert("Sociable",        1.3f, 30_000L);

    /**
     * @param nom                 nom de la personnalité
     * @param facteurEnthousiasme amplification de l'énergie sociale [1, 2.5]
     * @param seuilIsolationMs    millisecondes sans message avant déprime légère
     */
    public PersonnaliteExtrovert(String nom, float facteurEnthousiasme, long seuilIsolationMs) {
        super(nom,
              0.3f,   // tendance positive (l'extraverti est naturellement optimiste)
              0.8f,   // très réactif
              0.6f,   // récupère vite
              0.2f,   // peu de biais de confirmation (ouvert à tout)
              1.8f    // très charismatique
        );
        this.facteurEnthousiasme = Math.max(1f, Math.min(2.5f, facteurEnthousiasme));
        this.seuilIsolation      = seuilIsolationMs;
    }

    @Override
    protected void mettreAJourEtat(float bienveillance, String contenu) {
        super.mettreAJourEtat(bienveillance, contenu);

        // L'extraverti RECHARGE via les interactions (contrairement à l'introverti)
        energie = Math.min(1f, energie + 0.03f);

        // Bonus d'humeur à chaque interaction
        humeur = Math.min(1f, humeur + 0.02f);

        dernierMessageRecu = System.currentTimeMillis();
    }

    @Override
    protected float calculerReponse(float bienveillance, String contenu) {
        // -------------------------------------------------------
        // 1. Vérification de l'isolement (pas de message depuis un moment)
        // -------------------------------------------------------
        long depuisMessage = System.currentTimeMillis() - dernierMessageRecu;
        float facteurIsolation = 1f;
        if (depuisMessage > seuilIsolation) {
            // L'extraverti s'ennuie et amplifie encore plus pour attirer l'attention
            float surplusMs = (depuisMessage - seuilIsolation) / 1000f;
            facteurIsolation = Math.min(1.5f, 1f + surplusMs * 0.01f);
        }

        // -------------------------------------------------------
        // 2. Amplification enthousiaste de TOUT message
        // -------------------------------------------------------
        float resultat;
        if (bienveillance >= 0) {
            // Messages positifs : euphorie contagieuse
            resultat = bienveillance * facteurEnthousiasme * (1f + humeur * 0.4f);
        } else {
            // Messages négatifs : l'extraverti les partage quand même (drama = attention)
            // mais les atténue légèrement grâce à son optimisme naturel
            resultat = bienveillance * (facteurEnthousiasme * 0.6f) - humeur * 0.5f;
        }

        // -------------------------------------------------------
        // 3. Boost si le message contient des mots d'enthousiasme
        // -------------------------------------------------------
        float scoreTexte = scoreContenu(contenu);
        resultat += scoreTexte * facteurEnthousiasme * 0.5f;

        // -------------------------------------------------------
        // 4. Application du facteur d'isolement
        // -------------------------------------------------------
        resultat *= facteurIsolation;

        // -------------------------------------------------------
        // 5. Ajout de l'énergie sociale rayonnante
        //    L'extraverti "surjoue" toujours un peu
        // -------------------------------------------------------
        resultat += charisme * energie * 1.2f;

        // -------------------------------------------------------
        // 6. Urgence : l'extraverti ADORE les breaking news
        // -------------------------------------------------------
        if (estUrgent(contenu)) {
            resultat += 1.5f;
        }

        return resultat;
    }

    /** Retourne vrai si l'extraverti souffre d'un manque de contact social. */
    public boolean estIsole() {
        return System.currentTimeMillis() - dernierMessageRecu > seuilIsolation;
    }

    /** Durée en secondes depuis le dernier message reçu. */
    public long getSecondesDepuisDernierContact() {
        return (System.currentTimeMillis() - dernierMessageRecu) / 1000L;
    }

    @Override
    public String typePersonnalite() { return "Extraverti"; }

    @Override
    public String descriptionCourte() { return "Amplifie tout, adore le contact"; }

    @Override
    public String descriptionLongue() {
        return "Personnalité extravertie : se nourrit de l'énergie sociale. "
             + "Amplifie les messages avec enthousiasme (×" + String.format("%.1f", facteurEnthousiasme) + "). "
             + (estIsole() ? "⚠ En manque de contact depuis " + getSecondesDepuisDernierContact() + "s. "
                           : "En pleine forme sociale. ")
             + "Energie : " + String.format("%.0f%%", energie * 100) + ". "
             + "Humeur : " + String.format("%.2f", humeur);
    }
}
