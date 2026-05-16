package socialnetwork.back.simulation;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Moteur de simulation automatique du réseau social.
 *
 * Gère le déroulement temporel de la simulation :
 *  - Génère automatiquement des messages à intervalles configurables
 *  - Sélectionne les bavards selon différentes stratégies (aléatoire, hub, influence)
 *  - Enregistre l'historique des événements de propagation
 *  - Notifie les auditeurs à chaque tick pour mise à jour de l'interface
 *  - Supporte pause, reprise, vitesse variable, et scénarios prédéfinis
 *
 * Utilise {@link javax.swing.Timer} pour rester thread-safe avec Swing.
 */
public class MoteurSimulation {

    // =========================================================
    //  STRATÉGIES DE SÉLECTION DE L'ÉMETTEUR
    // =========================================================

    /** Stratégie de sélection du prochain émetteur. */
    public enum StrategieEmission {
        /** Sélection uniforme aléatoire. */
        ALEATOIRE,
        /** Préférence pour les bavards avec beaucoup de connexions (hubs). */
        PREFERENTIEL_HUB,
        /** Préférence pour les bavards qui ont peu émis récemment (équité). */
        ROBIN_ROUND,
        /** Toujours le même bavard (simule un influenceur unique). */
        SOURCE_FIXE
    }

    // =========================================================
    //  BANQUE DE MESSAGES
    // =========================================================

    /** Messages positifs pour les simulations. */
    private static final String[] MESSAGES_POSITIFS = {
        "Superbe journée aujourd'hui, le soleil brille !",
        "J'ai enfin trouvé ma passion, tout est possible !",
        "Solidarité et bienveillance, construisons ensemble !",
        "Une bonne nouvelle : le projet a été approuvé !",
        "Merci à tous pour votre soutien incroyable !",
        "Le succès vient de la persévérance et de l'entraide !",
        "Découverte scientifique majeure : espoir pour demain !",
        "Ensemble nous sommes plus forts que nos différences.",
        "Un enfant a sauvé un chaton aujourd'hui, adorable !",
        "La nature reprend ses droits, les rivières sont propres !",
        "Festival de musique ce soir, venez nombreux !",
        "Nos bénévoles ont nourri 500 personnes ce week-end !"
    };

    /** Messages négatifs pour les simulations. */
    private static final String[] MESSAGES_NEGATIFS = {
        "Attention : nouvelle arnaque en circulation, soyez prudents !",
        "Les prix augmentent encore, la vie devient impossible.",
        "Scandale politique : des millions détournés.",
        "Alerte : virus informatique qui vole vos données.",
        "Le chômage bat des records, l'économie s'effondre.",
        "Catastrophe naturelle : des milliers de sans-abri.",
        "Fake news dangereuse circule sur les réseaux.",
        "Conflit international, tensions géopolitiques graves.",
        "Crise sanitaire : hôpitaux débordés, urgence nationale.",
        "Pollution record, l'air est irrespirable en ville."
    };

    /** Messages neutres / informatifs. */
    private static final String[] MESSAGES_NEUTRES = {
        "Réunion demain à 10h, ordre du jour en pièce jointe.",
        "Rappel : inscription avant vendredi pour l'événement.",
        "Mise à jour disponible pour l'application.",
        "Le réseau sera en maintenance ce soir de 22h à 2h.",
        "Nouveau café ouvert rue de la Paix, premiers arrivés servis.",
        "Sondage en ligne, votre avis compte !",
        "Formation disponible sur la plateforme dès aujourd'hui.",
        "Compte-rendu de la réunion du 15 disponible.",
        "Changement d'horaire pour la conférence de jeudi.",
        "Statistiques du mois publiées sur le tableau de bord."
    };

