package socialnetwork.front;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import socialnetwork.front.fenetre.FenetreReseau;

/**
 * Point d'entree pour l'interface graphique.
 */
public class ApplicationGraphique {

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
