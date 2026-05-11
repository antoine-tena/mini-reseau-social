package socialnetwork.back;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Variante avec un facteur aleatoire pour amplifier la variation.
 */
public class PersonnaliteForte extends Personnalite {
    public static final PersonnaliteForte POSITIF_FORTE = new PersonnaliteForte("POSITIF_FORTE", 1f);
    public static final PersonnaliteForte NEGATIF_FORTE = new PersonnaliteForte("NEGATIF_FORTE", -1f);
    public static final PersonnaliteForte NEUTRE_FORTE = new PersonnaliteForte("NEUTRE_FORTE", 0f);

    private float facteur;
    
    /**
     * Cree une personnalite forte avec un facteur aleatoire initial.
     */
    public PersonnaliteForte(String nom, float variationBienveillance) {
        super(nom, variationBienveillance);
        randomizeFacteur();
    }

    // Tire un nouveau facteur pour la prochaine application.
    public void randomizeFacteur() {
        this.facteur = (float) ThreadLocalRandom.current().nextDouble(1.5f, 2.0f);
    }

    /**
     * Applique la variation multipliee par un facteur aleatoire.
     */
    @Override
    /**
     * Applique la variation multipliee par un facteur aleatoire.
     */
    public float appliquer(float bienveillance) {
        randomizeFacteur();
        float scaled = getVariationBienveillance() * facteur;
        float resultat = bienveillance + scaled;
        if (resultat > 10f) {
            return 10f;
        }
        if (resultat < -10f) {
            return -10f;
        }
        return resultat;
    }

    @Override
    public float appliquer(float bienveillance, String contenu) {
        // personnalite forte : ignore le contenu, utilise le facteur aleatoire
        return appliquer(bienveillance);
    }

    @Override
    public String toString() {
        return super.toString() + "(forte x" + String.format("%.2f", facteur) + ")";
    }
}
