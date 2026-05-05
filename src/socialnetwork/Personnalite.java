package socialnetwork;

public final class Personnalite {
	// chaque perso a juste une variation fixe
	public static final Personnalite POSITIF = new Personnalite("POSITIF", 2);
	public static final Personnalite NEGATIF = new Personnalite("NEGATIF", -2);
	public static final Personnalite NEUTRE = new Personnalite("NEUTRE", 0);

	private final String nom;
	private final int variationBienveillance;

	private Personnalite(String nom, int variationBienveillance) {
		this.nom = nom;
		this.variationBienveillance = variationBienveillance;
	}

	public String getNom() {
		return nom;
	}

	public int getVariationBienveillance() {
		return variationBienveillance;
	}

	public int appliquer(int bienveillance) {
		// ajoute ou retire selon la perso
		int resultat = bienveillance + variationBienveillance;
		if (resultat > 10) {
			return 10;
		}
		if (resultat < -10) {
			return -10;
		}
		return resultat;
	}

	@Override
	public String toString() {
		return nom;
	}
}