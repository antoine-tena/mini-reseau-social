package socialnetwork.front;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import socialnetwork.front.carte.FenetreCarteVille;
import socialnetwork.front.seirs.FenetreSimulationSEIRS;

/**
 * Point d'entree pour l'interface graphique.
 * Sans argument : ouvre la carte ville classique.
 * Avec argument "seirs" (ou un nombre) : ouvre le Crisis Center SEIRS.
 */
public class ApplicationGraphique {

	private ApplicationGraphique() {
	}

	public static void main(String[] args) {
		boolean launchSeirs = args.length > 0 &&
			(args[0].equalsIgnoreCase("seirs") || args[0].matches("\\d+"));

		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception exception) {
				// pas bloquant si le look and feel ne peut pas etre applique
			}

			if (launchSeirs) {
				int pop = 1200;
				try { if (args[0].matches("\\d+")) pop = Integer.parseInt(args[0]); }
				catch (NumberFormatException ignored) {}
				FenetreSimulationSEIRS fenSeirs = new FenetreSimulationSEIRS(pop);
				fenSeirs.chargerEtDemarrer();
			} else {
				FenetreCarteVille fenCarte = new FenetreCarteVille();
				fenCarte.chargerEtDemarrer();
			}
		});
	}
}

