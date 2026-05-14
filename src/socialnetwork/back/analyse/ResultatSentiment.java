package socialnetwork.back.analyse;

import java.util.List;

/**
 * Resultat de l'analyse de sentiment produit par {@link AnalyseurSentiment}.
 *
 * Contient :
 *   - score continu dans [-1.0 ; +1.0] (negatif -> positif)
 *   - emotion dominante parmi les 7 categories {@link Emotion}
 *   - listes des mots detectes positivement et negativement
 */
public final class ResultatSentiment {

	private final double      score;
	private final Emotion     emotion;
	private final List<String> motsPositifsTrouves;
	private final List<String> motsNegatifsTrouves;

	ResultatSentiment(double score, Emotion emotion,
			List<String> positifs, List<String> negatifs) {
		this.score                = score;
		this.emotion              = emotion;
		this.motsPositifsTrouves  = List.copyOf(positifs);
		this.motsNegatifsTrouves  = List.copyOf(negatifs);
	}

	/** Score dans [-1, +1]. */
	public double getScore()                    { return score; }
	/** Emotion dominante du texte. */
	public Emotion getEmotion()                 { return emotion; }
	/** Mots ayant contribue positivement. */
	public List<String> getMotsPositifs()       { return motsPositifsTrouves; }
	/** Mots ayant contribue negativement. */
	public List<String> getMotsNegatifs()       { return motsNegatifsTrouves; }

	public boolean estPositif()  { return score >  0.1; }
	public boolean estNegatif()  { return score < -0.1; }
	public boolean estNeutre()   { return !estPositif() && !estNegatif(); }

	/** Bienveillance suggeree (mis a l'echelle [-5, +5]). */
	public float bienveillanceSuggeree() {
		return (float)(score * 5.0);
	}

	@Override
	public String toString() {
		return String.format("score=%.2f  emotion=%s %s  (+%d mots / -%d mots)",
				score, emotion.label(), emotion.emoji(),
				motsPositifsTrouves.size(), motsNegatifsTrouves.size());
	}
}
