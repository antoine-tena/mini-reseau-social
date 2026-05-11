package socialnetwork.back;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un utilisateur du reseau: il recoit, transforme et transmet des messages.
 * Sert aussi de source d'evenements pour les fenetres (observer).
 */
public class Bavard implements MessageListener {
	private final String nom;
	private final Personnalite personnalite;
	// les amis a qui on peut parler
	private final List<Bavard> amis;
	// garde les messages transformes recus
	private final List<MessageEvent> messagesRecus;
	// un id = un message deja vu
	private final Set<UUID> messagesVus;
	// pour prevenir les fenetres
	private final List<BavardObserver> observateurs;

	// phase 4 - filtres de propagation
	// en dessous de ce seuil on refuse de retransmettre (defaut : -10 = tout passe)
	private float seuilTolerance = -10f;
	// probabilite de retransmettre entre 0.0 et 1.0 (defaut : 1.0 = toujours)
	private double probabiliteRetransmission = 1.0;
	// profondeur max pour les messages emis par ce bavard (-1 = infini)
	private int profondeurEmission = MessageEvent.PROFONDEUR_INFINIE;
	// compteur total de messages retransmis par ce bavard
	private int compteurTransmissions = 0;

	/**
	 * Cree un bavard avec un nom et une personnalite.
	 */
	public Bavard(String nom, Personnalite personnalite) {
		this.nom = nom;
		this.personnalite = personnalite;
		this.amis = new ArrayList<>();
		this.messagesRecus = new ArrayList<>();
		this.messagesVus = new HashSet<>();
		this.observateurs = new ArrayList<>();
	}

	public String getNom() {
		return nom;
	}

	public Personnalite getPersonnalite() {
		return personnalite;
	}

	public List<Bavard> getAmis() {
		return new ArrayList<>(amis);
	}

	public List<MessageEvent> getMessagesRecus() {
		return new ArrayList<>(messagesRecus);
	}

	/**
	 * Inscrit une fenetre (observer) pour etre notifiee des changements.
	 */
	public void ajouterObservateur(BavardObserver observateur) {
		if (observateur == null) {
			return;
		}
		if (observateurs.contains(observateur)) {
			return;
		}
		observateurs.add(observateur);
	}

	public void retirerObservateur(BavardObserver observateur) {
		observateurs.remove(observateur);
	}

	/**
	 * Ajoute un ami et notifie les observers si la liste change.
	 */
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
		// evite les doublons dans la liste
		amis.add(ami);
		notifierAmisMisAJour();
	}

	// ---- phase 4 : configuration ----

	public float getSeuilTolerance() { return seuilTolerance; }
	public void setSeuilTolerance(float seuil) { this.seuilTolerance = seuil; }

	public double getProbabiliteRetransmission() { return probabiliteRetransmission; }
	public void setProbabiliteRetransmission(double probabilite) {
		if (probabilite < 0.0) probabilite = 0.0;
		if (probabilite > 1.0) probabilite = 1.0;
		this.probabiliteRetransmission = probabilite;
	}

	public int getProfondeurEmission() { return profondeurEmission; }
	public void setProfondeurEmission(int profondeur) { this.profondeurEmission = profondeur; }

	public int getCompteurTransmissions() { return compteurTransmissions; }

	// ---- envoi / reception ----

	/**
	 * Envoie un message local puis le propage aux amis.
	 * La profondeur max est celle configuree sur ce bavard.
	 */
	public void envoyerMessage(String contenu) {
		if (contenu == null || contenu.isBlank()) {
			return;
		}
		// le message part de 0 puis la perso agit
		float bienveillance = personnalite.appliquer(0f, contenu);
		MessageEvent event = new MessageEvent(this, contenu, bienveillance, nom, profondeurEmission);
		messagesVus.add(event.getId());
		System.out.println(nom + " envoie : " + contenu + " | bienveillance = " + bienveillance
				+ " | profondeur max = " + (profondeurEmission == MessageEvent.PROFONDEUR_INFINIE ? "infinie" : profondeurEmission));
		retransmettreMessage(event, null);
	}

	@Override
	/**
	 * Recoit un message, le transforme selon la personnalite puis le retransmet.
	 * Phase 4 : verifie profondeur, seuil et probabilite avant retransmission.
	 */
	public void messageRecu(MessageEvent event) {
		if (event == null) {
			return;
		}
		// si deja vu on stop
		if (messagesVus.contains(event.getId())) {
			System.out.println(nom + " ignore \"" + event.getContenu() + "\" deja vu");
			return;
		}

		Bavard expediteur = null;
		if (event.getSource() instanceof Bavard) {
			// source = dernier bavard passe
			expediteur = (Bavard) event.getSource();
		}

		messagesVus.add(event.getId());
		// la perso change la valeur du message (phase 4 : passe le contenu)
		float ancienneBienveillance = event.getBienveillance();
		float nouvelleBienveillance = personnalite.appliquer(ancienneBienveillance, event.getContenu());
		// construit le nouveau event avec le noeud courant dans le chemin
		MessageEvent messageTransforme = event.avecNoeud(this, nouvelleBienveillance, nom);
		messagesRecus.add(messageTransforme);
		notifierMessageAjoute(messageTransforme);
		System.out.println(
			nom + " recoit \"" + event.getContenu() + "\" de " + event.getAuteur()
				+ " | bienveillance : " + ancienneBienveillance + " -> " + nouvelleBienveillance
				+ " | chemin : " + messageTransforme.getChemin());
		// phase 4 : filtre seuil de tolerance
		if (nouvelleBienveillance < seuilTolerance) {
			System.out.println(nom + " bloque la retransmission (seuil=" + seuilTolerance + ")");
			return;
		}
		// phase 4 : filtre profondeur
		if (messageTransforme.profondeurAtteinte()) {
			System.out.println(nom + " bloque la retransmission (profondeur max atteinte)");
			return;
		}
		// phase 4 : filtre probabiliste
		if (probabiliteRetransmission < 1.0 && Math.random() > probabiliteRetransmission) {
			System.out.println(nom + " ne retransmet pas (probabiliste)");
			return;
		}
		retransmettreMessage(messageTransforme, expediteur);
	}

	/**
	 * Relance un message recu en tant que nouveau message.
	 */
	public void faireSuivreMessage(MessageEvent message) {
		if (message == null) {
			return;
		}
		MessageEvent nouveauMessage = new MessageEvent(this, message.getContenu(), message.getBienveillance(), nom, profondeurEmission);
		messagesVus.add(nouveauMessage.getId());
		System.out.println(
			nom + " fait suivre : " + message.getContenu() + " | bienveillance = " + message.getBienveillance());
		retransmettreMessage(nouveauMessage, null);
	}

	// Propage aux amis en evitant celui qui vient d'envoyer.
	// Incremente le compteur local.
	private void retransmettreMessage(MessageEvent event, Bavard expediteur) {
		// on envoie aux amis sauf celui juste avant
		for (Bavard ami : amis) {
			if (ami == expediteur) {
				continue;
			}
			compteurTransmissions++;
			ami.messageRecu(event);
		}
	}

	// Notifie les observers que la liste de messages a change.
	private void notifierMessageAjoute(MessageEvent event) {
		for (BavardObserver observateur : observateurs) {
			observateur.messageAjoute(this, event);
		}
	}

	// Notifie les observers que la liste d'amis a change.
	private void notifierAmisMisAJour() {
		for (BavardObserver observateur : observateurs) {
			observateur.amisMisAJour(this);
		}
	}

	@Override
	public String toString() {
		return nom;
	}
}