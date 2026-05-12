package socialnetwork.back;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generateur de topologies de reseau preconfigurées.
 *
 * Chaque topologie presente des proprietes mathematiques differentes utiles
 * pour etudier la propagation d'information :
 *
 *   ANNEAU       : chemin lineaire ferme, propagation lente, equitable
 *   ETOILE       : hub central omnipresent, propagation ultra-rapide depuis le hub
 *   GRILLE       : structure 2D reguliere, propagation locale
 *   PETIT_MONDE  : (Watts-Strogatz) fort clustering + faible diametre
 *   ALEATOIRE    : (Erdos-Renyi G(n,p)) liens aleatoires, loi de Poisson
 *   COMPLET      : tout le monde connecte, propagation instantanee
 *
 * Les noms de bavards sont pioches dans un pool de prenoms predetermines.
 * Les personnalites sont distribuees cycliquement parmi le tableau fourni.
 */
public final class GenerateurTopologie {

	private GenerateurTopologie() {
	}

	// ─── Enum Topologie ───────────────────────────────────────────────────────

	public enum Topologie {
		ANNEAU("anneau"),
		ETOILE("etoile"),
		GRILLE("grille"),
		PETIT_MONDE("petit monde"),
		ALEATOIRE("aleatoire"),
		COMPLET("complet");

		private final String label;

		Topologie(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}

		/** Breve description didactique de la topologie. */
		public String description() {
			return switch (this) {
				case ANNEAU      -> "chaque noeud connecte a ses 2 voisins directs";
				case ETOILE      -> "un hub central relie a tous les autres";
				case GRILLE      -> "connexions en grille 2D (voisins haut/bas/gauche/droite)";
				case PETIT_MONDE -> "anneau avec raccourcis aleatoires (Watts-Strogatz)";
				case ALEATOIRE   -> "graphe d'Erdos-Renyi, p=0.30";
				case COMPLET     -> "graphe complet : tout le monde connait tout le monde";
			};
		}
	}

	// ─── Pool de prenoms ──────────────────────────────────────────────────────

	private static final String[] PRENOMS = {
		"Axel", "Bea",  "Cleo", "Dov",  "Elia", "Flo",  "Gao",  "Hana",
		"Igor", "Jade", "Karl", "Lena", "Mio",  "Noe",  "Orla", "Pax",
		"Quin", "Rina", "Sam",  "Tina", "Ugo",  "Vera", "Wolf", "Xia",
		"Yann", "Zara", "Ari",  "Bram", "Cara", "Dina", "Evan", "Faye",
		"Gage", "Hira", "Ivan", "Juno", "Kira", "Liam", "Maya", "Nash"
	};

	private static String prenom(int indexAbsolu) {
		return indexAbsolu < PRENOMS.length
				? PRENOMS[indexAbsolu]
				: "B" + indexAbsolu;
	}

	// ─── Topologies ───────────────────────────────────────────────────────────

	/**
	 * Anneau : chaque noeud i est connecte a i+1 et i-1 (modulo n).
	 * Diametre = n/2, clustering = 0.
	 */
	public static void anneau(Reseau reseau, int n, Personnalite[] persos) {
		if (n < 2) n = 2;
		List<Bavard> bavards = creerBavards(reseau, n, persos);
		for (int i = 0; i < n; i++) {
			reseau.connecter(bavards.get(i), bavards.get((i + 1) % n));
		}
	}

	/**
	 * Etoile : un hub central connecte a tous les autres noeuds (feuilles).
	 * @return le bavard hub (utile pour cibler la propagation depuis le centre)
	 */
	public static Bavard etoile(Reseau reseau, int n, Personnalite[] persos) {
		if (n < 2) n = 2;
		List<Bavard> bavards = creerBavards(reseau, n, persos);
		Bavard hub = bavards.get(0);
		for (int i = 1; i < n; i++) {
			reseau.connecter(hub, bavards.get(i));
		}
		return hub;
	}

	/**
	 * Grille rectangulaire : chaque cellule est connectee a ses voisins
	 * haut, bas, gauche, droite.
	 * La grille la plus proche de n noeuds est calculee automatiquement.
	 */
	public static void grille(Reseau reseau, int lignes, int colonnes, Personnalite[] persos) {
		if (lignes < 1) lignes = 1;
		if (colonnes < 1) colonnes = 1;
		int n = lignes * colonnes;
		List<Bavard> bavards = creerBavards(reseau, n, persos);
		for (int r = 0; r < lignes; r++) {
			for (int c = 0; c < colonnes; c++) {
				int idx = r * colonnes + c;
				if (c + 1 < colonnes) reseau.connecter(bavards.get(idx), bavards.get(idx + 1));
				if (r + 1 < lignes)   reseau.connecter(bavards.get(idx), bavards.get(idx + colonnes));
			}
		}
	}

