package socialnetwork.front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import socialnetwork.back.Bavard;
import socialnetwork.back.BavardObserver;
import socialnetwork.back.MessageEvent;

/**
 * Fenetre d'un bavard: affiche messages/amis et emet des actions.
 */
public class FenetreBavard extends JFrame implements BavardObserver {
	private final Bavard bavard;
	private final DefaultListModel<MessageEvent> modeleMessages;
	private final DefaultListModel<String> modeleAmis;
	private final JList<MessageEvent> listeMessages;
	private final JList<String> listeAmis;
	private final JTextField champMessage;
	// phase 4 : affichage du chemin du message selectionne
	private final JTextArea zoneChemin;
	// configuration bavard en direct
	private final JTextField champSeuil;
	private final JTextField champProba;
	private final JLabel labelCompteur;

	/**
	 * Cree la fenetre et s'inscrit comme observer du bavard.
	 */
	public FenetreBavard(Bavard bavard) {
		this.bavard = bavard;
		this.modeleMessages = new DefaultListModel<>();
		this.modeleAmis = new DefaultListModel<>();
		this.listeMessages = new JList<>(modeleMessages);
		this.listeAmis = new JList<>(modeleAmis);
		this.champMessage = new JTextField();
		this.zoneChemin = new JTextArea(2, 0);
		this.zoneChemin.setEditable(false);
		this.zoneChemin.setLineWrap(true);
		this.champSeuil = new JTextField(String.valueOf((int)bavard.getSeuilTolerance()), 4);
		this.champProba = new JTextField(String.format("%.0f", bavard.getProbabiliteRetransmission() * 100), 4);
		this.labelCompteur = new JLabel("tx: 0");

		setTitle("bavard - " + bavard.getNom());
		setSize(580, 540);
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		construireInterface();
		chargerDonnees();
		bavard.ajouterObservateur(this);
	}

