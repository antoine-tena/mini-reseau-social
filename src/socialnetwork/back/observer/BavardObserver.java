package socialnetwork.back.observer;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;

/**
 * Observer pour synchroniser l'interface quand un bavard change.
 */
public interface BavardObserver {
	/** Appele quand un nouveau message arrive dans la fenetre du bavard. */
	void messageAjoute(Bavard bavard, MessageEvent event);

	/** Appele quand la liste d'amis change. */
	void amisMisAJour(Bavard bavard);
}
