package socialnetwork;

/**
 * Recoit un message lors de la propagation.
 */
public interface MessageListener {
	// appele quand un bavard recoit un message
	void messageRecu(MessageEvent event);
}