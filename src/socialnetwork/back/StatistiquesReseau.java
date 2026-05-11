package socialnetwork.back;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 4 - collecteur de statistiques pour un Reseau.
 *
 * S'inscrit comme BavardObserver sur tous les bavards du reseau.
 * A appeler apres avoir construit le reseau complet.
 *
 * Statistiques disponibles :
 *  - nombre total de messages recus dans le reseau
 *  - bienveillance moyenne de tous les messages
 *  - nombre de transmissions par bavard
 *  - bavard ayant recu le plus de messages (influenceur recepteur)
 *  - longueur moyenne des chemins
 */
public class StatistiquesReseau implements BavardObserver {

	private final Reseau reseau;
	// nombre total de messages recus (toutes fenetres confondues)
	private int totalMessages = 0;
	// somme des bienveillances pour calculer la moyenne
	private double sommeBienveillance = 0.0;
	// nb messages recus par bavard (nom -> count)
	private final Map<String, Integer> messagesParBavard = new HashMap<>();
	// somme des longueurs de chemin pour calculer la moyenne
	private long sommeLongueursChemin = 0;

	public StatistiquesReseau(Reseau reseau) {
		this.reseau = reseau;
		// s'inscrire sur tous les bavards existants
		for (Bavard b : reseau.getBavards()) {
			b.ajouterObservateur(this);
		}
	}

	/**
	 * A appeler quand un nouveau bavard est ajoute au reseau apres la creation.
	 */
	public void observerBavard(Bavard bavard) {
		bavard.ajouterObservateur(this);
	}

	@Override
	public void messageAjoute(Bavard bavard, MessageEvent event) {
		totalMessages++;
		sommeBienveillance += event.getBienveillance();
		messagesParBavard.merge(bavard.getNom(), 1, Integer::sum);
		sommeLongueursChemin += event.getChemin().size();
	}

	@Override
	public void amisMisAJour(Bavard bavard) {
		// pas utile pour les stats
	}

	public int getTotalMessages() {
		return totalMessages;
	}

	public double getBienveillanceMoyenne() {
		if (totalMessages == 0) return 0.0;
		return sommeBienveillance / totalMessages;
	}

	public double getLongueurMoyenneChemin() {
		if (totalMessages == 0) return 0.0;
		return (double) sommeLongueursChemin / totalMessages;
	}

	public Map<String, Integer> getMessagesParBavard() {
		return new HashMap<>(messagesParBavard);
	}

	/**
	 * Retourne le bavard ayant recu le plus de messages, ou null si aucun.
	 */
	public String getBavardLePlusActif() {
		return messagesParBavard.entrySet().stream()
				.max(Comparator.comparingInt(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/**
	 * Retourne le compteur de transmissions total de tous les bavards du reseau.
	 */
	public int getCompteurTransmissionsTotal() {
		return reseau.getBavards().stream()
				.mapToInt(Bavard::getCompteurTransmissions)
				.sum();
	}

	/**
	 * Tableau de bord lisible.
	 */
	public String resume() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== statistiques ===\n");
		sb.append("messages recus total : ").append(totalMessages).append('\n');
		sb.append("transmissions total  : ").append(getCompteurTransmissionsTotal()).append('\n');
		sb.append(String.format("bienveillance moy    : %.2f%n", getBienveillanceMoyenne()));
		sb.append(String.format("longueur moy chemin  : %.2f%n", getLongueurMoyenneChemin()));
		sb.append("bavard le plus actif : ").append(getBavardLePlusActif()).append('\n');
		sb.append("detail par bavard :\n");
		// tri par nom pour un affichage stable
		List<String> noms = new ArrayList<>(messagesParBavard.keySet());
		noms.sort(String::compareToIgnoreCase);
		for (String nom : noms) {
			sb.append("  ").append(nom).append(" : ").append(messagesParBavard.get(nom)).append(" msg\n");
		}
		return sb.toString();
	}

	public void reset() {
		totalMessages = 0;
		sommeBienveillance = 0.0;
		messagesParBavard.clear();
		sommeLongueursChemin = 0;
	}
}
