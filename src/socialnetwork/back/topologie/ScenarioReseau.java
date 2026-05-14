package socialnetwork.back.topologie;

import java.util.List;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.personnalite.Personnalite;
import socialnetwork.back.personnalite.PersonnaliteAdaptative;
import socialnetwork.back.personnalite.PersonnaliteContextuelle;
import socialnetwork.back.personnalite.PersonnaliteForte;
import socialnetwork.back.personnalite.PersonnaliteOscillante;
import socialnetwork.back.personnalite.PersonnaliteVirale;
import socialnetwork.back.stats.JournalReseau;
import socialnetwork.back.stats.StatistiquesReseau;

/**
 * Scenarios pre-configures de propagation pour la simulation.
 *
 * Chaque scenario cree un reseau avec des bavards et des connexions
 * specifiques, configure leurs personnalites/seuils/probas, puis
 * retourne une description textuelle du scenario.
 *
 * Utilisation :
 *   Reseau r = new Reseau();
 *   StatistiquesReseau s = new StatistiquesReseau(r);
 *   JournalReseau j = new JournalReseau(r);
 *   String desc = ScenarioReseau.appliquerScenario(r, Scenario.FAKE_NEWS, s, j);
 *   // lancer les propagations ensuite
 */
public final class ScenarioReseau {

	private ScenarioReseau() {}

	// ─── Enum Scenario ────────────────────────────────────────────────────────

	public enum Scenario {
		FAKE_NEWS(
			"Fake News",
			"Un influenceur central diffuse des messages negatifs vers un reseau en etoile."
			+ " Les relayeurs ont une probabilite reduite de retransmission."),

		OPINION_POLARIZATION(
			"Polarisation d'opinions",
			"Deux camps (positif / negatif) relies par un pont central."
			+ " Personnalites fortes aux extremites, neutre au milieu."),

		EMOTIONAL_CONTAGION(
			"Contagion emotionnelle",
			"Reseau circulaire de bavards adaptatifs."
			+ " La bienveillance se propage et evolue selon les messages recus."),

		INFLUENCEUR(
			"Influenceur viral",
			"Un bavard avec une personnalite virale au centre d'un petit monde."
			+ " Sa probabilite augmente avec les buzz successifs.");

		private final String titre;
		private final String description;

		Scenario(String titre, String description) {
			this.titre       = titre;
			this.description = description;
		}

		public String getTitre()       { return titre; }
		public String getDescription() { return description; }

		@Override
		public String toString()       { return titre; }
	}

	// ─── Application ──────────────────────────────────────────────────────────

	/**
	 * Configure le reseau selon le scenario choisi.
	 *
	 * Le reseau doit etre vide avant l'appel.
	 * Les statistiques et le journal sont abonnes aux nouveaux bavards.
	 *
	 * @return description textuelle du scenario
	 */
	public static String appliquerScenario(Reseau reseau, Scenario scenario,
			StatistiquesReseau stats, JournalReseau journal) {
		return switch (scenario) {
			case FAKE_NEWS            -> scenarioFakeNews(reseau, stats, journal);
			case OPINION_POLARIZATION -> scenarioPolarisation(reseau, stats, journal);
			case EMOTIONAL_CONTAGION  -> scenarioContagion(reseau, stats, journal);
			case INFLUENCEUR          -> scenarioInfluenceur(reseau, stats, journal);
		};
	}

	// ─── Scenarios ────────────────────────────────────────────────────────────

	private static String scenarioFakeNews(Reseau reseau, StatistiquesReseau stats,
			JournalReseau journal) {
		// Hub central + 8 relayeurs + 2 terminaux par relayeur
		Bavard hub = new Bavard("Hub", Personnalite.NEGATIF);
		hub.setProbabiliteRetransmission(1.0);
		ajouterEtObserver(reseau, stats, journal, hub);

		for (int i = 1; i <= 8; i++) {
			Bavard relayeur = new Bavard("R" + i, PersonnaliteForte.NEGATIF_FORTE);
			relayeur.setProbabiliteRetransmission(0.6);
			ajouterEtObserver(reseau, stats, journal, relayeur);
			reseau.connecter(hub, relayeur);

			for (int j = 1; j <= 2; j++) {
				Bavard terminal = new Bavard("T" + i + "_" + j, Personnalite.NEUTRE);
				terminal.setProbabiliteRetransmission(0.4);
				terminal.setSeuilTolerance(-3f);
				ajouterEtObserver(reseau, stats, journal, terminal);
				reseau.connecter(relayeur, terminal);
			}
		}

		return Scenario.FAKE_NEWS.getTitre() + "\n\n" + Scenario.FAKE_NEWS.getDescription()
				+ "\n\nReseau : hub -> 8 relayeurs -> 2 terminaux chacun"
				+ "\nProba hub=100%  relayeurs=60%  terminaux=40%"
				+ "\nSeuil tolerance terminaux=-3"
				+ "\n\nLancez la propagation depuis Hub avec un message negatif.";
	}

