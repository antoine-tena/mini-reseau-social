package socialnetwork.front;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ApplicationGraphique {
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