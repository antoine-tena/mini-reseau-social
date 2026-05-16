package socialnetwork.front.theme;

import java.awt.*;

/**
 * Système de thèmes visuels pour l'interface animée.
 *
 * Centralise toutes les couleurs, polices et constantes visuelles
 * utilisées dans l'interface graphique animée. Supporte trois thèmes :
 *  - NEURAL  : fond sombre, bleu/violet, style réseau neuronal (défaut)
 *  - DARK    : fond très sombre, tons froids, style "hacker"
 *  - LIGHT   : fond clair, couleurs vives, style "dashboard"
 */
public final class ThemeVisuel {

    // =========================================================
    //  THÈMES DISPONIBLES
    // =========================================================

    public enum Theme { NEURAL, DARK, LIGHT }

    // =========================================================
    //  INSTANCE UNIQUE (singleton)
    // =========================================================

    private static ThemeVisuel instance = new ThemeVisuel(Theme.NEURAL);

    public static ThemeVisuel get()             { return instance; }
    public static void setTheme(Theme theme)    { instance = new ThemeVisuel(theme); }
    public Theme getTheme()                     { return theme; }

    // =========================================================
    //  COULEURS DU THÈME ACTIF
    // =========================================================

    private final Theme theme;

    // Fond et grille
    public final Color fond;
    public final Color fondGradientHaut;
    public final Color fondGradientBas;
    public final Color grille;
    public final Color texteUI;
    public final Color texteSombre;

    // Nœuds
    public final Color noeudDefaut;
    public final Color noeudSurvol;
    public final Color noeudSelectionne;
    public final Color noeudBordure;
    public final Color noeudEtiquette;
    public final Color glowDefaut;

    // Arêtes
    public final Color areteDefaut;
    public final Color areteSurvol;
    public final Color areteActive;

    // Particules (messages)
    public final Color particulaPositive;
    public final Color particulaNegative;
    public final Color particulaNeutre;

    // Halos de communauté
    public final float communauteHaloAlpha;

    // Panneaux et overlays
    public final Color panneauFond;
    public final Color panneauBordure;
    public final Color overlayFond;

    // Couleurs de bienveillance (gradient négatif → neutre → positif)
    public final Color bienveillanceNegative;  // bienveillance ≤ -5
    public final Color bienveillanceTresNeg;   // bienveillance ≈ -3
    public final Color bienveillanceNeutre;    // bienveillance ≈ 0
    public final Color bienveillanceTresPos;   // bienveillance ≈ +3
    public final Color bienveillancePositive;  // bienveillance ≥ +5

    // =========================================================
    //  POLICES
    // =========================================================

    public final Font policeEtiquette;
    public final Font policeTitre;
    public final Font policeStats;
    public final Font policeTooltip;

    // =========================================================
    //  PALETTES DE COMMUNAUTÉS
    // =========================================================

    /** Couleurs pour les communautés détectées (max 12 communautés). */
    public final Color[] palettesCommunautes;

    // =========================================================
    //  CONSTANTES DE RENDU
    // =========================================================

    public final int rayonNoeud;
    public final int rayonGlow;
    public final float epaisseurArete;
    public final float epaisseurAreteMax;
    public final int tailleParticule;
    public final int rayonPulse;
    public final float vitesseParticule;

    // =========================================================
    //  CONSTRUCTEUR PRIVÉ
    // =========================================================

