package socialnetwork.back.monde;

import java.util.Random;

/**
 * Les 20 Traits de Caractère d'un bavard.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  DESIGN : IMPLÉMENTATION ALGORITHMIQUE
 * ══════════════════════════════════════════════════════════════════════
 *
 * Chaque trait est un objet comportemental attaché à un bavard.
 * Il modifie trois aspects de la communication :
 *
 *  1. BIENVEILLANCE : transforme le score de bienveillance d'un message
 *     entrant avant que le bavard ne le retransmette.
 *
 *  2. PROPAGATION : modifie la probabilité/proportion de retransmission
 *     (facteur multiplicateur sur la liste d'amis cibles).
 *
 *  3. DÉLAI : ajoute un délai simulé avant la retransmission (en ms).
 *
 *  4. FILTRE : peut bloquer certains messages (renvoie false pour bloquer).
 *
 *  5. TRANSFORMATION : peut modifier le contenu textuel du message.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  LES 20 TRAITS
 * ══════════════════════════════════════════════════════════════════════
 *
 *   1. OPTIMISTE         → +2.0 bien., propagation 0.85, délai 0
 *   2. PESSIMISTE        → −2.5 bien., propagation 0.55, délai 500ms
 *   3. LEADER            → ×1.3 bien., propagation 1.00 (tous les amis), délai 0
 *   4. TIMIDE            → ×0.6 bien., propagation 0.20, délai 2000ms
 *   5. EMPATHIQUE        → amplifie l'émotion (×1.5 si |bienveillance|>2), propagation 0.90
 *   6. RATIONNEL         → pousse vers 0 (bienveillance/2 + 0.5), propagation 0.80
 *   7. IMPULSIF          → ×1.8 bien., propagation 1.00, délai -1000 (plus rapide)
 *   8. PRUDENT           → filtre si bienveillance < -3, propagation 0.65, délai 3000ms
 *   9. CRÉATIF           → ajoute un twist aléatoire ±3, propagation 0.75
 *  10. CONVENTIONNEL     → pas de modification, propagation 0.70
 *  11. GÉNÉREUX          → +3.0 bien., propagation 0.95
 *  12. AVARE             → −1.0 bien., propagation 0.40
 *  13. SOCIABLE          → ×1.2 bien., propagation 1.00, délai 0, réseau élargi
 *  14. SOLITAIRE         → propagation 0.10, délai 5000ms
 *  15. SENSIBLE          → si bien. > 0 : ×2.0 ; si bien. < 0 : ×2.5 (amplifie tout)
 *  16. STOÏQUE           → bien. → bien./4 (neutralise les extrêmes), propagation 0.60
 *  17. OPPORTUNISTE      → si bien. > 0 : +2 ; si bien. < 0 : bloque
 *  18. ALTRUISTE         → +4.0 bien., propagation 1.00, transforme les négatifs en positifs
 *  19. MANIPULATEUR      → 25% de chance d'inverser la polarité, propagation 0.90
 *  20. CHARISMATIQUE     → ×2.0 bien., propagation 1.00 + inspire les autres à ×1.3
 */
public enum TraitCaractere {

