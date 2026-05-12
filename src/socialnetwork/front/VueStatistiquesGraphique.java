package socialnetwork.front;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import socialnetwork.back.Bavard;
import socialnetwork.back.MessageEvent;
import socialnetwork.back.Reseau;
import socialnetwork.back.StatistiquesReseau;

/**
 * Panneau de statistiques graphiques dessinées avec Graphics2D.
 *
 * Deux visualisations cote a cote :
 *   GAUCHE : histogramme de distribution des bienveillances en 5 buckets
 *            (tres negatif / negatif / neutre / positif / tres positif)
 *   DROITE : diagramme en barres horizontales — messages recus par bavard
 *
 * Mise a jour : appeler mettreAJour(stats, reseau) apres chaque propagation.
 */
public class VueStatistiquesGraphique extends JPanel {

	// ─── Constantes visuelles ─────────────────────────────────────────────────

	private static final int    BUCKETS          = 5;
	private static final String[] LABELS_HISTO   = { "tres neg", "negatif", "neutre", "positif", "tres pos" };
	private static final Color[]  COULEURS_HISTO = {
		new Color(205, 60,  60),   // tres negatif : rouge
		new Color(255, 150, 90),   // negatif : orange
		new Color(200, 200, 200),  // neutre : gris
		new Color(140, 220, 140),  // positif : vert clair
		new Color(50,  180, 50)    // tres positif : vert
	};
	private static final Color COL_BARRE = new Color(100, 149, 237);   // cornflower blue
	private static final Color COL_AXE   = new Color(100, 100, 100);
	private static final Color COL_GRILLE = new Color(230, 230, 230);

	// ─── Données ──────────────────────────────────────────────────────────────

	private final int[] bucketsHisto = new int[BUCKETS];
	private final LinkedHashMap<String, Integer> messagesParBavard = new LinkedHashMap<>();
	private int totalMessages = 0;
	private double bienveillanceMoyenne = 0.0;

	// ─── Constructeur ─────────────────────────────────────────────────────────

