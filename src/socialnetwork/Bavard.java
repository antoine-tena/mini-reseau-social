package socialnetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Bavard implements MessageListener {
	private final String nom;
	private final Personnalite personnalite;
	private final Set<UUID> messagesVus;
	private final List<MessageListener> amis;

	public Bavard(String nom, Personnalite personnalite) {
		this.nom = Objects.requireNonNull(nom, "nom ne doit pas etre null");
		this.personnalite = Objects.requireNonNull(personnalite, "personnalite ne doit pas etre null");
		this.messagesVus = new HashSet<>();
		this.amis = new ArrayList<>();
	}

	public String getNom() {
		return nom;
	}

	public Personnalite getPersonnalite() {
		return personnalite;
	}

	public Set<UUID> getMessagesVus() {
		return Set.copyOf(messagesVus);
	}

	public List<MessageListener> getAmis() {
		return List.copyOf(amis);
	}

	public void ajouterAmi(MessageListener ami) {
		Objects.requireNonNull(ami, "ami ne doit pas etre null");
		if (!amis.contains(ami)) {
			amis.add(ami);
		}
	}

	public void envoyerMessage(String texte) {
		MessageEvent event = new MessageEvent(this, nom, texte, personnalite.getBienveillance());
		for (MessageListener ami : amis) {
			ami.messageRecu(event);
		}
	}

	@Override
	public void messageRecu(MessageEvent event) {
		Objects.requireNonNull(event, "event ne doit pas etre null");
		messagesVus.add(event.getId());
	}
}