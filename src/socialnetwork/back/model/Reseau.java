package socialnetwork.back.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Stocke les bavards et leurs connexions.
 */
public class Reseau {
	// liste simple de tous les bavards
	private final List<Bavard> bavards;

	public Reseau() {
		this.bavards = new ArrayList<>();
	}

	public void ajouterBavard(Bavard bavard) {
		if (bavard == null || bavards.contains(bavard)) return;
		bavards.add(bavard);
	}

	/**
	 * Cree un lien bidirectionnel entre deux bavards.
	 */
	public void connecter(Bavard premierBavard, Bavard deuxiemeBavard) {
		if (premierBavard == null || deuxiemeBavard == null) return;
		if (premierBavard == deuxiemeBavard) return;
		premierBavard.ajouterAmi(deuxiemeBavard);
		deuxiemeBavard.ajouterAmi(premierBavard);
	}

	public List<Bavard> getBavards() {
		return new ArrayList<>(bavards);
	}
}
