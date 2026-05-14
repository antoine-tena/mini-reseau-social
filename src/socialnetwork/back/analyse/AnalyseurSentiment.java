package socialnetwork.back.analyse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyse lexicale du sentiment d'un message en francais.
 *
 * Classe utilitaire stateless qui retourne pour un texte :
 *   - un score continu dans [-1.0 ; +1.0]   (negatif -> positif)
 *   - une {@link Emotion} dominante parmi 7 categories
 *   - les mots detectes (positifs et negatifs)
 *
 * Algorithme : bag-of-words pondere avec detection de negation
 * ("pas","ne","non"...). Le lexique integre ~200 termes francophones.
 *
 * Usage :
 *   ResultatSentiment r = AnalyseurSentiment.analyser("je suis super heureux !");
 *   float bienveillance  = AnalyseurSentiment.bienveillanceSuggeree("je suis triste");
 */
public final class AnalyseurSentiment {

	private AnalyseurSentiment() {}

	// ─── Lexique ──────────────────────────────────────────────────────────────

	private static final Map<Emotion, Set<String>> LEXIQUE;

	static {
		EnumMap<Emotion, Set<String>> map = new EnumMap<>(Emotion.class);

		map.put(Emotion.JOIE, Set.of(
			"heureux", "heureuse", "joie", "bonheur", "super", "genial", "fantastique",
			"bien", "excellent", "parfait", "merveilleux", "bravo", "felicitations",
			"amour", "aimer", "adorer", "sourire", "rire", "fete", "victoire",
			"reussi", "reussir", "succes", "champion", "formidable", "top", "cool",
			"magnifique", "splendide", "incroyable", "sympa", "content", "contente",
			"yes", "wow", "hourra", "yay", "beau", "belle", "fier", "fiere",
			"joyeux", "joyeuse", "radieux", "radieuse", "epanoui", "epanouie",
			"reconnaissant", "reconnaissante", "gratitude", "merci", "grace",
			"adorable", "charmant", "charmante", "sublime", "enchante", "enchantee"
		));

		map.put(Emotion.TRISTESSE, Set.of(
			"triste", "tristesse", "malheureux", "malheureuse", "pleurs", "pleurer",
			"larmes", "deprime", "deprimant", "cafard", "melancolie", "deuil", "peine",
			"souffrance", "souffrir", "echec", "rate", "perdu", "perdre", "seul", "seule",
			"solitude", "abandon", "abandonne", "douleur", "nul", "inutile",
			"honte", "humiliation", "decu", "decevoir", "desespoir", "adieu",
			"chagrin", "larmoyant", "abattu", "abattue", "inconsolable", "morose",
			"gris", "sombre", "obscur", "vide", "manque", "regret", "regretter"
		));

		map.put(Emotion.COLERE, Set.of(
			"colere", "enerve", "enervent", "rage", "furieux", "furieuse", "haine",
			"detester", "idiot", "idiote", "stupide", "cretin", "cretine",
			"imbecile", "enerver", "agacer", "insupportable", "inacceptable",
			"injuste", "menteur", "menteuse", "trahison", "trahir", "horrible",
			"scandale", "scandaleux", "honteux", "ignoble", "revolte", "revolter",
			"exaspere", "exasperee", "irrite", "irritee", "fureur", "indigne",
			"indignation", "inadmissible", "insense", "absurde", "ridicule"
		));

		map.put(Emotion.PEUR, Set.of(
			"peur", "crainte", "effrayant", "effrayante", "terreur", "terrifiant",
			"dangereux", "dangereuse", "risque", "menace", "alerte",
			"catastrophe", "desastre", "apocalypse", "fuite", "fuir", "cacher",
			"angoisse", "anxiete", "anxieux", "anxieuse", "tremblement", "trembler",
			"cauchemar", "terrifier", "horreur", "effroi", "panique",
			"incertain", "incertaine", "vulnerable", "fragile", "peril"
		));

		map.put(Emotion.SURPRISE, Set.of(
			"surprise", "surpris", "inattendu", "etonnant", "etonnante",
			"etonne", "etonnee", "oh", "ah", "insolite", "bizarre",
			"improbable", "soudain", "soudainement", "extraordinaire",
			"exceptionnel", "unique", "rare", "renversant",
			"stupefait", "stupefaite", "sidere", "sideree", "ebahi", "ebahie"
		));

		map.put(Emotion.DEGOUT, Set.of(
			"degout", "degouter", "ecoeurer", "ecoeurement", "nausee",
			"repugnant", "abject", "infame", "abominable", "ignoble", "sordide",
			"immonde", "contamination", "pollution", "toxique", "malsain", "sale",
			"ordure", "poubelle", "rebut", "crade", "crasseux", "nauseabond"
		));

		LEXIQUE = Collections.unmodifiableMap(map);
	}