    // ─── 1. OPTIMISTE ────────────────────────────────────────────────────────
    OPTIMISTE(
        "Optimiste",
        "😊",
        "Voit le positif en tout. Rehausse la tonalité de tous les messages transmis.",
        2.0f,    // delta bienveillance de base
        0.0f,    // facteur multiplicatif (0 = additive seulement)
        0.85f,   // taux de propagation [0, 1]
        0L,      // délai ms simulées
        false    // filtre (false = aucun filtre spécial)
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b + 2.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // transmet tout
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            // Ajoute parfois un emoji positif
            if (rng.nextFloat() < 0.3f) {
                String[] emojis = {"😊", "✨", "👍", "💪", "🌟"};
                return contenu + " " + emojis[rng.nextInt(emojis.length)];
            }
            return contenu;
        }
    },

    // ─── 2. PESSIMISTE ───────────────────────────────────────────────────────
    PESSIMISTE(
        "Pessimiste",
        "😞",
        "S'attend toujours au pire. Assombrit les messages et tarde à les transmettre.",
        -2.5f, 0.0f, 0.55f, 500L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b - 2.5f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Moins enclin à transmettre les bonnes nouvelles
            if (b > 3.0f && rng.nextFloat() < 0.4f) return false;
            return true;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (b > 0 && rng.nextFloat() < 0.5f) {
                String[] prefixes = {"Mouais, mais... ", "Ça durera pas : ", "J'y crois pas trop : "};
                return prefixes[rng.nextInt(prefixes.length)] + contenu;
            }
            return contenu;
        }
    },

    // ─── 3. LEADER ───────────────────────────────────────────────────────────
    LEADER(
        "Leader",
        "👑",
        "Influence naturellement son entourage. Amplifie les messages et les transmet à tous.",
        0.0f, 1.3f, 1.00f, 0L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 1.3f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // le leader transmet tout
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.25f) {
                return "Important : " + contenu;
            }
            return contenu;
        }
    },

    // ─── 4. TIMIDE ───────────────────────────────────────────────────────────
    TIMIDE(
        "Timide",
        "🫣",
        "Réticent à communiquer. Peu de messages transmis, toujours avec un délai.",
        0.0f, 0.6f, 0.20f, 2000L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 0.6f; // minimise l'expression émotionnelle
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Ne retransmet que si vraiment nécessaire
            return rng.nextFloat() < 0.20f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.4f) {
                String[] hedges = {"Euh... ", "Je sais pas si... ", "Peut-être que... "};
                return hedges[rng.nextInt(hedges.length)] + contenu;
            }
            return contenu;
        }
    },

    // ─── 5. EMPATHIQUE ───────────────────────────────────────────────────────
    EMPATHIQUE(
        "Empathique",
        "🫂",
        "Ressent et amplifie les émotions des autres. Plus réactif aux messages forts.",
        0.0f, 0.0f, 0.90f, 200L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            float abs = Math.abs(b);
            if (abs > 2.0f) {
                return b * 1.5f; // amplifie les messages forts
            } else {
                return b * 1.1f; // légèrement plus intense
            }
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Transmet surtout si l'émotion est forte
            float abs = Math.abs(b);
            if (abs > 3.0f) return true;
            return rng.nextFloat() < 0.90f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (b > 3.0f) return "❤️ " + contenu;
            if (b < -3.0f) return "💔 " + contenu;
            return contenu;
        }
    },

    // ─── 6. RATIONNEL ────────────────────────────────────────────────────────
    RATIONNEL(
        "Rationnel",
        "🧠",
        "Analyse avant d'agir. Neutralise les extrêmes et transmet avec modération.",
        0.0f, 0.0f, 0.80f, 1500L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            // Pousse vers la neutralité
            return b * 0.5f + 0.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Bloque les messages trop extrêmes
            if (Math.abs(b) > 8.0f && rng.nextFloat() < 0.5f) return false;
            return true;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.2f) {
                return "D'un point de vue objectif : " + contenu;
            }
            return contenu;
        }
    },

    // ─── 7. IMPULSIF ─────────────────────────────────────────────────────────
    IMPULSIF(
        "Impulsif",
        "💥",
        "Réagit sans réfléchir. Amplifie fortement et retransmet immédiatement.",
        0.0f, 1.8f, 1.00f, 0L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 1.8f; // amplifie beaucoup
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // retransmet toujours
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            String[] interjections = {"INCROYABLE ! ", "C'est DINGUE ! ", "WOW ! "};
            if (Math.abs(b) > 2.0f && rng.nextFloat() < 0.5f) {
                return interjections[rng.nextInt(interjections.length)] + contenu;
            }
            return contenu;
        }
    },

    // ─── 8. PRUDENT ──────────────────────────────────────────────────────────
    PRUDENT(
        "Prudent",
        "🛡️",
        "Filtre les messages négatifs. Prend le temps de vérifier avant de transmettre.",
        0.0f, 0.0f, 0.65f, 3000L, true
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            // Atténue légèrement les extrêmes
            if (b < 0) return b * 0.7f;
            return b;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Bloque les messages très négatifs
            if (b < -3.0f) return rng.nextFloat() < 0.2f;
            if (b < -1.0f) return rng.nextFloat() < 0.5f;
            return rng.nextFloat() < 0.65f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.15f) {
                return "À vérifier : " + contenu;
            }
            return contenu;
        }
    },

    // ─── 9. CRÉATIF ──────────────────────────────────────────────────────────
    CREATIF(
        "Créatif",
        "🎨",
        "Réinterprète les messages à sa façon. Ajoute un twist inattendu.",
        0.0f, 0.0f, 0.75f, 800L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            // Twist aléatoire ±3
            float twist = (rng.nextFloat() - 0.5f) * 6.0f;
            return b + twist;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return rng.nextFloat() < 0.75f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            // Reformule avec une métaphore ou un angle créatif
            String[] ouvertures = {
                "Comme dirait un artiste : ", "En version poétique : ",
                "Vu sous un autre angle : ", "Et si on disait : "
            };
            if (rng.nextFloat() < 0.35f) {
                return ouvertures[rng.nextInt(ouvertures.length)] + contenu;
            }
            return contenu;
        }
    },

    // ─── 10. CONVENTIONNEL ───────────────────────────────────────────────────
    CONVENTIONNEL(
        "Conventionnel",
        "📋",
        "Ne change rien. Transmet les messages tels quels avec une probabilité modérée.",
        0.0f, 0.0f, 0.70f, 1000L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b; // aucune modification
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return rng.nextFloat() < 0.70f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            return contenu; // aucune modification
        }
    },

    // ─── 11. GÉNÉREUX ────────────────────────────────────────────────────────
    GENEREUX(
        "Généreux",
        "🎁",
        "Toujours prêt à partager et à aider. Booste la bienveillance de tous les messages.",
        3.0f, 0.0f, 0.95f, 0L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b + 3.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // très communicatif
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.2f) {
                return contenu + " (n'hésitez pas à partager !)";
            }
            return contenu;
        }
    },

    // ─── 12. AVARE ───────────────────────────────────────────────────────────
    AVARE(
        "Avare",
        "🤑",
        "Garde tout pour lui. Retransmet peu et dégrade les messages.",
        -1.0f, 0.0f, 0.40f, 2000L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b - 1.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Ne retransmet que rarement
            return rng.nextFloat() < 0.40f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.3f) {
                return contenu + " (à titre d'information uniquement)";
            }
            return contenu;
        }
    },

    // ─── 13. SOCIABLE ────────────────────────────────────────────────────────
    SOCIABLE(
        "Sociable",
        "🎉",
        "Adore les interactions. Amplifie et retransmet à tout son réseau (élargi).",
        0.0f, 1.2f, 1.00f, 0L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 1.2f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // toujours
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.3f) {
                return contenu + " — vous êtes tous invités à en discuter !";
            }
            return contenu;
        }
    },

    // ─── 14. SOLITAIRE ───────────────────────────────────────────────────────
    SOLITAIRE(
        "Solitaire",
        "🧍",
        "Préfère la solitude. Rarement en contact, long délai avant de répondre.",
        0.0f, 0.0f, 0.10f, 5000L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 0.8f; // légèrement atténué
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Très rarement
            return rng.nextFloat() < 0.10f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.5f) {
                return "FYI : " + contenu;
            }
            return contenu;
        }
    },

    // ─── 15. SENSIBLE ────────────────────────────────────────────────────────
    SENSIBLE(
        "Sensible",
        "🥺",
        "Ressent tout très fort. Amplifie les positifs ×2, les négatifs ×2.5.",
        0.0f, 0.0f, 0.80f, 300L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            if (b > 0) return b * 2.0f;
            if (b < 0) return b * 2.5f;
            return b;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Transmet avec émotion — plus souvent si message fort
            if (Math.abs(b) > 3.0f) return true;
            return rng.nextFloat() < 0.80f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (b > 4.0f)  return "Je suis tellement touché : " + contenu;
            if (b < -4.0f) return "Ça me brise le cœur : " + contenu;
            return contenu;
        }
    },

    // ─── 16. STOÏQUE ─────────────────────────────────────────────────────────
    STOIQUE(
        "Stoïque",
        "🗿",
        "Impassible face aux émotions. Neutralise fortement les extrêmes.",
        0.0f, 0.0f, 0.60f, 2000L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            // Réduit fortement les variations
            return b / 4.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return rng.nextFloat() < 0.60f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.2f) {
                return "[Info neutre] " + contenu;
            }
            return contenu;
        }
    },

    // ─── 17. OPPORTUNISTE ────────────────────────────────────────────────────
    OPPORTUNISTE(
        "Opportuniste",
        "🦊",
        "Ne partage que ce qui l'avantage. Booste le positif, bloque le négatif.",
        0.0f, 0.0f, 0.70f, 500L, true
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            if (b > 0) return b + 2.0f;     // amplifie les positifs
            return b * 0.5f;                 // atténue les négatifs
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            // Bloque les messages trop négatifs
            if (b < -2.0f) return rng.nextFloat() < 0.15f;
            return rng.nextFloat() < 0.70f;
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (b > 2.0f && rng.nextFloat() < 0.4f) {
                return contenu + " (j'ai des infos exclusives sur ce sujet !)";
            }
            return contenu;
        }
    },

    // ─── 18. ALTRUISTE ───────────────────────────────────────────────────────
    ALTRUISTE(
        "Altruiste",
        "🕊️",
        "Met les autres avant soi. Transforme les messages négatifs en appels à l'aide bienveillants.",
        4.0f, 0.0f, 1.00f, 100L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            if (b < 0) return Math.abs(b) * 0.5f + 1.0f;  // retourne les négatifs en aide
            return b + 4.0f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // retransmet toujours
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (b < -2.0f) {
                return "Quelqu'un a besoin d'aide : " + contenu;
            }
            if (rng.nextFloat() < 0.2f) {
                return "Partageons : " + contenu;
            }
            return contenu;
        }
    },

    // ─── 19. MANIPULATEUR ────────────────────────────────────────────────────
    MANIPULATEUR(
        "Manipulateur",
        "🎭",
        "Déforme les messages à son avantage. 25% de chance d'inverser la polarité.",
        0.0f, 0.0f, 0.90f, 200L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            // 25% de chance d'inverser
            if (rng.nextFloat() < 0.25f) {
                return -b * 1.5f;
            }
            // Sinon amplifie dans le sens actuel
            return b * 1.2f;
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return rng.nextFloat() < 0.90f; // presque toujours
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.3f) {
                String[] twists = {
                    "J'ai entendu que ",
                    "On m'a confié que ",
                    "Entre nous : ",
                    "Selon une source sûre : "
                };
                return twists[rng.nextInt(twists.length)] + contenu;
            }
            return contenu;
        }
    },

    // ─── 20. CHARISMATIQUE ───────────────────────────────────────────────────
    CHARISMATIQUE(
        "Charismatique",
        "✨",
        "Fascine son entourage. ×2 sur la bienveillance, transmet à tous, inspire les voisins.",
        0.0f, 2.0f, 1.00f, 0L, false
    ) {
        @Override
        public float transformerBienveillance(float b, String contenu, Random rng) {
            return b * 2.0f;  // double la bienveillance
        }
        @Override
        public boolean filtrePropagation(float b, String contenu, Random rng) {
            return true; // toujours
        }
        @Override
        public String transformerContenu(String contenu, float b, Random rng) {
            if (rng.nextFloat() < 0.3f) {
                return "✨ " + contenu + " ✨";
            }
            return contenu;
        }
    };

    // ─── Champs communs ──────────────────────────────────────────────────────

    /** Nom du trait. */
    public final String nom;

    /** Icône Unicode. */
    public final String icone;

    /** Description du comportement. */
    public final String description;

    /** Delta de bienveillance de base (peut être surchargé par transformerBienveillance). */
    public final float deltaBienveillanceBase;

    /** Facteur multiplicatif de bienveillance. */
    public final float facteurBienveillanceBase;

    /** Taux de propagation par défaut [0, 1]. */
    public final float tauxPropagation;

    /** Délai avant retransmission en ms simulées. */
    public final long delaiMs;

    /** True si ce trait a un filtre de contenu actif. */
    public final boolean aFiltreContenu;

    // ─── Constructeur ────────────────────────────────────────────────────────

    TraitCaractere(String nom, String icone, String description,
                   float deltaBienveillanceBase, float facteurBienveillanceBase,
                   float tauxPropagation, long delaiMs, boolean aFiltreContenu) {
        this.nom                    = nom;
        this.icone                  = icone;
        this.description            = description;
        this.deltaBienveillanceBase = deltaBienveillanceBase;
        this.facteurBienveillanceBase = facteurBienveillanceBase;
        this.tauxPropagation        = tauxPropagation;
        this.delaiMs                = delaiMs;
        this.aFiltreContenu         = aFiltreContenu;
    }

    // ─── Méthodes comportementales abstraites ────────────────────────────────

    /**
     * Transforme la bienveillance d'un message entrant.
     * @param b       bienveillance originale
     * @param contenu texte du message
     * @param rng     générateur aléatoire pour les effets probabilistes
     * @return nouvelle bienveillance
     */
    public abstract float transformerBienveillance(float b, String contenu, Random rng);

    /**
     * Décide si le message doit être propagé.
     * @return true → propager, false → bloquer
     */
    public abstract boolean filtrePropagation(float b, String contenu, Random rng);

    /**
     * Transforme le contenu textuel du message.
     * @return contenu modifié (peut être identique si le trait ne change rien)
     */
    public abstract String transformerContenu(String contenu, float b, Random rng);

    // ─── Méthodes utilitaires ────────────────────────────────────────────────

    /** Renvoie le délai en secondes simulées. */
    public float getDelaiSecondes() {
        return delaiMs / 1000.0f;
    }

    /**
     * Renvoie un trait aléatoire.
     * Pondéré : les traits "humains" ordinaires sont plus fréquents.
     */
    public static TraitCaractere aleatoire(Random rng) {
        // Probabilités de sélection (somme ~100)
        TraitCaractere[] ponderee = {
            OPTIMISTE, OPTIMISTE, OPTIMISTE,    // 3x → plus fréquent
            PESSIMISTE, PESSIMISTE,             // 2x
            LEADER,                             // 1x
            TIMIDE, TIMIDE,                     // 2x
            EMPATHIQUE, EMPATHIQUE,             // 2x
            RATIONNEL, RATIONNEL,               // 2x
            IMPULSIF, IMPULSIF,                 // 2x
            PRUDENT, PRUDENT,                   // 2x
            CREATIF,                            // 1x
            CONVENTIONNEL, CONVENTIONNEL, CONVENTIONNEL,  // 3x → très commun
            GENEREUX,                           // 1x
            AVARE,                              // 1x
            SOCIABLE, SOCIABLE,                 // 2x
            SOLITAIRE,                          // 1x
            SENSIBLE, SENSIBLE,                 // 2x
            STOIQUE,                            // 1x
            OPPORTUNISTE,                       // 1x
            ALTRUISTE,                          // 1x
            MANIPULATEUR,                       // 1x
            CHARISMATIQUE                       // 1x — rare
        };
        return ponderee[rng.nextInt(ponderee.length)];
    }

    @Override
    public String toString() {
        return icone + " " + nom;
    }
}
