package socialnetwork.back.analyse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;

/**
 * Detection de communautes par propagation de labels (Label Propagation Algorithm).
 *
 * Algorithme :
 *   1. Chaque noeud reçoit un label unique (son nom).
 *   2. A chaque iteration, chaque noeud adopte le label le plus frequent
 *      parmi ses voisins (ties brises aleatoirement).
 *   3. On repete jusqu'a convergence (aucun changement) ou 100 iterations.
 *
 * Complexite : O(iterations * (n + m))
 *
 * Retourne : Map<String communaute, List<Bavard> membres>
 *
 * Exemple :
 *   Map<String, List<Bavard>> communautes = DetecteurCommunautes.detecter(reseau);
 *   String rapport = DetecteurCommunautes.rapport(reseau);
 */
public final class DetecteurCommunautes {

	private static final int MAX_ITERATIONS = 100;

	private DetecteurCommunautes() {}

	// ─── API principale ───────────────────────────────────────────────────────

	/**
	 * Detecte les communautes du reseau.
	 *
	 * @return map communaute (label) → liste des bavards membres
	 */
	public static Map<String, List<Bavard>> detecter(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		if (bavards.isEmpty()) return new LinkedHashMap<>();

		Random rng = new Random(42L);

		// initialisation : chaque bavard a son propre label
		Map<Bavard, String> labels = new HashMap<>();
		for (Bavard b : bavards) labels.put(b, b.getNom());

		for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
			boolean changed = false;
			// ordre de traitement aleatoire pour eviter les biais
			List<Bavard> ordre = new ArrayList<>(bavards);
			Collections.shuffle(ordre, rng);

			for (Bavard b : ordre) {
				List<Bavard> amis = b.getAmis();
				if (amis.isEmpty()) continue;

				// compte les labels des voisins
				Map<String, Integer> freq = new HashMap<>();
				for (Bavard ami : amis) {
					freq.merge(labels.get(ami), 1, Integer::sum);
				}

				// trouve le max (avec tie-breaking aleatoire)
				int maxFreq = freq.values().stream().max(Integer::compare).orElse(0);
				List<String> candidats = new ArrayList<>();
				for (Map.Entry<String, Integer> e : freq.entrySet()) {
					if (e.getValue() == maxFreq) candidats.add(e.getKey());
				}
				Collections.sort(candidats); // tri pour reproductibilite
				String nouveauLabel = candidats.get(rng.nextInt(candidats.size()));

				if (!nouveauLabel.equals(labels.get(b))) {
					labels.put(b, nouveauLabel);
					changed = true;
				}
			}

			if (!changed) break; // convergence
		}

		return regrouperParLabel(labels);
	}

	/**
	 * Nombre de communautes detectees.
	 */
	public static int nombreDeCommunautes(Reseau reseau) {
		return detecter(reseau).size();
	}

	/**
	 * Indice de modularite simplifie Q (mesure la qualite de la partition).
	 * Q dans [-0.5, 1.0] : plus Q est grand, mieux les communautes sont separees.
	 */
	public static double modularite(Reseau reseau) {
		Map<String, List<Bavard>> communautes = detecter(reseau);
		List<Bavard> tous = reseau.getBavards();
		int n = tous.size();
		if (n == 0) return 0.0;

		int m = tous.stream().mapToInt(b -> b.getAmis().size()).sum() / 2;
		if (m == 0) return 0.0;

		// construction d'une map bavard -> communaute
		Map<Bavard, String> labelsB = new HashMap<>();
		for (Map.Entry<String, List<Bavard>> entry : communautes.entrySet()) {
			for (Bavard b : entry.getValue()) labelsB.put(b, entry.getKey());
		}

		double q = 0.0;
		for (Bavard i : tous) {
			for (Bavard j : tous) {
				if (!labelsB.get(i).equals(labelsB.get(j))) continue;
				int aij = i.getAmis().contains(j) ? 1 : 0;
				double ki = i.getAmis().size();
				double kj = j.getAmis().size();
				q += aij - (ki * kj) / (2.0 * m);
			}
		}
		return q / (2.0 * m);
	}

	/**
	 * Rapport textuel des communautes detectees.
	 */
	public static String rapport(Reseau reseau) {
		Map<String, List<Bavard>> communautes = detecter(reseau);
		if (communautes.isEmpty()) return "reseau vide\n";

		StringBuilder sb = new StringBuilder();
		sb.append("=== detection de communautes (label propagation) ===\n");
		sb.append("noeuds : ").append(reseau.getBavards().size());
		sb.append("  communautes : ").append(communautes.size());
		sb.append(String.format("  modularite Q = %.3f%n", modularite(reseau)));
		sb.append('\n');

		List<Map.Entry<String, List<Bavard>>> triees =
				new ArrayList<>(communautes.entrySet());
		triees.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

		int rang = 1;
		for (Map.Entry<String, List<Bavard>> entry : triees) {
			sb.append(String.format("Communaute %d (%d membres) :", rang++, entry.getValue().size()));
			for (Bavard b : entry.getValue()) {
				sb.append("  ").append(b.getNom())
				  .append('(').append(b.getPersonnalite()).append(')');
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	/** Regroupe les bavards par label dans un LinkedHashMap trie par taille desc. */
	private static Map<String, List<Bavard>> regrouperParLabel(Map<Bavard, String> labels) {
		Map<String, List<Bavard>> groupes = new HashMap<>();
		for (Map.Entry<Bavard, String> e : labels.entrySet()) {
			groupes.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
		}
		// trier par taille decroissante
		LinkedHashMap<String, List<Bavard>> trie = new LinkedHashMap<>();
		groupes.entrySet().stream()
				.sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
				.forEach(e -> trie.put(e.getKey(), e.getValue()));
		return trie;
	}
}
