package socialnetwork.front.fenetre;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import socialnetwork.back.analyse.AlgorithmeInfluence;
import socialnetwork.back.analyse.DetecteurCommunautes;
import socialnetwork.back.analyse.HistoriqueReseau;
import socialnetwork.back.export.ExportateurReseau;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.stats.StatistiquesReseau;
import socialnetwork.front.vue.VueCommunautes;
import socialnetwork.front.vue.VueEvolution;

/**
 * Fenetre d'analyse avancee du reseau.
 *
 * Quatre onglets :
 *   1. Influence   — centralites (degre, proximite, intermediaire), PageRank
 *   2. Communautes — detection Label Propagation, vue graphique coloree
 *   3. Evolution   — courbes temporelles (bienveillance, messages)
 *   4. Export      — DOT, JSON, CSV matrice, GEXF
 *
 * Constructeur :
 *   FenetreAnalyse(Reseau, StatistiquesReseau, HistoriqueReseau)
 */
@SuppressWarnings("serial")
public class FenetreAnalyse extends JFrame {

	private final Reseau             reseau;
	private final StatistiquesReseau stats;
	private final HistoriqueReseau   historique;

	// ── Composants des onglets ──────────────────────────────────────────────
	private final JTextArea taInfluence   = new JTextArea();
	private final JTextArea taCommunautes = new JTextArea();
	private final VueCommunautes vueCom;
	private final VueEvolution   vueEvo   = new VueEvolution();

	public FenetreAnalyse(Reseau reseau, StatistiquesReseau stats, HistoriqueReseau historique) {
		this.reseau     = reseau;
		this.stats      = stats;
		this.historique = historique;
		this.vueCom     = new VueCommunautes(reseau);

		setTitle("Analyse avancee du reseau");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(860, 640));

		JTabbedPane onglets = new JTabbedPane();
		onglets.addTab("influence",    creerOngletInfluence());
		onglets.addTab("communautes",  creerOngletCommunautes());
		onglets.addTab("evolution",    creerOngletEvolution());
		onglets.addTab("export",       creerOngletExport());

		getContentPane().add(onglets, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
	}

	// ─── Onglet 1 : Influence ─────────────────────────────────────────────────