    private ThemeVisuel(Theme theme) {
        this.theme = theme;

        switch (theme) {
            case NEURAL -> {
                fond              = new Color(8, 8, 24);
                fondGradientHaut  = new Color(10, 10, 30);
                fondGradientBas   = new Color(5, 5, 18);
                grille            = new Color(20, 20, 50, 60);
                texteUI           = new Color(200, 210, 255);
                texteSombre       = new Color(100, 110, 150);

                noeudDefaut       = new Color(60, 100, 220);
                noeudSurvol       = new Color(100, 160, 255);
                noeudSelectionne  = new Color(255, 220, 50);
                noeudBordure      = new Color(150, 180, 255, 200);
                noeudEtiquette    = new Color(220, 230, 255);
                glowDefaut        = new Color(80, 120, 255, 60);

                areteDefaut       = new Color(50, 70, 120, 100);
                areteSurvol       = new Color(100, 150, 255, 180);
                areteActive       = new Color(130, 200, 255, 200);

                particulaPositive = new Color(100, 255, 150);
                particulaNegative = new Color(255, 80, 80);
                particulaNeutre   = new Color(180, 180, 255);

                communauteHaloAlpha = 0.07f;

                panneauFond       = new Color(15, 15, 40, 220);
                panneauBordure    = new Color(60, 80, 160, 150);
                overlayFond       = new Color(0, 0, 0, 160);

                bienveillanceNegative = new Color(220, 30, 30);
                bienveillanceTresNeg  = new Color(220, 100, 50);
                bienveillanceNeutre   = new Color(150, 150, 200);
                bienveillanceTresPos  = new Color(80, 200, 130);
                bienveillancePositive = new Color(50, 230, 100);

                palettesCommunautes = new Color[]{
                    new Color(80,  160, 255),  // bleu
                    new Color(255, 100, 100),  // rouge
                    new Color(100, 230, 130),  // vert
                    new Color(255, 200, 60),   // jaune
                    new Color(200, 100, 255),  // violet
                    new Color(255, 150, 50),   // orange
                    new Color(80,  230, 220),  // cyan
                    new Color(255, 100, 200),  // rose
                    new Color(150, 255, 100),  // vert lime
                    new Color(100, 180, 255),  // bleu ciel
                    new Color(255, 180, 255),  // lilas
                    new Color(200, 255, 200),  // vert pâle
                };
            }
            case DARK -> {
                fond              = new Color(12, 12, 12);
                fondGradientHaut  = new Color(15, 15, 15);
                fondGradientBas   = new Color(8, 8, 8);
                grille            = new Color(30, 30, 30, 80);
                texteUI           = new Color(210, 210, 210);
                texteSombre       = new Color(100, 100, 100);

                noeudDefaut       = new Color(80, 80, 80);
                noeudSurvol       = new Color(160, 160, 160);
                noeudSelectionne  = new Color(200, 200, 50);
                noeudBordure      = new Color(150, 150, 150, 200);
                noeudEtiquette    = new Color(220, 220, 220);
                glowDefaut        = new Color(100, 100, 100, 50);

                areteDefaut       = new Color(50, 50, 50, 120);
                areteSurvol       = new Color(150, 150, 150, 180);
                areteActive       = new Color(200, 200, 200, 200);

                particulaPositive = new Color(100, 200, 100);
                particulaNegative = new Color(200, 60, 60);
                particulaNeutre   = new Color(160, 160, 160);

                communauteHaloAlpha = 0.06f;

                panneauFond       = new Color(20, 20, 20, 220);
                panneauBordure    = new Color(60, 60, 60, 150);
                overlayFond       = new Color(0, 0, 0, 180);

                bienveillanceNegative = new Color(200, 30, 30);
                bienveillanceTresNeg  = new Color(200, 100, 50);
                bienveillanceNeutre   = new Color(130, 130, 130);
                bienveillanceTresPos  = new Color(80, 180, 100);
                bienveillancePositive = new Color(60, 200, 80);

                palettesCommunautes = new Color[]{
                    new Color(80,  130, 200),
                    new Color(200, 80,  80),
                    new Color(80,  180, 100),
                    new Color(200, 170, 50),
                    new Color(150, 80,  200),
                    new Color(200, 120, 50),
                    new Color(60,  180, 170),
                    new Color(200, 80,  160),
                    new Color(120, 200, 80),
                    new Color(80,  150, 200),
                    new Color(200, 150, 200),
                    new Color(160, 200, 160),
                };
            }
            default -> { // LIGHT
                fond              = new Color(245, 247, 252);
                fondGradientHaut  = new Color(240, 244, 252);
                fondGradientBas   = new Color(250, 250, 255);
                grille            = new Color(200, 210, 230, 80);
                texteUI           = new Color(30, 40, 80);
                texteSombre       = new Color(100, 110, 140);

                noeudDefaut       = new Color(60, 100, 220);
                noeudSurvol       = new Color(40, 80, 200);
                noeudSelectionne  = new Color(230, 160, 20);
                noeudBordure      = new Color(80, 120, 220, 200);
                noeudEtiquette    = new Color(20, 30, 80);
                glowDefaut        = new Color(100, 140, 255, 40);

                areteDefaut       = new Color(150, 170, 220, 120);
                areteSurvol       = new Color(80, 120, 220, 200);
                areteActive       = new Color(60, 100, 200, 220);

                particulaPositive = new Color(40, 180, 80);
                particulaNegative = new Color(200, 50, 50);
                particulaNeutre   = new Color(100, 120, 200);

                communauteHaloAlpha = 0.10f;

                panneauFond       = new Color(240, 244, 255, 230);
                panneauBordure    = new Color(160, 180, 230, 150);
                overlayFond       = new Color(255, 255, 255, 180);

                bienveillanceNegative = new Color(200, 30, 30);
                bienveillanceTresNeg  = new Color(210, 100, 40);
                bienveillanceNeutre   = new Color(120, 130, 180);
                bienveillanceTresPos  = new Color(40, 160, 80);
                bienveillancePositive = new Color(30, 180, 70);

                palettesCommunautes = new Color[]{
                    new Color(60,  120, 230),
                    new Color(220, 60,  60),
                    new Color(40,  180, 80),
                    new Color(230, 180, 20),
                    new Color(160, 60,  220),
                    new Color(230, 120, 30),
                    new Color(30,  180, 180),
                    new Color(220, 60,  160),
                    new Color(100, 200, 60),
                    new Color(60,  160, 220),
                    new Color(200, 120, 220),
                    new Color(120, 200, 120),
                };
            }
        }

        // Polices communes (indépendantes du thème)
        policeEtiquette = new Font("Segoe UI", Font.BOLD, 11);
        policeTitre     = new Font("Segoe UI", Font.BOLD, 14);
        policeStats     = new Font("Consolas", Font.PLAIN, 11);
        policeTooltip   = new Font("Segoe UI", Font.PLAIN, 12);

        // Constantes de rendu communes
        rayonNoeud         = 22;
        rayonGlow          = 40;
        epaisseurArete     = 1.2f;
        epaisseurAreteMax  = 5.0f;
        tailleParticule    = 8;
        rayonPulse         = 50;
        vitesseParticule   = 0.018f;
    }