	/** Mots de negation qui inversent le prochain terme. */
	private static final Set<String> NEGATIONS = Set.of(
		"pas", "non", "jamais", "rien", "sans", "ni", "ne", "plus", "guere", "nullement"
	);

	/** Score emotionnel de base par categorie. */
	private static double valeurEmotion(Emotion e) {
		return switch (e) {
			case JOIE      ->  1.0;
			case SURPRISE  ->  0.4;
			case NEUTRE    ->  0.0;
			case PEUR      -> -0.5;
			case TRISTESSE -> -0.7;
			case DEGOUT    -> -0.8;
			case COLERE    -> -1.0;
		};
	}

	// ─── API publique ─────────────────────────────────────────────────────────

	/**
	 * Analyse un texte et retourne le detail du sentiment.
	 *
	 * @param texte le message a analyser (peut etre null ou vide)
	 * @return un {@link ResultatSentiment} (jamais null)
	 */
	public static ResultatSentiment analyser(String texte) {
		if (texte == null || texte.isBlank()) {
			return new ResultatSentiment(0.0, Emotion.NEUTRE, List.of(), List.of());
		}

		String[] tokens = texte.toLowerCase().split("[\\s\\p{Punct}\\d]+");

		EnumMap<Emotion, Integer> comptes = new EnumMap<>(Emotion.class);
		for (Emotion e : Emotion.values()) comptes.put(e, 0);

		List<String> motsPos = new ArrayList<>();
		List<String> motsNeg = new ArrayList<>();
		boolean negation = false;

		for (String token : tokens) {
			if (token.isBlank()) { negation = false; continue; }

			if (NEGATIONS.contains(token)) { negation = true; continue; }

			Emotion trouvee = null;
			for (Map.Entry<Emotion, Set<String>> entry : LEXIQUE.entrySet()) {
				if (entry.getValue().contains(token)) { trouvee = entry.getKey(); break; }
			}

			if (trouvee != null) {
				Emotion effective = negation ? inverser(trouvee) : trouvee;
				comptes.merge(effective, 1, Integer::sum);
				if (valeurEmotion(effective) >= 0) motsPos.add(token);
				else                               motsNeg.add(token);
			}
			negation = false;
		}

		double sommePonderee = 0;
		int totalMots = 0;
		for (Map.Entry<Emotion, Integer> entry : comptes.entrySet()) {
			int count = entry.getValue();
			if (count > 0 && entry.getKey() != Emotion.NEUTRE) {
				sommePonderee += valeurEmotion(entry.getKey()) * count;
				totalMots     += count;
			}
		}

		double score = totalMots == 0 ? 0.0 : sommePonderee / totalMots;
		score = Math.max(-1.0, Math.min(1.0, score));

		Emotion dominante = comptes.entrySet().stream()
				.filter(e -> e.getKey() != Emotion.NEUTRE)
				.max(Map.Entry.comparingByValue())
				.filter(e -> e.getValue() > 0)
				.map(Map.Entry::getKey)
				.orElse(Emotion.NEUTRE);

		return new ResultatSentiment(score, dominante, motsPos, motsNeg);
	}

	/**
	 * Raccourci : bienveillance suggeree dans [-5, +5].
	 */
	public static float bienveillanceSuggeree(String texte) {
		return analyser(texte).bienveillanceSuggeree();
	}

	/**
	 * Retourne un label colore HTML pour affichage Swing JLabel.
	 */
	public static String labelHtml(Emotion emotion) {
		return "<html><b style='color:" + emotion.couleurHtml() + "'>"
				+ emotion.label() + " " + emotion.emoji() + "</b></html>";
	}

	// ─── Interne ──────────────────────────────────────────────────────────────

	private static Emotion inverser(Emotion e) {
		return switch (e) {
			case JOIE      -> Emotion.TRISTESSE;
			case TRISTESSE -> Emotion.JOIE;
			case COLERE    -> Emotion.SURPRISE;
			default        -> e;
		};
	}
}
