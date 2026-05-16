package socialnetwork.back.simulation;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.MessageEvent;

import java.time.Instant;
import java.util.List;

/**
 * Enregistrement immuable d'un événement de propagation dans la simulation.
 *
 * Capture tout ce qui s'est passé lors de la transmission d'un message :
 * qui l'a envoyé, qui l'a reçu, à quel moment, avec quelle bienveillance,
 * et quel chemin a été parcouru jusqu'ici.
 *
 * Utilisé par le {@link MoteurSimulation} pour construire l'historique
 * de la simulation et permettre des analyses post-hoc.
 */
public class EvenementPropagation {

    // =========================================================
    //  TYPES D'ÉVÉNEMENTS
    // =========================================================

    /** Type de l'événement de propagation. */
    public enum Type {
        /** Un bavard envoie un message pour la première fois. */
        EMISSION,
        /** Un bavard reçoit et retransmet un message. */
        PROPAGATION,
        /** Un bavard reçoit mais bloque la retransmission (seuil ou probabilité). */
        BLOCAGE,
        /** Un bavard a déjà vu ce message (cycle détecté). */
        DOUBLON,
        /** Un bavard marque la fin d'une vague de propagation. */
        FIN_VAGUE
    }

    // =========================================================
    //  DONNÉES DE L'ÉVÉNEMENT
    // =========================================================

    /** Type de cet événement. */
    private final Type type;

    /** Timestamp (heure réelle de simulation). */
    private final Instant timestamp;

    /** Tick logique de la simulation au moment de l'événement. */
    private final int tick;

    /** Le bavard source (émetteur direct dans cette étape). */
    private final String nomSource;

    /** Le bavard destination (receveur). */
    private final String nomDestination;

    /** L'identifiant du message propagé. */
    private final String idMessage;

    /** Contenu du message. */
    private final String contenuMessage;

    /** Bienveillance du message AVANT transformation par la destination. */
    private final float bienveillanceEntree;

    /** Bienveillance du message APRÈS transformation par la destination. */
    private final float bienveillanceSortie;

    /** Chemin complet du message au moment de l'événement. */
    private final List<String> chemin;

    /** Hop actuel dans la propagation. */
    private final int hop;

    /** Cause du blocage (si type == BLOCAGE), null sinon. */
    private final String causeBlocage;

    // =========================================================
    //  CONSTRUCTEUR COMPLET
    // =========================================================

    /**
     * Constructeur principal utilisé par le moteur de simulation.
     */
    public EvenementPropagation(Type type, int tick, String nomSource, String nomDestination,
                                 MessageEvent event, float bienveillanceSortie, String causeBlocage) {
        this.type               = type;
        this.timestamp          = Instant.now();
        this.tick               = tick;
        this.nomSource          = nomSource != null ? nomSource : "?";
        this.nomDestination     = nomDestination != null ? nomDestination : "?";
        this.idMessage          = event != null ? event.getId().toString() : "?";
        this.contenuMessage     = event != null ? event.getContenu() : "";
        this.bienveillanceEntree = event != null ? event.getBienveillance() : 0f;
        this.bienveillanceSortie = bienveillanceSortie;
        this.chemin             = event != null ? event.getChemin() : List.of();
        this.hop                = event != null ? event.getHopActuel() : 0;
        this.causeBlocage       = causeBlocage;
    }

    /** Constructeur simplifié pour les événements d'émission. */
    public static EvenementPropagation emission(int tick, Bavard emetteur, MessageEvent event) {
        return new EvenementPropagation(
                Type.EMISSION, tick,
                null, emetteur.getNom(),
                event, event.getBienveillance(),
                null
        );
    }

    /** Constructeur simplifié pour une propagation normale. */
    public static EvenementPropagation propagation(int tick, String source, String dest,
                                                     MessageEvent event, float bienveillanceSortie) {
        return new EvenementPropagation(
                Type.PROPAGATION, tick, source, dest, event, bienveillanceSortie, null
        );
    }

    /** Constructeur simplifié pour un blocage. */
    public static EvenementPropagation blocage(int tick, String source, String dest,
                                                MessageEvent event, String cause) {
        return new EvenementPropagation(
                Type.BLOCAGE, tick, source, dest, event, event.getBienveillance(), cause
        );
    }

    // =========================================================
    //  GETTERS
    // =========================================================

    public Type             getType()                  { return type; }
    public Instant          getTimestamp()             { return timestamp; }
    public int              getTick()                  { return tick; }
    public String           getNomSource()             { return nomSource; }
    public String           getNomDestination()        { return nomDestination; }
    public String           getIdMessage()             { return idMessage; }
    public String           getContenuMessage()        { return contenuMessage; }
    public float            getBienveillanceEntree()   { return bienveillanceEntree; }
    public float            getBienveillanceSortie()   { return bienveillanceSortie; }
    public List<String>     getChemin()                { return chemin; }
    public int              getHop()                   { return hop; }
    public String           getCauseBlocage()          { return causeBlocage; }

    /** Variation de bienveillance produite par la transformation. */
    public float getDeltaBienveillance() {
        return bienveillanceSortie - bienveillanceEntree;
    }

    /** Vrai si cet événement est une propagation réussie (pas un blocage). */
    public boolean estPropagationReussie() {
        return type == Type.EMISSION || type == Type.PROPAGATION;
    }

    // =========================================================
    //  AFFICHAGE
    // =========================================================

    @Override
    public String toString() {
        String fleche = nomSource.equals("?") ? "" : nomSource + " → ";
        String cheminStr = chemin.isEmpty() ? "" : " [" + String.join("→", chemin) + "]";
        String blocStr   = (causeBlocage != null) ? " ⛔ " + causeBlocage : "";

        return String.format("[tick=%d | %s] %s%s : \"%s\" b=%.1f→%.1f hop=%d%s%s",
                tick, type.name(),
                fleche, nomDestination,
                contenuMessage.length() > 30 ? contenuMessage.substring(0, 30) + "…" : contenuMessage,
                bienveillanceEntree, bienveillanceSortie,
                hop, cheminStr, blocStr);
    }
}
