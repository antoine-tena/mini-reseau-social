package socialnetwork.front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import socialnetwork.back.Bavard;
import socialnetwork.back.JournalReseau;
import socialnetwork.back.Reseau;
import socialnetwork.back.StatistiquesReseau;

/**
 * Fenetre de simulation automatique de propagations en mode batch.
 *
 * Parametres configurables :
 *   - emetteur : aleatoire ou fixe
 *   - message  : texte libre, aleatoire dans une liste ou detecte par sentiment
 *   - nombre de cycles
 *   - delai entre cycles (ms)
 *
 * Affichage en temps reel :
 *   - barre de progression
 *   - journal des evenements (via JournalReseau local)
 *   - resume des statistiques a la fin
 *
 * La simulation est non-bloquante : javax.swing.Timer sur l'EDT.
 */
public class FenetreSimulation extends javax.swing.JFrame {

	// ─── Messages predefinies pour la simulation ──────────────────────────────

	private static final String[] MESSAGES_POSITIFS = {
		"super nouvelle pour tout le monde",
		"j'ai reussi mon examen bravo",
		"belle journee pleine de soleil",
		"merci a tous pour votre soutien",
		"victoire de notre equipe",
		"je suis tres content content",
		"magnifique coucher de soleil",
		"bonne nouvelle pour la sante"
	};

	private static final String[] MESSAGES_NEGATIFS = {
		"mauvaise journee terrible",
		"j'ai echoue a l'examen triste",
		"colere contre cette situation injuste",
		"honte a ceux qui font ca",
		"catastrophe dans le quartier",
		"je suis tres fatigue et deprime",
		"scandale inacceptable",
		"rien ne va plus desespoir"
	};

	private static final String[] MESSAGES_NEUTRES = {
		"bonjour tout le monde",
		"nouvelle information du jour",
		"reunion demain a 14h",
		"rappel de la semaine derniere",
		"message pour information",
		"question pour le groupe"
	};

	private static final String[] TOUS_MESSAGES;
	static {
		TOUS_MESSAGES = new String[
			MESSAGES_POSITIFS.length + MESSAGES_NEGATIFS.length + MESSAGES_NEUTRES.length];
		int idx = 0;
		for (String s : MESSAGES_POSITIFS) TOUS_MESSAGES[idx++] = s;
		for (String s : MESSAGES_NEGATIFS)  TOUS_MESSAGES[idx++] = s;
		for (String s : MESSAGES_NEUTRES)   TOUS_MESSAGES[idx++] = s;
	}

	// ─── Champs ───────────────────────────────────────────────────────────────

	private final Reseau              reseau;
	private final StatistiquesReseau  statsLocales;
	private final JournalReseau       journalLocal;
	private final Random              rng = new Random();

	// controls
	private final JComboBox<Bavard>   choixEmetteur;
	private final JCheckBox           cbAleatoire;
	private final JCheckBox           cbMessageAleatoire;
	private final JCheckBox           cbSentimentAuto;
	private final JSpinner            spinnerCycles;
	private final JSpinner            spinnerDelai;
	private final JTextField          champMessage;

	// affichage
	private final JTextArea           zoneLog;
	private final JProgressBar        progressBar;
	private final JLabel              labelCycle;
	private final JLabel              labelBMoyenne;
	private final JButton             boutonDemarrer;
	private final JButton             boutonArreter;
	private final JButton             boutonReset;

	// etat simulation
	private javax.swing.Timer timer;
	private int cyclesCourants;
	private int cyclesMax;

	// ─── Constructeur ─────────────────────────────────────────────────────────

	public FenetreSimulation(Reseau reseau) {
		this.reseau       = reseau;
		this.statsLocales = new StatistiquesReseau(reseau);
		this.journalLocal = new JournalReseau(reseau);

		// combo bavards
		this.choixEmetteur = new JComboBox<>();
		for (Bavard b : reseau.getBavards()) choixEmetteur.addItem(b);

		this.cbAleatoire         = new JCheckBox("emetteur aleatoire", true);
		this.cbMessageAleatoire  = new JCheckBox("message aleatoire", true);
		this.cbSentimentAuto     = new JCheckBox("sentiment aleatoire (mix positif/negatif)", false);
		this.spinnerCycles       = new JSpinner(new SpinnerNumberModel(20, 1, 2000, 1));
		this.spinnerDelai        = new JSpinner(new SpinnerNumberModel(400, 50, 10000, 50));
		this.champMessage        = new JTextField("message de simulation");

		this.zoneLog       = new JTextArea();
		this.zoneLog.setEditable(false);
		this.zoneLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		this.progressBar   = new JProgressBar(0, 20);
		this.progressBar.setStringPainted(true);
		this.labelCycle    = new JLabel("cycle: 0/0");
		this.labelBMoyenne = new JLabel("bienveillance moy: —");

		this.boutonDemarrer = new JButton("▶  demarrer");
		this.boutonArreter  = new JButton("■  arreter");
		this.boutonReset    = new JButton("↺  reinitialiser");
		this.boutonArreter.setEnabled(false);

		setTitle("simulateur de propagation");
		setSize(720, 560);
		setLocationByPlatform(true);
		setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);

