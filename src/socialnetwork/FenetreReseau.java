package socialnetwork;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class FenetreReseau extends JFrame {
	private final Reseau reseau;
	private final Map<Bavard, FenetreBavard> fenetres;
	private final DefaultListModel<Bavard> modeleBavards;
	private final JList<Bavard> listeBavards;
	private final JTextField champNom;
	private final JComboBox<Personnalite> choixPersonnalite;
	private final JComboBox<Bavard> choixLien1;
	private final JComboBox<Bavard> choixLien2;
	private final JComboBox<Bavard> choixEmission;
	private final JTextField champMessage;
	private final JTextArea zoneResume;

	public FenetreReseau() {
		this.reseau = new Reseau();
		this.fenetres = new LinkedHashMap<>();
		this.modeleBavards = new DefaultListModel<>();
		this.listeBavards = new JList<>(modeleBavards);
		this.champNom = new JTextField();
		this.choixPersonnalite = new JComboBox<>(creerChoixPersonnalites());
		this.choixLien1 = new JComboBox<>();
		this.choixLien2 = new JComboBox<>();
		this.choixEmission = new JComboBox<>();
		this.champMessage = new JTextField();
		this.zoneResume = new JTextArea();

		setTitle("reseau social emotionnel");
		setSize(860, 540);
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		construireInterface();
		actualiserChoix();
		actualiserResume();
	}

	private void construireInterface() {
		JPanel creation = new JPanel(new GridLayout(3, 2, 5, 5));
		creation.setBorder(BorderFactory.createTitledBorder("creer un bavard"));
		creation.add(new JLabel("nom"));
		creation.add(champNom);
		creation.add(new JLabel("personnalite"));
		creation.add(choixPersonnalite);
		JButton boutonAjouter = new JButton("ajouter");
		JButton boutonExemple = new JButton("creer exemple");
		creation.add(boutonAjouter);
		creation.add(boutonExemple);

		JPanel liens = new JPanel(new GridLayout(3, 2, 5, 5));
		liens.setBorder(BorderFactory.createTitledBorder("connecter"));
		liens.add(new JLabel("bavard 1"));
		liens.add(choixLien1);
		liens.add(new JLabel("bavard 2"));
		liens.add(choixLien2);
		JButton boutonConnecter = new JButton("connecter");
		JButton boutonOuvrir = new JButton("ouvrir fenetre");
		liens.add(boutonConnecter);
		liens.add(boutonOuvrir);

		JPanel propagation = new JPanel(new GridLayout(3, 2, 5, 5));
		propagation.setBorder(BorderFactory.createTitledBorder("lancer une propagation"));
		propagation.add(new JLabel("emetteur"));
		propagation.add(choixEmission);
		propagation.add(new JLabel("message"));
		propagation.add(champMessage);
		JButton boutonPropager = new JButton("propager");
		JButton boutonToutOuvrir = new JButton("ouvrir tout");
		propagation.add(boutonPropager);
		propagation.add(boutonToutOuvrir);

		JPanel gauche = new JPanel(new GridLayout(3, 1, 5, 5));
		gauche.add(creation);
		gauche.add(liens);
		gauche.add(propagation);

		JScrollPane scrollBavards = new JScrollPane(listeBavards);
		scrollBavards.setBorder(BorderFactory.createTitledBorder("bavards du reseau"));

		zoneResume.setEditable(false);
		JScrollPane scrollResume = new JScrollPane(zoneResume);
		scrollResume.setBorder(BorderFactory.createTitledBorder("resume du reseau"));

		JSplitPane droite = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollBavards, scrollResume);
		droite.setResizeWeight(0.45);

		JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gauche, droite);
		principal.setResizeWeight(0.45);

		setLayout(new BorderLayout(5, 5));
		add(principal, BorderLayout.CENTER);

		boutonAjouter.addActionListener(event -> ajouterBavard());
		boutonExemple.addActionListener(event -> creerExemple());
		boutonConnecter.addActionListener(event -> connecterBavards());
		boutonOuvrir.addActionListener(event -> ouvrirSelection());
		boutonPropager.addActionListener(event -> lancerPropagation());
		boutonToutOuvrir.addActionListener(event -> ouvrirToutesLesFenetres());
	}

	private Personnalite[] creerChoixPersonnalites() {
		return new Personnalite[] {
			Personnalite.POSITIF,
			Personnalite.NEGATIF,
			Personnalite.NEUTRE,
			PersonnaliteForte.POSITIF_FORTE,
			PersonnaliteForte.NEGATIF_FORTE,
			PersonnaliteForte.NEUTRE_FORTE
		};
	}

	private void ajouterBavard() {
		String nom = champNom.getText().trim();
		if (nom.isEmpty()) {
			JOptionPane.showMessageDialog(this, "il faut un nom");
			return;
		}
		if (chercherParNom(nom) != null) {
			JOptionPane.showMessageDialog(this, "ce nom existe deja");
			return;
		}

		Personnalite personnalite = (Personnalite) choixPersonnalite.getSelectedItem();
		Bavard bavard = new Bavard(nom, personnalite);
		reseau.ajouterBavard(bavard);
		modeleBavards.addElement(bavard);
		fenetres.put(bavard, new FenetreBavard(bavard));

		champNom.setText("");
		actualiserChoix();
		actualiserResume();
	}

	private void connecterBavards() {
		Bavard premier = (Bavard) choixLien1.getSelectedItem();
		Bavard deuxieme = (Bavard) choixLien2.getSelectedItem();
		if (premier == null || deuxieme == null) {
			return;
		}
		if (premier == deuxieme) {
			JOptionPane.showMessageDialog(this, "choisir 2 bavards differents");
			return;
		}

		reseau.connecter(premier, deuxieme);
		actualiserResume();
	}

	private void lancerPropagation() {
		Bavard emetteur = (Bavard) choixEmission.getSelectedItem();
		String message = champMessage.getText().trim();
		if (emetteur == null || message.isEmpty()) {
			return;
		}
		emetteur.envoyerMessage(message);
		champMessage.setText("");
	}

	private void ouvrirSelection() {
		Bavard bavard = listeBavards.getSelectedValue();
		if (bavard == null) {
			return;
		}
		ouvrirFenetre(bavard);
	}

	private void ouvrirToutesLesFenetres() {
		for (Bavard bavard : reseau.getBavards()) {
			ouvrirFenetre(bavard);
		}
	}

	private void ouvrirFenetre(Bavard bavard) {
		FenetreBavard fenetre = fenetres.get(bavard);
		if (fenetre == null) {
			fenetre = new FenetreBavard(bavard);
			fenetres.put(bavard, fenetre);
		}
		fenetre.setVisible(true);
	}

	private void actualiserChoix() {
		choixLien1.removeAllItems();
		choixLien2.removeAllItems();
		choixEmission.removeAllItems();
		for (Bavard bavard : reseau.getBavards()) {
			choixLien1.addItem(bavard);
			choixLien2.addItem(bavard);
			choixEmission.addItem(bavard);
		}
	}

	private void actualiserResume() {
		StringBuilder builder = new StringBuilder();
		for (Bavard bavard : reseau.getBavards()) {
			builder.append(bavard.getNom())
				.append(" (")
				.append(bavard.getPersonnalite())
				.append(") -> ");

			if (bavard.getAmis().isEmpty()) {
				builder.append("aucun ami");
			} else {
				for (int i = 0; i < bavard.getAmis().size(); i++) {
					if (i > 0) {
						builder.append(", ");
					}
					builder.append(bavard.getAmis().get(i).getNom());
				}
			}
			builder.append('\n');
		}
		zoneResume.setText(builder.toString());
	}

	private Bavard chercherParNom(String nom) {
		for (Bavard bavard : reseau.getBavards()) {
			if (bavard.getNom().equalsIgnoreCase(nom)) {
				return bavard;
			}
		}
		return null;
	}

	private void creerExemple() {
		if (!reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau n'est plus vide");
			return;
		}

		Bavard lina = new Bavard("Lina", Personnalite.POSITIF);
		Bavard yanis = new Bavard("Yanis", Personnalite.NEGATIF);
		Bavard nora = new Bavard("Nora", Personnalite.NEUTRE);
		Bavard sami = new Bavard("Sami", PersonnaliteForte.POSITIF_FORTE);

		ajouterExemple(lina);
		ajouterExemple(yanis);
		ajouterExemple(nora);
		ajouterExemple(sami);

		reseau.connecter(lina, yanis);
		reseau.connecter(yanis, nora);
		reseau.connecter(nora, sami);
		reseau.connecter(lina, sami);

		actualiserChoix();
		actualiserResume();
		ouvrirToutesLesFenetres();
	}

	private void ajouterExemple(Bavard bavard) {
		reseau.ajouterBavard(bavard);
		modeleBavards.addElement(bavard);
		fenetres.put(bavard, new FenetreBavard(bavard));
	}
}