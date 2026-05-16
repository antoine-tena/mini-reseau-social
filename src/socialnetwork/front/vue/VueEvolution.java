package socialnetwork.front.vue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import socialnetwork.back.analyse.HistoriqueReseau;
import socialnetwork.back.analyse.HistoriqueReseau.ClicheReseau;

/**
 * Panneau de courbes temporelles pour l'historique du reseau.
 *
 * Deux series tracees en superposition :
 *   - Bienveillance moyenne  (axe Y gauche,  -10 .. +10, bleu)
 *   - Total messages          (axe Y droite, 0 .. max, orange)
 *
 * Grille, axe X (temps / indices), legende integree.
 *
 * Mise a jour : appeler {@code mettreAJour(HistoriqueReseau)} apres
 * chaque prise de cliche.
 */
@SuppressWarnings("serial")
public class VueEvolution extends JPanel {

	private static final Color COL_BIENVEILLANCE = new Color(60,  110, 220);
	private static final Color COL_MESSAGES      = new Color(230, 140, 30);
	private static final Color COL_GRILLE        = new Color(230, 230, 230);
	private static final Color COL_FOND          = Color.WHITE;
	private static final Color COL_AXE           = new Color(80,  80,  80);

	private double[] bienveillanceData = new double[0];
	private int[]    messagesData      = new int[0];

	public VueEvolution() {
		setBackground(COL_FOND);
		setBorder(BorderFactory.createTitledBorder("evolution temporelle"));
	}

	/** Recharge toutes les donnees et repeint. */
	public void mettreAJour(HistoriqueReseau historique) {
		bienveillanceData = historique.courbesBienveillance();
		messagesData      = historique.courbesMessages();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (bienveillanceData.length == 0) {
			g2.setColor(Color.GRAY);
			g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 13f));
			g2.drawString("aucun cliche \u2014 lancez des simulations et prenez des cliches", 20, 50);
			return;
		}

		int w = getWidth(), h = getHeight();
		int mHaut = 30, mBas = 40, mGauche = 55, mDroite = 55;
		int chartW = w - mGauche - mDroite;
		int chartH = h - mHaut - mBas;
		int n = bienveillanceData.length;

		double bMax = 10.0, bMin = -10.0;
		int mMax = 0;
		for (int v : messagesData) if (v > mMax) mMax = v;
		if (mMax == 0) mMax = 1;

		// ─── grille horizontale ──────────────────────────────────────────────
		g2.setStroke(new BasicStroke(0.5f));
		g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
		FontMetrics fm = g2.getFontMetrics();
		int baseX = mGauche, baseY = h - mBas;

		// 5 lignes de grille pour bienveillance : -10, -5, 0, +5, +10
		double[] ticks = { -10, -5, 0, 5, 10 };
		for (double tick : ticks) {
			int gy = baseY - (int)((tick - bMin) / (bMax - bMin) * chartH);
			g2.setColor(COL_GRILLE);
			g2.drawLine(baseX, gy, baseX + chartW, gy);
			g2.setColor(COL_BIENVEILLANCE);
			String lbl = String.format("%+.0f", tick);
			g2.drawString(lbl, baseX - fm.stringWidth(lbl) - 4, gy + fm.getAscent() / 2);
		}

		// axe Y droit : messages
		for (int k = 0; k <= 4; k++) {
			int val = (int)(mMax * k / 4.0);
			int gy  = baseY - (int)((double) k / 4 * chartH);
			g2.setColor(COL_MESSAGES);
			String lbl = String.valueOf(val);
			g2.drawString(lbl, baseX + chartW + 6, gy + fm.getAscent() / 2);
		}

		// ─── axes ────────────────────────────────────────────────────────────
		g2.setColor(COL_AXE);
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawLine(baseX, mHaut, baseX, baseY);                 // Y gauche
		g2.drawLine(baseX + chartW, mHaut, baseX + chartW, baseY); // Y droit
		g2.drawLine(baseX, baseY, baseX + chartW, baseY);         // X

		// ligne 0 bienveillance en pointille
		int y0 = baseY - (int)((0.0 - bMin) / (bMax - bMin) * chartH);
		float[] dash = { 6f, 4f };
		g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
		g2.setColor(new Color(180, 180, 180));
		g2.drawLine(baseX, y0, baseX + chartW, y0);

		// ─── axe X labels ────────────────────────────────────────────────────
		g2.setColor(COL_AXE);
		g2.setStroke(new BasicStroke(1f));
		int pasX = Math.max(1, n / 8);
		for (int i = 0; i < n; i += pasX) {
			int x = baseX + (n == 1 ? chartW / 2 : i * chartW / (n - 1));
			g2.drawLine(x, baseY, x, baseY + 4);
			String lbl = String.valueOf(i + 1);
			g2.drawString(lbl, x - fm.stringWidth(lbl) / 2, baseY + 14);
		}

		// ─── courbe bienveillance ─────────────────────────────────────────────
		g2.setColor(COL_BIENVEILLANCE);
		g2.setStroke(new BasicStroke(2f));
		int[] xb = new int[n], yb = new int[n];
		for (int i = 0; i < n; i++) {
			xb[i] = baseX + (n == 1 ? chartW / 2 : i * chartW / (n - 1));
			yb[i] = baseY - (int)((bienveillanceData[i] - bMin) / (bMax - bMin) * chartH);
		}
		for (int i = 0; i < n - 1; i++) g2.drawLine(xb[i], yb[i], xb[i + 1], yb[i + 1]);
		for (int i = 0; i < n; i++) {
			g2.fillOval(xb[i] - 3, yb[i] - 3, 6, 6);
		}

		// ─── courbe messages ──────────────────────────────────────────────────
		g2.setColor(COL_MESSAGES);
		g2.setStroke(new BasicStroke(2f));
		int[] xm = new int[n], ym = new int[n];
		for (int i = 0; i < n; i++) {
			xm[i] = baseX + (n == 1 ? chartW / 2 : i * chartW / (n - 1));
			ym[i] = baseY - (int)((double) messagesData[i] / mMax * chartH);
		}
		for (int i = 0; i < n - 1; i++) g2.drawLine(xm[i], ym[i], xm[i + 1], ym[i + 1]);
		for (int i = 0; i < n; i++) {
			g2.fillRect(xm[i] - 3, ym[i] - 3, 6, 6);
		}

		// ─── legende ─────────────────────────────────────────────────────────
		int lx = baseX + 8, ly = mHaut + 8;
		g2.setColor(COL_BIENVEILLANCE);
		g2.setStroke(new BasicStroke(2f));
		g2.drawLine(lx, ly, lx + 20, ly);
		g2.fillOval(lx + 7, ly - 3, 6, 6);
		g2.setColor(COL_AXE);
		g2.drawString("bienveillance moy (L)", lx + 25, ly + fm.getAscent() / 2);
		ly += 16;
		g2.setColor(COL_MESSAGES);
		g2.drawLine(lx, ly, lx + 20, ly);
		g2.fillRect(lx + 7, ly - 3, 6, 6);
		g2.setColor(COL_AXE);
		g2.drawString("total messages (R)", lx + 25, ly + fm.getAscent() / 2);

		// ─── titre axe X ─────────────────────────────────────────────────────
		g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 10f));
		g2.setColor(Color.GRAY);
		String axeX = "cliche n°";
		g2.drawString(axeX, baseX + chartW / 2 - fm.stringWidth(axeX) / 2, h - 4);
	}
}
