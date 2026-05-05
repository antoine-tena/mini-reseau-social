package socialnetwork.back;

public class Personnalite {
	// chaque perso a juste une variation fixe
	public static final Personnalite POSITIF = new Personnalite("POSITIF", 1f);
	public static final Personnalite NEGATIF = new Personnalite("NEGATIF", -1f);
	public static final Personnalite NEUTRE = new Personnalite("NEUTRE", 0f);

	private final String nom;
	private final float variationBienveillance;

	public Personnalite(String nom, float variationBienveillance) {
		this.nom = nom;
		this.variationBienveillance = variationBienveillance;
	}

	public String getNom() {
		return nom;
	}

	public float getVariationBienveillance() {
		return variationBienveillance;
	}

	public float appliquer(float bienveillance) {
		// ajoute ou retire selon la perso
		float resultat = bienveillance + variationBienveillance;
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
		return nom;
	}
}
