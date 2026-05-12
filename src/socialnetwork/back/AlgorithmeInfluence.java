package socialnetwork.back;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Algorithmes d'analyse d'influence sur le graphe de Bavards.
 *
 * Quatre metriques implementees (calcul exact pour petits reseaux) :
 *
 *   DEGRE          nb amis / (n-1)         O(n)
 *   PROXIMITE      inverse somme dist BFS   O(n^2)
 *   INTERMEDIAIRE  betweenness (Brandes)    O(n * (n + m))
 *   PAGE_RANK      iteration 100 cycles     O(iterations * n)
 *
 * Toutes les methodes retournent un LinkedHashMap trie par score decroissant
 * pour un affichage immediat en top-N.
 *
 * Exemple :
 *   LinkedHashMap<Bavard,Double> top = AlgorithmeInfluence.pageRank(reseau);
 *   top.entrySet().stream().limit(3).forEach(e ->
 *       System.out.println(e.getKey().getNom() + " : " + e.getValue()));
 */
public final class AlgorithmeInfluence {

	private AlgorithmeInfluence() {
	}

	// ─── Centralite de degre ──────────────────────────────────────────────────

	/**
	 * Proportion de voisins directs parmi tous les bavards.
	 * Mesure l'activite locale d'un noeud.
	 */
	public static LinkedHashMap<Bavard, Double> centraliteDegre(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		Map<Bavard, Double> scores = new HashMap<>();
		for (Bavard b : bavards) {
			double score = n <= 1 ? 0.0 : (double) b.getAmis().size() / (n - 1);
			scores.put(b, score);
		}
		return trierDecroissant(scores);
	}

	// ─── Centralite de proximite (closeness) ──────────────────────────────────

	/**
	 * Inverse de la distance moyenne vers les autres noeuds.
	 * Un bavard proche peut diffuser rapidement ses messages.
	 */
	public static LinkedHashMap<Bavard, Double> centraliteProximite(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		Map<Bavard, Double> scores = new HashMap<>();
		for (Bavard source : bavards) {
			Map<Bavard, Integer> dist = bfs(source, bavards);
			long sumDist = dist.values().stream().mapToLong(Integer::longValue).sum();
			// composante connexe : on normalise par le nb de noeuds accessibles
			int accessibles = dist.size(); // exclut source (bfs retire source)
			double score = (sumDist == 0 || n <= 1) ? 0.0
					: ((double) accessibles / (n - 1)) * ((double) accessibles / sumDist);
			scores.put(source, score);
		}
		return trierDecroissant(scores);
	}

	// ─── Centralite d'intermediarité (betweenness) ────────────────────────────

	/**
	 * Fraction des plus courts chemins passant par chaque noeud.
	 * Algorithme de Brandes (2001), version non normalisee puis normalisee.
	 * Un noeud a haute betweenness est un "pont" critique du reseau.
	 */
	public static LinkedHashMap<Bavard, Double> centraliteIntermediaire(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		Map<Bavard, Double> betweenness = new HashMap<>();
		for (Bavard b : bavards) betweenness.put(b, 0.0);

		for (Bavard source : bavards) {
			Deque<Bavard> pile = new ArrayDeque<>();
			Map<Bavard, List<Bavard>> pred = new HashMap<>();
			Map<Bavard, Long>         sigma = new HashMap<>();
			Map<Bavard, Integer>      dist  = new HashMap<>();

			for (Bavard b : bavards) {
				pred.put(b, new ArrayList<>());
				sigma.put(b, 0L);
				dist.put(b, -1);
			}
			sigma.put(source, 1L);
			dist.put(source, 0);

			Queue<Bavard> queue = new LinkedList<>();
			queue.add(source);

			while (!queue.isEmpty()) {
				Bavard v = queue.poll();
				pile.push(v);
				for (Bavard w : v.getAmis()) {
					if (dist.get(w) < 0) {
						queue.add(w);
						dist.put(w, dist.get(v) + 1);
					}
					if (dist.get(w) == dist.get(v) + 1) {
						sigma.put(w, sigma.get(w) + sigma.get(v));
						pred.get(w).add(v);
					}
				}
			}

			Map<Bavard, Double> delta = new HashMap<>();
			for (Bavard b : bavards) delta.put(b, 0.0);

			while (!pile.isEmpty()) {
				Bavard w = pile.pop();
				for (Bavard v : pred.get(w)) {
					long sw = sigma.get(w);
					long sv = sigma.get(v);
					double coeff = (sw == 0) ? 0.0
							: ((double) sv / sw) * (1.0 + delta.get(w));
					delta.merge(v, coeff, Double::sum);
				}
				if (!w.equals(source)) {
					betweenness.merge(w, delta.get(w), Double::sum);
				}
			}
		}

		// normalisation par (n-1)(n-2) pour graphes non orientes
		double norm = (n <= 2) ? 1.0 : ((n - 1.0) * (n - 2.0) / 2.0);
		Map<Bavard, Double> normalise = new HashMap<>();
		for (Map.Entry<Bavard, Double> e : betweenness.entrySet()) {
			normalise.put(e.getKey(), e.getValue() / norm);
		}
		return trierDecroissant(normalise);
	}

	// ─── PageRank ──────────────────────────────────────────────────────────────

