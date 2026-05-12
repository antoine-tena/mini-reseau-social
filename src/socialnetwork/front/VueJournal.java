package socialnetwork.front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

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

import socialnetwork.back.AnalyseurSentiment;
import socialnetwork.back.JournalReseau;
import socialnetwork.back.MessageEvent;

/**
 * Panneau Swing affichant le journal chronologique des messages.
 *
 * Fonctionnalites :
 *   - affichage temps reel (ecoute JournalReseau.ajouterEcouteur)
 *   - recherche par mot cle dans le contenu
 *   - filtre par niveau de bienveillance
 *   - filtre par emotion (AnalyseurSentiment)
 *   - export CSV vers le presse-papier
 *   - effacement avec confirmation
 */
public class VueJournal extends JPanel {

	private final JournalReseau journal;

	private final JTextArea   zoneTexte;
	private final JTextField  champRecherche;
	private final JComboBox<String> filtreBienveillance;
	private final JComboBox<String> filtreEmotion;

	private static final String[] OPTIONS_BIENVEILLANCE = {
		"tout", "positif (b > 1)", "tres positif (b > 5)",
		"negatif (b < -1)", "tres negatif (b < -5)", "neutre [-1 ; 1]"
	};

	private static final String[] OPTIONS_EMOTION;
	static {
		AnalyseurSentiment.Emotion[] emotions = AnalyseurSentiment.Emotion.values();
		OPTIONS_EMOTION = new String[emotions.length + 1];
		OPTIONS_EMOTION[0] = "toutes emotions";
		for (int i = 0; i < emotions.length; i++) {
			OPTIONS_EMOTION[i + 1] = emotions[i].label();
		}
	}

	public VueJournal(JournalReseau journal) {
		this.journal = journal;
		this.zoneTexte = new JTextArea();
		this.zoneTexte.setEditable(false);
		this.zoneTexte.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 11));
		this.champRecherche     = new JTextField(16);
		this.filtreBienveillance = new JComboBox<>(OPTIONS_BIENVEILLANCE);
		this.filtreEmotion       = new JComboBox<>(OPTIONS_EMOTION);

		setLayout(new BorderLayout(4, 4));
		setBorder(BorderFactory.createTitledBorder("journal des messages"));

		construireInterface();

		// mise a jour automatique quand un nouveau message arrive
		journal.ajouterEcouteur(() -> SwingUtilities.invokeLater(this::actualiser));
	}

	// ─── Construction UI ──────────────────────────────────────────────────────

	private void construireInterface() {
		// barre de controles en haut
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

		JPanel panneauNord = new JPanel(new BorderLayout());
		panneauNord.add(barre, BorderLayout.CENTER);

		add(panneauNord, BorderLayout.NORTH);
		add(new JScrollPane(zoneTexte), BorderLayout.CENTER);

		// listener sur les filtres
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

	// ─── Logique de filtrage ──────────────────────────────────────────────────

	/** Rafraichit la zone de texte en appliquant les filtres actifs. */
	public void actualiser() {
		List<JournalReseau.EntreeJournal> entrees = getEntreesFiltrees();
		StringBuilder sb = new StringBuilder();

		int total = journal.getTaille();
		sb.append("--- ").append(entrees.size()).append(" entrees");
		if (entrees.size() != total) sb.append(" / ").append(total).append(" total");
		sb.append(" ---\n");
		if (!journal.resumeRapide().startsWith("journal vide")) {
			sb.append(journal.resumeRapide()).append('\n');
		}
		sb.append('\n');

		for (JournalReseau.EntreeJournal e : entrees) {
			sb.append(formatEntree(e)).append('\n');
		}

		zoneTexte.setText(sb.toString());
		zoneTexte.setCaretPosition(0);
	}

	/** Formate une entree avec indicateur de bienveillance visuel. */
	private String formatEntree(JournalReseau.EntreeJournal e) {
		MessageEvent m = e.getMessage();
		float b = m.getBienveillance();
		String indicateur = b >= 5f ? "+++" : b >= 1f ? "+  " : b >= -1f ? "~  " : b >= -5f ? "-  " : "---";
		String chemin = String.join("->", m.getChemin());
		return String.format("%s %s  [%s]  %s",
				indicateur, e, chemin, e.getSentiment().getEmotion().emoji());
	}

	private List<JournalReseau.EntreeJournal> getEntreesFiltrees() {
		// 1. filtre recherche texte
		String recherche = champRecherche.getText().trim();
		List<JournalReseau.EntreeJournal> base = journal.rechercherContenu(recherche);

		// 2. filtre bienveillance
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
			}).collect(java.util.stream.Collectors.toList());
		}

		// 3. filtre emotion
		int idxE = filtreEmotion.getSelectedIndex();
		if (idxE > 0) {
			AnalyseurSentiment.Emotion cible =
					AnalyseurSentiment.Emotion.values()[idxE - 1];
			base = base.stream()
					.filter(e -> e.getSentiment().getEmotion() == cible)
					.collect(java.util.stream.Collectors.toList());
		}

		return base;
	}

	// ─── Export ───────────────────────────────────────────────────────────────

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