	private static String scenarioPolarisation(Reseau reseau, StatistiquesReseau stats,
			JournalReseau journal) {
		// Camps positif + negatif (5 chacun), pont central
		Bavard pont = new Bavard("Pont", Personnalite.NEUTRE);
		ajouterEtObserver(reseau, stats, journal, pont);

		String[] nomsCamp1 = { "Aria", "Bram", "Cara", "Dina", "Evan" };
		String[] nomsCamp2 = { "Finn", "Gaia", "Hugo", "Iris", "Jade" };

		Bavard precedent1 = pont;
		for (String nom : nomsCamp1) {
			Bavard b = new Bavard(nom, PersonnaliteForte.POSITIF_FORTE);
			ajouterEtObserver(reseau, stats, journal, b);
			reseau.connecter(precedent1, b);
			precedent1 = b;
		}
		// fermer le camp positif en anneau
		reseau.connecter(precedent1, pont);

		Bavard precedent2 = pont;
		for (String nom : nomsCamp2) {
			Bavard b = new Bavard(nom, PersonnaliteForte.NEGATIF_FORTE);
			ajouterEtObserver(reseau, stats, journal, b);
			reseau.connecter(precedent2, b);
			precedent2 = b;
		}
		reseau.connecter(precedent2, pont);

		return Scenario.OPINION_POLARIZATION.getTitre() + "\n\n"
				+ Scenario.OPINION_POLARIZATION.getDescription()
				+ "\n\nReseau : deux camps de 5 bavards relies par Pont"
				+ "\nCamp positif : Aria-Bram-Cara-Dina-Evan"
				+ "\nCamp negatif : Finn-Gaia-Hugo-Iris-Jade"
				+ "\n\nEssayez d'envoyer un message positif depuis Aria,"
				+ "\npuis negatif depuis Finn, et observez l'evolution de la bienveillance.";
	}

	private static String scenarioContagion(Reseau reseau, StatistiquesReseau stats,
			JournalReseau journal) {
		// Anneau de 10 bavards adaptatifs a oscillations differentes
		int n = 10;
		Bavard[] bavards = new Bavard[n];
		String[] noms = { "Ana", "Ben", "Clo", "Dan", "Eva",
		                  "Fea", "Gil", "Hau", "Ian", "Jul" };
		for (int i = 0; i < n; i++) {
			PersonnaliteAdaptative pa = new PersonnaliteAdaptative(
					"ADAPT", (i % 2 == 0 ? 0.3f : -0.3f), 0.15f, 4f);
			bavards[i] = new Bavard(noms[i], pa);
			ajouterEtObserver(reseau, stats, journal, bavards[i]);
		}
		for (int i = 0; i < n; i++) {
			reseau.connecter(bavards[i], bavards[(i + 1) % n]);
		}

		return Scenario.EMOTIONAL_CONTAGION.getTitre() + "\n\n"
				+ Scenario.EMOTIONAL_CONTAGION.getDescription()
				+ "\n\nReseau : anneau de " + n + " bavards adaptatifs"
				+ "\nAlternance positif/negatif initial"
				+ "\n\nLancez la simulation automatique et observez comment"
				+ "\nla bienveillance evolue au fil des cycles.";
	}

	private static String scenarioInfluenceur(Reseau reseau, StatistiquesReseau stats,
			JournalReseau journal) {
		// Petit monde 12 noeuds, un noeud avec PersonnaliteVirale au centre
		int n = 12;
		GenerateurTopologie.petitMonde(reseau, n, 4, 0.2, new Personnalite[]{
			Personnalite.NEUTRE, Personnalite.POSITIF, Personnalite.NEGATIF
		}, 2024L);

		// Abonner tous les bavards et configurer le premier comme viral
		List<Bavard> bavards = reseau.getBavards();
		for (Bavard b : bavards) {
			stats.observerBavard(b);
			journal.observerBavard(b);
		}
		if (!bavards.isEmpty()) {
			Bavard influenceur = bavards.get(0);
			PersonnaliteVirale pv = new PersonnaliteVirale(
				"VIRAL", 0.5f, 0.5, 0.08, 0.95, 2.0f, -1.0f);
			pv.synchroniserAvecBavard(influenceur);
			influenceur.setProbabiliteRetransmission(pv.getProbaActuelle());
		}

		return Scenario.INFLUENCEUR.getTitre() + "\n\n"
				+ Scenario.INFLUENCEUR.getDescription()
				+ "\n\nReseau : petit monde (Watts-Strogatz) de " + n + " noeuds"
				+ "\nBavard[0] a une personnalite virale (proba augmente avec les buzz)"
				+ "\n\nLancez plusieurs propagations depuis " + bavards.get(0).getNom()
				+ "\net observez le score viral progresser.";
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	private static void ajouterEtObserver(Reseau reseau, StatistiquesReseau stats,
			JournalReseau journal, Bavard bavard) {
		reseau.ajouterBavard(bavard);
		stats.observerBavard(bavard);
		journal.observerBavard(bavard);
	}
}