		construireInterface();
		connecterActions();

		// mise a jour en temps reel via le journal local
		journalLocal.ajouterEcouteur(() ->
				SwingUtilities.invokeLater(this::onNouveauMessage));
	}

	// ─── Interface ────────────────────────────────────────────────────────────

	private void construireInterface() {
		// ── panneau configuration ──
		JPanel confEmetteur = new JPanel(new GridLayout(3, 2, 5, 4));
		confEmetteur.setBorder(BorderFactory.createTitledBorder("emetteur"));
		confEmetteur.add(new JLabel("bavard"));
		confEmetteur.add(choixEmetteur);
		confEmetteur.add(cbAleatoire);
		confEmetteur.add(new JLabel(""));
		confEmetteur.add(new JLabel(""));
		confEmetteur.add(new JLabel(""));

		JPanel confMessage = new JPanel(new GridLayout(3, 2, 5, 4));
		confMessage.setBorder(BorderFactory.createTitledBorder("message"));
		confMessage.add(new JLabel("texte fixe"));
		confMessage.add(champMessage);
		confMessage.add(cbMessageAleatoire);
		confMessage.add(new JLabel(""));
		confMessage.add(cbSentimentAuto);
		confMessage.add(new JLabel(""));

		JPanel confSim = new JPanel(new GridLayout(2, 2, 5, 4));
		confSim.setBorder(BorderFactory.createTitledBorder("simulation"));
		confSim.add(new JLabel("nb cycles"));
		confSim.add(spinnerCycles);
		confSim.add(new JLabel("delai (ms)"));
		confSim.add(spinnerDelai);

		JPanel confGlobal = new JPanel(new GridLayout(1, 3, 8, 0));
		confGlobal.add(confEmetteur);
		confGlobal.add(confMessage);
		confGlobal.add(confSim);

		// ── barre de progression ──
		JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		stats.add(labelCycle);
		stats.add(labelBMoyenne);

		JPanel progression = new JPanel(new BorderLayout(4, 4));
		progression.setBorder(BorderFactory.createTitledBorder("progression"));
		progression.add(progressBar, BorderLayout.CENTER);
		progression.add(stats, BorderLayout.SOUTH);

		// ── boutons ──
		JPanel boutonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
		boutonsPanel.add(boutonDemarrer);
		boutonsPanel.add(boutonArreter);
		boutonsPanel.add(boutonReset);

		// ── zone nord ──
		JPanel nord = new JPanel(new BorderLayout(5, 5));
		nord.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		nord.add(confGlobal, BorderLayout.CENTER);
		nord.add(progression, BorderLayout.SOUTH);

		// ── log ──
		JScrollPane scrollLog = new JScrollPane(zoneLog);
		scrollLog.setBorder(BorderFactory.createTitledBorder("journal de simulation"));

		setLayout(new BorderLayout(5, 5));
		add(nord,        BorderLayout.NORTH);
		add(scrollLog,   BorderLayout.CENTER);
		add(boutonsPanel, BorderLayout.SOUTH);
	}

	private void connecterActions() {
		cbAleatoire.addActionListener(e ->
				choixEmetteur.setEnabled(!cbAleatoire.isSelected()));
		cbMessageAleatoire.addActionListener(e ->
				champMessage.setEnabled(!cbMessageAleatoire.isSelected()));
		boutonDemarrer.addActionListener(e -> demarrer());
		boutonArreter.addActionListener(e  -> arreter());
		boutonReset.addActionListener(e    -> reinitialiser());
	}

	// ─── Simulation ───────────────────────────────────────────────────────────

	private void demarrer() {
		if (reseau.getBavards().isEmpty()) {
			JOptionPane.showMessageDialog(this, "le reseau est vide");
			return;
		}
		cyclesCourants = 0;
		cyclesMax = (Integer) spinnerCycles.getValue();
		int delai = (Integer) spinnerDelai.getValue();

		progressBar.setMaximum(cyclesMax);
		progressBar.setValue(0);
		progressBar.setString("0 / " + cyclesMax);

		statsLocales.reset();
		journalLocal.effacer();
		zoneLog.setText("=== simulation demarre — " + cyclesMax + " cycles ===\n\n");

		boutonDemarrer.setEnabled(false);
		boutonArreter.setEnabled(true);

		timer = new javax.swing.Timer(delai, e -> {
			if (cyclesCourants >= cyclesMax) {
				finSimulation();
				return;
			}
			executerCycle();
			cyclesCourants++;
			progressBar.setValue(cyclesCourants);
			progressBar.setString(cyclesCourants + " / " + cyclesMax);
			labelCycle.setText("cycle: " + cyclesCourants + "/" + cyclesMax);
			actualiserBMoyenne();
		});
		timer.start();
	}

	private void executerCycle() {
		Bavard emetteur = choisirEmetteur();
		if (emetteur == null) return;
		String msg = choisirMessage();
		emetteur.envoyerMessage(msg);
	}

	private Bavard choisirEmetteur() {
		List<Bavard> bavards = reseau.getBavards();
		if (bavards.isEmpty()) return null;
		if (cbAleatoire.isSelected()) {
			return bavards.get(rng.nextInt(bavards.size()));
		}
		return (Bavard) choixEmetteur.getSelectedItem();
	}

	private String choisirMessage() {
		if (!cbMessageAleatoire.isSelected()) {
			String txt = champMessage.getText().trim();
			return txt.isEmpty() ? "message" : txt;
		}
		if (cbSentimentAuto.isSelected()) {
			// alterner positif / negatif selon le cycle
			if (cyclesCourants % 3 == 0) {
				return MESSAGES_NEGATIFS[rng.nextInt(MESSAGES_NEGATIFS.length)];
			}
			return MESSAGES_POSITIFS[rng.nextInt(MESSAGES_POSITIFS.length)];
		}
		return TOUS_MESSAGES[rng.nextInt(TOUS_MESSAGES.length)];
	}

	private void finSimulation() {
		if (timer != null) timer.stop();
		boutonDemarrer.setEnabled(true);
		boutonArreter.setEnabled(false);
		zoneLog.append("\n=== simulation terminee (" + cyclesCourants + " cycles) ===\n");
		zoneLog.append(statsLocales.resume());
		zoneLog.append("\njournal : " + journalLocal.resumeRapide() + "\n");
		zoneLog.setCaretPosition(zoneLog.getDocument().getLength());
		actualiserBMoyenne();
	}

	private void arreter() {
		if (timer != null) timer.stop();
		finSimulation();
	}

	private void reinitialiser() {
		if (timer != null) timer.stop();
		statsLocales.reset();
		journalLocal.effacer();
		cyclesCourants = 0;
		zoneLog.setText("(reinitialise)\n");
		progressBar.setValue(0);
		progressBar.setString("");
		labelCycle.setText("cycle: 0/0");
		labelBMoyenne.setText("bienveillance moy: —");
		boutonDemarrer.setEnabled(true);
		boutonArreter.setEnabled(false);
	}

	// ─── Mise a jour UI ───────────────────────────────────────────────────────

	private void onNouveauMessage() {
		List<JournalReseau.EntreeJournal> entrees = journalLocal.getEntrees();
		if (!entrees.isEmpty()) {
			JournalReseau.EntreeJournal derniere = entrees.get(entrees.size() - 1);
			float b = derniere.getMessage().getBienveillance();
			String prefixe = b >= 3f ? "[+++]" : b >= 0f ? "[ + ]" : b >= -3f ? "[ - ]" : "[---]";
			zoneLog.append(prefixe + " " + derniere + "\n");
			zoneLog.setCaretPosition(zoneLog.getDocument().getLength());
		}
	}

	private void actualiserBMoyenne() {
		if (statsLocales.getTotalMessages() == 0) return;
		double bm = statsLocales.getBienveillanceMoyenne();
		String couleurHtml = bm >= 1.0 ? "green" : bm <= -1.0 ? "red" : "gray";
		labelBMoyenne.setText(String.format(
				"<html>bienveillance moy: <b style='color:%s'>%+.2f</b></html>",
				couleurHtml, bm));
	}
}
