package socialnetwork.back.observer;

import socialnetwork.back.model.MessageEvent;

/**
 * Recoit un message lors de la propagation.
 */
public interface MessageListener {
	// appele quand un bavard recoit un message
	void messageRecu(MessageEvent event);
}
