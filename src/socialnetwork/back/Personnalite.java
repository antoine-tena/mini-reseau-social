package socialnetwork.back;

/**
 * Regle simple: applique une variation fixe de bienveillance.
 */
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

	/**
	 * Modifie la bienveillance en appliquant la variation.
	 */
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

	/**
	 * Phase 4 : version contextuelle qui prend en compte le contenu du message.
	 * Par defaut, ignore le contenu (comportement identique a appliquer(float)).
	 */
	public float appliquer(float bienveillance, String contenu) {
		return appliquer(bienveillance);
	}

	@Override
	public String toString() {
		return nom;
	}
}
