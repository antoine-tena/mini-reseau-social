package socialnetwork.back.analyse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;

/**
 * Modele epidemique SIR applique a la propagation d'information dans un reseau.
 *
 * Chaque bavard est dans un etat :
 *   S (Susceptible)  : n'a pas encore recu l'information
 *   I (Infecte)      : a recu l'information et peut la transmettre
 *   R (Remis)        : a recu l'information mais ne la transmet plus
 *
 * A chaque pas de temps (tick) :
 *   - chaque I tente d'infecter chacun de ses voisins S avec probabilite beta
 *   - chaque I passe a R avec probabilite gamma
 *
 * Parametres epidemiologiques :
 *   beta  : taux de transmission [0, 1]
 *   gamma : taux de guerison     [0, 1]
 *   R0 = beta / gamma : nombre de reproduction de base
 *
 * Deux modes de simulation :
 *   simuler(n)       : n pas de temps, retourne la serie temporelle
 *   simulerJusquaFin : jusqu'a I = 0 (max 1000 pas)
 *
 * Exemple :
 *   ModeleEpidemique sir = new ModeleEpidemique(reseau, 0.3, 0.1, 42L);
 *   sir.infecterInitialement(Set.of("Lina"));
 *   List<SnapshotSIR> histo = sir.simulerJusquaFin();
 */
public class ModeleEpidemique {

	// ─── Etat ────────────────────────────────────────────────────────────────

	public enum Etat { S, I, R }

	// ─── Champs ───────────────────────────────────────────────────────────────

	private final Reseau reseau;
	private final double beta;
	private final double gamma;
	private final Random rng;

	/** Etat courant de chaque bavard. */
	private final Map<Bavard, Etat> etats = new HashMap<>();
	/** Historique des snapshots depuis le debut de la simulation. */
	private final List<SnapshotSIR> historique = new ArrayList<>();

	private int pasCourant = 0;

	// ─── Constructeur ─────────────────────────────────────────────────────────

	/**
	 * @param reseau le reseau a simuler
	 * @param beta   taux de transmission (0.0 – 1.0)
	 * @param gamma  taux de guerison     (0.0 – 1.0)
	 * @param seed   graine RNG pour la reproductibilite
	 */
	public ModeleEpidemique(Reseau reseau, double beta, double gamma, long seed) {
		this.reseau = reseau;
		this.beta   = Math.max(0, Math.min(1, beta));
		this.gamma  = Math.max(0, Math.min(1, gamma));
		this.rng    = new Random(seed);
		reinitialiser();
	}

	// ─── Initialisation ───────────────────────────────────────────────────────

	/** Remet tous les bavards en etat S et vide l'historique. */
	public void reinitialiser() {
		etats.clear();
		historique.clear();
		pasCourant = 0;
		for (Bavard b : reseau.getBavards()) etats.put(b, Etat.S);
		enregistrerSnapshot();
	}

	/**
	 * Infecte initialement un sous-ensemble de bavards par leurs noms.
	 * Les bavards non trouves sont ignores silencieusement.
	 */
	public void infecterInitialement(Set<String> noms) {
		for (Bavard b : reseau.getBavards()) {
			if (noms.contains(b.getNom())) etats.put(b, Etat.I);
		}
		if (!historique.isEmpty()) historique.remove(historique.size() - 1);
		enregistrerSnapshot();
	}

	/**
	 * Infecte un bavard aleatoire (patient zero).
	 *
	 * @return le nom du bavard infecte, ou null si reseau vide
	 */
	public String infecterAleatoire() {
		List<Bavard> susceptibles = getSusceptibles();
		if (susceptibles.isEmpty()) return null;
		Bavard choisi = susceptibles.get(rng.nextInt(susceptibles.size()));
		etats.put(choisi, Etat.I);
		if (!historique.isEmpty()) historique.remove(historique.size() - 1);
		enregistrerSnapshot();
		return choisi.getNom();
	}

	// ─── Simulation ───────────────────────────────────────────────────────────

	/** Avance la simulation d'un pas de temps. */
	public SnapshotSIR tick() {
		pasCourant++;
		Map<Bavard, Etat> nouvelEtat = new HashMap<>(etats);

		for (Bavard b : reseau.getBavards()) {
			if (etats.get(b) != Etat.I) continue;

			// tentatives d'infection des voisins S
			for (Bavard voisin : b.getAmis()) {
				if (etats.get(voisin) == Etat.S && rng.nextDouble() < beta) {
					nouvelEtat.put(voisin, Etat.I);
				}
			}
			// guerison
			if (rng.nextDouble() < gamma) {
				nouvelEtat.put(b, Etat.R);
			}
		}

		etats.putAll(nouvelEtat);
		enregistrerSnapshot();
		return historique.get(historique.size() - 1);
	}

