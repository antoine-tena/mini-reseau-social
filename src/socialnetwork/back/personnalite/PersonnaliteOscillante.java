package socialnetwork.back.personnalite;

/**
 * Personnalite oscillante sinusoidalement.
 *
 * La variation de bienveillance suit :
 *     variation(t) = amplitude * sin(2*pi*t / periode + decalage)
 * ou t est le numero de message traite depuis la creation du bavard.
 *
 * Simule un bavard "humeur changeante" dont l'attitude fluctue cycliquement.
 *
 * Exemple :
 *   new PersonnaliteOscillante("LUNAIRE", 2.0f, 8, 0.0)
 *   -> la variation oscille entre -2 et +2 sur 8 messages
 */
public class PersonnaliteOscillante extends Personnalite {

	private final float  amplitude;
	private final int    periode;
	private final double decalage;

	/** Numero de message traite depuis la creation (calcul de phase). */
	private int tick = 0;

	public PersonnaliteOscillante(String nom, float amplitude, int periode, double decalage) {
		super(nom, amplitude);
		this.amplitude = Math.abs(amplitude);
		this.periode   = Math.max(1, periode);
		this.decalage  = decalage;
	}

	/** Variation courante selon le tick actuel. */
	public float getVariationCourante() {
		return (float)(amplitude * Math.sin(2 * Math.PI * tick / periode + decalage));
	}

	public int getTick()    { return tick; }

	/** Phase actuelle en radians. */
	public double getPhase() {
		return (2 * Math.PI * tick / periode + decalage) % (2 * Math.PI);
	}

	@Override
	public float appliquer(float bienveillance) {
		float variation = getVariationCourante();
		tick++;
		float resultat = bienveillance + variation;
		if (resultat >  10f) return  10f;
		if (resultat < -10f) return -10f;
		return resultat;
	}

	@Override
	public float appliquer(float bienveillance, String contenu) {
		return appliquer(bienveillance);
	}

	/** Remet le compteur a zero. */
	public void reinitialiserTick() { tick = 0; }

	@Override
	public String toString() {
		return getNom() + String.format("[osc amp=%.1f T=%d phi=%.2f t=%d]",
				amplitude, periode, decalage, tick);
	}
}
