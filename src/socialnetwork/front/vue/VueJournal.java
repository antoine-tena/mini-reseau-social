package socialnetwork.front.vue;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import socialnetwork.back.analyse.Emotion;
import socialnetwork.back.model.MessageEvent;
import socialnetwork.back.stats.EntreeJournal;
import socialnetwork.back.stats.JournalReseau;

/**
 * Panneau Swing affichant le journal chronologique des messages.
 *
 * Fonctionnalites :
 *   - affichage temps reel (ecoute JournalReseau.ajouterEcouteur)
 *   - recherche par mot cle dans le contenu
 *   - filtre par niveau de bienveillance
 *   - filtre par emotion
 *   - export CSV vers le presse-papier
 *   - effacement avec confirmation
 */
@SuppressWarnings("serial")
public class VueJournal extends JPanel {

	private final JournalReseau journal;

	private final JTextArea             zoneTexte;
	private final JTextField            champRecherche;
	private final JComboBox<String>     filtreBienveillance;
	private final JComboBox<String>     filtreEmotion;

	private static final String[] OPTIONS_BIENVEILLANCE = {
		"tout", "positif (b > 1)", "tres positif (b > 5)",
		"negatif (b < -1)", "tres negatif (b < -5)", "neutre [-1 ; 1]"
	};

	private static final String[] OPTIONS_EMOTION;
	static {
		Emotion[] emotions = Emotion.values();
		OPTIONS_EMOTION = new String[emotions.length + 1];
		OPTIONS_EMOTION[0] = "toutes emotions";
		for (int i = 0; i < emotions.length; i++) {
			OPTIONS_EMOTION[i + 1] = emotions[i].label();
		}
	}

	public VueJournal(JournalReseau journal) {
		this.journal             = journal;
		this.zoneTexte           = new JTextArea();
		this.zoneTexte.setEditable(false);
		this.zoneTexte.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 11));
		this.champRecherche      = new JTextField(16);
		this.filtreBienveillance = new JComboBox<>(OPTIONS_BIENVEILLANCE);
		this.filtreEmotion       = new JComboBox<>(OPTIONS_EMOTION);

		setLayout(new BorderLayout(4, 4));
		setBorder(BorderFactory.createTitledBorder("journal des messages"));

		construireInterface();
		journal.ajouterEcouteur(() -> SwingUtilities.invokeLater(this::actualiser));
	}

	private void construireInterface() {
		JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		gauche.add(new JLabel("filtrer b:"));
		gauche.add(filtreBienveillance);
		gauche.add(new JLabel("emotion:"));
		gauche.add(filtreEmotion);
		gauche.add(new JLabel("recherche:"));
		gauche.add(champRecherche);

		JButton boutonActualiser = new JButton("actualiser");
		JButton boutonEffacer    = new JButton("effacer");
		JButton boutonCsv        = new JButton("CSV");
		JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
		droite.add(boutonActualiser);
		droite.add(boutonEffacer);
		droite.add(boutonCsv);

		JPanel barre = new JPanel(new BorderLayout());
		barre.add(gauche, BorderLayout.CENTER);
		barre.add(droite, BorderLayout.EAST);

		add(barre, BorderLayout.NORTH);
		add(new JScrollPane(zoneTexte), BorderLayout.CENTER);

		filtreBienveillance.addActionListener(e -> actualiser());
		filtreEmotion.addActionListener(e -> actualiser());
		champRecherche.addActionListener(e -> actualiser());
		boutonActualiser.addActionListener(e -> actualiser());

		boutonEffacer.addActionListener(e -> {
			int rep = JOptionPane.showConfirmDialog(this,
					"Effacer le journal ?", "confirmation", JOptionPane.YES_NO_OPTION);
			if (rep == JOptionPane.YES_OPTION) {
				journal.effacer();
				zoneTexte.setText("(journal vide)");
			}
		});

		boutonCsv.addActionListener(e -> exporterCsv());
	}

	public void actualiser() {
		List<EntreeJournal> entrees = getEntreesFiltrees();
		StringBuilder sb = new StringBuilder();

		int total = journal.getTaille();
		sb.append("--- ").append(entrees.size()).append(" entrees");
		if (entrees.size() != total) sb.append(" / ").append(total).append(" total");
		sb.append(" ---\n");
		if (!journal.resumeRapide().startsWith("journal vide")) {
			sb.append(journal.resumeRapide()).append('\n');
		}
		sb.append('\n');

		for (EntreeJournal e : entrees) sb.append(formatEntree(e)).append('\n');

		zoneTexte.setText(sb.toString());
		zoneTexte.setCaretPosition(0);
	}

	private String formatEntree(EntreeJournal e) {
		MessageEvent m = e.getMessage();
		float b = m.getBienveillance();
		String indicateur = b >= 5f ? "+++" : b >= 1f ? "+  " : b >= -1f ? "~  " : b >= -5f ? "-  " : "---";
		String chemin = String.join("->", m.getChemin());
		return String.format("%s %s  [%s]  %s",
				indicateur, e, chemin, e.getSentiment().getEmotion().emoji());
	}

	private List<EntreeJournal> getEntreesFiltrees() {
		String recherche = champRecherche.getText().trim();
		List<EntreeJournal> base = journal.rechercherContenu(recherche);

		int idxB = filtreBienveillance.getSelectedIndex();
		if (idxB > 0) {
			base = base.stream().filter(e -> {
				float b = e.getMessage().getBienveillance();
				return switch (idxB) {
					case 1 -> b > 1f;
					case 2 -> b > 5f;
					case 3 -> b < -1f;
					case 4 -> b < -5f;
					case 5 -> b >= -1f && b <= 1f;
					default -> true;
				};
			}).collect(Collectors.toList());
		}

		int idxE = filtreEmotion.getSelectedIndex();
		if (idxE > 0) {
			Emotion cible = Emotion.values()[idxE - 1];
			base = base.stream()
					.filter(e -> e.getSentiment().getEmotion() == cible)
					.collect(Collectors.toList());
		}

		return base;
	}

	private void exporterCsv() {
		String csv = journal.exporterCsv();
		java.awt.datatransfer.StringSelection sel =
				new java.awt.datatransfer.StringSelection(csv);
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
		JOptionPane.showMessageDialog(this,
				"Journal CSV copie dans le presse-papier\n("
				+ journal.getTaille() + " entrees)");
	}
}