	public VueStatistiquesGraphique() {
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createTitledBorder("statistiques graphiques"));
	}

	// ─── API publique ─────────────────────────────────────────────────────────

	/**
	 * Met a jour les donnees internes et redessine le panneau.
	 *
	 * @param stats statistiques du reseau (source des comptages)
	 * @param reseau reseau courant (pour scanner les messages recus)
	 */
	public void mettreAJour(StatistiquesReseau stats, Reseau reseau) {
		totalMessages = stats.getTotalMessages();
		bienveillanceMoyenne = stats.getBienveillanceMoyenne();

		// reconstruire la map bavard -> messages (triee par valeur desc)
		messagesParBavard.clear();
		Map<String, Integer> mpb = stats.getMessagesParBavard();
		mpb.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.forEach(e -> messagesParBavard.put(e.getKey(), e.getValue()));

		// reconstruire l'histogramme depuis les messages des bavards
		Arrays.fill(bucketsHisto, 0);
		for (Bavard b : reseau.getBavards()) {
			for (MessageEvent m : b.getMessagesRecus()) {
				bucketsHisto[getBucket(m.getBienveillance())]++;
			}
		}

		repaint();
	}

	// ─── Rendu ────────────────────────────────────────────────────────────────

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (totalMessages == 0) {
			g2.setColor(Color.GRAY);
			g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 13f));
			g2.drawString("aucune donnee — lancez une propagation", 20, 50);
			return;
		}

		int w   = getWidth();
		int h   = getHeight();
		int mHaut  = 45;
		int mBas   = 50;
		int mGauche = 8;
		int mDroite = 8;
		int chartH = h - mHaut - mBas;
		int halfW  = w / 2 - 4;

		// titre global avec la moyenne
		g2.setColor(Color.DARK_GRAY);
		g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
		String titre = String.format("total: %d messages   bienveillance moy: %+.2f",
				totalMessages, bienveillanceMoyenne);
		g2.drawString(titre, mGauche, 18);

		// ligne de separation centrale
		g2.setColor(new Color(220, 220, 220));
		g2.setStroke(new BasicStroke(1f));
		g2.drawLine(w / 2, mHaut - 4, w / 2, h - mBas + 4);

		dessinerHistogramme(g2, mGauche, mHaut, halfW - mGauche, chartH);
		dessinerBarChart(g2, w / 2 + 4, mHaut, halfW - mDroite, chartH);
	}

	// ─── Histogramme ──────────────────────────────────────────────────────────

	private void dessinerHistogramme(Graphics2D g2, int ox, int oy, int w, int h) {
		FontMetrics fm = g2.getFontMetrics(g2.getFont().deriveFont(Font.PLAIN, 10f));

		// titre
		g2.setColor(Color.DARK_GRAY);
		g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
		g2.drawString("distribution bienveillance", ox, oy - 6);
		g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));

		int maxVal = Arrays.stream(bucketsHisto).max().orElse(1);
		if (maxVal == 0) maxVal = 1;

		int baseY  = oy + h - 22;
		int barW   = (w - 16) / BUCKETS;
		int chartAreaH = h - 30;

		// lignes de grille horizontales
		g2.setStroke(new BasicStroke(0.5f));
		for (int tick = 0; tick <= 4; tick++) {
			int gy = baseY - (int)((double) tick / 4 * chartAreaH);
			g2.setColor(COL_GRILLE);
			g2.drawLine(ox, gy, ox + w - 8, gy);
			if (tick > 0) {
				g2.setColor(Color.GRAY);
				String label = String.valueOf((int)((double) tick / 4 * maxVal));
				g2.drawString(label, ox - fm.stringWidth(label) - 2, gy + fm.getAscent() / 2);
			}
		}

		// barres
		for (int i = 0; i < BUCKETS; i++) {
			int barH = (int)((double) bucketsHisto[i] / maxVal * chartAreaH);
			int bx   = ox + 4 + i * barW + 2;
			int by   = baseY - barH;

			g2.setColor(COULEURS_HISTO[i]);
			g2.fillRect(bx, by, barW - 4, barH);
			g2.setColor(COULEURS_HISTO[i].darker());
			g2.setStroke(new BasicStroke(1.2f));
			g2.drawRect(bx, by, barW - 4, barH);

			// valeur au sommet
			if (bucketsHisto[i] > 0) {
				g2.setColor(Color.DARK_GRAY);
				String val = String.valueOf(bucketsHisto[i]);
				int tw = fm.stringWidth(val);
				g2.drawString(val, bx + (barW - 4) / 2 - tw / 2, by - 2);
			}

			// label sous la barre
			g2.setColor(Color.DARK_GRAY);
			String label = LABELS_HISTO[i];
			int lw = fm.stringWidth(label);
			g2.drawString(label, bx + (barW - 4) / 2 - lw / 2, baseY + 14);
		}

		// axe X
		g2.setColor(COL_AXE);
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawLine(ox, baseY, ox + w - 8, baseY);
	}

	// ─── Diagramme barres horizontales ────────────────────────────────────────

	private void dessinerBarChart(Graphics2D g2, int ox, int oy, int w, int h) {
		if (messagesParBavard.isEmpty()) return;

		FontMetrics fm = g2.getFontMetrics(g2.getFont().deriveFont(Font.PLAIN, 10f));

		g2.setColor(Color.DARK_GRAY);
		g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
		g2.drawString("messages par bavard", ox, oy - 6);
		g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));

		List<Map.Entry<String, Integer>> entries = new ArrayList<>(messagesParBavard.entrySet());
		int n = entries.size();
		if (n == 0) return;

		int maxVal = entries.get(0).getValue(); // deja trie desc
		if (maxVal == 0) maxVal = 1;

		int labelW  = 60;
		int chartW  = w - labelW - 30;
		int barH    = Math.max(5, Math.min(22, (h - 20) / n - 3));
		int espacement = barH + 3;

		for (int i = 0; i < n; i++) {
			String nom = entries.get(i).getKey();
			int    val = entries.get(i).getValue();
			int    bw  = (int)((double) val / maxVal * chartW);

			int bx = ox + labelW;
			int by = oy + 4 + i * espacement;

			// barre
			Color couleur = valeurCouleur(val, maxVal);
			g2.setColor(couleur);
			g2.fillRect(bx, by, bw, barH);
			g2.setColor(couleur.darker());
			g2.setStroke(new BasicStroke(1f));
			g2.drawRect(bx, by, bw, barH);

			// nom a gauche
			g2.setColor(Color.DARK_GRAY);
			String nomAffiche = nom.length() > 9 ? nom.substring(0, 8) + "." : nom;
			g2.drawString(nomAffiche, ox, by + barH - 2);

			// valeur a droite
			g2.drawString(String.valueOf(val), bx + bw + 4, by + barH - 2);
		}

		// axe Y
		g2.setColor(COL_AXE);
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawLine(ox + labelW, oy, ox + labelW, oy + n * espacement + 6);
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	/** Buckets : [<-5], [-5..-1], [-1..1], [1..5], [>5]. */
	private int getBucket(float b) {
		if (b < -5f) return 0;
		if (b < -1f) return 1;
		if (b <=  1f) return 2;
		if (b <=  5f) return 3;
		return 4;
	}

	/** Gradient de couleur selon l'intensite relative de la barre. */
	private Color valeurCouleur(int val, int max) {
		if (max == 0) return COL_BARRE;
		float ratio = (float) val / max;
		if (ratio > 0.66f) return new Color(80, 130, 220);
		if (ratio > 0.33f) return COL_BARRE;
		return new Color(170, 195, 240);
	}
}
