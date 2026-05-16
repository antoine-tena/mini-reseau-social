package socialnetwork.front.fenetre;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import socialnetwork.back.analyse.AlgorithmeInfluence;
import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.personnalite.Personnalite;
import socialnetwork.back.personnalite.PersonnaliteAdaptative;
import socialnetwork.back.personnalite.PersonnaliteContextuelle;
import socialnetwork.back.personnalite.PersonnaliteForte;
import socialnetwork.back.stats.JournalReseau;
import socialnetwork.back.stats.StatistiquesReseau;
import socialnetwork.back.topologie.GenerateurTopologie;
import socialnetwork.front.vue.VueJournal;
import socialnetwork.front.vue.VueReseau;
import socialnetwork.front.vue.VueStatistiquesGraphique;

/**
 * Fenetre principale : creation des bavards et actions sur le reseau.
 */
@SuppressWarnings("serial")
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
	private final JTextField champProfondeur;
	private final JTextArea zoneResume;
	private final JTextArea zoneStats;
	private StatistiquesReseau stats;
	private VueReseau vueReseau;
	private final JournalReseau journal;
	private final VueStatistiquesGraphique vueStatsGraph;
	private final JTextArea zoneInfluence;
	private final JComboBox<GenerateurTopologie.Topologie> choixTopologie;
	private final JTextField champNbNoeuds;

	public FenetreReseau() {
		this.reseau           = new Reseau();
		this.fenetres         = new LinkedHashMap<>();
		this.modeleBavards    = new DefaultListModel<>();
		this.listeBavards     = new JList<>(modeleBavards);
		this.champNom         = new JTextField();
		this.choixPersonnalite= new JComboBox<>(creerChoixPersonnalites());
		this.choixLien1       = new JComboBox<>();
		this.choixLien2       = new JComboBox<>();
		this.choixEmission    = new JComboBox<>();
		this.champMessage     = new JTextField();
		this.champProfondeur  = new JTextField();
		this.zoneResume       = new JTextArea();
		this.zoneStats        = new JTextArea();
		this.zoneStats.setEditable(false);
		this.stats            = null;
		this.journal          = new JournalReseau(reseau);
		this.vueStatsGraph    = new VueStatistiquesGraphique();
		this.zoneInfluence    = new JTextArea();
		this.zoneInfluence.setEditable(false);
		this.zoneInfluence.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		this.choixTopologie   = new JComboBox<>(GenerateurTopologie.Topologie.values());
		this.champNbNoeuds    = new JTextField("8", 4);

		setTitle("reseau social emotionnel");
		setSize(1150, 680);
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
		JButton boutonOuvrir    = new JButton("ouvrir fenetre");
		liens.add(boutonConnecter);
		liens.add(boutonOuvrir);

		JPanel propagation = new JPanel(new GridLayout(5, 2, 5, 5));
		propagation.setBorder(BorderFactory.createTitledBorder("lancer une propagation"));
		propagation.add(new JLabel("emetteur"));
		propagation.add(choixEmission);
		propagation.add(new JLabel("message"));
		propagation.add(champMessage);
		propagation.add(new JLabel("profondeur max (-1 = infini)"));
		propagation.add(champProfondeur);
		JButton boutonPropager   = new JButton("propager");
		JButton boutonToutOuvrir = new JButton("ouvrir tout");
		JButton boutonSimuler    = new JButton("simuler...");
		propagation.add(boutonPropager);
		propagation.add(boutonToutOuvrir);
		propagation.add(boutonSimuler);
		propagation.add(new JLabel(""));

		JPanel topologie = new JPanel(new GridLayout(3, 2, 5, 5));
		topologie.setBorder(BorderFactory.createTitledBorder("topologie"));
		topologie.add(new JLabel("type"));
		topologie.add(choixTopologie);
		topologie.add(new JLabel("nb noeuds"));
		topologie.add(champNbNoeuds);
		JButton boutonGenerer = new JButton("generer reseau");
		topologie.add(boutonGenerer);
		topologie.add(new JLabel(""));

		JPanel gauche = new JPanel(new GridLayout(4, 1, 5, 5));
		gauche.add(creation);
		gauche.add(liens);
		gauche.add(propagation);
		gauche.add(topologie);

		JScrollPane scrollBavards = new JScrollPane(listeBavards);
		scrollBavards.setBorder(BorderFactory.createTitledBorder("bavards du reseau"));

		zoneResume.setEditable(false);
		JScrollPane scrollResume = new JScrollPane(zoneResume);
		scrollResume.setBorder(BorderFactory.createTitledBorder("resume du reseau"));

		vueReseau = new VueReseau(reseau);

		zoneStats.setEditable(false);
		JScrollPane scrollStats = new JScrollPane(zoneStats);
		scrollStats.setBorder(BorderFactory.createTitledBorder("statistiques"));

		JButton boutonStats      = new JButton("actualiser stats");
		JButton boutonResetStats = new JButton("reinitialiser stats");
		JPanel boutonsStats = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2));
		boutonsStats.add(boutonStats);
		boutonsStats.add(boutonResetStats);
		JPanel panneauStatsContenu = new JPanel(new BorderLayout(5, 5));
		panneauStatsContenu.add(boutonsStats, BorderLayout.NORTH);
		panneauStatsContenu.add(scrollStats, BorderLayout.CENTER);

		JScrollPane scrollInfluence = new JScrollPane(zoneInfluence);
		scrollInfluence.setBorder(BorderFactory.createTitledBorder("rapport"));
		JButton boutonInfluence = new JButton("calculer influence");
		JPanel panneauInfluence = new JPanel(new BorderLayout(5, 5));
		panneauInfluence.add(boutonInfluence, BorderLayout.NORTH);
		panneauInfluence.add(scrollInfluence, BorderLayout.CENTER);

		JTabbedPane onglets = new JTabbedPane();
		onglets.addTab("reseau",      vueReseau);
		onglets.addTab("statistiques", panneauStatsContenu);
		onglets.addTab("stats graph",  vueStatsGraph);
		onglets.addTab("journal",      new VueJournal(journal));
		onglets.addTab("influence",    panneauInfluence);

		JSplitPane droite    = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollBavards, scrollResume);
		droite.setResizeWeight(0.35);
		JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gauche, droite);
		principal.setResizeWeight(0.40);
		JSplitPane tout      = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, principal, onglets);
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
		boutonSimuler.addActionListener(event -> ouvrirSimulation());
		boutonGenerer.addActionListener(event -> genererTopologie());
		boutonInfluence.addActionListener(event -> calculerInfluence());
	}

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

	private void ajouterBavard() {
		String nom = champNom.getText().trim();
		if (nom.isEmpty()) { JOptionPane.showMessageDialog(this, "il faut un nom"); return; }
		if (chercherParNom(nom) != null) { JOptionPane.showMessageDialog(this, "ce nom existe deja"); return; }

		Personnalite personnalite = (Personnalite) choixPersonnalite.getSelectedItem();
		Bavard bavard = new Bavard(nom, personnalite);
		reseau.ajouterBavard(bavard);
		modeleBavards.addElement(bavard);
		fenetres.put(bavard, new FenetreBavard(bavard));
		if (stats != null) stats.observerBavard(bavard);
		vueReseau.observerBavard(bavard);
		journal.observerBavard(bavard);
		champNom.setText("");
		actualiserChoix();
		actualiserResume();
	}

	private void connecterBavards() {
		Bavard premier   = (Bavard) choixLien1.getSelectedItem();
		Bavard deuxieme  = (Bavard) choixLien2.getSelectedItem();
		if (premier == null || deuxieme == null) return;
		if (premier == deuxieme) { JOptionPane.showMessageDialog(this, "choisir 2 bavards differents"); return; }
		reseau.connecter(premier, deuxieme);
		vueReseau.repaint();
		actualiserResume();
		actualiserStats();
	}

	private void lancerPropagation() {
		Bavard emetteur = (Bavard) choixEmission.getSelectedItem();
		String message  = champMessage.getText().trim();
		if (emetteur == null || message.isEmpty()) return;

		String profStr = champProfondeur.getText().trim();
		int profondeur;
		try {
			profondeur = profStr.isEmpty() ? MessageEvent.PROFONDEUR_INFINIE : Integer.parseInt(profStr);
		} catch (NumberFormatException ex) {
			profondeur = MessageEvent.PROFONDEUR_INFINIE;
		}
		emetteur.setProfondeurEmission(profondeur);
		if (stats == null) stats = new StatistiquesReseau(reseau);
		emetteur.envoyerMessage(message);
		champMessage.setText("");
		actualiserStats();
	}

	private void actualiserStats() {
		if (stats == null) { zoneStats.setText("pas encore de propagation lancee"); return; }
		zoneStats.setText(stats.resume());
		vueStatsGraph.mettreAJour(stats, reseau);
	}

	private void ouvrirSelection() {
		Bavard bavard = listeBavards.getSelectedValue();
		if (bavard != null) ouvrirFenetre(bavard);
	}

	private void ouvrirToutesLesFenetres() {
		for (Bavard bavard : reseau.getBavards()) ouvrirFenetre(bavard);
	}

	private void ouvrirFenetre(Bavard bavard) {
		FenetreBavard fenetre = fenetres.computeIfAbsent(bavard, FenetreBavard::new);
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
			builder.append(bavard.getNom()).append(" (").append(bavard.getPersonnalite()).append(") -> ");
			if (bavard.getAmis().isEmpty()) {
				builder.append("aucun ami");
			} else {
				for (int i = 0; i < bavard.getAmis().size(); i++) {
					if (i > 0) builder.append(", ");
					builder.append(bavard.getAmis().get(i).getNom());
				}
			}
			builder.append('\n');
		}
		zoneResume.setText(builder.toString());
	}

	private Bavard chercherParNom(String nom) {
		for (Bavard bavard : reseau.getBavards()) {
			if (bavard.getNom().equalsIgnoreCase(nom)) return bavard;
		}
		return null;
	}

	private void creerExemple() {
		if (!reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau n'est plus vide");
			return;
		}
		Bavard lina  = new Bavard("Lina",  Personnalite.POSITIF);
		Bavard yanis = new Bavard("Yanis", Personnalite.NEGATIF);
		Bavard nora  = new Bavard("Nora",  Personnalite.NEUTRE);
		Bavard sami  = new Bavard("Sami",  PersonnaliteForte.POSITIF_FORTE);
		Bavard cam   = new Bavard("Cam",   new PersonnaliteAdaptative("ADAPTATIF+", 0.5f, 0.15f, 3f));
		Bavard leo   = new Bavard("Leo",   Personnalite.NEUTRE);
		leo.setSeuilTolerance(0f);
		Bavard jade  = new Bavard("Jade",  Personnalite.POSITIF);
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

		stats = new StatistiquesReseau(reseau);
		actualiserChoix();
		actualiserResume();
		ouvrirToutesLesFenetres();
	}

	private void ajouterExemple(Bavard bavard) {
		reseau.ajouterBavard(bavard);
		modeleBavards.addElement(bavard);
		fenetres.put(bavard, new FenetreBavard(bavard));
		vueReseau.observerBavard(bavard);
		journal.observerBavard(bavard);
	}

	private void reinitialiserStats() {
		if (stats != null) { stats.reset(); actualiserStats(); }
	}

	private void ouvrirSimulation() {
		if (reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau est vide");
			return;
		}
		new FenetreSimulation(reseau).setVisible(true);
	}

	private void genererTopologie() {
		if (!reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau n'est pas vide");
			return;
		}
		GenerateurTopologie.Topologie topo =
				(GenerateurTopologie.Topologie) choixTopologie.getSelectedItem();
		int n;
		try {
			n = Integer.parseInt(champNbNoeuds.getText().trim());
			if (n < 2 || n > 60) throw new NumberFormatException();
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "nombre de noeuds invalide (2..60)");
			return;
		}
		Personnalite[] persos = {
			Personnalite.POSITIF, Personnalite.NEGATIF, Personnalite.NEUTRE,
			PersonnaliteForte.POSITIF_FORTE, PersonnaliteForte.NEGATIF_FORTE
		};
		GenerateurTopologie.generer(reseau, topo, n, persos);
		stats = new StatistiquesReseau(reseau);
		for (Bavard b : reseau.getBavards()) {
			journal.observerBavard(b);
			vueReseau.observerBavard(b);
			modeleBavards.addElement(b);
			fenetres.put(b, new FenetreBavard(b));
		}
		actualiserChoix();
		actualiserResume();
		JOptionPane.showMessageDialog(this,
			"Reseau " + topo + " cree !\n" + GenerateurTopologie.description(reseau));
	}

	private void calculerInfluence() {
		if (reseau.getBavards().isEmpty()) { zoneInfluence.setText("reseau vide"); return; }
		zoneInfluence.setText("calcul en cours...");
		new Thread(() -> {
			String rapport = AlgorithmeInfluence.rapport(reseau);
			SwingUtilities.invokeLater(() -> zoneInfluence.setText(rapport));
		}, "influence-thread").start();
	}
}
