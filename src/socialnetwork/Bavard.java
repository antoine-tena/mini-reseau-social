package socialnetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bavard implements MessageListener {
	private final String nom;
	private final Personnalite personnalite;
	private final List<Bavard> amis;
	private final Set<UUID> messagesVus;

	public Bavard(String nom, Personnalite personnalite) {
		this.nom = nom;
		this.personnalite = personnalite;
		this.amis = new ArrayList<>();
		this.messagesVus = new HashSet<>();
	}

	public String getNom() {
		return nom;
	}

	public Personnalite getPersonnalite() {
		return personnalite;
	}

	public void ajouterAmi(Bavard ami) {
		if (ami == null) {
			return;
		}
		if (ami == this) {
			return;
		}
		if (amis.contains(ami)) {
			return;
		}
		amis.add(ami);
	}

	public void envoyerMessage(String contenu) {
		int bienveillance = personnalite.appliquer(0);
		MessageEvent event = new MessageEvent(this, contenu, bienveillance, nom);
		messagesVus.add(event.getId());
		System.out.println(nom + " envoie : " + contenu + " | bienveillance = " + bienveillance);
		retransmettreMessage(event);
	}

	@Override
	public void messageRecu(MessageEvent event) {
		if (messagesVus.contains(event.getId())) {
			System.out.println(nom + " ignore le message deja recu.");
			return;
		}
		messagesVus.add(event.getId());
		int nouvelleBienveillance = personnalite.appliquer(event.getBienveillance());
		MessageEvent messageTransforme = new MessageEvent(
			event.getId(),
			this,
			event.getContenu(),
			nouvelleBienveillance,
			event.getAuteur());
		System.out.println(
			nom + " recoit \"" + event.getContenu() + "\" de " + event.getAuteur()
				+ " | bienveillance : " + event.getBienveillance() + " -> " + nouvelleBienveillance);
		retransmettreMessage(messageTransforme);
	}

	private void retransmettreMessage(MessageEvent event) {
		for (Bavard ami : amis) {
			ami.messageRecu(event);
		}
	}
}