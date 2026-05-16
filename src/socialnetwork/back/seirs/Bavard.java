package socialnetwork.back.seirs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class Bavard {

    public enum EtatFSM {
        LOCAL,
        AU_TRAVAIL,
        EN_TRANSIT,
        EN_VACANCES,
        EN_EVENEMENT,
        RETOUR_DOMICILE
    }

    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    private final long id;
    private final String nom;
    private final Vector2D position;
    private final Vector2D velocity;
    private final Vector2D forceBuffer;
    private final Vector2D snapshotPosition;
    private final Vector2D snapshotVelocity;
    private final ArrayList<Vector2D> transitPath;
    private final SplittableRandom random;

    private final ZoneManager.ZoneId zoneDomicile;
    private final ZoneManager.ZoneId zoneTravail;
    private ZoneManager.ZoneId zoneActuelle;
    private ZoneManager.ZoneId zoneDestination;

    private final boolean travailleurPendulaire;
    private final double traitSociabilite;
    private final double propensionVoyage;
    private final double prudenceSanitaire;
    private final double vitesseBase;
    private final double forceMax;
    private final double rayonPerception;
    private final double rayonInfection;

    private EtatSEIRS etatSEIRS;
    private EtatFSM etatFSM;
    private SimulationEngine engine;

    private double jaugeFaim;
    private double etatRemainingHours;
    private double infectionCooldownHours;
    private double stayRemainingHours;
    private double eventRemainingHours;
    private double decisionCooldownHours;
    private double localTargetCooldownSeconds;
    private double densiteLocaleRecente;
    private int transitIndex;
    private int expositionsRecues;

    private String eventAssignment;
    private ZoneManager.ZoneId eventZone;
    private Vector2D localTarget;
    private Rumeur rumeurPortee;
    private String messageMutant;

    // ── Daily-life enrichment (set via setPOIs after construction) ──
    /** Specific home address (a DOMICILE POI position). May be null. */
    private Vector2D poiDomicile = null;
    /** Specific workplace position (a TRAVAIL POI position). May be null. */
    private Vector2D poiTravail  = null;
    /** Favorite POI per category (assigned at bootstrap). */
    private Map<PlaceInteret.TypeLieu, PlaceInteret> favorisLieux = new EnumMap<>(PlaceInteret.TypeLieu.class);
    /** How easily this agent believes a rumor [0..1]. */
    private double credulite = 0.5;
    /** Label of the current activity (for UI display). */
    private String activiteActuelle = "";
    /**
     * How many real-seconds until the agent re-evaluates its scheduled POI target.
     * Keep short so the schedule is tightly followed.
     */
    private double scheduleTargetCooldownSeconds = 0.0;
    /**
     * POI the agent is currently walking towards inside a zone (different from
     * the inter-zone transit path).
     */
    private Vector2D currentPoiTarget = null;

    public Bavard(
        String nom,
        Vector2D positionInitiale,
        ZoneManager.ZoneId zoneDomicile,
        ZoneManager.ZoneId zoneTravail,
        double traitSociabilite,
        double propensionVoyage,
        double prudenceSanitaire,
        boolean travailleurPendulaire,
        long seed
    ) {
        this.id = NEXT_ID.getAndIncrement();
        this.nom = nom;
        this.position = positionInitiale.copy();
        this.velocity = new Vector2D();
        this.forceBuffer = new Vector2D();
        this.snapshotPosition = positionInitiale.copy();
        this.snapshotVelocity = new Vector2D();
        this.transitPath = new ArrayList<>();
        this.random = new SplittableRandom(seed ^ id ^ Double.doubleToLongBits(positionInitiale.getX() + positionInitiale.getY()));
        this.zoneDomicile = zoneDomicile;
        this.zoneTravail = zoneTravail;
        this.zoneActuelle = zoneDomicile;
        this.zoneDestination = null;
        this.travailleurPendulaire = travailleurPendulaire;
        this.traitSociabilite = clamp01(traitSociabilite);
        this.propensionVoyage = clamp01(propensionVoyage);
        this.prudenceSanitaire = clamp01(prudenceSanitaire);
        // Base speed in world-units/second. ~350 wu/s → 2500 wu takes ~7s real time to cross.
        this.vitesseBase = 300.0 + this.traitSociabilite * 80.0 + this.propensionVoyage * 50.0;
        this.forceMax    = 600.0 + this.traitSociabilite * 200.0;
        this.rayonPerception = 80.0 + this.traitSociabilite * 50.0;
        this.rayonInfection  = 28.0 + this.traitSociabilite * 20.0;
        this.etatSEIRS = EtatSEIRS.SUSCEPTIBLE;
        this.etatFSM = EtatFSM.LOCAL;
        this.jaugeFaim = random.nextDouble(10.0, 52.0);
        this.etatRemainingHours = 0.0;
        this.infectionCooldownHours = 0.0;
        this.stayRemainingHours = 0.0;
        this.eventRemainingHours = 0.0;
        this.decisionCooldownHours = random.nextDouble(0.05, 0.45);
        this.localTargetCooldownSeconds = 0.0;
        this.densiteLocaleRecente = 0.0;
        this.transitIndex = 0;
        this.expositionsRecues = 0;
        this.eventAssignment = null;
        this.eventZone = null;
        this.localTarget = positionInitiale.copy();
        this.rumeurPortee = null;
        this.messageMutant = "";
    }

    /**
     * Called by SimulationEngine after bootstrapping all agents.
     * Assigns specific POIs and per-agent favorites for realistic daily routines.
     */
    public void setPOIs(Vector2D domicile, Vector2D travail,
                        Map<PlaceInteret.TypeLieu, PlaceInteret> favorites,
                        double credulite) {
        this.poiDomicile  = domicile;
        this.poiTravail   = travail;
        this.favorisLieux = favorites != null ? favorites : new EnumMap<>(PlaceInteret.TypeLieu.class);
        this.credulite    = clamp01(credulite);
        if (domicile != null) this.localTarget = domicile.copy();
    }

    public void attachEngine(SimulationEngine engine) {
        this.engine = engine;
        this.zoneActuelle = engine.getZoneManager().findZoneId(position);
        this.localTarget = engine.getZoneManager().randomInterestPoint(zoneActuelle, random);
        captureSnapshot();
    }

    public void updateLogic(double deltaSeconds) {
        if (engine == null) {
            return;
        }

        double deltaHours = engine.toSimulationHours(deltaSeconds);
        decisionCooldownHours = Math.max(0.0, decisionCooldownHours - deltaHours);
        infectionCooldownHours = Math.max(0.0, infectionCooldownHours - deltaHours);
        localTargetCooldownSeconds = Math.max(0.0, localTargetCooldownSeconds - deltaSeconds);
        scheduleTargetCooldownSeconds = Math.max(0.0, scheduleTargetCooldownSeconds - deltaSeconds);
        if (stayRemainingHours > 0.0) {
            stayRemainingHours = Math.max(0.0, stayRemainingHours - deltaHours);
        }
        if (eventRemainingHours > 0.0) {
            eventRemainingHours = Math.max(0.0, eventRemainingHours - deltaHours);
        }

        updateNeeds(deltaHours);
        updateEpidemicState(deltaHours);
        evaluateStrategicState(deltaHours);

        forceBuffer.set(0.0, 0.0);
        double speedLimit = switch (etatFSM) {
            case EN_TRANSIT     -> computeTransitSteering();
            case EN_VACANCES    -> computeLocalSteering(true,  1.10, 0.70);
            case EN_EVENEMENT   -> computeLocalSteering(true,  1.20, 1.00);
            case AU_TRAVAIL     -> computeLocalSteering(false, 0.80, 0.45);
            case RETOUR_DOMICILE-> computeLocalSteering(false, 0.85, 0.40);
            case LOCAL          -> computeLocalSteering(false, 0.95, 0.60);
        };

        applyMovement(deltaSeconds, speedLimit);

        if (etatFSM != EtatFSM.EN_TRANSIT) {
            zoneActuelle = engine.getZoneManager().findZoneId(position);
        }

        if (etatSEIRS == EtatSEIRS.SUSCEPTIBLE) {
            attemptInfection(deltaHours);
        }
    }

    public void captureSnapshot() {
        snapshotPosition.set(position);
        snapshotVelocity.set(velocity);
        if (engine != null) {
            zoneActuelle = engine.getZoneManager().findZoneId(snapshotPosition);
        }
    }

    public void assignEventMigration(String eventName, ZoneManager.ZoneId targetZone, double durationHours) {
        if (durationHours <= 0.0) {
            return;
        }
        this.eventAssignment = eventName;
        this.eventZone = targetZone;
        this.eventRemainingHours = Math.max(this.eventRemainingHours, durationHours);
        if (zoneActuelle != targetZone && etatFSM != EtatFSM.EN_TRANSIT) {
            startTransit(targetZone, false, durationHours, EtatFSM.EN_EVENEMENT);
        }
    }

    public void clearEventMigration(String eventName) {
        if (eventAssignment != null && eventAssignment.equals(eventName)) {
            eventRemainingHours = 0.0;
            eventAssignment = null;
            eventZone = null;
            if (etatFSM == EtatFSM.EN_EVENEMENT && zoneActuelle != zoneDomicile) {
                startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
            }
        }
    }

    public void forceExpose(Rumeur rumor) {
        if (etatSEIRS != EtatSEIRS.SUSCEPTIBLE) {
            return;
        }
        receiveExposure(rumor, false);
    }

    public void forceInfectious(Rumeur rumor) {
        rumeurPortee = rumor;
        messageMutant = rumor.getTitre();
        etatSEIRS = EtatSEIRS.INFECTIOUS;
        etatRemainingHours = 2.4 + random.nextDouble(4.0);
        infectionCooldownHours = 0.35;
    }

    public boolean isAssignedToEvent(String eventName) {
        return eventAssignment != null && eventAssignment.equals(eventName);
    }

    public long getId() {
        return id;
    }

    public double getCredulite()        { return credulite; }
    public String getActiviteActuelle() { return activiteActuelle; }
    public Vector2D getPoiDomicile()    { return poiDomicile != null ? poiDomicile.copy() : null; }
    public Vector2D getPoiTravail()     { return poiTravail  != null ? poiTravail.copy()  : null; }

    public String getNom() {
        return nom;
    }

    public Vector2D getPosition() {
        return position.copy();
    }

    public Vector2D getVelocity() {
        return velocity.copy();
    }

    public EtatSEIRS getEtatSEIRS() {
        return etatSEIRS;
    }

    public EtatFSM getEtatFSM() {
        return etatFSM;
    }

    public ZoneManager.ZoneId getZoneDomicile() {
        return zoneDomicile;
    }

    public ZoneManager.ZoneId getZoneActuelle() {
        return zoneActuelle;
    }

    public ZoneManager.ZoneId getZoneTravail() {
        return zoneTravail;
    }

    public double getJaugeFaim() {
        return jaugeFaim;
    }

    public double getTraitSociabilite() {
        return traitSociabilite;
    }

    public double getPropensionVoyage() {
        return propensionVoyage;
    }

    public double getPrudenceSanitaire() {
        return prudenceSanitaire;
    }

    public boolean isTravailleurPendulaire() {
        return travailleurPendulaire;
    }

    public String getMessageMutant() {
        return messageMutant;
    }

    public Rumeur getRumeurPortee() {
        return rumeurPortee;
    }

    public int getExpositionsRecues() {
        return expositionsRecues;
    }

    /** Returns a defensive copy of the last captured snapshot position (thread-safe for renderers). */
    public Vector2D getSnapshotPosition() {
        return snapshotPosition.copy();
    }

    /** Returns a defensive copy of the last captured snapshot velocity (thread-safe for renderers). */
    public Vector2D getSnapshotVelocity() {
        return snapshotVelocity.copy();
    }

    Vector2D snapshotPositionRef() {
        return snapshotPosition;
    }

    Vector2D snapshotVelocityRef() {
        return snapshotVelocity;
    }

    private void updateNeeds(double deltaHours) {
        double hungerRate = 3.5 + (etatSEIRS == EtatSEIRS.INFECTIOUS ? 1.6 : 0.0);
        jaugeFaim = clamp(jaugeFaim + hungerRate * deltaHours, 0.0, 100.0);
    }

    private void updateEpidemicState(double deltaHours) {
        if (etatSEIRS == EtatSEIRS.SUSCEPTIBLE) {
            return;
        }

        etatRemainingHours -= deltaHours;
        if (etatRemainingHours > 0.0) {
            return;
        }

        switch (etatSEIRS) {
            case EXPOSED -> {
                etatSEIRS = EtatSEIRS.INFECTIOUS;
                etatRemainingHours = 1.8 + random.nextDouble(3.8);
                infectionCooldownHours = 0.15;
            }
            case INFECTIOUS -> {
                etatSEIRS = EtatSEIRS.RECOVERED;
                etatRemainingHours = 5.0 + random.nextDouble(7.5);
            }
            case RECOVERED -> {
                etatSEIRS = EtatSEIRS.SUSCEPTIBLE;
                etatRemainingHours = 0.0;
                rumeurPortee = null;
                messageMutant = "";
            }
            default -> {
            }
        }
    }

    private void evaluateStrategicState(double deltaHours) {
        double hour = engine.getHourOfDay();
        RumorManager rumorManager = engine.getRumorManager();
        double repulsion = rumorManager.repulsionPressure(zoneActuelle);

        if (eventAssignment != null && eventRemainingHours <= 0.0) {
            eventAssignment = null;
            eventZone = null;
            if (etatFSM == EtatFSM.EN_EVENEMENT && zoneActuelle != zoneDomicile) {
                startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
                return;
            }
        }

        if (etatFSM == EtatFSM.EN_VACANCES && stayRemainingHours <= 0.0 && zoneActuelle != zoneDomicile) {
            startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
            return;
        }

        if (etatFSM == EtatFSM.EN_EVENEMENT && eventRemainingHours <= 0.0 && zoneActuelle != zoneDomicile) {
            startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
            return;
        }

        if (etatFSM == EtatFSM.EN_TRANSIT) {
            return;
        }

        if (decisionCooldownHours > 0.0) {
            return;
        }

        if (eventAssignment != null && eventRemainingHours > 0.0 && eventZone != null && zoneActuelle != eventZone) {
            startTransit(eventZone, false, eventRemainingHours, EtatFSM.EN_EVENEMENT);
            decisionCooldownHours = 0.45;
            return;
        }

        if (travailleurPendulaire && zoneTravail != zoneDomicile) {
            if (zoneActuelle != zoneTravail && hour >= 6.8 && hour <= 9.4 && jaugeFaim < 86.0) {
                startTransit(zoneTravail, false, 0.0, EtatFSM.AU_TRAVAIL);
                decisionCooldownHours = 0.8;
                return;
            }
            if (zoneActuelle == zoneTravail && (hour >= 16.4 || jaugeFaim >= 84.0)) {
                startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
                decisionCooldownHours = 0.8;
                return;
            }
        }

        if (repulsion > 0.9) {
            ZoneManager.ZoneId retreatZone = zoneDomicile != zoneActuelle ? zoneDomicile : ZoneManager.ZoneId.PETITE_VILLE;
            if (retreatZone != zoneActuelle) {
                startTransit(retreatZone, false, 0.0, EtatFSM.RETOUR_DOMICILE);
                decisionCooldownHours = 0.7;
                return;
            }
        }

        ZoneManager.ZoneId rumorTarget = rumorManager.pickMigrationTarget(zoneActuelle, traitSociabilite, propensionVoyage, random);
        if (rumorTarget != null && rumorTarget != zoneActuelle && shouldFollowRumor(rumorTarget, deltaHours)) {
            boolean vacation = rumorTarget == ZoneManager.ZoneId.ZONE_VACANCES_MER;
            double stayHours = vacation ? 2.0 + random.nextDouble(8.0 + engine.getRumorManager().getSummerLevel() * 10.0) : 0.0;
            startTransit(rumorTarget, vacation, stayHours, vacation ? EtatFSM.EN_VACANCES : EtatFSM.LOCAL);
            decisionCooldownHours = 0.6;
            return;
        }

        if (canStartSpontaneousVacation(hour, deltaHours)) {
            startTransit(
                ZoneManager.ZoneId.ZONE_VACANCES_MER,
                true,
                3.5 + random.nextDouble(10.0 + engine.getRumorManager().getSummerLevel() * 8.0),
                EtatFSM.EN_VACANCES
            );
            decisionCooldownHours = 1.1;
            return;
        }

        if (jaugeFaim > 92.0 && zoneActuelle != zoneDomicile) {
            startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
            decisionCooldownHours = 0.55;
            return;
        }

        if (etatFSM == EtatFSM.RETOUR_DOMICILE && zoneActuelle == zoneDomicile) {
            etatFSM = EtatFSM.LOCAL;
        }

        // ── Daily schedule: strict time-blocks ──────────────────────────────
        // 22:00-07:00 → sleep at home
        // 07:00-09:00 → morning activity (café, gym, park)
        // 09:00-12:30 → work / daytime activity
        // 12:30-13:30 → lunch break
        // 13:30-18:00 → work / daytime activity
        // 18:00-22:00 → evening activity (bar, cinema, restaurant, gym)
        if (etatFSM != EtatFSM.EN_TRANSIT && scheduleTargetCooldownSeconds <= 0.0) {
            PlacesManager pm = engine.getPlacesManager();
            if (pm != null) {
                Vector2D schedTarget = pm.getScheduledTarget(
                        zoneActuelle,
                        poiDomicile,
                        poiTravail,
                        favorisLieux,
                        travailleurPendulaire,
                        traitSociabilite,
                        hour,
                        random
                );
                if (schedTarget != null) {
                    localTarget = schedTarget.copy();
                    currentPoiTarget = schedTarget.copy();
                }
                // Short cooldown → agents quickly update destination when schedule changes
                scheduleTargetCooldownSeconds = 0.3 + random.nextDouble(0.8);
            }
        }

        // Force inter-zone transit to respect strict daily schedule
        if (etatFSM != EtatFSM.EN_TRANSIT) {
            boolean shouldBeHome = (hour >= 22.0 || hour < 7.0);
            boolean shouldBeAtWork = travailleurPendulaire && hour >= 9.0 && hour < 18.0
                    && zoneTravail != null && zoneTravail != zoneDomicile;

            if (shouldBeHome && zoneActuelle != zoneDomicile && decisionCooldownHours <= 0.0) {
                startTransit(zoneDomicile, false, 0.0, EtatFSM.RETOUR_DOMICILE);
                decisionCooldownHours = 0.1;
                return;
            }
            if (shouldBeAtWork && zoneActuelle != zoneTravail && decisionCooldownHours <= 0.0) {
                startTransit(zoneTravail, false, 0.0, EtatFSM.AU_TRAVAIL);
                decisionCooldownHours = 0.1;
                return;
            }
        }
    }

    private boolean shouldFollowRumor(ZoneManager.ZoneId rumorTarget, double deltaHours) {
        // Agents with low credulity are less likely to act on rumors
        double probability = (0.08 + traitSociabilite * 0.12 + propensionVoyage * 0.14) * credulite;
        if (rumorTarget == ZoneManager.ZoneId.ZONE_VACANCES_MER) {
            probability += engine.getRumorManager().getSummerLevel() * 0.10;
        }
        return random.nextDouble() < probability * Math.max(0.08, deltaHours * 2.0);
    }

    private boolean canStartSpontaneousVacation(double hour, double deltaHours) {
        if (zoneActuelle == ZoneManager.ZoneId.ZONE_VACANCES_MER || etatFSM == EtatFSM.EN_VACANCES) {
            return false;
        }
        if (hour < 9.0 || hour > 18.5) {
            return false;
        }
        double summerLevel = engine.getRumorManager().getSummerLevel();
        double probability = (0.006 + propensionVoyage * 0.02 + summerLevel * 0.04) * Math.max(0.1, deltaHours * 2.0);
        if (travailleurPendulaire && zoneActuelle == zoneTravail && hour < 17.0) {
            probability *= 0.3;
        }
        return random.nextDouble() < probability;
    }

    private double computeLocalSteering(boolean relaxed, double speedMultiplier, double wanderWeight) {
        ZoneManager zoneManager = engine.getZoneManager();
        RumorManager rumorManager = engine.getRumorManager();
        EventManager eventManager = engine.getEventManager();
        ZoneManager.ZoneId steeringZone = zoneActuelle;

        densiteLocaleRecente = 0.0;

        if (localTargetCooldownSeconds <= 0.0 || snapshotPosition.distanceSquared(localTarget) < 1600.0) {
            localTarget = chooseLocalTarget();
            localTargetCooldownSeconds = random.nextDouble(1.5, 5.0);
        }

        List<Bavard> neighbors = engine.getSpatialHashGrid().queryRadius(snapshotPosition, rayonPerception, this);
        Vector2D separation = new Vector2D();
        Vector2D alignment = new Vector2D();
        Vector2D cohesion = new Vector2D();
        int alignmentCount = 0;

        for (Bavard neighbor : neighbors) {
            Vector2D neighborPos = neighbor.snapshotPositionRef();
            double distanceSquared = snapshotPosition.distanceSquared(neighborPos);
            if (distanceSquared <= 1.0e-6) {
                continue;
            }
            double distance = Math.sqrt(distanceSquared);
            if (distance < rayonPerception) {
                densiteLocaleRecente += 1.0;
                alignment.addLocal(neighbor.snapshotVelocityRef());
                cohesion.addLocal(neighborPos);
                alignmentCount++;
            }
            double comfortRadius = 18.0 + (1.0 - traitSociabilite) * 20.0;
            if (distance < comfortRadius) {
                Vector2D away = snapshotPosition.subtract(neighborPos).divide(Math.max(4.0, distance));
                separation.addLocal(away);
            }
        }

        if (alignmentCount > 0) {
            alignment.divideLocal(alignmentCount);
            if (alignment.lengthSquared() > 1.0e-6) {
                alignment = alignment.withLength(vitesseBase * speedMultiplier).subtract(velocity).limitLocal(forceMax * 0.6);
            }
            cohesion.divideLocal(alignmentCount);
            cohesion = cohesion.subtract(snapshotPosition).withLength(vitesseBase * speedMultiplier).subtract(velocity).limitLocal(forceMax * 0.58);
        } else {
            alignment.set(0.0, 0.0);
            cohesion.set(0.0, 0.0);
        }

        Vector2D wander = localTarget.subtract(snapshotPosition);
        if (wander.lengthSquared() > 1.0e-6) {
            wander = wander.withLength(vitesseBase * speedMultiplier).subtract(velocity).limitLocal(forceMax * 0.55);
        }

        Vector2D containment = zoneManager.containmentForce(steeringZone, snapshotPosition, 115.0, forceMax * 0.8);
        Vector2D rumorInfluence = rumorManager.computeSpatialInfluence(zoneManager, steeringZone, snapshotPosition, propensionVoyage)
            .limitLocal(forceMax * 0.9);
        Vector2D eventInfluence = eventManager.computeEventForce(this, zoneManager).limitLocal(forceMax * 1.2);
        Vector2D hungerInfluence = computeHungerInfluence();

        forceBuffer
            .addLocal(separation.multiply(1.25 + (1.0 - traitSociabilite) * 1.2))
            .addLocal(alignment.multiply(0.22 + traitSociabilite * 0.48))
            .addLocal(cohesion.multiply(0.18 + traitSociabilite * 0.72))
            .addLocal(wander.multiply(wanderWeight))
            .addLocal(containment)
            .addLocal(rumorInfluence)
            .addLocal(eventInfluence)
            .addLocal(hungerInfluence);

        if (!relaxed && velocity.lengthSquared() < 20.0) {
            forceBuffer.addLocal(Vector2D.randomUnit(random).multiply(1.8));
        }

        return vitesseBase * speedMultiplier * computeDensitySpeedFactor();
    }

    private Vector2D computeHungerInfluence() {
        if (jaugeFaim < 68.0) {
            return new Vector2D();
        }
        Vector2D homeCenter = engine.getZoneManager().getZone(zoneDomicile).getCentre();
        double intensity = (jaugeFaim - 68.0) / 32.0;
        return homeCenter.subtract(snapshotPosition).normalized().multiply(intensity * forceMax * 0.85);
    }

    private double computeTransitSteering() {
        if (zoneDestination == null || transitPath.isEmpty()) {
            etatFSM = zoneActuelle == zoneTravail ? EtatFSM.AU_TRAVAIL : EtatFSM.LOCAL;
            zoneDestination = null;
            velocity.multiplyLocal(0.5);
            return vitesseBase;
        }

        if (transitIndex >= transitPath.size()) {
            finishTransit();
            return vitesseBase;
        }

        Vector2D waypoint = transitPath.get(transitIndex);
        double distance = snapshotPosition.distance(waypoint);
        // Wider arrival radius so agents don't slow down to thread narrow waypoints
        if (distance < 80.0) {
            transitIndex++;
            if (transitIndex >= transitPath.size()) {
                finishTransit();
                return vitesseBase;
            }
            waypoint = transitPath.get(transitIndex);
        }

        Vector2D desired = waypoint.subtract(snapshotPosition);
        if (desired.lengthSquared() > 1.0e-6) {
            // High force multiplier — agents sprint to waypoints
            desired = desired.withLength(vitesseBase * 3.0).subtract(velocity).limitLocal(forceMax * 2.0);
            forceBuffer.addLocal(desired);
        }
        return vitesseBase * 3.0;
    }

    private void applyMovement(double deltaSeconds, double speedLimit) {
        forceBuffer.limitLocal(forceMax * (etatFSM == EtatFSM.EN_TRANSIT ? 2.5 : 1.2));
        velocity.addLocal(forceBuffer.getX() * deltaSeconds, forceBuffer.getY() * deltaSeconds);
        velocity.limitLocal(speedLimit);

        double damping = switch (etatFSM) {
            case EN_TRANSIT  -> 0.995;  // high damping to maintain speed in transit
            case EN_VACANCES -> 0.985;
            case EN_EVENEMENT-> 0.980;
            case AU_TRAVAIL  -> 0.975;
            default          -> 0.970;
        };
        velocity.multiplyLocal(Math.pow(damping, deltaSeconds * 60.0));

        if (velocity.lengthSquared() < 1.0 && etatFSM != EtatFSM.EN_TRANSIT) {
            Vector2D nudge = Vector2D.randomUnit(random).multiply(2.0 + traitSociabilite * 1.2);
            velocity.addLocal(nudge);
        }

        position.addLocal(velocity.getX() * deltaSeconds, velocity.getY() * deltaSeconds);
        position.clampToBoundsLocal(0.0, 0.0, ZoneManager.WORLD_WIDTH, ZoneManager.WORLD_HEIGHT);
    }

    private void attemptInfection(double deltaHours) {
        if (infectionCooldownHours > 0.0) {
            return;
        }
        List<Bavard> neighbors = engine.getSpatialHashGrid().queryRadius(snapshotPosition, rayonInfection, this);
        Bavard source = null;
        double highestPressure = 0.0;
        for (Bavard neighbor : neighbors) {
            if (neighbor.etatSEIRS != EtatSEIRS.INFECTIOUS || neighbor.rumeurPortee == null) {
                continue;
            }
            double distance = Math.max(1.0, snapshotPosition.distance(neighbor.snapshotPositionRef()));
            double exposure = neighbor.rumeurPortee.getContagiosite()
                * (1.0 - distance / rayonInfection)
                * (0.65 + neighbor.traitSociabilite * 0.55)
                * (1.0 + neighbor.densiteLocaleRecente * 0.01)
                * (1.0 - prudenceSanitaire * 0.5);
            if (neighbor.zoneActuelle == zoneActuelle && engine.getZoneManager().getZone(zoneActuelle).isDense()) {
                exposure *= 1.22;
            }
            if (exposure > highestPressure) {
                highestPressure = exposure;
                source = neighbor;
            }
        }

        if (source != null) {
            double probability = Math.min(0.92, highestPressure * Math.max(0.12, deltaHours * 2.6));
            if (random.nextDouble() < probability) {
                receiveExposure(engine.getRumorManager().mutateForTransmission(source.rumeurPortee, random), true);
                infectionCooldownHours = 0.75;
            }
        }
    }

    private void receiveExposure(Rumeur rumor, boolean mutated) {
        etatSEIRS = EtatSEIRS.EXPOSED;
        rumeurPortee = mutated ? rumor : rumor.mutate(random);
        messageMutant = rumeurPortee.getTitre();
        etatRemainingHours = 0.7 + random.nextDouble(2.6);
        expositionsRecues++;
    }

    private void startTransit(ZoneManager.ZoneId destination, boolean vacation, double stayHours, EtatFSM onArrivalState) {
        if (destination == null || engine == null) {
            return;
        }
        zoneDestination = destination;
        transitPath.clear();
        transitPath.addAll(engine.getZoneManager().buildTransitPath(zoneActuelle, destination));
        transitPath.add(engine.getZoneManager().randomInterestPoint(destination, random));
        transitIndex = 0;
        stayRemainingHours = vacation ? Math.max(stayRemainingHours, stayHours) : stayRemainingHours;
        if (onArrivalState == EtatFSM.EN_EVENEMENT && eventRemainingHours <= 0.0) {
            eventRemainingHours = stayHours;
        }
        etatFSM = EtatFSM.EN_TRANSIT;
        if (vacation) {
            stayRemainingHours = Math.max(stayHours, 1.0);
        }
    }

    private void finishTransit() {
        if (zoneDestination == null) {
            etatFSM = EtatFSM.LOCAL;
            return;
        }
        position.set(engine.getZoneManager().randomInterestPoint(zoneDestination, random));
        zoneActuelle = zoneDestination;
        velocity.multiplyLocal(0.38);
        transitPath.clear();
        transitIndex = 0;

        if (eventAssignment != null && eventZone == zoneDestination && eventRemainingHours > 0.0) {
            etatFSM = EtatFSM.EN_EVENEMENT;
        } else if (stayRemainingHours > 0.0 && zoneDestination == ZoneManager.ZoneId.ZONE_VACANCES_MER) {
            etatFSM = EtatFSM.EN_VACANCES;
        } else if (zoneDestination == zoneTravail && zoneTravail != zoneDomicile) {
            etatFSM = EtatFSM.AU_TRAVAIL;
        } else if (zoneDestination == zoneDomicile) {
            etatFSM = EtatFSM.LOCAL;
        } else {
            etatFSM = EtatFSM.LOCAL;
        }
        zoneDestination = null;
    }

    private Vector2D chooseLocalTarget() {
        ZoneManager zoneManager = engine.getZoneManager();
        if (etatFSM == EtatFSM.EN_EVENEMENT && eventZone != null) {
            return zoneManager.hotspotForFestivalMer();
        }
        if (etatFSM == EtatFSM.AU_TRAVAIL && zoneTravail == ZoneManager.ZoneId.VILLE_PRINCIPALE) {
            return zoneManager.hotspotForCityCenter();
        }
        if (etatFSM == EtatFSM.EN_VACANCES) {
            return zoneManager.randomInterestPoint(ZoneManager.ZoneId.ZONE_VACANCES_MER, random);
        }
        return zoneManager.randomInterestPoint(zoneActuelle, random);
    }

    private double computeDensitySpeedFactor() {
        return Math.max(0.62, 1.0 - densiteLocaleRecente * 0.012);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "Bavard{" +
            "id=" + id +
            ", nom='" + nom + '\'' +
            ", etatSEIRS=" + etatSEIRS +
            ", zoneActuelle=" + zoneActuelle +
            ", etatFSM=" + etatFSM +
            '}';
    }
}
