package socialnetwork.back.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.stats.StatistiquesReseau;

/**
 * Exportateur de graphes de reseau dans plusieurs formats standards.
 *
 * Formats supportes :
 *   DOT   — Graphviz (affichable avec dot, neato, fdp…)
 *   JSON  — structure JSON simple avec noeuds et aretes
 *   CSV   — matrice d'adjacence (1/0 par paire de bavards)
 *   GEXF  — format XML pour Gephi (import/analyse reseau)
 *
 * Toutes les methodes sont stateless.
 *
 * Exemple :
 *   String dot = ExportateurReseau.exporterDot(reseau);
 *   ExportateurReseau.sauvegarderFichier(dot, "reseau.dot", mainFrame);
 */
public final class ExportateurReseau {

	private ExportateurReseau() {}

	// ─── Export DOT (Graphviz) ────────────────────────────────────────────────

	/**
	 * Genere un fichier .dot pour Graphviz.
	 *
	 * Chaque noeud est affiche avec son nom et sa personnalite.
	 * Les aretes sont non-orientees.
	 */
	public static String exporterDot(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		StringBuilder sb = new StringBuilder();
		sb.append("graph SocialNetwork {\n");
		sb.append("  rankdir=LR;\n");
		sb.append("  node [shape=ellipse, fontname=\"Helvetica\", style=filled];\n\n");

		// noeuds avec couleur selon la personnalite
		for (Bavard b : bavards) {
			float v = b.getPersonnalite().getVariationBienveillance();
			String couleur = v >= 0.5f ? "\"#90EE90\"" : v <= -0.5f ? "\"#FFB6C1\"" : "\"#FFFACD\"";
			sb.append(String.format("  \"%s\" [label=\"%s\\n%s\", fillcolor=%s];%n",
					b.getNom(), b.getNom(), b.getPersonnalite(), couleur));
		}

		sb.append('\n');

		// aretes (une seule fois par paire)
		for (Bavard b : bavards) {
			for (Bavard ami : b.getAmis()) {
				if (b.getNom().compareTo(ami.getNom()) < 0) {
					sb.append(String.format("  \"%s\" -- \"%s\";%n", b.getNom(), ami.getNom()));
				}
			}
		}

		sb.append("}\n");
		return sb.toString();
	}

	// ─── Export JSON ─────────────────────────────────────────────────────────

	/**
	 * Genere un objet JSON simple (pas de dependance externe).
	 * Structure : { "noeuds": [...], "aretes": [...], "stats": {...} }
	 */
	public static String exporterJson(Reseau reseau, StatistiquesReseau stats) {
		List<Bavard> bavards = reseau.getBavards();
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"noeuds\": [\n");
		for (int i = 0; i < bavards.size(); i++) {
			Bavard b = bavards.get(i);
			int nbMsg = stats.getMessagesParBavard().getOrDefault(b.getNom(), 0);
			sb.append(String.format(
				"    { \"id\": \"%s\", \"personnalite\": \"%s\","
				+ " \"variation\": %.3f, \"messages_recus\": %d, \"transmissions\": %d }",
				b.getNom(), b.getPersonnalite(),
				b.getPersonnalite().getVariationBienveillance(),
				nbMsg, b.getCompteurTransmissions()));
			if (i < bavards.size() - 1) sb.append(',');
			sb.append('\n');
		}
		sb.append("  ],\n");

		sb.append("  \"aretes\": [\n");
		boolean premiereLigne = true;
		for (Bavard b : bavards) {
			for (Bavard ami : b.getAmis()) {
				if (b.getNom().compareTo(ami.getNom()) < 0) {
					if (!premiereLigne) sb.append(",\n");
					sb.append(String.format("    { \"source\": \"%s\", \"cible\": \"%s\" }",
							b.getNom(), ami.getNom()));
					premiereLigne = false;
				}
			}
		}
		sb.append("\n  ],\n");

