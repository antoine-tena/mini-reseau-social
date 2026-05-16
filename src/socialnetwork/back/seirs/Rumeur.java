package socialnetwork.back.seirs;

import java.util.Objects;
import java.util.Random;
import java.util.SplittableRandom;

public final class Rumeur {

    private final String id;
    private final String titre;
    private final double impactBienveillance;
    private final double contagiosite;
    private final double dureeDeVie;
    private final ZoneManager.ZoneId zoneAttractive;
    private final ZoneManager.ZoneId zoneRepulsive;
    private final double intensiteAttraction;
    private final double intensiteRepulsion;
    private final int generation;

    public Rumeur(String id, String titre, double impactBienveillance, double contagiosite, double dureeDeVie) {
        this(id, titre, impactBienveillance, contagiosite, dureeDeVie, null, null, 0.0, 0.0, 0);
    }

    public Rumeur(
        String id,
        String titre,
        double impactBienveillance,
        double contagiosite,
        double dureeDeVie,
        ZoneManager.ZoneId zoneAttractive,
        ZoneManager.ZoneId zoneRepulsive,
        double intensiteAttraction,
        double intensiteRepulsion,
        int generation
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.titre = Objects.requireNonNull(titre, "titre");
        this.impactBienveillance = impactBienveillance;
        this.contagiosite = clamp(contagiosite, 0.01, 0.99);
        this.dureeDeVie = Math.max(0.25, dureeDeVie);
        this.zoneAttractive = zoneAttractive;
        this.zoneRepulsive = zoneRepulsive;
        this.intensiteAttraction = Math.max(0.0, intensiteAttraction);
        this.intensiteRepulsion = Math.max(0.0, intensiteRepulsion);
        this.generation = Math.max(0, generation);
    }

    public String getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public double getImpactBienveillance() {
        return impactBienveillance;
    }

    public double getContagiosite() {
        return contagiosite;
    }

    public double getDureeDeVie() {
        return dureeDeVie;
    }

    public ZoneManager.ZoneId getZoneAttractive() {
        return zoneAttractive;
    }

    public ZoneManager.ZoneId getZoneRepulsive() {
        return zoneRepulsive;
    }

    public double getIntensiteAttraction() {
        return intensiteAttraction;
    }

    public double getIntensiteRepulsion() {
        return intensiteRepulsion;
    }

    public int getGeneration() {
        return generation;
    }

    public boolean attireVers(ZoneManager.ZoneId zoneId) {
        return zoneAttractive == zoneId && intensiteAttraction > 0.0;
    }

    public boolean repousseDepuis(ZoneManager.ZoneId zoneId) {
        return zoneRepulsive == zoneId && intensiteRepulsion > 0.0;
    }

    public Rumeur mutate(Random random) {
        return mutateInternal(
            random.nextDouble(-0.18, 0.18),
            random.nextDouble(-0.08, 0.15),
            random.nextDouble(0.68, 0.92),
            mutateTitle(random)
        );
    }

    public Rumeur mutate(SplittableRandom random) {
        return mutateInternal(
            random.nextDouble(-0.18, 0.18),
            random.nextDouble(-0.08, 0.15),
            random.nextDouble(0.68, 0.92),
            mutateTitle(random)
        );
    }

    public Rumeur withLifetime(double remainingLifetime) {
        return new Rumeur(
            id,
            titre,
            impactBienveillance,
            contagiosite,
            remainingLifetime,
            zoneAttractive,
            zoneRepulsive,
            intensiteAttraction,
            intensiteRepulsion,
            generation
        );
    }

    public Rumeur withSpatialBias(ZoneManager.ZoneId attraction, ZoneManager.ZoneId repulsion,
                                  double attractionPower, double repulsionPower) {
        return new Rumeur(
            id,
            titre,
            impactBienveillance,
            contagiosite,
            dureeDeVie,
            attraction,
            repulsion,
            attractionPower,
            repulsionPower,
            generation
        );
    }

    private Rumeur mutateInternal(double deltaImpact, double deltaContagion, double lifeFactor, String titreMutant) {
        return new Rumeur(
            id,
            titreMutant,
            clamp(impactBienveillance + deltaImpact, -1.0, 1.0),
            clamp(contagiosite + deltaContagion, 0.02, 0.995),
            Math.max(0.15, dureeDeVie * lifeFactor),
            zoneAttractive,
            zoneRepulsive,
            intensiteAttraction,
            intensiteRepulsion,
            generation + 1
        );
    }

    private String mutateTitle(Random random) {
        return switch (random.nextInt(7)) {
            case 0 -> titre + " !!";
            case 1 -> titre.replace("!", " !!!");
            case 2 -> "[A confirmer] " + titre;
            case 3 -> titre + " Selon plusieurs témoins.";
            case 4 -> titre.replace("quelqu'un", "plusieurs personnes").replace("Quelqu'un", "Plusieurs personnes");
            case 5 -> titre + " Source locale non vérifiée.";
            default -> "URGENT - " + titre;
        };
    }

    private String mutateTitle(SplittableRandom random) {
        return switch (random.nextInt(7)) {
            case 0 -> titre + " !!";
            case 1 -> titre.replace("!", " !!!");
            case 2 -> "[A confirmer] " + titre;
            case 3 -> titre + " Selon plusieurs témoins.";
            case 4 -> titre.replace("quelqu'un", "plusieurs personnes").replace("Quelqu'un", "Plusieurs personnes");
            case 5 -> titre + " Source locale non vérifiée.";
            default -> "URGENT - " + titre;
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "Rumeur{" +
            "id='" + id + '\'' +
            ", titre='" + titre + '\'' +
            ", impactBienveillance=" + impactBienveillance +
            ", contagiosite=" + contagiosite +
            ", dureeDeVie=" + dureeDeVie +
            ", generation=" + generation +
            '}';
    }
}
