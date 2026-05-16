package socialnetwork.front.vue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.observer.BavardObserver;
import socialnetwork.back.personnalite.Personnalite;

/**
 * Panneau graphique qui dessine le reseau de bavards.
 * Noeuds colores par personnalite, liens entre amis.
 */
@SuppressWarnings("serial")
public class VueReseau extends JPanel implements BavardObserver {

	protected final Reseau reseau;
	protected static final int RAYON_NOEUD = 28;

	public VueReseau(Reseau reseau) {
		this.reseau = reseau;
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createTitledBorder("visualisation du reseau"));
		for (Bavard b : reseau.getBavards()) b.ajouterObservateur(this);
	}

	public void observerBavard(Bavard bavard) {
		bavard.ajouterObservateur(this);
		SwingUtilities.invokeLater(this::repaint);
	}

	protected Map<Bavard, Point> calculerPositions() {
		Map<Bavard, Point> positions = new LinkedHashMap<>();
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		if (n == 0) return positions;
		int w = getWidth();
		int h = getHeight();
		int cx = w > 0 ? w / 2 : 200;
		int cy = h > 0 ? h / 2 : 200;
		int rayon = Math.min(cx, cy) - RAYON_NOEUD - 30;
		if (rayon < 20) rayon = 20;
		for (int i = 0; i < n; i++) {
			double angle = 2 * Math.PI * i / n - Math.PI / 2;
			int x = (int)(cx + rayon * Math.cos(angle));
			int y = (int)(cy + rayon * Math.sin(angle));
			positions.put(bavards.get(i), new Point(x, y));
		}
		return positions;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Map<Bavard, Point> positions = calculerPositions();
		if (positions.isEmpty()) {
			g.setColor(Color.GRAY);
			g.drawString("aucun bavard dans le reseau", 20, 30);
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		FontMetrics fm = g2.getFontMetrics();

		// liens
		g2.setStroke(new BasicStroke(1.8f));
		g2.setColor(new Color(160, 160, 160));
		for (Map.Entry<Bavard, Point> entry : positions.entrySet()) {
			Bavard b = entry.getKey();
			Point p1 = entry.getValue();
			for (Bavard ami : b.getAmis()) {
				Point p2 = positions.get(ami);
				if (p2 != null && b.getNom().compareTo(ami.getNom()) < 0) {
					g2.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
			}
		}

		// noeuds
		for (Map.Entry<Bavard, Point> entry : positions.entrySet()) {
			Bavard b = entry.getKey();
			Point p = entry.getValue();
			dessinerNoeud(g2, fm, b, p);
		}

		dessinerLegende(g2, fm);
	}

	protected void dessinerNoeud(Graphics2D g2, FontMetrics fm, Bavard b, Point p) {
		Color fill = couleurPersonnalite(b.getPersonnalite());
		g2.setColor(fill);
		g2.fillOval(p.x - RAYON_NOEUD, p.y - RAYON_NOEUD, 2 * RAYON_NOEUD, 2 * RAYON_NOEUD);
		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(2f));
		g2.drawOval(p.x - RAYON_NOEUD, p.y - RAYON_NOEUD, 2 * RAYON_NOEUD, 2 * RAYON_NOEUD);
		g2.setColor(Color.BLACK);
		String nom = b.getNom();
		int tw = fm.stringWidth(nom);
		g2.drawString(nom, p.x - tw / 2, p.y + fm.getAscent() / 2 - 1);

		String conf = configBavard(b);
		if (!conf.isEmpty()) {
			g2.setColor(new Color(80, 80, 80));
			int cw = fm.stringWidth(conf);
			g2.drawString(conf, p.x - cw / 2, p.y + RAYON_NOEUD + fm.getHeight());
		}
	}

	protected void dessinerLegende(Graphics2D g2, FontMetrics fm) {
		int x = 8;
		int y = getHeight() - 3 * (fm.getHeight() + 2) - 8;
		if (y < 10) return;
		Color[] couleurs = { new Color(144, 238, 144), new Color(255, 255, 153), new Color(255, 182, 193) };
		String[] labels  = { "positif", "neutre", "negatif" };
		for (int i = 0; i < couleurs.length; i++) {
			int ly = y + i * (fm.getHeight() + 2);
			g2.setColor(couleurs[i]);
			g2.fillRect(x, ly, fm.getHeight(), fm.getHeight());
			g2.setColor(Color.DARK_GRAY);
			g2.setStroke(new BasicStroke(1f));
			g2.drawRect(x, ly, fm.getHeight(), fm.getHeight());
			g2.setColor(Color.BLACK);
			g2.drawString(labels[i], x + fm.getHeight() + 4, ly + fm.getAscent());
		}
	}

	protected Color couleurPersonnalite(Personnalite p) {
		float v = p.getVariationBienveillance();
		if (v >= 0.5f)  return new Color(144, 238, 144);
		if (v <= -0.5f) return new Color(255, 182, 193);
		return new Color(255, 255, 153);
	}

	private String configBavard(Bavard b) {
		StringBuilder sb = new StringBuilder();
		if (b.getSeuilTolerance() > -10f) sb.append("s=").append(String.format("%.0f", b.getSeuilTolerance()));
		if (b.getProbabiliteRetransmission() < 1.0) {
			if (sb.length() > 0) sb.append(' ');
			sb.append("p=").append(String.format("%.0f%%", b.getProbabiliteRetransmission() * 100));
		}
		return sb.toString();
	}

	@Override
	public void messageAjoute(Bavard bavard, MessageEvent event) { /* pas de repaint */ }

	@Override
	public void amisMisAJour(Bavard bavard) { SwingUtilities.invokeLater(this::repaint); }
}
