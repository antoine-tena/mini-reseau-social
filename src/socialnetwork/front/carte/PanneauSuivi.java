package socialnetwork.front.carte;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.monde.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panneau de suivi latéral affiché dans FenetreCarteVille.
 *
 * Affiche des informations sur le bavard sélectionné ou des statistiques
 * générales sur la ville simulée.
 */
@SuppressWarnings("serial")
public class PanneauSuivi extends JPanel {

    // ─── Données ─────────────────────────────────────────────────────────────

    private VilleSociale          ville;
    private GestionnaireDeplacements gestionnaire;
    private Bavard                bavardSuivi;

    // ─── Composants ──────────────────────────────────────────────────────────

    private final JLabel   labelTitre      = new JLabel("Aucune sélection");
    private final JLabel   labelStatut     = new JLabel();
    private final JLabel   labelPersonnalite = new JLabel();
    private final JLabel   labelTrait      = new JLabel();
    private final JLabel   labelProfession = new JLabel();
    private final JLabel   labelAge        = new JLabel();
    private final JLabel   labelLieuActuel = new JLabel();
    private final JLabel   labelLieuDest   = new JLabel();
    private final JTextArea aireRelations  = new JTextArea(4, 20);
    private final JTextArea aireStats      = new JTextArea(5, 20);

    // Statistiques ville
    private final JLabel labelNbBavards     = new JLabel();
    private final JLabel labelNbLieux       = new JLabel();
    private final JLabel labelNbRelations   = new JLabel();
    private final JLabel labelNbDeplacement = new JLabel();
    private final JLabel labelLieuPleinAssuivi = new JLabel();

    // ─── Timer de mise à jour ─────────────────────────────────────────────────

    private final Timer timerMaj;

    // ──────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTEUR
    // ──────────────────────────────────────────────────────────────────────────

