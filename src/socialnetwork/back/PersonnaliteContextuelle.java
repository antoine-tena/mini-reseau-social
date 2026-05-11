package socialnetwork.back;

import java.util.Set;

/**
 * Phase 4 - personnalite avancee.
 *
 * Se comporte comme la personnalite de base SAUF si le message contient
 * l'un des mots cles definissables. Dans ce cas, la variation est multipliee
 * par un facteur d'amplification (positif ou negatif selon le contexte).
 *
 * Exemple : un bavard tres sensible aux messages negatifs
 *   new PersonnaliteContextuelle("SENSIBLE", -0.5f, Set.of("haine","colere","triste"), 3f)
 * -> si le message contient "haine", variation = -0.5 * 3 = -1.5 par hop
 */
public class PersonnaliteContextuelle extends Personnalite {

	// mots cles qui declenchent le mode contextuel (insensible a la casse)
	private final Set<String> motsCles;
	// facteur applique quand un mot cle est detecte
	private final float facteurContextuel;

	public PersonnaliteContextuelle(String nom, float variationBienveillance,
			Set<String> motsCles, float facteurContextuel) {
		super(nom, variationBienveillance);
		this.motsCles = Set.copyOf(motsCles);
		this.facteurContextuel = facteurContextuel;
	}

	@Override
	public float appliquer(float bienveillance) {
		// sans contenu on fait le comportement de base
		return super.appliquer(bienveillance);
	}

	@Override
	public float appliquer(float bienveillance, String contenu) {
		if (contenu == null) {
			return super.appliquer(bienveillance);
		}
		String lower = contenu.toLowerCase();
		boolean motTrouve = motsCles.stream().anyMatch(lower::contains);
		float variation = motTrouve
				? getVariationBienveillance() * facteurContextuel
				: getVariationBienveillance();
		float resultat = bienveillance + variation;
		if (resultat > 10f) return 10f;
		if (resultat < -10f) return -10f;
		return resultat;
	}

	@Override
	public String toString() {
		return super.toString() + "[contextuel x" + facteurContextuel + " sur " + motsCles + "]";
	}
}