	private JPanel creerOngletInfluence() {
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		configTextArea(taInfluence);
		JScrollPane scroll = new JScrollPane(taInfluence);

		JButton btnCalculer = new JButton("Calculer les scores d'influence");
		btnCalculer.setFont(btnCalculer.getFont().deriveFont(Font.BOLD, 13f));
		btnCalculer.addActionListener((ActionEvent e) -> calculerInfluence(btnCalculer));

		JPanel sud = new JPanel();
		sud.add(btnCalculer);

		panel.add(new JLabel("<html><b>Centralites et PageRank</b></html>"), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(sud,    BorderLayout.SOUTH);
		return panel;
	}

	private void calculerInfluence(JButton btn) {
		if (reseau.getBavards().isEmpty()) {
			taInfluence.setText("Aucun bavard dans le reseau.");
			return;
		}
		btn.setEnabled(false);
		taInfluence.setText("Calcul en cours...");

		new SwingWorker<String, Void>() {
			@Override protected String doInBackground() {
				return AlgorithmeInfluence.rapport(reseau);
			}
			@Override protected void done() {
				try {
					taInfluence.setText(get());
					taInfluence.setCaretPosition(0);
				} catch (Exception ex) {
					taInfluence.setText("Erreur : " + ex.getMessage());
				}
				btn.setEnabled(true);
			}
		}.execute();
	}

	// ─── Onglet 2 : Communautes ───────────────────────────────────────────────

	private JSplitPane creerOngletCommunautes() {
		configTextArea(taCommunautes);
		JScrollPane scrollTxt = new JScrollPane(taCommunautes);
		scrollTxt.setMinimumSize(new Dimension(220, 80));

		vueCom.setMinimumSize(new Dimension(200, 200));
		vueCom.setPreferredSize(new Dimension(500, 400));

		JPanel droite = new JPanel(new BorderLayout(6, 6));
		droite.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JButton btnDefecter = new JButton("Detecter les communautes");
		btnDefecter.setFont(btnDefecter.getFont().deriveFont(Font.BOLD, 12f));
		btnDefecter.addActionListener(e -> detecterCommunautes(btnDefecter));

		JPanel haut = new JPanel();
		haut.add(btnDefecter);
		droite.add(haut, BorderLayout.NORTH);
		droite.add(vueCom, BorderLayout.CENTER);

		JPanel gauche = new JPanel(new BorderLayout(4, 4));
		gauche.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));
		gauche.add(new JLabel("<html><b>Rapport</b></html>"), BorderLayout.NORTH);
		gauche.add(scrollTxt, BorderLayout.CENTER);
		gauche.setMinimumSize(new Dimension(200, 100));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gauche, droite);
		split.setDividerLocation(240);
		split.setResizeWeight(0.3);
		return split;
	}

	private void detecterCommunautes(JButton btn) {
		if (reseau.getBavards().isEmpty()) {
			taCommunautes.setText("Aucun bavard dans le reseau.");
			return;
		}
		btn.setEnabled(false);
		taCommunautes.setText("Detection...");
		vueCom.actualiser(reseau);

		new SwingWorker<String, Void>() {
			@Override protected String doInBackground() {
				return DetecteurCommunautes.rapport(reseau);
			}
			@Override protected void done() {
				try {
					taCommunautes.setText(get());
					taCommunautes.setCaretPosition(0);
				} catch (Exception ex) {
					taCommunautes.setText("Erreur : " + ex.getMessage());
				}
				btn.setEnabled(true);
			}
		}.execute();
	}

	// ─── Onglet 3 : Evolution ─────────────────────────────────────────────────

	private JPanel creerOngletEvolution() {
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		vueEvo.setPreferredSize(new Dimension(800, 480));

		JButton btnCliche  = new JButton("Prendre un cliche maintenant");
		JButton btnRefresh = new JButton("Rafraichir");
		JButton btnEffacer = new JButton("Effacer l'historique");

		btnCliche.addActionListener(e -> {
			historique.prendreCliche(stats, reseau);
			vueEvo.mettreAJour(historique);
		});
		btnRefresh.addActionListener(e -> vueEvo.mettreAJour(historique));
		btnEffacer.addActionListener(e -> {
			historique.effacer();
			vueEvo.mettreAJour(historique);
		});

		JPanel sud = new JPanel();
		sud.add(btnCliche);
		sud.add(btnRefresh);
		sud.add(btnEffacer);

		JLabel info = new JLabel("<html><b>Evolution temporelle</b> — "
				+ "bienveillance moyenne (bleu/gauche) et total messages (orange/droite)</html>");
		panel.add(info,   BorderLayout.NORTH);
		panel.add(vueEvo, BorderLayout.CENTER);
		panel.add(sud,    BorderLayout.SOUTH);
		return panel;
	}

	// ─── Onglet 4 : Export ────────────────────────────────────────────────────

	private JPanel creerOngletExport() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

		JLabel titre = new JLabel("<html><b>Exporter le graphe du reseau</b></html>");
		titre.setFont(titre.getFont().deriveFont(Font.BOLD, 14f));
		titre.setAlignmentX(0.5f);
		panel.add(titre);
		panel.add(Box.createVerticalStrut(20));

		panel.add(creerBoutonExport(
			"DOT (Graphviz)",
			"reseau.dot",
			() -> ExportateurReseau.exporterDot(reseau)));
		panel.add(Box.createVerticalStrut(10));
		panel.add(creerBoutonExport(
			"JSON",
			"reseau.json",
			() -> ExportateurReseau.exporterJson(reseau, stats)));
		panel.add(Box.createVerticalStrut(10));
		panel.add(creerBoutonExport(
			"CSV — matrice d'adjacence",
			"matrice.csv",
			() -> ExportateurReseau.exporterMatriceAdj(reseau)));
		panel.add(Box.createVerticalStrut(10));
		panel.add(creerBoutonExport(
			"GEXF (Gephi)",
			"reseau.gexf",
			() -> ExportateurReseau.exporterGexf(reseau)));
		panel.add(Box.createVerticalStrut(20));

		// export CSV historique
		JButton btnHisto = new JButton("Historique (CSV)");
		btnHisto.setAlignmentX(0.5f);
		btnHisto.addActionListener(e -> {
			if (historique.getTaille() == 0) {
				javax.swing.JOptionPane.showMessageDialog(this, "Aucun cliche dans l'historique.");
				return;
			}
			ExportateurReseau.sauvegarderFichier(
				historique.exporterCsv(), "historique.csv", (JPanel) panel.getParent());
		});
		panel.add(btnHisto);
		return panel;
	}

	private JButton creerBoutonExport(String label, String nomFichier,
			java.util.function.Supplier<String> generateur) {
		JButton btn = new JButton(label);
		btn.setAlignmentX(0.5f);
		btn.setMaximumSize(new Dimension(260, 36));
		btn.addActionListener(e -> {
			if (reseau.getBavards().isEmpty()) {
				javax.swing.JOptionPane.showMessageDialog(this, "Le reseau est vide.");
				return;
			}
			ExportateurReseau.sauvegarderFichier(generateur.get(), nomFichier, this.getRootPane());
		});
		return btn;
	}

	// ─── Utilitaire ──────────────────────────────────────────────────────────

	private static void configTextArea(JTextArea ta) {
		ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		ta.setEditable(false);
		ta.setLineWrap(false);
	}

	/** Ouvre la fenetre (thread Swing). */
	public static void ouvrir(Reseau reseau, StatistiquesReseau stats, HistoriqueReseau historique) {
		SwingUtilities.invokeLater(() -> new FenetreAnalyse(reseau, stats, historique).setVisible(true));
	}
}