	// Construit les composants Swing.
	private void construireInterface() {
		JPanel infos = new JPanel(new GridLayout(2, 1));
		infos.setBorder(BorderFactory.createTitledBorder("infos"));
		infos.add(new JLabel("nom : " + bavard.getNom()));
		infos.add(new JLabel("personnalite : " + bavard.getPersonnalite()));

		listeMessages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// phase 4 : clic sur un message -> affiche le chemin en bas
		listeMessages.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				afficherChemin(listeMessages.getSelectedValue());
			}
		});
		// couleurs selon bienveillance du message
		listeMessages.setCellRenderer(new MessageRenderer());

		JScrollPane scrollMessages = new JScrollPane(listeMessages);
		scrollMessages.setBorder(BorderFactory.createTitledBorder("messages recus"));

		JScrollPane scrollAmis = new JScrollPane(listeAmis);
		scrollAmis.setBorder(BorderFactory.createTitledBorder("amis"));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollMessages, scrollAmis);
		split.setResizeWeight(0.72);

		JButton boutonEnvoyer = new JButton("envoyer");
		JButton boutonSuivre = new JButton("faire suivre");

		JPanel boutons = new JPanel(new GridLayout(1, 2, 5, 5));
		boutons.add(boutonEnvoyer);
		boutons.add(boutonSuivre);

		JPanel bas = new JPanel(new BorderLayout(5, 5));
		bas.setBorder(BorderFactory.createTitledBorder("actions"));
		bas.add(champMessage, BorderLayout.CENTER);
		bas.add(boutons, BorderLayout.SOUTH);

		// phase 4 : zone chemin en bas de la fenetre
		JScrollPane scrollChemin = new JScrollPane(zoneChemin);
		scrollChemin.setBorder(BorderFactory.createTitledBorder("chemin du message selectionne"));

		// configuration bavard (seuil / probabilite) en direct
		JPanel config = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		config.setBorder(BorderFactory.createTitledBorder("configuration"));
		config.add(new JLabel("seuil [-10..10]:"));
		config.add(champSeuil);
		config.add(new JLabel("proba %:"));
		config.add(champProba);
		config.add(labelCompteur);
		JButton boutonConfig = new JButton("appliquer");
		config.add(boutonConfig);
		boutonConfig.addActionListener(e -> appliquerConfig());

		JPanel panneauBas = new JPanel(new BorderLayout(5, 5));
		panneauBas.add(scrollChemin, BorderLayout.NORTH);
		panneauBas.add(config, BorderLayout.CENTER);
		panneauBas.add(bas, BorderLayout.SOUTH);

		setLayout(new BorderLayout(5, 5));
		add(infos, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		add(panneauBas, BorderLayout.SOUTH);

		boutonEnvoyer.addActionListener(event -> envoyerMessage());
		boutonSuivre.addActionListener(event -> faireSuivreMessage());
	}

	// Charge l'etat initial (messages + amis).
	private void chargerDonnees() {
		modeleMessages.clear();
		for (MessageEvent message : bavard.getMessagesRecus()) {
			modeleMessages.addElement(message);
		}
		actualiserAmis();
	}

	private void actualiserAmis() {
		modeleAmis.clear();
		for (Bavard ami : bavard.getAmis()) {
			modeleAmis.addElement(ami.getNom());
		}
	}

	// Envoi direct depuis le champ texte.
	private void envoyerMessage() {
		String contenu = champMessage.getText().trim();
		if (contenu.isEmpty()) {
			return;
		}
		bavard.envoyerMessage(contenu);
	}

	// Relance un message selectionne.
	private void faireSuivreMessage() {
		MessageEvent message = listeMessages.getSelectedValue();
		if (message == null) {
			return;
		}
		bavard.faireSuivreMessage(message);
	}

	// Phase 4 : met a jour la zone chemin selon la selection.
	private void afficherChemin(MessageEvent event) {
		if (event == null) {
			zoneChemin.setText("");
			return;
		}
		String chemin = String.join(" -> ", event.getChemin());
		zoneChemin.setText(chemin
				+ "  |  hop=" + event.getHopActuel()
				+ "  tx=" + event.getTransmissions()
				+ "  b=" + String.format("%.1f", event.getBienveillance()));
	}

	@Override
	// Callback observe: nouveau message.
	public void messageAjoute(Bavard bavard, MessageEvent event) {
		SwingUtilities.invokeLater(() -> {
			modeleMessages.addElement(event);
			actualiserCompteur();
		});
	}

	@Override
	// Callback observe: liste d'amis modifiee.
	public void amisMisAJour(Bavard bavard) {
		SwingUtilities.invokeLater(this::actualiserAmis);
	}

	// Lit seuil et proba depuis les champs et les applique au bavard.
	private void appliquerConfig() {
		try {
			float seuil = Float.parseFloat(champSeuil.getText().trim());
			bavard.setSeuilTolerance(seuil);
		} catch (NumberFormatException ex) {
			// saisie invalide, on ignore
		}
		try {
			double proba = Double.parseDouble(champProba.getText().trim()) / 100.0;
			bavard.setProbabiliteRetransmission(proba);
		} catch (NumberFormatException ex) {
			// saisie invalide, on ignore
		}
	}

	// Met a jour le compteur de transmissions dans le label.
	private void actualiserCompteur() {
		labelCompteur.setText("tx: " + bavard.getCompteurTransmissions());
	}

	// Renderer qui colorie les messages selon la bienveillance.
	private static final class MessageRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof MessageEvent && !isSelected) {
				float b = ((MessageEvent) value).getBienveillance();
				if (b >= 5f)       setBackground(new Color(100, 210, 100));
				else if (b >= 1f)  setBackground(new Color(200, 240, 200));
				else if (b >= -1f) setBackground(Color.WHITE);
				else if (b >= -5f) setBackground(new Color(255, 230, 150));
				else               setBackground(new Color(255, 180, 180));
			}
			return this;
		}
	}
}