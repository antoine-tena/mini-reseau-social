package socialnetwork.back.personnalite;

import java.util.Set;

/**
 * Personnalite avancee qui reagit au contenu du message.
 *
 * Se comporte comme la personnalite de base SAUF si le message contient
 * l'un des mots cles. Dans ce cas, la variation est multipliee par un facteur.
 *
 * Exemple :
 *   new PersonnaliteContextuelle("SENSIBLE", -0.5f, Set.of("haine","colere","triste"), 3f)
 *   -> si le message contient "haine", variation = -0.5 * 3 = -1.5 par hop
 */
public class PersonnaliteContextuelle extends Personnalite {

	/** Amplifie les messages positifs (joie, amour, merci...). */
	public static final PersonnaliteContextuelle SENSIBLE_POSITIF = new PersonnaliteContextuelle(
		"SENSIBLE+", 0.5f,
		Set.of("amour", "joie", "merci", "bravo", "super", "genial", "bien"), 3f);

	/** Amplifie les messages negatifs (haine, colere, triste...). */
	public static final PersonnaliteContextuelle SENSIBLE_NEGATIF = new PersonnaliteContextuelle(
		"SENSIBLE-", -0.5f,
		Set.of("haine", "colere", "triste", "nul", "mauvais", "horrible", "non"), 3f);

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
		return super.appliquer(bienveillance);
	}

	@Override
	public float appliquer(float bienveillance, String contenu) {
		if (contenu == null) return super.appliquer(bienveillance);
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
