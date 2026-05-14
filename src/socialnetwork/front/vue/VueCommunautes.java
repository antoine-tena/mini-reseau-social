package socialnetwork.front.vue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import socialnetwork.back.analyse.DetecteurCommunautes;
import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;

/**
 * Variante de VueReseau qui colore les noeuds selon leur communaute.
 *
 * Extends VueReseau pour heriter de la geometrie (positions, rayons) et
 * surcharge dessinerNoeud / dessinerLegende afin d'utiliser les couleurs
 * de communautes plutot que les couleurs de personnalite.
 *
 * Mise a jour : appeler {@code actualiser(Reseau)} apres chaque detection.
 */
public class VueCommunautes extends VueReseau {

	/** Palette de 8 couleurs vives pour distinguer les communautes. */
	private static final Color[] PALETTE = {
		new Color(70,  130, 220),   // bleu
		new Color(220, 90,  60),    // rouge
		new Color(60,  180, 80),    // vert
		new Color(210, 170, 30),    // jaune-or
		new Color(160, 60,  200),   // violet
		new Color(30,  190, 180),   // cyan
		new Color(230, 130, 40),    // orange
		new Color(180, 60,  130),   // rose
	};

	/** Map nom_bavard → index couleur communaute. */
	private final Map<String, Integer> communauteIndex = new HashMap<>();
	/** Noms des communautes dans l'ordre de la palette. */
	private final List<String> nomsCommunautes = new ArrayList<>();

	public VueCommunautes(Reseau reseau) {
		super(reseau);
	}

	/**
	 * Detecte les communautes dans le reseau donne, met a jour la
	 * correspondance nom→couleur et repeint.
	 */
	public void actualiser(Reseau r) {
		communauteIndex.clear();
		nomsCommunautes.clear();

		Map<String, List<Bavard>> communautes = DetecteurCommunautes.detecter(r);
		int idx = 0;
		for (Map.Entry<String, List<Bavard>> entry : communautes.entrySet()) {
			nomsCommunautes.add(entry.getKey());
			for (Bavard b : entry.getValue()) {
				communauteIndex.put(b.getNom(), idx);
			}
			idx++;
		}
		repaint();
	}

	// ─── Surcharges ──────────────────────────────────────────────────────────

	@Override
	protected void dessinerNoeud(Graphics2D g2, FontMetrics fm, Bavard b, Point p) {
		int idx = communauteIndex.getOrDefault(b.getNom(), -1);
		Color couleur = (idx >= 0) ? PALETTE[idx % PALETTE.length] : Color.LIGHT_GRAY;
		Color bordure = couleur.darker();

		// cercle rempli
		g2.setColor(couleur);
		g2.fillOval(p.x - RAYON_NOEUD, p.y - RAYON_NOEUD, 2 * RAYON_NOEUD, 2 * RAYON_NOEUD);

		// contour
		g2.setColor(bordure);
		g2.drawOval(p.x - RAYON_NOEUD, p.y - RAYON_NOEUD, 2 * RAYON_NOEUD, 2 * RAYON_NOEUD);

		// label nom
		g2.setColor(Color.WHITE);
		Font f = g2.getFont().deriveFont(Font.BOLD, 11f);
		g2.setFont(f);
		FontMetrics fm2 = g2.getFontMetrics(f);
		String nom = b.getNom().length() > 8 ? b.getNom().substring(0, 7) + "." : b.getNom();
		g2.drawString(nom, p.x - fm2.stringWidth(nom) / 2, p.y + fm2.getAscent() / 2 - 1);
	}

	@Override
	protected void dessinerLegende(Graphics2D g2, FontMetrics fm) {
		if (nomsCommunautes.isEmpty()) return;

		Font boldFont = g2.getFont().deriveFont(Font.BOLD, 11f);
		Font normFont = g2.getFont().deriveFont(Font.PLAIN, 10f);
		g2.setFont(boldFont);
		FontMetrics fmBold = g2.getFontMetrics(boldFont);

		int lx = 10, ly = 10;
		g2.setColor(new Color(255, 255, 255, 210));
		g2.fillRoundRect(lx - 4, ly - 4, 140, nomsCommunautes.size() * 18 + 24, 8, 8);
		g2.setColor(Color.DARK_GRAY);
		g2.drawRoundRect(lx - 4, ly - 4, 140, nomsCommunautes.size() * 18 + 24, 8, 8);

		g2.drawString("communautes", lx + 2, ly + fmBold.getAscent());
		ly += fmBold.getHeight() + 4;
		g2.setFont(normFont);
		FontMetrics fmNorm = g2.getFontMetrics(normFont);

		for (int i = 0; i < nomsCommunautes.size(); i++) {
			Color c = PALETTE[i % PALETTE.length];
			g2.setColor(c);
			g2.fillOval(lx, ly, 12, 12);
			g2.setColor(c.darker());
			g2.drawOval(lx, ly, 12, 12);
			g2.setColor(Color.DARK_GRAY);
			String label = nomsCommunautes.get(i);
			if (label.length() > 14) label = label.substring(0, 13) + ".";
			g2.drawString(label, lx + 16, ly + fmNorm.getAscent());
			ly += 18;
		}
	}
}
