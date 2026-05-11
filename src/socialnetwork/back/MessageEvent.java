package socialnetwork.back;

import java.util.EventObject;
import java.util.UUID;

/**
 * Evenement transporte lors d'une propagation de message.
 */
public class MessageEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	// sert a reconnaitre le meme message
	private final UUID id;
	private final String contenu;
	private final float bienveillance;
	private final String auteur;

	/**
	 * Nouveau message avec un identifiant genere.
	 */
	public MessageEvent(Object source, String contenu, float bienveillance, String auteur) {
		// premier envoi donc nouvel id
		this(UUID.randomUUID(), source, contenu, bienveillance, auteur);
	}

	/**
	 * Message retransmis: conserve l'identifiant.
	 */
	public MessageEvent(UUID id, Object source, String contenu, float bienveillance, String auteur) {
		super(source);
		// quand on retransmet on garde le meme id
		this.id = id;
		this.contenu = contenu;
		this.bienveillance = bornerBienveillance(bienveillance);
		this.auteur = auteur;
	}

	// reste entre -10 et 10
	// Garde la bienveillance dans [-10, 10].
	private float bornerBienveillance(float valeur) {
		if (valeur > 10f) {
			return 10f;
		}
		if (valeur < -10f) {
			return -10f;
		}
		return valeur;
	}

	public UUID getId() {
		return id;
	}

	public String getContenu() {
		return contenu;
	}

	public float getBienveillance() {
		return bienveillance;
	}

	public String getAuteur() {
		return auteur;
	}

	@Override
	public String toString() {
		return auteur + " : " + contenu + " | b = " + String.format("%.2f", bienveillance);
	}
}