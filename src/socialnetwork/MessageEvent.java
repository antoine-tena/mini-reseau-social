package socialnetwork;

import java.util.EventObject;
import java.util.UUID;

public class MessageEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final UUID id;
	private final String contenu;
	private final int bienveillance;
	private final String auteur;

	public MessageEvent(Object source, String contenu, int bienveillance, String auteur) {
		this(UUID.randomUUID(), source, contenu, bienveillance, auteur);
	}

	public MessageEvent(UUID id, Object source, String contenu, int bienveillance, String auteur) {
		super(source);
		this.id = id;
		this.contenu = contenu;
		this.bienveillance = bienveillance;
		this.auteur = auteur;
	}

	public UUID getId() {
		return id;
	}

	public String getContenu() {
		return contenu;
	}

	public int getBienveillance() {
		return bienveillance;
	}

	public String getAuteur() {
		return auteur;
	}
}