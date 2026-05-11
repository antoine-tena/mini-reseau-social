package socialnetwork.back;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;

/**
 * Evenement transporte lors d'une propagation de message.
 *
 * Phase 4 :
 *  - chemin : liste ordonnee des bavards traverses (noms)
 *  - hopActuel / profondeurMax : controle de profondeur (-1 = infini)
 *  - transmissions : nombre de retransmissions subies
 */
public class MessageEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	/** Profondeur illimitee. */
	public static final int PROFONDEUR_INFINIE = -1;

	// sert a reconnaitre le meme message
	private final UUID id;
	private final String contenu;
	private final float bienveillance;
	private final String auteur;

	// phase 4 - chemin et propagation
	private final List<String> chemin;
	private final int hopActuel;
	private final int profondeurMax;
	private final int transmissions;

	/**
	 * Nouveau message emis par un bavard (profondeur infinie par defaut).
	 */
	public MessageEvent(Object source, String contenu, float bienveillance, String auteur) {
		this(UUID.randomUUID(), source, contenu, bienveillance, auteur,
				Collections.singletonList(auteur), 0, PROFONDEUR_INFINIE, 0);
	}

	/**
	 * Nouveau message avec profondeur max choisie par l'emetteur.
	 */
	public MessageEvent(Object source, String contenu, float bienveillance, String auteur, int profondeurMax) {
		this(UUID.randomUUID(), source, contenu, bienveillance, auteur,
				Collections.singletonList(auteur), 0, profondeurMax, 0);
	}

	/**
	 * Message retransmis: conserve l'identifiant (retrocompatibilite).
	 */
	public MessageEvent(UUID id, Object source, String contenu, float bienveillance, String auteur) {
		this(id, source, contenu, bienveillance, auteur,
				Collections.singletonList(auteur), 0, PROFONDEUR_INFINIE, 0);
	}

	/**
	 * Constructeur complet interne.
	 */
	public MessageEvent(UUID id, Object source, String contenu, float bienveillance, String auteur,
			List<String> chemin, int hopActuel, int profondeurMax, int transmissions) {
		super(source);
		this.id = id;
		this.contenu = contenu;
		this.bienveillance = bornerBienveillance(bienveillance);
		this.auteur = auteur;
		// copie defensive pour que deux branches n'aient pas le meme objet
		this.chemin = Collections.unmodifiableList(new ArrayList<>(chemin));
		this.hopActuel = hopActuel;
		this.profondeurMax = profondeurMax;
		this.transmissions = transmissions;
	}

	/**
	 * Construit un nouvel evenement en ajoutant un noeud au chemin.
	 * Incremente hop et transmissions.
	 */
	public MessageEvent avecNoeud(Object source, float nouvelleBienveillance, String nomNoeud) {
		List<String> nouveauChemin = new ArrayList<>(chemin);
		nouveauChemin.add(nomNoeud);
		return new MessageEvent(id, source, contenu, nouvelleBienveillance, auteur,
				nouveauChemin, hopActuel + 1, profondeurMax, transmissions + 1);
	}

	/**
	 * Vrai si la profondeur max est atteinte (on ne doit plus retransmettre).
	 */
	public boolean profondeurAtteinte() {
		if (profondeurMax == PROFONDEUR_INFINIE) {
			return false;
		}
		return hopActuel >= profondeurMax;
	}

	// reste entre -10 et 10
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

	public List<String> getChemin() {
		return chemin;
	}

	public int getHopActuel() {
		return hopActuel;
	}

	public int getProfondeurMax() {
		return profondeurMax;
	}

	public int getTransmissions() {
		return transmissions;
	}

	@Override
	public String toString() {
		String cheminStr = String.join(" -> ", chemin);
		return auteur + " : \"" + contenu + "\" | b=" + String.format("%.1f", bienveillance)
				+ " | hop=" + hopActuel + " | tx=" + transmissions
				+ " | chemin=[" + cheminStr + "]";
	}
}