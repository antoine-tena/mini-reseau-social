package socialnetwork;

public class JeuDeTest {
	public static void main(String[] args) {
		Reseau reseau = new Reseau();

		Bavard b1 = new Bavard("Antoine", Personnalite.POSITIF);
		Bavard b2 = new Bavard("Nathan", Personnalite.NEGATIF);
		Bavard b3 = new Bavard("Pavel", PersonnaliteForte.NEUTRE_FORTE);
		Bavard b4 = new Bavard("Sorana", PersonnaliteForte.POSITIF_FORTE);

		reseau.ajouterBavard(b1);
		reseau.ajouterBavard(b2);
		reseau.ajouterBavard(b3);
		reseau.ajouterBavard(b4);

		reseau.connecter(b1, b2);
		reseau.connecter(b2, b3);
		reseau.connecter(b3, b1);
		reseau.connecter(b3, b4);

		System.out.println("Test 1 : message d'Antoine");
		b1.envoyerMessage("Bonjour a tous");

		System.out.println();
		System.out.println("Test 2 : message de Nathan");
		b2.envoyerMessage("Salut, comment ca va ?");

        System.out.println();
        System.out.println("Test 3 : message de Pavel");
        b3.envoyerMessage("Hey, je vais mal");

        System.out.println();
        System.out.println("Test 4 : message de Sorana");
        b4.envoyerMessage("Coucou, comment allez-vous ?");
	}
}