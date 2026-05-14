// Module Java du projet: expose le back et le front Swing.
module SocialNetwork {
	requires java.desktop;
	exports socialnetwork.back.model;
	exports socialnetwork.back.observer;
	exports socialnetwork.back.personnalite;
	exports socialnetwork.back.stats;
	exports socialnetwork.back.analyse;
	exports socialnetwork.back.topologie;
	exports socialnetwork.back.export;
	exports socialnetwork.back.test;
	exports socialnetwork.front;
	exports socialnetwork.front.fenetre;
	exports socialnetwork.front.vue;
}