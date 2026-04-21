package socialnetwork;

public enum Personnalite {
	POSITIF(1),
	NEGATIF(-1),
	NEUTRE(0);

	private final int bienveillance;

	Personnalite(int bienveillance) {
		this.bienveillance = bienveillance;
	}

	public int getBienveillance() {
		return bienveillance;
	}
}