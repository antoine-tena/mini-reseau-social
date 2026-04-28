package socialnetwork;

import java.util.concurrent.ThreadLocalRandom;

public class PersonnaliteForte extends Personnalite {
    public static final PersonnaliteForte POSITIF_FORTE = new PersonnaliteForte("POSITIF_FORTE", 1f);
    public static final PersonnaliteForte NEGATIF_FORTE = new PersonnaliteForte("NEGATIF_FORTE", -1f);
    public static final PersonnaliteForte NEUTRE_FORTE = new PersonnaliteForte("NEUTRE_FORTE", 0f);

    private float facteur;
    
    public PersonnaliteForte(String nom, float variationBienveillance) {
        super(nom, variationBienveillance);
        randomizeFacteur();
    }

    public void randomizeFacteur() {
        this.facteur = (float) ThreadLocalRandom.current().nextDouble(1.5f, 2.0f);
    }

    @Override
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
    public String toString() {
        return super.toString() + "(forte x" + String.format("%.2f", facteur) + ")";
    }
}
