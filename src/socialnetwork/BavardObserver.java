package socialnetwork;

/**
 * Observer pour synchroniser l'interface quand un bavard change.
 */
public interface BavardObserver {
	// quand un nouveau message arrive dans la fenetre
	void messageAjoute(Bavard bavard, MessageEvent event);

	// quand la liste d'amis change
	void amisMisAJour(Bavard bavard);
}