	/**
	 * Simule exactement n pas de temps.
	 *
	 * @return liste de tous les snapshots (inclut l'etat initial)
	 */
	public List<SnapshotSIR> simuler(int n) {
		for (int i = 0; i < n; i++) tick();
		return Collections.unmodifiableList(historique);
	}

	/**
	 * Simule jusqu'a extinction des infectes (I = 0) ou 1000 pas max.
	 *
	 * @return la liste complete des snapshots
	 */
	public List<SnapshotSIR> simulerJusquaFin() {
		int maxPas = 1000;
		while (compterEtat(Etat.I) > 0 && pasCourant < maxPas) tick();
		return Collections.unmodifiableList(historique);
	}

	// ─── Acces aux resultats ──────────────────────────────────────────────────

	public Etat getEtat(Bavard b)               { return etats.getOrDefault(b, Etat.S); }
	public Map<Bavard, Etat> getEtats()         { return Collections.unmodifiableMap(etats); }
	public List<SnapshotSIR> getHistorique()    { return Collections.unmodifiableList(historique); }
	public int getPasCourant()                  { return pasCourant; }
	public double getR0() { return gamma == 0 ? Double.POSITIVE_INFINITY : beta / gamma; }

	/** Proportion finale d'infectes et gueris (taux d'attaque). */
	public double getTauxAttaqueFinal() {
		int n = reseau.getBavards().size();
		if (n == 0) return 0.0;
		return (double)(compterEtat(Etat.R) + compterEtat(Etat.I)) / n;
	}

	// ─── Rapport ──────────────────────────────────────────────────────────────

	/** Resume textuel lisible apres simulation. */
	public String rapport() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== modele epidemique SIR ===\n");
		sb.append(String.format("beta=%.2f  gamma=%.2f  R0=%.2f%n", beta, gamma, getR0()));
		sb.append("noeuds : ").append(reseau.getBavards().size()).append('\n');
		sb.append("pas de temps : ").append(pasCourant).append('\n');
		if (!historique.isEmpty()) {
			SnapshotSIR dernier = historique.get(historique.size() - 1);
			sb.append("etat final : S=").append(dernier.susceptibles)
			  .append(" I=").append(dernier.infectes)
			  .append(" R=").append(dernier.remis).append('\n');
		}
		sb.append(String.format("taux d'attaque final : %.1f%%%n",
				getTauxAttaqueFinal() * 100));

		SnapshotSIR pic = historique.stream()
				.max((a, b2) -> Integer.compare(a.infectes, b2.infectes))
				.orElse(null);
		if (pic != null) {
			sb.append(String.format("pic infectes : %d a t=%d%n", pic.infectes, pic.pas));
		}

		sb.append("\n--- courbe SIR ---\n");
		int step = Math.max(1, historique.size() / 20);
		for (int i = 0; i < historique.size(); i += step) {
			sb.append(historique.get(i)).append('\n');
		}
		return sb.toString();
	}

	/** Serie temporelle de la courbe I sous forme de tableau. */
	public int[] courbeInfectes() {
		int[] courbe = new int[historique.size()];
		for (int i = 0; i < historique.size(); i++) courbe[i] = historique.get(i).infectes;
		return courbe;
	}

	/** Serie temporelle S, I, R : result[t] = {S, I, R}. */
	public int[][] courbeSIR() {
		int[][] courbe = new int[historique.size()][3];
		for (int i = 0; i < historique.size(); i++) {
			SnapshotSIR s = historique.get(i);
			courbe[i][0] = s.susceptibles;
			courbe[i][1] = s.infectes;
			courbe[i][2] = s.remis;
		}
		return courbe;
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	private int compterEtat(Etat cible) {
		int count = 0;
		for (Etat e : etats.values()) if (e == cible) count++;
		return count;
	}

	private List<Bavard> getSusceptibles() {
		List<Bavard> liste = new ArrayList<>();
		for (Map.Entry<Bavard, Etat> e : etats.entrySet()) {
			if (e.getValue() == Etat.S) liste.add(e.getKey());
		}
		return liste;
	}

	private void enregistrerSnapshot() {
		int s = compterEtat(Etat.S);
		int i = compterEtat(Etat.I);
		int r = compterEtat(Etat.R);
		historique.add(new SnapshotSIR(pasCourant, s, i, r));
	}
}
