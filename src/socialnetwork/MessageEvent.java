package socialnetwork;

import java.util.EventObject;
import java.util.Objects;
import java.util.UUID;

public class MessageEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final UUID id;
	private final String auteur;
	private final String contenu;
	private final int bienveillance;

	public MessageEvent(Object source, String auteur, String contenu, int bienveillance) {
		super(Objects.requireNonNull(source, "source ne doit pas etre null"));
		this.id = UUID.randomUUID();
		this.auteur = Objects.requireNonNull(auteur, "auteur ne doit pas etre null");
		this.contenu = Objects.requireNonNull(contenu, "contenu ne doit pas etre null");
		this.bienveillance = bienveillance;
	}

	public UUID getId() {
		return id;
	}

	public String getAuteur() {
		return auteur;
	}

	public String getContenu() {
		return contenu;
	}

	public int getBienveillance() {
		return bienveillance;
	}
}