		sb.append(String.format(
			"  \"stats\": { \"total_messages\": %d, \"bienveillance_moy\": %.4f,"
			+ " \"taux_propagation\": %.4f }%n",
			stats.getTotalMessages(),
			stats.getBienveillanceMoyenne(),
			stats.getTauxPropagation()));
		sb.append("}\n");
		return sb.toString();
	}

	// ─── Export CSV matrice d'adjacence ──────────────────────────────────────

	/**
	 * Genere une matrice d'adjacence CSV.
	 * Premiere ligne et premiere colonne = noms des bavards.
	 * Valeur 1 si lien direct, 0 sinon.
	 */
	public static String exporterMatriceAdj(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		int n = bavards.size();
		StringBuilder sb = new StringBuilder();

		// entete
		sb.append(';');
		for (Bavard b : bavards) sb.append(b.getNom()).append(';');
		sb.append('\n');

		// lignes
		for (int i = 0; i < n; i++) {
			sb.append(bavards.get(i).getNom()).append(';');
			for (int j = 0; j < n; j++) {
				sb.append(bavards.get(i).getAmis().contains(bavards.get(j)) ? '1' : '0');
				sb.append(';');
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	// ─── Export GEXF (Gephi) ─────────────────────────────────────────────────

	/**
	 * Genere un fichier GEXF pour Gephi.
	 * Inclut un attribut "variation" (personnalite) pour chaque noeud.
	 */
	public static String exporterGexf(Reseau reseau) {
		List<Bavard> bavards = reseau.getBavards();
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<gexf xmlns=\"http://www.gexf.net/1.2draft\""
				+ " xmlns:viz=\"http://www.gexf.net/1.2draft/viz\" version=\"1.2\">\n");
		sb.append("  <graph defaultedgetype=\"undirected\">\n");

		// attributs noeuds
		sb.append("    <attributes class=\"node\">\n");
		sb.append("      <attribute id=\"0\" title=\"personnalite\" type=\"string\"/>\n");
		sb.append("      <attribute id=\"1\" title=\"variation\" type=\"float\"/>\n");
		sb.append("      <attribute id=\"2\" title=\"transmissions\" type=\"integer\"/>\n");
		sb.append("    </attributes>\n");

		// noeuds
		sb.append("    <nodes>\n");
		for (Bavard b : bavards) {
			float v = b.getPersonnalite().getVariationBienveillance();
			sb.append(String.format(
				"      <node id=\"%s\" label=\"%s\">\n"
				+ "        <attvalues>\n"
				+ "          <attvalue for=\"0\" value=\"%s\"/>\n"
				+ "          <attvalue for=\"1\" value=\"%.3f\"/>\n"
				+ "          <attvalue for=\"2\" value=\"%d\"/>\n"
				+ "        </attvalues>\n"
				+ "      </node>\n",
				b.getNom(), b.getNom(), b.getPersonnalite(), v, b.getCompteurTransmissions()));
		}
		sb.append("    </nodes>\n");

		// aretes
		sb.append("    <edges>\n");
		int edgeId = 0;
		for (Bavard b : bavards) {
			for (Bavard ami : b.getAmis()) {
				if (b.getNom().compareTo(ami.getNom()) < 0) {
					sb.append(String.format(
						"      <edge id=\"%d\" source=\"%s\" target=\"%s\"/>\n",
						edgeId++, b.getNom(), ami.getNom()));
				}
			}
		}
		sb.append("    </edges>\n");
		sb.append("  </graph>\n");
		sb.append("</gexf>\n");
		return sb.toString();
	}

	// ─── Sauvegarde fichier ───────────────────────────────────────────────────

	/**
	 * Ouvre une boite de dialogue JFileChooser pour sauvegarder le contenu.
	 *
	 * @param contenu    texte a sauvegarder
	 * @param nomFichier nom de fichier suggere (ex: "reseau.dot")
	 * @param parent     composant parent pour la boite de dialogue (peut etre null)
	 */
	public static void sauvegarderFichier(String contenu, String nomFichier, JComponent parent) {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(nomFichier));

		String ext = getExtension(nomFichier);
		if (!ext.isEmpty()) {
			chooser.setFileFilter(new FileNameExtensionFilter(
					"Fichiers " + ext.toUpperCase() + " (*." + ext + ")", ext));
		}

		int result = chooser.showSaveDialog(parent);
		if (result != JFileChooser.APPROVE_OPTION) return;

		File cible = chooser.getSelectedFile();
		// ajouter l'extension si absente
		if (!ext.isEmpty() && !cible.getName().contains(".")) {
			cible = new File(cible.getAbsolutePath() + "." + ext);
		}

		try (FileWriter fw = new FileWriter(cible)) {
			fw.write(contenu);
			JOptionPane.showMessageDialog(parent,
					"Fichier sauvegarde :\n" + cible.getAbsolutePath());
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(parent,
					"Erreur lors de la sauvegarde :\n" + ex.getMessage(),
					"Erreur", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ─── Utilitaires ──────────────────────────────────────────────────────────

	private static String getExtension(String nom) {
		int dot = nom.lastIndexOf('.');
		return (dot >= 0 && dot < nom.length() - 1) ? nom.substring(dot + 1).toLowerCase() : "";
	}
}
