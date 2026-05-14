package socialnetwork.back.analyse;

/**
 * Enumeration des 7 categories emotionnelles utilisees par l'analyseur de sentiment.
 *
 * Chaque emotion possede un libelle lisible et un emoji associe pour l'affichage UI.
 */
public enum Emotion {
	JOIE, TRISTESSE, COLERE, PEUR, SURPRISE, DEGOUT, NEUTRE;

	/** Libelle lisible en francais. */
	public String label() {
		return switch (this) {
			case JOIE      -> "joie";
			case TRISTESSE -> "tristesse";
			case COLERE    -> "colere";
			case PEUR      -> "peur";
			case SURPRISE  -> "surprise";
			case DEGOUT    -> "degout";
			case NEUTRE    -> "neutre";
		};
	}

	/** Emoji ASCII associe (affichage dans les interfaces texte/Swing). */
	public String emoji() {
		return switch (this) {
			case JOIE      -> ":)";
			case TRISTESSE -> ":(";
			case COLERE    -> ">:(";
			case PEUR      -> ":/";
			case SURPRISE  -> ":O";
			case DEGOUT    -> ":/";
			case NEUTRE    -> ":|";
		};
	}

	/** Couleur HTML suggeree pour l'affichage dans les JLabel Swing. */
	public String couleurHtml() {
		return switch (this) {
			case JOIE      -> "green";
			case TRISTESSE -> "#555588";
			case COLERE    -> "red";
			case PEUR      -> "darkorange";
			case SURPRISE  -> "purple";
			case DEGOUT    -> "brown";
			case NEUTRE    -> "gray";
		};
	}
}
