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
	// same id = same message
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
		if (contenu == null || contenu.isBlank()) {
			return;
		}
		int bienveillance = personnalite.appliquer(0);
		MessageEvent event = new MessageEvent(this, contenu, bienveillance, nom);
		messagesVus.add(event.getId());
		System.out.println(nom + " envoie : " + contenu + " | bienveillance = " + bienveillance);
		retransmettreMessage(event, null);
	}

	@Override
	public void messageRecu(MessageEvent event) {
		if (event == null) {
			return;
		}
		if (messagesVus.contains(event.getId())) {
			System.out.println(nom + " ignore \"" + event.getContenu() + "\" deja vu");
			return;
		}

		Bavard expediteur = null;
		if (event.getSource() instanceof Bavard) {
			expediteur = (Bavard) event.getSource();
		}

		messagesVus.add(event.getId());
		int ancienneBienveillance = event.getBienveillance();
		int nouvelleBienveillance = personnalite.appliquer(ancienneBienveillance);
		MessageEvent messageTransforme = new MessageEvent(
			event.getId(),
			this,
			event.getContenu(),
			nouvelleBienveillance,
			event.getAuteur());
		System.out.println(
			nom + " recoit \"" + event.getContenu() + "\" de " + event.getAuteur()
				+ " | bienveillance : " + ancienneBienveillance + " -> " + nouvelleBienveillance);
		retransmettreMessage(messageTransforme, expediteur);
	}

	private void retransmettreMessage(MessageEvent event, Bavard expediteur) {
		for (Bavard ami : amis) {
			// avoid direct return
			if (ami == expediteur) {
				continue;
			}
			ami.messageRecu(event);
		}
	}
}