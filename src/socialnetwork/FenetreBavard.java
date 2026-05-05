package socialnetwork;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class FenetreBavard extends JFrame implements BavardObserver {
	private final Bavard bavard;
	private final DefaultListModel<MessageEvent> modeleMessages;
	private final DefaultListModel<String> modeleAmis;
	private final JList<MessageEvent> listeMessages;
	private final JList<String> listeAmis;
	private final JTextField champMessage;

	public FenetreBavard(Bavard bavard) {
		this.bavard = bavard;
		this.modeleMessages = new DefaultListModel<>();
		this.modeleAmis = new DefaultListModel<>();
		this.listeMessages = new JList<>(modeleMessages);
		this.listeAmis = new JList<>(modeleAmis);
		this.champMessage = new JTextField();

		setTitle("bavard - " + bavard.getNom());
		setSize(520, 420);
		setLocationByPlatform(true);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		construireInterface();
		chargerDonnees();
		bavard.ajouterObservateur(this);
	}

	private void construireInterface() {
		JPanel infos = new JPanel(new GridLayout(2, 1));
		infos.setBorder(BorderFactory.createTitledBorder("infos"));
		infos.add(new JLabel("nom : " + bavard.getNom()));
		infos.add(new JLabel("personnalite : " + bavard.getPersonnalite()));

		listeMessages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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

		setLayout(new BorderLayout(5, 5));
		add(infos, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		add(bas, BorderLayout.SOUTH);

		boutonEnvoyer.addActionListener(event -> envoyerMessage());
		boutonSuivre.addActionListener(event -> faireSuivreMessage());
	}

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

	private void envoyerMessage() {
		String contenu = champMessage.getText().trim();
		if (contenu.isEmpty()) {
			return;
		}
		bavard.envoyerMessage(contenu);
		champMessage.setText("");
	}

	private void faireSuivreMessage() {
		MessageEvent message = listeMessages.getSelectedValue();
		if (message == null) {
			return;
		}
		bavard.faireSuivreMessage(message);
	}

	@Override
	public void messageAjoute(Bavard bavard, MessageEvent event) {
		SwingUtilities.invokeLater(() -> modeleMessages.addElement(event));
	}

	@Override
	public void amisMisAJour(Bavard bavard) {
		SwingUtilities.invokeLater(this::actualiserAmis);
	}
}