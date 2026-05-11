package socialnetwork.back;

/**
 * Phase 4 - personnalite avec apprentissage.
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

	// variation actuelle, evolue a chaque appel
	private float variationCourante;
	// pas d'evolution a chaque message
	private final float delta;
	// borne absolue de la variation
	private final float variationMax;
	// compteur d'adaptations
	private int nbAdaptations = 0;

	public PersonnaliteAdaptative(String nom, float variationInitiale, float delta, float variationMax) {
		super(nom, variationInitiale);
		this.variationCourante = variationInitiale;
		this.delta = delta;
		this.variationMax = Math.abs(variationMax);
	}

	@Override
	public float appliquer(float bienveillance) {
		// adapte avant d'appliquer
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

	// Fait evoluer la variation selon le signe de la bienveillance entrante.
	private void adapter(float bienveillance) {
		if (bienveillance > 0f) {
			variationCourante = Math.min(variationCourante + delta, variationMax);
		} else if (bienveillance < 0f) {
			variationCourante = Math.max(variationCourante - delta, -variationMax);
		}
		nbAdaptations++;
	}

	public float getVariationCourante() {
		return variationCourante;
	}

	public int getNbAdaptations() {
		return nbAdaptations;
	}

	@Override
	public String toString() {
		return getNom() + "[adaptatif v=" + String.format("%.2f", variationCourante)
				+ " #" + nbAdaptations + "]";
	}
}
