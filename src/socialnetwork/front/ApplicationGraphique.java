package socialnetwork.front;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Point d'entree pour l'interface graphique.
 */
public class ApplicationGraphique {
	// Demarre l'UI sur le thread Swing.
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception exception) {
				// pas bloquant si le look and feel ne peut pas etre applique
			}

			FenetreReseau fenetre = new FenetreReseau();
			fenetre.setVisible(true);
		});
	}
}
