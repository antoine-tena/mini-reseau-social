package socialnetwork.back;

public interface MessageListener {
	// appele quand un bavard recoit un message
	void messageRecu(MessageEvent event);
}