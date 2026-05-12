package socialnetwork.front;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import socialnetwork.back.Bavard;
import socialnetwork.back.MessageEvent;
import socialnetwork.back.Personnalite;
import socialnetwork.back.PersonnaliteAdaptative;
import socialnetwork.back.PersonnaliteContextuelle;
import socialnetwork.back.PersonnaliteForte;
import socialnetwork.back.Reseau;
import socialnetwork.back.StatistiquesReseau;

/**
 * Fenetre principale: creation des bavards et actions sur le reseau.
 */
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
	// phase 4 : profondeur de propagation (vide = infini)
	private final JTextField champProfondeur;
	private final JTextArea zoneResume;
	// phase 4 : panneau statistiques
	private final JTextArea zoneStats;
	private StatistiquesReseau stats;
	// visualisation graphique du reseau
	private VueReseau vueReseau;

	/**
	 * Initialise le reseau et construit l'interface.
	 */
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
		this.champProfondeur = new JTextField();
		this.zoneResume = new JTextArea();
		this.zoneStats = new JTextArea();
		this.zoneStats.setEditable(false);
		this.stats = null;

		setTitle("reseau social emotionnel - phase 4");
		setSize(1000, 620);
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		construireInterface();
		actualiserChoix();
		actualiserResume();
	}

	// Construit la mise en page et les actions UI.
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

		JPanel propagation = new JPanel(new GridLayout(4, 2, 5, 5));
		propagation.setBorder(BorderFactory.createTitledBorder("lancer une propagation"));
		propagation.add(new JLabel("emetteur"));
		propagation.add(choixEmission);
		propagation.add(new JLabel("message"));
		propagation.add(champMessage);
		propagation.add(new JLabel("profondeur max (-1 = infini)"));
		propagation.add(champProfondeur);
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

		// visualisation graphique du reseau (bonus)
		vueReseau = new VueReseau(reseau);

		// onglets droite : reseau graphique + statistiques
		zoneStats.setEditable(false);
		JScrollPane scrollStats = new JScrollPane(zoneStats);
		scrollStats.setBorder(BorderFactory.createTitledBorder("statistiques"));

		JButton boutonStats = new JButton("actualiser stats");
		JButton boutonResetStats = new JButton("reinitialiser stats");
		JPanel boutonsStats = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2));
		boutonsStats.add(boutonStats);
		boutonsStats.add(boutonResetStats);
		JPanel panneauStatsContenu = new JPanel(new BorderLayout(5, 5));
		panneauStatsContenu.add(boutonsStats, BorderLayout.NORTH);
		panneauStatsContenu.add(scrollStats, BorderLayout.CENTER);

		JTabbedPane onglets = new JTabbedPane();
		onglets.addTab("reseau", vueReseau);
		onglets.addTab("statistiques", panneauStatsContenu);

		JSplitPane droite = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollBavards, scrollResume);
		droite.setResizeWeight(0.35);

		JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gauche, droite);
		principal.setResizeWeight(0.40);

		JSplitPane tout = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, principal, onglets);
		tout.setResizeWeight(0.70);

		setLayout(new BorderLayout(5, 5));
		add(tout, BorderLayout.CENTER);

		boutonAjouter.addActionListener(event -> ajouterBavard());
		boutonExemple.addActionListener(event -> creerExemple());
		boutonConnecter.addActionListener(event -> connecterBavards());
		boutonOuvrir.addActionListener(event -> ouvrirSelection());
		boutonPropager.addActionListener(event -> lancerPropagation());
		boutonToutOuvrir.addActionListener(event -> ouvrirToutesLesFenetres());
		boutonStats.addActionListener(event -> actualiserStats());
		boutonResetStats.addActionListener(event -> reinitialiserStats());
	}

	// Liste des personnalites disponibles.
	private Personnalite[] creerChoixPersonnalites() {
		return new Personnalite[] {
			Personnalite.POSITIF,
			Personnalite.NEGATIF,
			Personnalite.NEUTRE,
			PersonnaliteForte.POSITIF_FORTE,
			PersonnaliteForte.NEGATIF_FORTE,
			PersonnaliteForte.NEUTRE_FORTE,
			new PersonnaliteAdaptative("ADAPTATIF+", 0.5f, 0.1f, 3f),
			new PersonnaliteAdaptative("ADAPTATIF-", -0.5f, 0.1f, 3f),
			PersonnaliteContextuelle.SENSIBLE_POSITIF,
			PersonnaliteContextuelle.SENSIBLE_NEGATIF
		};
	}

	// Ajoute un nouveau bavard au reseau.
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
		// phase 4 : inscrire dans les stats si elles existent
		if (stats != null) {
			stats.observerBavard(bavard);
		}
		vueReseau.observerBavard(bavard);

		champNom.setText("");
		actualiserChoix();
		actualiserResume();
	}

	// Lie deux bavards selectionnes.
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
		vueReseau.repaint();
		actualiserResume();
		actualiserStats();
	}

	// Envoi un message depuis l'emetteur selectionne.
	// Phase 4 : lit la profondeur et configure l'emetteur avant l'envoi.
	private void lancerPropagation() {
		Bavard emetteur = (Bavard) choixEmission.getSelectedItem();
		String message = champMessage.getText().trim();
		if (emetteur == null || message.isEmpty()) {
			return;
		}
		// lecture de la profondeur (-1 si vide ou invalide)
		String profStr = champProfondeur.getText().trim();
		int profondeur;
		try {
			profondeur = profStr.isEmpty() ? MessageEvent.PROFONDEUR_INFINIE : Integer.parseInt(profStr);
		} catch (NumberFormatException ex) {
			profondeur = MessageEvent.PROFONDEUR_INFINIE;
		}
		emetteur.setProfondeurEmission(profondeur);
		// initialiser les stats au premier envoi
		if (stats == null) {
			stats = new StatistiquesReseau(reseau);
		}
		emetteur.envoyerMessage(message);
		champMessage.setText("");
		actualiserStats();
	}

	// Phase 4 : met a jour la zone de statistiques.
	private void actualiserStats() {
		if (stats == null) {
			zoneStats.setText("pas encore de propagation lancee");
			return;
		}
		zoneStats.setText(stats.resume());
	}

	// Ouvre la fenetre du bavard selectionne.
	private void ouvrirSelection() {
		Bavard bavard = listeBavards.getSelectedValue();
		if (bavard == null) {
			return;
		}
		ouvrirFenetre(bavard);
	}

	// Ouvre toutes les fenetres de bavards.
	private void ouvrirToutesLesFenetres() {
		for (Bavard bavard : reseau.getBavards()) {
			ouvrirFenetre(bavard);
		}
	}

	// Recupere ou cree la fenetre associee.
	private void ouvrirFenetre(Bavard bavard) {
		FenetreBavard fenetre = fenetres.get(bavard);
		if (fenetre == null) {
			fenetre = new FenetreBavard(bavard);
			fenetres.put(bavard, fenetre);
		}
		fenetre.setVisible(true);
	}

	// Recharge les listes deroulantes.
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

	// Met a jour le texte de resume du reseau.
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

	// Recherche un bavard par son nom.
	private Bavard chercherParNom(String nom) {
		for (Bavard bavard : reseau.getBavards()) {
			if (bavard.getNom().equalsIgnoreCase(nom)) {
				return bavard;
			}
		}
		return null;
	}

	// Cree un petit reseau pre-rempli. Phase 4 : ajout de bavards avec persos avancees.
	private void creerExemple() {
		if (!reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau n'est plus vide");
			return;
		}

		Bavard lina = new Bavard("Lina", Personnalite.POSITIF);
		Bavard yanis = new Bavard("Yanis", Personnalite.NEGATIF);
		Bavard nora = new Bavard("Nora", Personnalite.NEUTRE);
		Bavard sami = new Bavard("Sami", PersonnaliteForte.POSITIF_FORTE);
		// phase 4 : bavard avec personnalite adaptative
		Bavard cam = new Bavard("Cam", new PersonnaliteAdaptative("ADAPTATIF+", 0.5f, 0.15f, 3f));
		// phase 4 : bavard avec seuil de tolerance (refuse les messages negatifs)
		Bavard leo = new Bavard("Leo", Personnalite.NEUTRE);
		leo.setSeuilTolerance(0f);
		// phase 4 : bavard avec propagation probabiliste 70%
		Bavard jade = new Bavard("Jade", Personnalite.POSITIF);
		jade.setProbabiliteRetransmission(0.7);

		ajouterExemple(lina);
		ajouterExemple(yanis);
		ajouterExemple(nora);
		ajouterExemple(sami);
		ajouterExemple(cam);
		ajouterExemple(leo);
		ajouterExemple(jade);

		reseau.connecter(lina, yanis);
		reseau.connecter(yanis, nora);
		reseau.connecter(nora, sami);
		reseau.connecter(lina, sami);
		reseau.connecter(sami, cam);
		reseau.connecter(cam, leo);
		reseau.connecter(leo, jade);
		reseau.connecter(jade, lina);

		// phase 4 : initialiser les statistiques sur cet exemple
		stats = new StatistiquesReseau(reseau);

		actualiserChoix();
		actualiserResume();
		ouvrirToutesLesFenetres();
	}

	// Ajoute un bavard d'exemple au reseau et a l'UI.
	private void ajouterExemple(Bavard bavard) {
		reseau.ajouterBavard(bavard);
		modeleBavards.addElement(bavard);
		fenetres.put(bavard, new FenetreBavard(bavard));
		vueReseau.observerBavard(bavard);
	}

	// Reinitialise les statistiques.
	private void reinitialiserStats() {
		if (stats != null) {
			stats.reset();
			actualiserStats();
		}
	}
}