package socialnetwork.back.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.observer.BavardObserver;

/**
 * Collecteur de statistiques pour un Reseau.
 *
 * S'inscrit comme BavardObserver sur tous les bavards du reseau.
 * A appeler apres avoir construit le reseau complet.
 *
 * Statistiques disponibles :
 *  - nombre total de messages recus dans le reseau
 *  - bienveillance moyenne de tous les messages
 *  - variance de bienveillance
 *  - nombre de transmissions par bavard
 *  - bavard le plus/moins actif
 *  - longueur moyenne des chemins
 *  - taux de propagation
 */
public class StatistiquesReseau implements BavardObserver {

	private final Reseau reseau;
	private int     totalMessages      = 0;
	private double  sommeBienveillance = 0.0;
	private double  sommeCarresBienveillance = 0.0;
	private final Map<String, Integer> messagesParBavard = new HashMap<>();
	private long    sommeLongueursChemin = 0;
	private int     maxTransmissions    = 0;

	public StatistiquesReseau(Reseau reseau) {
		this.reseau = reseau;
		for (Bavard b : reseau.getBavards()) b.ajouterObservateur(this);
	}

	/** A appeler quand un nouveau bavard est ajoute au reseau apres la creation. */
	public void observerBavard(Bavard bavard) {
		bavard.ajouterObservateur(this);
	}

	@Override
	public void messageAjoute(Bavard bavard, MessageEvent event) {
		totalMessages++;
		double b = event.getBienveillance();
		sommeBienveillance       += b;
		sommeCarresBienveillance += b * b;
		messagesParBavard.merge(bavard.getNom(), 1, Integer::sum);
		sommeLongueursChemin += event.getChemin().size();
		maxTransmissions = Math.max(maxTransmissions, event.getTransmissions());
	}

	@Override
	public void amisMisAJour(Bavard bavard) { /* non utilise */ }

	// ─── Getters ──────────────────────────────────────────────────────────────

	public int getTotalMessages()            { return totalMessages; }

	public double getBienveillanceMoyenne() {
		if (totalMessages == 0) return 0.0;
		return sommeBienveillance / totalMessages;
	}

	/**
	 * Variance de la bienveillance : E[X²] - E[X]².
	 * Mesure la dispersion des messages : 0 = tous identiques.
	 */
	public double getVarianceBienveillance() {
		if (totalMessages == 0) return 0.0;
		double moy = getBienveillanceMoyenne();
		return sommeCarresBienveillance / totalMessages - moy * moy;
	}

	/** Ecart-type de la bienveillance. */
	public double getEcartTypeBienveillance() {
		return Math.sqrt(Math.max(0.0, getVarianceBienveillance()));
	}

	public double getLongueurMoyenneChemin() {
		if (totalMessages == 0) return 0.0;
		return (double) sommeLongueursChemin / totalMessages;
	}

	public Map<String, Integer> getMessagesParBavard() {
		return new HashMap<>(messagesParBavard);
	}

	/** Bavard ayant recu le plus de messages, ou null si aucun. */
	public String getBavardLePlusActif() {
		return messagesParBavard.entrySet().stream()
				.max(Comparator.comparingInt(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/** Bavard ayant recu le moins de messages (parmi ceux ayant recu au moins 1). */
	public String getBavardLeMoinsActif() {
		return messagesParBavard.entrySet().stream()
				.min(Comparator.comparingInt(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/** Compteur de transmissions total de tous les bavards. */
	public int getCompteurTransmissionsTotal() {
		return reseau.getBavards().stream()
				.mapToInt(Bavard::getCompteurTransmissions).sum();
	}

	/** Nombre maximum de transmissions subies par un seul message. */
	public int getMaxTransmissions()         { return maxTransmissions; }

	/**
	 * Taux de propagation : totalMessages / (n × (n-1)).
	 * 1.0 = chaque bavard a recu chaque message de chaque autre bavard.
	 */
	public double getTauxPropagation() {
		int n = reseau.getBavards().size();
		if (n <= 1) return 0.0;
		return (double) totalMessages / ((long) n * (n - 1));
	}

	// ─── Resume ───────────────────────────────────────────────────────────────

	/** Tableau de bord lisible. */
	public String resume() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== statistiques ===\n");
		sb.append("messages recus total : ").append(totalMessages).append('\n');
		sb.append("transmissions total  : ").append(getCompteurTransmissionsTotal()).append('\n');
		sb.append(String.format("bienveillance moy    : %.2f%n", getBienveillanceMoyenne()));
		sb.append(String.format("variance bienveillance: %.2f  ecart-type: %.2f%n",
				getVarianceBienveillance(), getEcartTypeBienveillance()));
		sb.append(String.format("longueur moy chemin  : %.2f%n", getLongueurMoyenneChemin()));
		sb.append(String.format("taux propagation     : %.4f%n", getTauxPropagation()));
		sb.append("bavard le plus actif : ").append(getBavardLePlusActif()).append('\n');
		sb.append("bavard le moins actif: ").append(getBavardLeMoinsActif()).append('\n');
		sb.append("detail par bavard :\n");
		List<String> noms = new ArrayList<>(messagesParBavard.keySet());
		noms.sort(String::compareToIgnoreCase);
		for (String nom : noms) {
			sb.append("  ").append(nom).append(" : ").append(messagesParBavard.get(nom)).append(" msg\n");
		}
		return sb.toString();
	}

	/** Remet toutes les statistiques a zero. */
	public void reset() {
		totalMessages            = 0;
		sommeBienveillance       = 0.0;
		sommeCarresBienveillance = 0.0;
		messagesParBavard.clear();
		sommeLongueursChemin     = 0;
		maxTransmissions         = 0;
	}
}