    /** Messages de rumeurs / fake news (pour simuler la désinformation). */
    private static final String[] MESSAGES_RUMEURS = {
        "URGENT : le gouvernement cache la vérité sur ce vaccin !",
        "Exclusif : les médias ne veulent pas que vous sachiez ça !",
        "Révélation choc : les élites contrôlent nos données !",
        "Breaking : découverte que les autorités tentent d'étouffer !",
        "Partagez avant que ce soit censuré, la vérité éclate !",
        "Ce que vous ne verrez jamais dans les médias officiels...",
        "Alerte virale : remède miracle interdit par les labos pharma !",
        "Complot révélé : agenda caché derrière les nouvelles lois !"
    };

    // =========================================================
    //  ÉTAT DU MOTEUR
    // =========================================================

    private Reseau reseau;
    private final Timer timer;
    private int tick = 0;
    private boolean enCours = false;

    private StrategieEmission strategie = StrategieEmission.ALEATOIRE;
    private Bavard sourceFixe = null;
    private int indexRobinRound = 0;

    private double proportionPositif  = 0.5;
    private double proportionNegatif  = 0.3;
    private double proportionRumeur   = 0.1;
    // Le reste (0.1) sera des messages neutres

    private final Random rng = new Random();

    /** Historique des événements (thread-safe). */
    private final List<EvenementPropagation> historique = new CopyOnWriteArrayList<>();

    /** Taille maximale de l'historique conservé en mémoire. */
    private static final int TAILLE_MAX_HISTORIQUE = 5_000;

    /** Auditeurs notifiés à chaque tick (pour mise à jour de l'interface). */
    private final List<Consumer<Integer>> auditeursTick = new ArrayList<>();

    /** Auditeurs notifiés quand un événement de propagation est enregistré. */
    private final List<Consumer<EvenementPropagation>> auditeursEvenement = new ArrayList<>();

    /** Intervalle de base en ms entre deux ticks (400ms = ~2.5 ticks/sec). */
    private int intervalleMs = 400;

    // =========================================================
    //  CONSTRUCTEUR
    // =========================================================

    /**
     * Crée le moteur de simulation.
     *
     * @param reseau le réseau social à simuler
     */
    public MoteurSimulation(Reseau reseau) {
        this.reseau = reseau;
        this.timer  = new Timer(intervalleMs, e -> executerTick());
        this.timer.setCoalesce(true);
    }

    // =========================================================
    //  CONTRÔLE DE LA SIMULATION
    // =========================================================

    /** Démarre (ou reprend) la simulation. */
    public void demarrer() {
        if (!enCours && reseau != null && !reseau.getBavards().isEmpty()) {
            enCours = true;
            timer.start();
        }
    }

    /** Met la simulation en pause. */
    public void pause() {
        enCours = false;
        timer.stop();
    }

    /** Arrête la simulation et réinitialise le tick. */
    public void arreter() {
        enCours = false;
        timer.stop();
        tick = 0;
        historique.clear();
        indexRobinRound = 0;
    }

    /** Exécute un seul tick manuellement (mode pas à pas). */
    public void pasAPas() {
        if (reseau != null && !reseau.getBavards().isEmpty()) {
            executerTick();
        }
    }

    /** Change la vitesse de simulation. */
    public void setVitesse(int ticksParSeconde) {
        ticksParSeconde = Math.max(1, Math.min(20, ticksParSeconde));
        intervalleMs = 1000 / ticksParSeconde;
        timer.setDelay(intervalleMs);
    }

    /** Retourne la vitesse courante en ticks/seconde. */
    public int getVitesse() {
        return 1000 / intervalleMs;
    }

    // =========================================================
    //  CŒUR DE LA SIMULATION
    // =========================================================

