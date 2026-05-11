package socialnetwork;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Point d'entree pour l'interface graphique.
 */
public class ApplicationGraphique {
	// Demarre l'UI sur le thread Swing.
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			// look simple du systeme
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception exception) {
				// pas grave si ca rate
			}

			FenetreReseau fenetre = new FenetreReseau();
			fenetre.setVisible(true);
		});
	}
}