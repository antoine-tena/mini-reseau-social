package socialnetwork.back;

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
}