    /**
     * Exécute un tick de simulation :
     * 1. Sélectionne un émetteur selon la stratégie
     * 2. Sélectionne un message selon les proportions configurées
     * 3. L'émetteur envoie le message
     * 4. Notifie les auditeurs
     */
    private void executerTick() {
        tick++;

        List<Bavard> bavards = reseau.getBavards();
        if (bavards.isEmpty()) {
            timer.stop();
            return;
        }

        // --- Sélection de l'émetteur ---
        Bavard emetteur = selectionnerEmetteur(bavards);
        if (emetteur == null) return;

        // --- Sélection du message ---
        String message = selectionnerMessage();

        // --- Émission ---
        emetteur.envoyerMessage(message);

        // --- Notification des auditeurs ---
        int tickActuel = this.tick;
        for (Consumer<Integer> auditeur : auditeursTick) {
            try { auditeur.accept(tickActuel); }
            catch (Exception ignored) {}
        }
    }

    /**
     * Sélectionne l'émetteur selon la stratégie courante.
     */
    private Bavard selectionnerEmetteur(List<Bavard> bavards) {
        return switch (strategie) {
            case ALEATOIRE         -> bavards.get(rng.nextInt(bavards.size()));
            case PREFERENTIEL_HUB  -> selectionnerHub(bavards);
            case ROBIN_ROUND       -> selectionnerRobinRound(bavards);
            case SOURCE_FIXE       -> (sourceFixe != null && bavards.contains(sourceFixe))
                                       ? sourceFixe
                                       : bavards.get(0);
        };
    }

    /**
     * Sélectionne le bavard avec le plus de connexions (probabilistique).
     * Probabilité ∝ degré du bavard.
     */
    private Bavard selectionnerHub(List<Bavard> bavards) {
        int totalDegres = bavards.stream().mapToInt(b -> b.getAmis().size()).sum();
        if (totalDegres == 0) return bavards.get(rng.nextInt(bavards.size()));

        int tirage = rng.nextInt(totalDegres);
        int cumul  = 0;
        for (Bavard b : bavards) {
            cumul += b.getAmis().size();
            if (tirage < cumul) return b;
        }
        return bavards.get(bavards.size() - 1);
    }

    /**
     * Sélectionne les bavards en rotation (chacun son tour).
     */
    private Bavard selectionnerRobinRound(List<Bavard> bavards) {
        if (indexRobinRound >= bavards.size()) indexRobinRound = 0;
        return bavards.get(indexRobinRound++);
    }

    /**
     * Sélectionne un message selon les proportions configurées.
     */
    private String selectionnerMessage() {
        double rand = rng.nextDouble();

        if (rand < proportionPositif) {
            return MESSAGES_POSITIFS[rng.nextInt(MESSAGES_POSITIFS.length)];
        } else if (rand < proportionPositif + proportionNegatif) {
            return MESSAGES_NEGATIFS[rng.nextInt(MESSAGES_NEGATIFS.length)];
        } else if (rand < proportionPositif + proportionNegatif + proportionRumeur) {
            return MESSAGES_RUMEURS[rng.nextInt(MESSAGES_RUMEURS.length)];
        } else {
            return MESSAGES_NEUTRES[rng.nextInt(MESSAGES_NEUTRES.length)];
        }
    }

    // =========================================================
    //  SCÉNARIOS PRÉDÉFINIS
    // =========================================================

    /** Configure la simulation pour un scénario de fake news / désinformation. */
    public void configurerScenarioFakeNews() {
        proportionPositif = 0.1;
        proportionNegatif = 0.2;
        proportionRumeur  = 0.6;
        strategie         = StrategieEmission.PREFERENTIEL_HUB;
    }

    /** Configure la simulation pour un scénario de contagion émotionnelle positive. */
    public void configurerScenarioContagionPositive() {
        proportionPositif = 0.8;
        proportionNegatif = 0.05;
        proportionRumeur  = 0.0;
        strategie         = StrategieEmission.ALEATOIRE;
    }

    /** Configure la simulation pour un scénario d'opinion polarisée. */
    public void configurerScenarioPolarisation() {
        proportionPositif = 0.4;
        proportionNegatif = 0.4;
        proportionRumeur  = 0.15;
        strategie         = StrategieEmission.ALEATOIRE;
    }