	/**
	 * Probabilite de visiter un noeud apres une marche aleatoire longue.
	 * Facteur d'amortissement d=0.85, convergence en 100 iterations.
	 * Mesure l'influence globale (un noeud influence par des noeuds influents
	 * est lui-meme tres influent).
	 */
	public static LinkedHashMap<Bavard, Double> pageRank(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		if (n == 0) return new LinkedHashMap<>();

		final double d = 0.85;
		final int ITERATIONS = 100;

		Map<Bavard, Double> pr = new HashMap<>();
		for (Bavard b : bavards) pr.put(b, 1.0 / n);

		for (int iter = 0; iter < ITERATIONS; iter++) {
			Map<Bavard, Double> nouveau = new HashMap<>();
			double base = (1.0 - d) / n;
			for (Bavard b : bavards) {
				double somme = 0.0;
				for (Bavard ami : b.getAmis()) {
					int degreAmi = ami.getAmis().size();
					if (degreAmi > 0) somme += pr.get(ami) / degreAmi;
				}
				nouveau.put(b, base + d * somme);
			}
			pr = nouveau;
		}
		return trierDecroissant(pr);
	}

	// ─── Score composite ──────────────────────────────────────────────────────

	/**
	 * Score composite = 0.25*degre + 0.25*proximite + 0.25*intermediaire + 0.25*pagerank.
	 * Normalise chaque metrique dans [0,1] avant fusion.
	 * Donne une vision equilibree de l'influence.
	 */
	public static LinkedHashMap<Bavard, Double> scoreComposite(Reseau reseau) {
		LinkedHashMap<Bavard, Double> deg  = centraliteDegre(reseau);
		LinkedHashMap<Bavard, Double> prox = centraliteProximite(reseau);
		LinkedHashMap<Bavard, Double> btw  = centraliteIntermediaire(reseau);
		LinkedHashMap<Bavard, Double> pr   = pageRank(reseau);

		Map<Bavard, Double> composite = new HashMap<>();
		for (Bavard b : reseau.getBavards()) {
			double score = 0.25 * deg.getOrDefault(b, 0.0)
						 + 0.25 * prox.getOrDefault(b, 0.0)
						 + 0.25 * btw.getOrDefault(b, 0.0)
						 + 0.25 * pr.getOrDefault(b, 0.0);
			composite.put(b, score);
		}
		return trierDecroissant(composite);
	}

	// ─── Rapport textuel ──────────────────────────────────────────────────────

	/**
	 * Rapport complet listant les 4 metriques et le score composite.
	 */
	public static String rapport(Reseau reseau) {
		if (reseau.getBavards().isEmpty()) return "reseau vide\n";

		StringBuilder sb = new StringBuilder();
		sb.append("=== analyse d'influence ===\n");
		sb.append("noeuds : ").append(reseau.getBavards().size());
		int liens = reseau.getBavards().stream().mapToInt(b -> b.getAmis().size()).sum() / 2;
		sb.append("   liens : ").append(liens).append("\n\n");

		sb.append("rang  nom            degre   proximite  interm.  pagerank\n");
		sb.append("----  -------------- ------  ---------  -------  --------\n");

		LinkedHashMap<Bavard, Double> deg  = centraliteDegre(reseau);
		LinkedHashMap<Bavard, Double> prox = centraliteProximite(reseau);
		LinkedHashMap<Bavard, Double> btw  = centraliteIntermediaire(reseau);
		LinkedHashMap<Bavard, Double> pr   = pageRank(reseau);
		LinkedHashMap<Bavard, Double> comp = scoreComposite(reseau);

		int rang = 1;
		for (Map.Entry<Bavard, Double> entry : comp.entrySet()) {
			Bavard b = entry.getKey();
			sb.append(String.format("%-4d  %-14s  %.3f   %.3f      %.3f    %.4f%n",
					rang++, b.getNom(),
					deg.getOrDefault(b, 0.0),
					prox.getOrDefault(b, 0.0),
					btw.getOrDefault(b, 0.0),
					pr.getOrDefault(b, 0.0)));
		}

		sb.append("\n-- influenceur principal (score composite) --\n");
		comp.entrySet().stream().findFirst().ifPresent(e ->
				sb.append("  ").append(e.getKey().getNom())
				  .append("  (score=").append(String.format("%.3f", e.getValue())).append(")\n"));

		sb.append("\n-- noeud le plus intermediaire (pont) --\n");
		btw.entrySet().stream().findFirst().ifPresent(e ->
				sb.append("  ").append(e.getKey().getNom())
				  .append("  (betweenness=").append(String.format("%.3f", e.getValue())).append(")\n"));

		return sb.toString();
	}

	// ─── Utilitaires internes ─────────────────────────────────────────────────

	/** BFS depuis une source, retourne distance par noeud (source exclue). */
	private static Map<Bavard, Integer> bfs(Bavard source, List<Bavard> tous) {
		Map<Bavard, Integer> dist = new HashMap<>();
		dist.put(source, 0);
		Queue<Bavard> queue = new LinkedList<>();
		queue.add(source);
		while (!queue.isEmpty()) {
			Bavard current = queue.poll();
			for (Bavard voisin : current.getAmis()) {
				if (!dist.containsKey(voisin)) {
					dist.put(voisin, dist.get(current) + 1);
					queue.add(voisin);
				}
			}
		}
		dist.remove(source);
		return dist;
	}

	/** Trie une Map par valeur decroissante dans un LinkedHashMap. */
	private static LinkedHashMap<Bavard, Double> trierDecroissant(Map<Bavard, Double> scores) {
		LinkedHashMap<Bavard, Double> result = new LinkedHashMap<>();
		scores.entrySet().stream()
				.sorted(Map.Entry.<Bavard, Double>comparingByValue().reversed())
				.forEach(e -> result.put(e.getKey(), e.getValue()));
		return result;
	}
}
