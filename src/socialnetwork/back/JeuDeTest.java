package socialnetwork.back;

import java.util.Set;

/**
 * Lancement de tests console pour verifier la propagation.
 * Phase 4 : nouveaux tests chemin, profondeur, seuil, proba, personnalites avancees.
 */
public class JeuDeTest {
	// Enchaine plusieurs scenarios simples puis phase 4.
	public static void main(String[] args) {
		testSimple();
		testAvecBoucle();
		testReseauPlusGrand();
		testBorneBienveillance();
		// phase 4
		testCheminMessage();
		testProfondeurLimitee();
		testSeuilTolerance();
		testPropagationProbabiliste();
		testPersonnaliteContextuelle();
		testPersonnaliteAdaptative();
		testStatistiques();
	}

	// debut simple avec 2 bavards
	// Cas minimal avec deux bavards.
	private static void testSimple() {
		System.out.println("test 1 - 2 bavards");

		// un cas tout petit pour voir le debut
		Reseau reseau = new Reseau();
		Bavard lucie = new Bavard("Lucie", Personnalite.POSITIF);
		Bavard tom = new Bavard("Tom", Personnalite.NEGATIF);

		reseau.ajouterBavard(lucie);
		reseau.ajouterBavard(tom);
		reseau.connecter(lucie, tom);

		lucie.envoyerMessage("salut tom");
		System.out.println();
	}

	// ici il y a une boucle
	// Triangle: verifie l'absence de boucle infinie.
	private static void testAvecBoucle() {
		System.out.println("test 2 - triangle avec boucle");

		// les 3 sont relies en triangle
		Reseau reseau = new Reseau();
		Bavard anna = new Bavard("Anna", Personnalite.POSITIF);
		Bavard mehdi = new Bavard("Mehdi", Personnalite.NEGATIF);
		Bavard ines = new Bavard("Ines", Personnalite.NEUTRE);

		reseau.ajouterBavard(anna);
		reseau.ajouterBavard(mehdi);
		reseau.ajouterBavard(ines);

		reseau.connecter(anna, mehdi);
		reseau.connecter(mehdi, ines);
		reseau.connecter(ines, anna);

		// anna doit pas repasser sans fin
		anna.envoyerMessage("message dans la boucle");
		System.out.println("fin du test 2 = pas de boucle infinie");
		System.out.println();
	}

	// graphe un peu plus grand
	// Graphe plus large pour observer la diffusion.
	private static void testReseauPlusGrand() {
		System.out.println("test 3 - propagation dans un graphe");

		// ici il y a plusieurs chemins possibles
		Reseau reseau = new Reseau();
		Bavard noah = new Bavard("Noah", Personnalite.NEUTRE);
		Bavard lea = new Bavard("Lea", Personnalite.POSITIF);
		Bavard yassine = new Bavard("Yassine", Personnalite.NEGATIF);
		Bavard chloe = new Bavard("Chloe", PersonnaliteForte.POSITIF_FORTE);
		Bavard sam = new Bavard("Sam", PersonnaliteForte.NEUTRE_FORTE);

		reseau.ajouterBavard(noah);
		reseau.ajouterBavard(lea);
		reseau.ajouterBavard(yassine);
		reseau.ajouterBavard(chloe);
		reseau.ajouterBavard(sam);

		reseau.connecter(noah, lea);
		reseau.connecter(noah, yassine);
		reseau.connecter(lea, chloe);
		reseau.connecter(yassine, sam);
		reseau.connecter(chloe, sam);

		// ici on voit mieux la propagation
		noah.envoyerMessage("bonjour le reseau");
		System.out.println();
	}

	// on verifie la borne max
	// Verifie la borne de bienveillance.
	private static void testBorneBienveillance() {
		System.out.println("test 4 - borne de bienveillance");

		// chaine simple assez longue pour monter a 10
		Reseau reseau = new Reseau();
		Bavard[] bavards = new Bavard[10];
		for (int i = 0; i < bavards.length; i++) {
			bavards[i] = new Bavard("B" + (i + 1), Personnalite.POSITIF);
			reseau.ajouterBavard(bavards[i]);
			if (i > 0) {
				reseau.connecter(bavards[i - 1], bavards[i]);
			}
		}

		bavards[0].envoyerMessage("message tres positif");
		System.out.println("on doit voir la valeur bloquee a 10 a la fin");
	}

	// ======================== phase 4 ========================

	// Verifie que le chemin du message est bien enregistre.
	private static void testCheminMessage() {
		System.out.println("\ntest 5 (phase 4) - chemin du message");
		Reseau reseau = new Reseau();
		Bavard alice = new Bavard("Alice", Personnalite.NEUTRE);
		Bavard bob   = new Bavard("Bob",   Personnalite.POSITIF);
		Bavard carl  = new Bavard("Carl",  Personnalite.NEGATIF);
		reseau.ajouterBavard(alice);
		reseau.ajouterBavard(bob);
		reseau.ajouterBavard(carl);
		reseau.connecter(alice, bob);
		reseau.connecter(bob, carl);

		alice.envoyerMessage("chemin test");
		System.out.println("messages de Carl :");
		for (MessageEvent m : carl.getMessagesRecus()) {
			System.out.println("  " + m);
		}
		System.out.println("chemin attendu : [Alice, Bob, Carl]");
		System.out.println();
	}