    /** Configure la simulation pour un scénario d'influenceur dominant. */
    public void configurerScenarioInfluenceur(Bavard influenceur) {
        this.sourceFixe   = influenceur;
        proportionPositif = 0.6;
        proportionNegatif = 0.1;
        proportionRumeur  = 0.2;
        strategie         = StrategieEmission.SOURCE_FIXE;
    }

    /** Configure une simulation équilibrée et neutre. */
    public void configurerScenarioNeutre() {
        proportionPositif = 0.4;
        proportionNegatif = 0.2;
        proportionRumeur  = 0.05;
        strategie         = StrategieEmission.ROBIN_ROUND;
    }

    // =========================================================
    //  ENREGISTREMENT D'ÉVÉNEMENTS
    // =========================================================

    /**
     * Enregistre un événement de propagation dans l'historique.
     * Appelé typiquement par des observateurs externes.
     */
    public void enregistrerEvenement(EvenementPropagation evt) {
        if (historique.size() >= TAILLE_MAX_HISTORIQUE) {
            // On garde les plus récents
            historique.remove(0);
        }
        historique.add(evt);

        for (Consumer<EvenementPropagation> auditeur : auditeursEvenement) {
            try { auditeur.accept(evt); }
            catch (Exception ignored) {}
        }
    }

    // =========================================================
    //  AUDITEURS
    // =========================================================

    /** Ajoute un auditeur notifié à chaque tick (reçoit le numéro de tick). */
    public void ajouterAuditeurTick(Consumer<Integer> auditeur) {
        if (auditeur != null) auditeursTick.add(auditeur);
    }

    /** Retire un auditeur de tick. */
    public void retirerAuditeurTick(Consumer<Integer> auditeur) {
        auditeursTick.remove(auditeur);
    }

    /** Ajoute un auditeur notifié pour chaque événement de propagation. */
    public void ajouterAuditeurEvenement(Consumer<EvenementPropagation> auditeur) {
        if (auditeur != null) auditeursEvenement.add(auditeur);
    }

    // =========================================================
    //  ACCESSEURS
    // =========================================================

    public Reseau  getReseau()           { return reseau; }
    public int     getTick()             { return tick; }
    public boolean isEnCours()           { return enCours; }
    public int     getIntervalleMs()     { return intervalleMs; }
    public List<EvenementPropagation> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    /** Change le réseau simulé (remet le tick à zéro). */
    public void setReseau(Reseau reseau) {
        pause();
        this.reseau = reseau;
        tick = 0;
        historique.clear();
        indexRobinRound = 0;
    }

    public void setStrategie(StrategieEmission s) { this.strategie = s; }
    public StrategieEmission getStrategie()        { return strategie; }
    public void setSourceFixe(Bavard b)            { this.sourceFixe = b; }
    public Bavard getSourceFixe()                  { return sourceFixe; }

    /** Configure les proportions de types de messages. Doit totaliser ≤ 1.0. */
    public void setProportions(double positif, double negatif, double rumeur) {
        double total = positif + negatif + rumeur;
        if (total > 1.0) {
            double facteur = 1.0 / total;
            positif *= facteur;
            negatif *= facteur;
            rumeur  *= facteur;
        }
        this.proportionPositif = positif;
        this.proportionNegatif = negatif;
        this.proportionRumeur  = rumeur;
    }

    public double getProportionPositif() { return proportionPositif; }
    public double getProportionNegatif() { return proportionNegatif; }
    public double getProportionRumeur()  { return proportionRumeur; }

    /** Retourne des statistiques textuelles du moteur. */
    public String getResume() {
        return String.format(
            "Moteur [tick=%d | %s | vitesse=%d/s | stratégie=%s | historique=%d événements]",
            tick,
            enCours ? "EN COURS" : "ARRÊTÉ",
            getVitesse(),
            strategie.name(),
            historique.size()
        );
    }

    @Override
    public String toString() {
        return getResume();
    }
}