    // =========================================================
    //  MÉTHODES UTILITAIRES
    // =========================================================

    /**
     * Calcule la couleur correspondant à une valeur de bienveillance [-10, +10].
     */
    public Color couleurBienveillance(float bienveillance) {
        float norm = bienveillance / 10f; // [-1, +1]

        if (norm <= -0.5f) {
            // Interpolation rouge intense → orange
            float t = (norm + 1f) / 0.5f; // [0, 1]
            return interpoler(bienveillanceNegative, bienveillanceTresNeg, t);
        } else if (norm <= 0f) {
            // Interpolation orange → neutre
            float t = (norm + 0.5f) / 0.5f;
            return interpoler(bienveillanceTresNeg, bienveillanceNeutre, t);
        } else if (norm <= 0.5f) {
            // Interpolation neutre → vert clair
            float t = norm / 0.5f;
            return interpoler(bienveillanceNeutre, bienveillanceTresPos, t);
        } else {
            // Interpolation vert clair → vert vif
            float t = (norm - 0.5f) / 0.5f;
            return interpoler(bienveillanceTresPos, bienveillancePositive, t);
        }
    }

    /**
     * Retourne la couleur d'une communauté par son index.
     */
    public Color couleurCommunaute(int index) {
        return palettesCommunautes[index % palettesCommunautes.length];
    }

    /**
     * Retourne une couleur avec une transparence modifiée.
     */
    public static Color avecAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /**
     * Interpolation linéaire entre deux couleurs.
     * @param t paramètre [0, 1]
     */
    public static Color interpoler(Color c1, Color c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int)(c1.getRed()   + t * (c2.getRed()   - c1.getRed()));
        int g = (int)(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int)(c1.getBlue()  + t * (c2.getBlue()  - c1.getBlue()));
        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b))
        );
    }

    /**
     * Éclaircit une couleur d'un facteur [0, 1].
     */
    public static Color eclaircir(Color c, float facteur) {
        int r = Math.min(255, (int)(c.getRed()   + facteur * (255 - c.getRed())));
        int g = Math.min(255, (int)(c.getGreen() + facteur * (255 - c.getGreen())));
        int b = Math.min(255, (int)(c.getBlue()  + facteur * (255 - c.getBlue())));
        return new Color(r, g, b);
    }

    /**
     * Assombrit une couleur d'un facteur [0, 1].
     */
    public static Color assombrir(Color c, float facteur) {
        int r = (int)(c.getRed()   * (1f - facteur));
        int g = (int)(c.getGreen() * (1f - facteur));
        int b = (int)(c.getBlue()  * (1f - facteur));
        return new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }
}
