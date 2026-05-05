package socialnetwork;

public class JeuDeTest {
	public static void main(String[] args) {
		testSimple();
		testAvecBoucle();
		testReseauPlusGrand();
		testBorneBienveillance();
	}

	// debut simple avec 2 bavards
	private static void testSimple() {
		System.out.println("test 1 - 2 bavards");

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
	private static void testAvecBoucle() {
		System.out.println("test 2 - triangle avec boucle");

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

		anna.envoyerMessage("message dans la boucle");
		System.out.println("fin du test 2 = pas de boucle infinie");
		System.out.println();
	}

	// graphe un peu plus grand
	private static void testReseauPlusGrand() {
		System.out.println("test 3 - propagation dans un graphe");

		Reseau reseau = new Reseau();
		Bavard noah = new Bavard("Noah", Personnalite.NEUTRE);
		Bavard lea = new Bavard("Lea", Personnalite.POSITIF);
		Bavard yassine = new Bavard("Yassine", Personnalite.NEGATIF);
		Bavard chloe = new Bavard("Chloe", Personnalite.POSITIF);
		Bavard sam = new Bavard("Sam", Personnalite.NEUTRE);

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

		noah.envoyerMessage("bonjour le reseau");
		System.out.println();
	}

	// on verifie la borne max
	private static void testBorneBienveillance() {
		System.out.println("test 4 - borne de bienveillance");

		Reseau reseau = new Reseau();
		Bavard b1 = new Bavard("B1", Personnalite.POSITIF);
		Bavard b2 = new Bavard("B2", Personnalite.POSITIF);
		Bavard b3 = new Bavard("B3", Personnalite.POSITIF);
		Bavard b4 = new Bavard("B4", Personnalite.POSITIF);
		Bavard b5 = new Bavard("B5", Personnalite.POSITIF);
		Bavard b6 = new Bavard("B6", Personnalite.POSITIF);

		reseau.ajouterBavard(b1);
		reseau.ajouterBavard(b2);
		reseau.ajouterBavard(b3);
		reseau.ajouterBavard(b4);
		reseau.ajouterBavard(b5);
		reseau.ajouterBavard(b6);

		reseau.connecter(b1, b2);
		reseau.connecter(b2, b3);
		reseau.connecter(b3, b4);
		reseau.connecter(b4, b5);
		reseau.connecter(b5, b6);

		b1.envoyerMessage("message tres positif");
		System.out.println("on doit voir la valeur bloquee a 10 a la fin");
	}
}