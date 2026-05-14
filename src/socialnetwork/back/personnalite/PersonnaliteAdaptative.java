package socialnetwork.back.personnalite;

/**
 * Personnalite avec apprentissage.
 *
 * Sa variation evolue a chaque message recu :
 *  - si la bienveillance entrante est positive  -> variation augmente de delta
 *  - si la bienveillance entrante est negative  -> variation diminue de delta
 *
 * La variation reste bornee dans [-variationMax, variationMax].
 *
 * Simule un bavard qui s'adapte a son environnement : entourage positif ->
 * il devient plus positif au fil du temps.
 */
public class PersonnaliteAdaptative extends Personnalite {

	private float variationCourante;
	private final float delta;
	private final float variationMax;
	private int nbAdaptations = 0;

	public PersonnaliteAdaptative(String nom, float variationInitiale, float delta, float variationMax) {
		super(nom, variationInitiale);
		this.variationCourante = variationInitiale;
		this.delta = delta;
		this.variationMax = Math.abs(variationMax);
	}

	@Override
	public float appliquer(float bienveillance) {
		adapter(bienveillance);
		float resultat = bienveillance + variationCourante;
		if (resultat > 10f) return 10f;
		if (resultat < -10f) return -10f;
		return resultat;
	}

	@Override
	public float appliquer(float bienveillance, String contenu) {
		return appliquer(bienveillance);
	}

	private void adapter(float bienveillance) {
		if (bienveillance > 0f) {
			variationCourante = Math.min(variationCourante + delta, variationMax);
		} else if (bienveillance < 0f) {
			variationCourante = Math.max(variationCourante - delta, -variationMax);
		}
		nbAdaptations++;
	}

	public float getVariationCourante() { return variationCourante; }
	public int getNbAdaptations()       { return nbAdaptations; }

	@Override
	public String toString() {
		return getNom() + "[adaptatif v=" + String.format("%.2f", variationCourante)
				+ " #" + nbAdaptations + "]";
	}
}
