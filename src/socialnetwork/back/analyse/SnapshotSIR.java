package socialnetwork.back.analyse;

/**
 * Snapshot de l'etat SIR a un pas de temps donne dans {@link ModeleEpidemique}.
 *
 * Classe immuable — toutes les instances sont creees par ModeleEpidemique.
 *
 * Champs :
 *   pas           : numero du pas de temps (0 = etat initial)
 *   susceptibles  : nombre de noeuds en etat S
 *   infectes      : nombre de noeuds en etat I
 *   remis         : nombre de noeuds en etat R
 *   taux          : proportion d'infectes parmi tous les noeuds
 */
public final class SnapshotSIR {

	public final int    pas;
	public final int    susceptibles;
	public final int    infectes;
	public final int    remis;
	/** Taux d'infection instantane : I / (S + I + R). */
	public final double taux;

	SnapshotSIR(int pas, int s, int i, int r) {
		this.pas          = pas;
		this.susceptibles = s;
		this.infectes     = i;
		this.remis        = r;
		int total = s + i + r;
		this.taux = total == 0 ? 0.0 : (double) i / total;
	}

	/** Vrai si l'epidemie est eteinte (aucun infecte). */
	public boolean estEteinte()  { return infectes == 0; }

	/** Proportion de la population ayant ete infectee (I + R) / N. */
	public double tauxAttaque() {
		int n = susceptibles + infectes + remis;
		return n == 0 ? 0.0 : (double)(infectes + remis) / n;
	}

	@Override
	public String toString() {
		return String.format("t=%3d  S=%3d  I=%3d  R=%3d  taux=%.2f",
				pas, susceptibles, infectes, remis, taux);
	}
}