	/**
	 * Petit monde (Watts-Strogatz simplifie) :
	 *   1. Anneau k-regulier (chaque noeud relie a ses k/2 voisins de chaque cote)
	 *   2. Chaque lien est recable avec probabilite probRewire
	 * Produit un reseau a fort coefficient de clustering et faible diametre.
	 *
	 * @param k          degre initial (pair, >= 2)
	 * @param probRewire probabilite de recablage [0.0 .. 1.0]
	 * @param seed       graine pour la reproductibilite
	 */
	public static void petitMonde(Reseau reseau, int n, int k, double probRewire,
			Personnalite[] persos, long seed) {
		if (n < 4) n = 4;
		if (k < 2) k = 2;
		if (k % 2 != 0) k++;
		int halfK = k / 2;
		List<Bavard> bavards = creerBavards(reseau, n, persos);

		// anneau k-regulier
		for (int i = 0; i < n; i++) {
			for (int j = 1; j <= halfK; j++) {
				reseau.connecter(bavards.get(i), bavards.get((i + j) % n));
			}
		}

		// recablage aleatoire
		Random rng = new Random(seed);
		for (int i = 0; i < n; i++) {
			for (int j = 1; j <= halfK; j++) {
				if (rng.nextDouble() < probRewire) {
					int cible = rng.nextInt(n);
					int essais = 0;
					while ((cible == i) && essais < 10) {
						cible = rng.nextInt(n);
						essais++;
					}
					if (cible != i) {
						reseau.connecter(bavards.get(i), bavards.get(cible));
					}
				}
			}
		}
	}

	/**
	 * Aleatoire (Erdos-Renyi G(n,p)) : chaque paire de noeuds est connectee
	 * independamment avec probabilite p.
	 *
	 * @param p    probabilite de lien, typiquement 0.20..0.40
	 * @param seed graine pour la reproductibilite
	 */
	public static void aleatoire(Reseau reseau, int n, double p, Personnalite[] persos, long seed) {
		if (n < 2) n = 2;
		List<Bavard> bavards = creerBavards(reseau, n, persos);
		Random rng = new Random(seed);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (rng.nextDouble() < p) {
					reseau.connecter(bavards.get(i), bavards.get(j));
				}
			}
		}
		// garantir la connexite : chainer tous les noeuds isoles
		for (int i = 0; i < n - 1; i++) {
			if (bavards.get(i).getAmis().isEmpty() || bavards.get(i + 1).getAmis().isEmpty()) {
				reseau.connecter(bavards.get(i), bavards.get(i + 1));
			}
		}
	}

	/**
	 * Graphe complet (clique) : chaque noeud est connecte a tous les autres.
	 * Diametre = 1, propagation instantanee en 1 hop.
	 */
	public static void complet(Reseau reseau, int n, Personnalite[] persos) {
		if (n < 2) n = 2;
		List<Bavard> bavards = creerBavards(reseau, n, persos);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				reseau.connecter(bavards.get(i), bavards.get(j));
			}
		}
	}

	// ─── Dispatching generique ────────────────────────────────────────────────

	/**
	 * Genere un reseau selon la topologie choisie avec des parametres par defaut
	 * raisonnables. Les personnalites sont distribuees cycliquement.
	 *
	 * @param reseau     reseau cible (doit etre vide)
	 * @param topologie  type de topologie
	 * @param n          nombre de bavards souhaites (approx pour grille)
	 * @param persos     tableau de personnalites (peut etre null -> NEUTRE)
	 */
	public static void generer(Reseau reseau, Topologie topologie, int n, Personnalite[] persos) {
		if (persos == null || persos.length == 0) {
			persos = new Personnalite[] {
				Personnalite.POSITIF, Personnalite.NEGATIF, Personnalite.NEUTRE
			};
		}
		long seed = System.currentTimeMillis();
		switch (topologie) {
			case ANNEAU      -> anneau(reseau, n, persos);
			case ETOILE      -> etoile(reseau, n, persos);
			case GRILLE      -> {
				int c = (int) Math.ceil(Math.sqrt(n));
				int l = (int) Math.ceil((double) n / c);
				grille(reseau, l, c, persos);
			}
			case PETIT_MONDE -> petitMonde(reseau, n, 4, 0.15, persos, seed);
			case ALEATOIRE   -> aleatoire(reseau, n, 0.30, persos, seed);
			case COMPLET     -> complet(reseau, n, persos);
		}
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	/**
	 * Cree n bavards dans le reseau avec des noms et personnalites automatiques.
	 * L'offset evite les collisions de noms si le reseau contient deja des bavards.
	 */
	private static List<Bavard> creerBavards(Reseau reseau, int n, Personnalite[] persos) {
		List<Bavard> liste = new ArrayList<>();
		int offset = reseau.getBavards().size();
		for (int i = 0; i < n; i++) {
			Personnalite p = (persos != null && persos.length > 0)
					? persos[i % persos.length]
					: Personnalite.NEUTRE;
			Bavard b = new Bavard(prenom(offset + i), p);
			reseau.ajouterBavard(b);
			liste.add(b);
		}
		return liste;
	}

	/** Calcule le degre moyen du reseau. */
	public static double degreMoyen(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		if (bavards.isEmpty()) return 0.0;
		return bavards.stream().mapToInt(b -> b.getAmis().size()).average().orElse(0.0);
	}

	/** Calcule la densite du graphe (liens existants / liens max possibles). */
	public static double densite(Reseau reseau) {
		int n = reseau.getBavards().size();
		if (n <= 1) return 0.0;
		int liens = reseau.getBavards().stream().mapToInt(b -> b.getAmis().size()).sum() / 2;
		int maxLiens = n * (n - 1) / 2;
		return (double) liens / maxLiens;
	}

	/** Description textuelle du reseau (topologie, degres, densite). */
	public static String description(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		int liens = bavards.stream().mapToInt(b -> b.getAmis().size()).sum() / 2;
		int degreMin = bavards.stream().mapToInt(b -> b.getAmis().size()).min().orElse(0);
		int degreMax = bavards.stream().mapToInt(b -> b.getAmis().size()).max().orElse(0);
		return String.format(
			"noeuds=%d  liens=%d  degre moy=%.2f  min=%d  max=%d  densite=%.2f",
			n, liens, degreMoyen(reseau), degreMin, degreMax, densite(reseau));
	}
}
