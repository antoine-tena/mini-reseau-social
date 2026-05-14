package socialnetwork.back.analyse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.stats.StatistiquesReseau;

/**
 * Historique temporel des statistiques d'un reseau.
 *
 * Chaque appel a {@code prendreCliche} enregistre un {@link ClicheReseau}
 * qui capture le moment, le nombre total de messages et la bienveillance
 * moyenne ainsi que les valeurs individuelles par bavard.
 *
 * Utilise par {@code VueEvolution} pour tracer des courbes temporelles.
 *
 * Exemple :
 *   HistoriqueReseau hist = new HistoriqueReseau();
 *   hist.prendreCliche(stats, reseau);  // apres chaque simulation
 *   VueEvolution vue = new VueEvolution();
 *   vue.mettreAJour(hist);
 */
public class HistoriqueReseau {

	// ─── Classe interne : instantane ─────────────────────────────────────────

	/** Un instantane du reseau a un moment donne. */
	public static final class ClicheReseau {

		private final LocalDateTime horodatage;
		private final int           totalMessages;
		private final double        bienveillanceMoyenne;
		private final Map<String, Float>   bienveillanceParBavard;
		private final Map<String, Integer> messagesParBavard;

		/** Constructeur package-private : seul HistoriqueReseau cree des instances. */
		ClicheReseau(int totalMessages, double bienveillanceMoyenne,
				Map<String, Float> bienveillanceParBavard,
				Map<String, Integer> messagesParBavard) {
			this.horodatage            = LocalDateTime.now();
			this.totalMessages         = totalMessages;
			this.bienveillanceMoyenne  = bienveillanceMoyenne;
			this.bienveillanceParBavard = Collections.unmodifiableMap(new HashMap<>(bienveillanceParBavard));
			this.messagesParBavard      = Collections.unmodifiableMap(new HashMap<>(messagesParBavard));
		}

		public LocalDateTime getHorodatage()                        { return horodatage; }
		public int getTotalMessages()                               { return totalMessages; }
		public double getBienveillanceMoyenne()                     { return bienveillanceMoyenne; }
		public Map<String, Float>   getBienveillanceParBavard()     { return bienveillanceParBavard; }
		public Map<String, Integer> getMessagesParBavard()          { return messagesParBavard; }

		/** Ligne CSV : horodatage;totalMessages;bienveillanceMoyenne */
		public String toCsvLine() {
			return String.format("%s;%d;%.4f",
					horodatage.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
					totalMessages,
					bienveillanceMoyenne);
		}

		@Override
		public String toString() {
			return String.format("[%s] msgs=%d  b_moy=%+.2f",
					horodatage.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
					totalMessages, bienveillanceMoyenne);
		}
	}

	// ─── Historique ───────────────────────────────────────────────────────────

	private final List<ClicheReseau> snapshots = new ArrayList<>();

	/**
	 * Enregistre un nouvel instantane des statistiques courantes.
	 * A appeler periodiquement (apres chaque cycle de simulation par exemple).
	 */
	public void prendreCliche(StatistiquesReseau stats, Reseau reseau) {
		// bienveillance moyenne par bavard : moyenne des messages recus
		Map<String, Float> bMoy = new LinkedHashMap<>();
		for (Bavard b : reseau.getBavards()) {
			if (b.getMessagesRecus().isEmpty()) {
				bMoy.put(b.getNom(), 0f);
			} else {
				float somme = 0f;
				for (var m : b.getMessagesRecus()) somme += m.getBienveillance();
				bMoy.put(b.getNom(), somme / b.getMessagesRecus().size());
			}
		}

		// Downcast Integer to int map
		Map<String, Integer> mPar = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> e : stats.getMessagesParBavard().entrySet()) {
			mPar.put(e.getKey(), e.getValue());
		}

		snapshots.add(new ClicheReseau(
				stats.getTotalMessages(),
				stats.getBienveillanceMoyenne(),
				bMoy,
				mPar));
	}

	public List<ClicheReseau> getSnapshots()        { return Collections.unmodifiableList(snapshots); }
	public int getTaille()                          { return snapshots.size(); }
	public void effacer()                           { snapshots.clear(); }

	/** Export CSV complet : une ligne d'entete + une ligne par cliche. */
	public String exporterCsv() {
		StringBuilder sb = new StringBuilder("horodatage;totalMessages;bienveillanceMoyenne\n");
		for (ClicheReseau c : snapshots) sb.append(c.toCsvLine()).append('\n');
		return sb.toString();
	}

	/** Tableau des valeurs de bienveillance moyenne dans l'ordre chronologique. */
	public double[] courbesBienveillance() {
		return snapshots.stream().mapToDouble(ClicheReseau::getBienveillanceMoyenne).toArray();
	}

	/** Tableau des totaux de messages dans l'ordre chronologique. */
	public int[] courbesMessages() {
		return snapshots.stream().mapToInt(ClicheReseau::getTotalMessages).toArray();
	}
}