    public PanneauSuivi() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(12, 15, 25));
        setPreferredSize(new Dimension(260, 600));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        construireUI();

        // Mise à jour toutes les 500ms
        timerMaj = new Timer(500, e -> mettreAJour());
        timerMaj.start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTION DE L'UI
    // ──────────────────────────────────────────────────────────────────────────

    private void construireUI() {
        // ── Section bavard sélectionné ─────────────────────────────────────
        JPanel panelBavard = creerSectionBavard();

        // ── Section statistiques ville ────────────────────────────────────
        JPanel panelStats = creerSectionStats();

        // Assemblage vertical
        JPanel contenu = new JPanel();
        contenu.setLayout(new BoxLayout(contenu, BoxLayout.Y_AXIS));
        contenu.setBackground(new Color(12, 15, 25));
        contenu.add(panelBavard);
        contenu.add(Box.createVerticalStrut(12));
        contenu.add(panelStats);

        JScrollPane scroll = new JScrollPane(contenu);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(new Color(12, 15, 25));
        scroll.getViewport().setBackground(new Color(12, 15, 25));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel creerSectionBavard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(18, 22, 40));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 80, 160), 1, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        styleLabel(labelTitre, new Font("Segoe UI", Font.BOLD, 14), new Color(220, 230, 255));
        styleLabel(labelStatut, null, new Color(150, 200, 255));
        styleLabel(labelPersonnalite, null, new Color(200, 200, 255));
        styleLabel(labelTrait, null, new Color(200, 220, 200));
        styleLabel(labelProfession, null, new Color(180, 190, 220));
        styleLabel(labelAge, null, new Color(180, 190, 220));
        styleLabel(labelLieuActuel, null, new Color(150, 220, 180));
        styleLabel(labelLieuDest, null, new Color(150, 220, 180));

        p.add(labelTitre);
        p.add(Box.createVerticalStrut(4));
        p.add(labelStatut);
        p.add(labelPersonnalite);
        p.add(labelTrait);
        p.add(labelProfession);
        p.add(labelAge);
        p.add(Box.createVerticalStrut(4));
        p.add(labelLieuActuel);
        p.add(labelLieuDest);

        p.add(Box.createVerticalStrut(8));
        p.add(creerEntete("Relations"));
        aireRelations.setEditable(false);
        aireRelations.setBackground(new Color(12, 16, 28));
        aireRelations.setForeground(new Color(160, 180, 230));
        aireRelations.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        aireRelations.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(new JScrollPane(aireRelations));

        return p;
    }

    private JPanel creerSectionStats() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(18, 22, 40));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 80, 160), 1, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        p.add(creerEntete("Statistiques ville"));
        p.add(Box.createVerticalStrut(4));

        styleLabel(labelNbBavards,     null, new Color(200, 220, 255));
        styleLabel(labelNbLieux,       null, new Color(200, 220, 255));
        styleLabel(labelNbRelations,   null, new Color(200, 220, 255));
        styleLabel(labelNbDeplacement, null, new Color(200, 220, 255));
        styleLabel(labelLieuPleinAssuivi, null, new Color(255, 200, 100));

        p.add(labelNbBavards);
        p.add(labelNbLieux);
        p.add(labelNbRelations);
        p.add(labelNbDeplacement);
        p.add(Box.createVerticalStrut(4));
        p.add(labelLieuPleinAssuivi);

        p.add(Box.createVerticalStrut(8));
        p.add(creerEntete("Activité récente"));
        aireStats.setEditable(false);
        aireStats.setBackground(new Color(12, 16, 28));
        aireStats.setForeground(new Color(140, 180, 220));
        aireStats.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        aireStats.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(new JScrollPane(aireStats));

        return p;
    }

    private JLabel creerEntete(String titre) {
        JLabel l = new JLabel(titre.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(80, 120, 200));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleLabel(JLabel l, Font font, Color fg) {
        if (font != null) l.setFont(font);
        else l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(fg);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  MISE À JOUR
    // ──────────────────────────────────────────────────────────────────────────

    private void mettreAJour() {
        mettreAJourBavard();
        mettreAJourStats();
    }

    private void mettreAJourBavard() {
        if (bavardSuivi == null || ville == null) {
            labelTitre.setText("Aucune sélection");
            labelStatut.setText("");
            labelPersonnalite.setText("");
            labelTrait.setText("");
            labelProfession.setText("");
            labelAge.setText("");
            labelLieuActuel.setText("");
            labelLieuDest.setText("");
            aireRelations.setText("(cliquez sur un bavard)");
            return;
        }

        labelTitre.setText(bavardSuivi.getNom());
        StatutPresence statut = ville.getStatut(bavardSuivi);
        labelStatut.setText(statut.icone + " " + statut.label);
        labelPersonnalite.setText("🧬 " + bavardSuivi.getPersonnalite().getNom());
        TraitCaractere trait = ville.getTrait(bavardSuivi);
        labelTrait.setText(trait.icone + " " + trait.nom);
        labelProfession.setText("💼 " + ville.getProfession(bavardSuivi));
        labelAge.setText("📅 " + ville.getAge(bavardSuivi) + " ans");

        if (gestionnaire != null) {
            Lieu lieuCourant = gestionnaire.getLieuCourant(bavardSuivi);
            Lieu lieuDest    = gestionnaire.getLieuDestination(bavardSuivi);

            if (lieuCourant != null) {
                labelLieuActuel.setText("📍 " + lieuCourant.getNom());
            }
            if (lieuDest != null && !lieuDest.equals(lieuCourant)) {
                labelLieuDest.setText("→ " + lieuDest.getNom());
            } else {
                labelLieuDest.setText("");
            }
        }

        // Relations
        List<RelationSociale> rels = ville.getRelationsDe(bavardSuivi);
        StringBuilder sb = new StringBuilder();
        int maxRels = Math.min(rels.size(), 10);
        for (int i = 0; i < maxRels; i++) {
            RelationSociale r = rels.get(i);
            Bavard autre = r.getAutre(bavardSuivi);
            if (autre == null) continue;
            sb.append(r.getType().icone).append(" ")
              .append(autre.getNom()).append(" (")
              .append(r.getType().label).append(")\n");
        }
        if (rels.size() > 10) sb.append("... et ").append(rels.size() - 10).append(" autres");
        aireRelations.setText(sb.toString());
    }

    private void mettreAJourStats() {
        if (ville == null) return;

        labelNbBavards.setText("👥 " + ville.getNbBavards() + " personnes");
        labelNbLieux.setText("🏢 " + ville.getNbLieux() + " lieux");
        labelNbRelations.setText("🔗 " + ville.getNbRelations() + " relations");

        if (gestionnaire != null) {
            labelNbDeplacement.setText("🚶 " + gestionnaire.getDeplacementsActifs().size()
                + " déplacements actifs");

            // Lieu le plus fréquenté
            Lieu plein = ville.getLieuLePlusFrequente();
            if (plein != null) {
                labelLieuPleinAssuivi.setText("🔥 " + plein.getNom()
                    + " (" + plein.getNbOccupants() + " pers.)");
            }
        }

        // Activité récente : liste des lieux les plus peuplés
        StringBuilder sb = new StringBuilder();
        ville.getLieux().stream()
            .filter(l -> !l.estHorsCarte() && l.getNbOccupants() > 0)
            .sorted((a, b) -> Integer.compare(b.getNbOccupants(), a.getNbOccupants()))
            .limit(8)
            .forEach(l -> sb.append(l.getType().icone).append(" ")
                             .append(l.getNom()).append(" : ")
                             .append(l.getNbOccupants()).append("\n"));
        aireStats.setText(sb.toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  API PUBLIQUE
    // ──────────────────────────────────────────────────────────────────────────

    public void setContexte(VilleSociale ville, GestionnaireDeplacements gestionnaire) {
        this.ville        = ville;
        this.gestionnaire = gestionnaire;
        mettreAJour();
    }

    public void setBavardSuivi(Bavard b) {
        this.bavardSuivi = b;
        mettreAJourBavard();
    }
}
