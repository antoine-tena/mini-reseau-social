package socialnetwork.back;

import java.util.ArrayList;
import java.util.List;

public class Reseau {
	// liste simple de tous les bavards
	private final List<Bavard> bavards;

	public Reseau() {
		this.bavards = new ArrayList<>();
	}

	public void ajouterBavard(Bavard bavard) {
		if (bavard == null) {
			return;
		}
		if (bavards.contains(bavard)) {
			return;
		}
		bavards.add(bavard);
	}

	public void connecter(Bavard premierBavard, Bavard deuxiemeBavard) {
		if (premierBavard == null) {
			return;
		}
		if (deuxiemeBavard == null) {
			return;
		}
		if (premierBavard == deuxiemeBavard) {
			return;
		}
		// lien dans les 2 sens
		premierBavard.ajouterAmi(deuxiemeBavard);
		deuxiemeBavard.ajouterAmi(premierBavard);
	}

	public List<Bavard> getBavards() {
		return new ArrayList<>(bavards);
	}
}