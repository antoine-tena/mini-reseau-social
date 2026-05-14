package socialnetwork.back.personnalite;

import socialnetwork.back.model.Bavard;

/**
 * Personnalite "virale" : la probabilite de retransmission du bavard
 * augmente apres chaque message positif et diminue apres chaque message negatif.
 *
 * Modele le FOMO (Fear Of Missing Out) et l'effet de buzz :
 * plus un message positif circule, plus ce bavard a envie de le partager.
 *
 * Parametres :
 *   probaBase    : probabilite initiale de retransmission [0, 1]
 *   deltaBuzz    : increment/decrement de probabilite a chaque buzz
 *   probaMax     : probabilite max (typiquement 0.95)
 *   seuilBuzz    : bienveillance min pour declencher le buzz (ex: 2.0f)
 *   seuilNegatif : bienveillance max pour declarer negatif   (ex: -1.0f)
 *
 * Exemple :
 *   new PersonnaliteVirale("VIRAL+", 0.3f, 0.5, 0.08, 0.95, 2.0f, -1.0f)
 */
public class PersonnaliteVirale extends Personnalite {

	private final double probaBase;
	private final double deltaBuzz;
	private final double probaMax;
	private final double probaMin;
	private final float  seuilBuzz;
	private final float  seuilNegatif;

	private double probaActuelle;
	private int    compteurBuzz    = 0;
	private int    compteurNegatif = 0;

	public PersonnaliteVirale(String nom, float variation,
			double probaBase, double deltaBuzz, double probaMax,
			float seuilBuzz, float seuilNegatif) {
		super(nom, variation);
		this.probaBase     = clamp01(probaBase);
		this.deltaBuzz     = Math.abs(deltaBuzz);
		this.probaMax      = clamp01(probaMax);
		this.probaMin      = 0.05;
		this.seuilBuzz     = seuilBuzz;
		this.seuilNegatif  = seuilNegatif;
		this.probaActuelle = this.probaBase;
	}

	public double getProbaActuelle()    { return probaActuelle; }
	public int getCompteurBuzz()        { return compteurBuzz; }
	public int getCompteurNegatif()     { return compteurNegatif; }

	public void reinitialiser() {
		probaActuelle   = probaBase;
		compteurBuzz    = 0;
		compteurNegatif = 0;
	}

	@Override
	public float appliquer(float bienveillance, String contenu) {
		float nouvelleB = appliquer(bienveillance);
		mettreAJourProba(nouvelleB);
		return nouvelleB;
	}

	@Override
	public float appliquer(float bienveillance) {
		float resultat = bienveillance + getVariationBienveillance();
		if (resultat >  10f) return  10f;
		if (resultat < -10f) return -10f;
		return resultat;
	}

	/**
	 * Ajuste la probabilite interne selon la bienveillance resultante.
	 * Peut etre appele manuellement pour forcer la mise a jour.
	 */
	public void mettreAJourProba(float bienveillanceResultante) {
		if (bienveillanceResultante >= seuilBuzz) {
			compteurBuzz++;
			probaActuelle = Math.min(probaMax, probaActuelle + deltaBuzz);
		} else if (bienveillanceResultante <= seuilNegatif) {
			compteurNegatif++;
			probaActuelle = Math.max(probaMin, probaActuelle - deltaBuzz);
		}
	}

	/**
	 * Applique la probabilite virale courante sur un bavard.
	 * A appeler une fois apres avoir cree le bavard avec cette personnalite.
	 */
	public void synchroniserAvecBavard(Bavard bavard) {
		bavard.setProbabiliteRetransmission(probaActuelle);
	}

	@Override
	public String toString() {
		return getNom() + String.format("[viral p=%.2f buzz=%d neg=%d]",
				probaActuelle, compteurBuzz, compteurNegatif);
	}

	private static double clamp01(double v) {
		return Math.max(0.0, Math.min(1.0, v));
	}
}
