package socialnetwork.back.stats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import socialnetwork.back.analyse.AnalyseurSentiment;
import socialnetwork.back.analyse.ResultatSentiment;
import socialnetwork.back.model.MessageEvent;

/**
 * Une entree dans le {@link JournalReseau}.
 *
 * Capture immuablement :
 *   - horodatage  : moment de reception du message
 *   - nomBavard   : nom du bavard recepteur
 *   - message     : l'evenement MessageEvent associe
 *   - sentiment   : resultat de l'analyse de sentiment (calcule a la creation)
 *
 * Extrait de JournalReseau pour une meilleure lisibilite et separation des roles.
 */
public final class EntreeJournal {

	private static final DateTimeFormatter FMT_COURT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final DateTimeFormatter FMT_ISO   = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final LocalDateTime  horodatage;
	private final String         nomBavard;
	private final MessageEvent   message;
	private final ResultatSentiment sentiment;

	EntreeJournal(String nomBavard, MessageEvent message) {
		this.horodatage = LocalDateTime.now();
		this.nomBavard  = nomBavard;
		this.message    = message;
		this.sentiment  = AnalyseurSentiment.analyser(message.getContenu());
	}

	public LocalDateTime getHorodatage()       { return horodatage; }
	public String getNomBavard()               { return nomBavard; }
	public MessageEvent getMessage()           { return message; }
	public ResultatSentiment getSentiment()    { return sentiment; }

	/** Format ISO de l'horodatage (pour l'export CSV). */
	public String getHorodatageIso() {
		return horodatage.format(FMT_ISO);
	}

	/**
	 * Representation courte pour l'affichage dans la liste Swing.
	 */
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
