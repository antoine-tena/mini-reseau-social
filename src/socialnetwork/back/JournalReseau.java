package socialnetwork.back;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Journal chronologique de tous les messages propages dans le reseau.
 *
 * S'inscrit comme BavardObserver sur chaque bavard.
 * Chaque entree capture : horodatage, nom du recepteur, MessageEvent.
 *
 * Fonctionnalites :
 *   - filtrage par bavard, par bienveillance, par contenu (recherche)
 *   - export texte plein et CSV
 *   - ecouteurs (Runnable) notifies a chaque nouvel evenement
 *     (utile pour mettre a jour l'interface sans couplage fort)
 */
public class JournalReseau implements BavardObserver {

	private static final DateTimeFormatter FMT_COURT  = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final DateTimeFormatter FMT_ISO    = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	// ─── EntreeJournal ────────────────────────────────────────────────────────

	public static final class EntreeJournal {
		private final LocalDateTime horodatage;
		private final String        nomBavard;
		private final MessageEvent  message;
		/** Sentiment analyse a la creation de l'entree. */
		private final AnalyseurSentiment.ResultatSentiment sentiment;

		EntreeJournal(String nomBavard, MessageEvent message) {
			this.horodatage = LocalDateTime.now();
			this.nomBavard  = nomBavard;
			this.message    = message;
			this.sentiment  = AnalyseurSentiment.analyser(message.getContenu());
		}

		public LocalDateTime getHorodatage()   { return horodatage; }
		public String getNomBavard()            { return nomBavard; }
		public MessageEvent getMessage()        { return message; }
		public AnalyseurSentiment.ResultatSentiment getSentiment() { return sentiment; }

		/** Representation courte pour l'affichage dans la liste. */
		@Override
		public String toString() {
			return String.format("[%s] %-10s | b=%+5.1f | %-8s | %s",
					horodatage.format(FMT_COURT),
					nomBavard,
					message.getBienveillance(),
					sentiment.getEmotion().label(),
					message.getContenu());
		}
	}

	// ─── Etat ─────────────────────────────────────────────────────────────────

	private final List<EntreeJournal> entrees   = new ArrayList<>();
	private final List<Runnable>      ecouteurs = new ArrayList<>();
	private final Reseau              reseau;

	public JournalReseau(Reseau reseau) {
		this.reseau = reseau;
		for (Bavard b : reseau.getBavards()) {
			b.ajouterObservateur(this);
		}
	}

	// ─── Observer ─────────────────────────────────────────────────────────────

	/** Abonne ce journal a un nouveau bavard ajoute apres la creation du journal. */
	public void observerBavard(Bavard bavard) {
		bavard.ajouterObservateur(this);
	}

	@Override
	public void messageAjoute(Bavard bavard, MessageEvent event) {
		entrees.add(new EntreeJournal(bavard.getNom(), event));
		notifierEcouteurs();
	}

	@Override
	public void amisMisAJour(Bavard bavard) {
		/* non utilise dans le journal */
	}

	// ─── Ecouteurs ────────────────────────────────────────────────────────────

	/**
	 * Ajoute un ecouteur (Runnable) appele apres chaque nouveau message.
	 * L'ecouteur est invoque depuis le thread qui a declenche la propagation —
	 * utiliser SwingUtilities.invokeLater si mise a jour de l'interface.
	 */
	public void ajouterEcouteur(Runnable ecouteur) {
		if (ecouteur != null) ecouteurs.add(ecouteur);
	}

	private void notifierEcouteurs() {
		for (Runnable r : ecouteurs) r.run();
	}

	// ─── Acces direct ─────────────────────────────────────────────────────────

	/** Toutes les entrees dans l'ordre d'arrivee (vue non modifiable). */
	public List<EntreeJournal> getEntrees() {
		return Collections.unmodifiableList(entrees);
	}

	/** Nombre d'entrees. */
	public int getTaille() { return entrees.size(); }

	/** Vide le journal. */
	public void effacer()  { entrees.clear(); notifierEcouteurs(); }

	// ─── Filtres ──────────────────────────────────────────────────────────────

	/** Entrees concernant un bavard precis (insensible a la casse). */
	public List<EntreeJournal> filtrerParBavard(String nom) {
		return entrees.stream()
				.filter(e -> e.getNomBavard().equalsIgnoreCase(nom))
				.collect(Collectors.toList());
	}

	/** Entrees dont la bienveillance est dans [min, max]. */
	public List<EntreeJournal> filtrerParBienveillance(float min, float max) {
		return entrees.stream()
				.filter(e -> {
					float b = e.getMessage().getBienveillance();
					return b >= min && b <= max;
				})
				.collect(Collectors.toList());
	}

	/** Entrees dont le contenu du message contient le mot cle (insensible casse). */
	public List<EntreeJournal> rechercherContenu(String motCle) {
		if (motCle == null || motCle.isBlank()) return new ArrayList<>(entrees);
		String lower = motCle.toLowerCase();
		return entrees.stream()
				.filter(e -> e.getMessage().getContenu().toLowerCase().contains(lower))
				.collect(Collectors.toList());
	}

	/** Entrees ayant une emotion dominante donnee. */
	public List<EntreeJournal> filtrerParEmotion(AnalyseurSentiment.Emotion emotion) {
		return entrees.stream()
				.filter(e -> e.getSentiment().getEmotion() == emotion)
				.collect(Collectors.toList());
	}

	// ─── Export ───────────────────────────────────────────────────────────────

	/** Export texte lisible. */
	public String exporterTexte() {
		if (entrees.isEmpty()) return "(journal vide)\n";
		StringBuilder sb = new StringBuilder();
		sb.append("=== journal du reseau  (").append(entrees.size()).append(" entrees) ===\n");
		for (EntreeJournal e : entrees) {
			sb.append(e).append('\n');
		}
		return sb.toString();
	}

	/** Export CSV avec entete. */
	public String exporterCsv() {
		StringBuilder sb = new StringBuilder(
			"horodatage;bavard;bienveillance;emotion;score_sentiment;auteur;contenu;chemin\n");
		for (EntreeJournal e : entrees) {
			MessageEvent m = e.getMessage();
			sb.append(e.getHorodatage().format(FMT_ISO)).append(';');
			sb.append(e.getNomBavard()).append(';');
			sb.append(String.format("%.2f", m.getBienveillance())).append(';');
			sb.append(e.getSentiment().getEmotion().label()).append(';');
			sb.append(String.format("%.3f", e.getSentiment().getScore())).append(';');
			sb.append(m.getAuteur()).append(';');
			sb.append('"').append(m.getContenu().replace("\"", "\"\"")).append('"').append(';');
			sb.append(String.join("->", m.getChemin())).append('\n');
		}
		return sb.toString();
	}

	/** Statistiques rapides du journal (comptages, moyennes). */
	public String resumeRapide() {
		if (entrees.isEmpty()) return "journal vide\n";
		double bMoy = entrees.stream()
				.mapToDouble(e -> e.getMessage().getBienveillance()).average().orElse(0);
		long pos = entrees.stream().filter(e -> e.getMessage().getBienveillance() > 1f).count();
		long neg = entrees.stream().filter(e -> e.getMessage().getBienveillance() < -1f).count();
		return String.format("entrees=%d  bienveillance moy=%.2f  positifs=%d  negatifs=%d",
				entrees.size(), bMoy, pos, neg);
	}
}