	// Verifie que la propagation s'arrete a la profondeur max.
	private static void testProfondeurLimitee() {
		System.out.println("test 6 (phase 4) - profondeur limitee a 2");
		Reseau reseau = new Reseau();
		Bavard[] bavards = new Bavard[6];
		for (int i = 0; i < bavards.length; i++) {
			bavards[i] = new Bavard("N" + i, Personnalite.NEUTRE);
			reseau.ajouterBavard(bavards[i]);
			if (i > 0) reseau.connecter(bavards[i - 1], bavards[i]);
		}
		// profondeur max = 2 : N0 -> N1 -> N2, N3 ne doit rien recevoir
		bavards[0].setProfondeurEmission(2);
		bavards[0].envoyerMessage("stop a 2");
		System.out.println("N2 recoit : " + bavards[2].getMessagesRecus().size() + " message(s) (attendu 1)");
		System.out.println("N3 recoit : " + bavards[3].getMessagesRecus().size() + " message(s) (attendu 0)");
		System.out.println();
	}

	// Verifie que le seuil de tolerance bloque la retransmission.
	private static void testSeuilTolerance() {
		System.out.println("test 7 (phase 4) - seuil de tolerance");
		Reseau reseau = new Reseau();
		Bavard emetteur = new Bavard("Emetteur", Personnalite.NEGATIF);
		// milieu : devient tres negatif, seuil = 0 -> bloque
		Bavard milieu   = new Bavard("Milieu",   Personnalite.NEGATIF);
		milieu.setSeuilTolerance(0f);
		Bavard fin      = new Bavard("Fin",      Personnalite.POSITIF);
		reseau.ajouterBavard(emetteur);
		reseau.ajouterBavard(milieu);
		reseau.ajouterBavard(fin);
		reseau.connecter(emetteur, milieu);
		reseau.connecter(milieu, fin);

		emetteur.envoyerMessage("message negatif");
		System.out.println("Fin recoit : " + fin.getMessagesRecus().size() + " message(s) (attendu 0 car bloque par Milieu)");
		System.out.println();
	}

	// Verifie que la propagation probabiliste ne bloque pas systematiquement.
	private static void testPropagationProbabiliste() {
		System.out.println("test 8 (phase 4) - propagation probabiliste (proba=0.5)");
		Reseau reseau = new Reseau();
		Bavard source = new Bavard("Source", Personnalite.NEUTRE);
		Bavard cible  = new Bavard("Cible",  Personnalite.NEUTRE);
		cible.setProbabiliteRetransmission(0.5);
		Bavard fin    = new Bavard("Fin",    Personnalite.NEUTRE);
		reseau.ajouterBavard(source);
		reseau.ajouterBavard(cible);
		reseau.ajouterBavard(fin);
		reseau.connecter(source, cible);
		reseau.connecter(cible, fin);

		// on envoie 10 messages independants (chacun avec un nouveau id)
		int recu = 0;
		for (int i = 0; i < 10; i++) {
			source.envoyerMessage("msg " + i);
		}
		recu = fin.getMessagesRecus().size();
		System.out.println("Fin a recu " + recu + "/10 messages (attendu ~5, peut varier)");
		System.out.println();
	}

	// Verifie le comportement de la personnalite contextuelle.
	private static void testPersonnaliteContextuelle() {
		System.out.println("test 9 (phase 4) - personnalite contextuelle");
		Reseau reseau = new Reseau();
		Bavard emetteur = new Bavard("E", Personnalite.NEUTRE);
		// reactionne fortement aux messages contenant "haine"
		Bavard sensible = new Bavard("Sensible",
				new PersonnaliteContextuelle("SENSIBLE", -0.5f, Set.of("haine", "colere"), 4f));
		Bavard fin = new Bavard("Fin", Personnalite.NEUTRE);
		reseau.ajouterBavard(emetteur);
		reseau.ajouterBavard(sensible);
		reseau.ajouterBavard(fin);
		reseau.connecter(emetteur, sensible);
		reseau.connecter(sensible, fin);

		emetteur.envoyerMessage("message normal");
		emetteur.envoyerMessage("message de haine");
		System.out.println("messages recus par Fin :");
		for (MessageEvent m : fin.getMessagesRecus()) {
			System.out.println("  " + m);
		}
		System.out.println("le message avec 'haine' doit avoir une bienveillance plus basse");
		System.out.println();
	}

	// Verifie que la personnalite adaptative evolue avec le temps.
	private static void testPersonnaliteAdaptative() {
		System.out.println("test 10 (phase 4) - personnalite adaptative");
		PersonnaliteAdaptative perso = new PersonnaliteAdaptative("ADAPTATIF+", 0.5f, 0.2f, 3f);
		System.out.println("variation initiale : " + perso.getVariationCourante());
		// simulation de messages avec bienveillance croissante
		perso.appliquer(2f, "test positif");
		perso.appliquer(3f, "encore plus positif");
		perso.appliquer(4f, "super positif");
		System.out.println("variation apres 3 messages positifs : " + perso.getVariationCourante()
				+ " (doit avoir augmente)");
		perso.appliquer(-5f, "message tres negatif");
		System.out.println("variation apres 1 message negatif : " + perso.getVariationCourante()
				+ " (doit avoir diminue)");
		System.out.println();
	}

	// Verifie que les statistiques sont bien collectees.
	private static void testStatistiques() {
		System.out.println("test 11 (phase 4) - statistiques");
		Reseau reseau = new Reseau();
		Bavard a = new Bavard("A", Personnalite.POSITIF);
		Bavard b = new Bavard("B", Personnalite.NEUTRE);
		Bavard c = new Bavard("C", Personnalite.NEGATIF);
		reseau.ajouterBavard(a);
		reseau.ajouterBavard(b);
		reseau.ajouterBavard(c);
		reseau.connecter(a, b);
		reseau.connecter(b, c);

		StatistiquesReseau stats = new StatistiquesReseau(reseau);
		a.envoyerMessage("test stats 1");
		a.envoyerMessage("test stats 2");
		System.out.print(stats.resume());
	}
}