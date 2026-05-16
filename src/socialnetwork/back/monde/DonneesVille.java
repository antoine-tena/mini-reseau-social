package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.personnalite.PersonnaliteAnxieux;
import socialnetwork.back.personnalite.PersonnaliteBienveillant;
import socialnetwork.back.personnalite.PersonnaliteComplotiste;
import socialnetwork.back.personnalite.PersonnaliteCynique;
import socialnetwork.back.personnalite.PersonnaliteDepressive;
import socialnetwork.back.personnalite.PersonnaliteEmotive;
import socialnetwork.back.personnalite.PersonnaliteExtrovert;
import socialnetwork.back.personnalite.PersonnaliteInfluenceur;
import socialnetwork.back.personnalite.PersonnaliteIntrovert;
import socialnetwork.back.personnalite.PersonnaliteMediateur;
import socialnetwork.back.personnalite.PersonnaliteRationnelle;

/**
 * Données concrètes de la ville simulée : Grenoble fictive.
 *
 * 30 bavards répartis en familles et individus, 15 lieux clés,
 * relations sociales et routines journalières complètes.
 *
 * ──────────────────────────────────────────────────────────────────────
 *  FAMILLES :
 *   - Martin       : Pierre(48) + Sophie(46) + Lucas(22) + Emma(19)
 *   - Dubois       : Robert(72) + Mireille(70) + Marc(45) + Isabelle(43) + Théo(17)
 *   - Lefèvre      : Antoine(26) + Léa(24) — coloc
 *   - Moreau       : Louis(32) + Chloé(30) — couple
 *
 *  CÉLIBATAIRES/GROUPES :
 *   Hugo(23), Inès(25), Nathan(27), Eva(22), Tom(29),
 *   Zoé(24), Adrien(31), Claire(28), Rayan(25), Camille(26),
 *   Julien(33), Manon(21), Baptiste(24), Yasmine(27), Léo(20)
 *
 *  LIEUX (15) :
 *   01. IUT Grenoble               (UNIVERSITE)
 *   02. EDF Centrale               (CENTRALE_ENERGIE)
 *   03. CAF Grenoble               (ASSOCIATION)
 *   04. Café des Arts              (CAFE_RESTAURANT)
 *   05. Le Bloc Escalade           (SALLE_ESCALADE)
 *   06. La Belle Électrique        (SALLE_CONCERT)
 *   07. BU Sciences Grenoble       (BIBLIOTHEQUE)
 *   08. Bio Coop Grenoble          (SUPERMARCHE)
 *   09. Parc de la Bastille        (PARC)
 *   10. Piscine Olympique          (PISCINE)
 *   11. Lycée Stendhal             (LYCEE)
 *   12. Studio Yoga Zen            (YOGA_BIENETRE)
 *   13. Restaurant Universitaire   (CAFE_RESTAURANT)
 *   14. Marché des Halles          (MARCHE)
 *   15. Gare de Grenoble           (GARE)
 *   + domiciles par famille/groupe
 * ──────────────────────────────────────────────────────────────────────
 */
public final class DonneesVille {

    private DonneesVille() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════════

    public static VilleSociale creer() {
        VilleSociale ville = new VilleSociale("Grenoble");

        // ── 1. CRÉER LES LIEUX ─────────────────────────────────────────────

        // Lieux publics
        Lieu iut        = lieu("IUT Grenoble",              TypeLieu.UNIVERSITE,
                               "1 rue des Universités, 38000 Grenoble",  600, 400);
        Lieu edf        = lieu("EDF Centrale",              TypeLieu.CENTRALE_ENERGIE,
                               "15 av. des Martyrs, 38000 Grenoble",     900, 600);
        Lieu caf        = lieu("CAF Grenoble",              TypeLieu.ASSOCIATION,
                               "8 rue Ampère, 38000 Grenoble",           200, 200);
        Lieu cafe       = lieu("Café des Arts",             TypeLieu.CAFE_RESTAURANT,
                               "3 place Grenette, 38000 Grenoble",       450, 350);
        Lieu escalade   = lieu("Le Bloc Escalade",          TypeLieu.SALLE_ESCALADE,
                               "12 rue des Eaux-Claires, 38000 Grenoble",350, 500);
        Lieu salleConcert = lieu("La Belle Électrique",     TypeLieu.SALLE_CONCERT,
                               "24 bd des Alpes, 38000 Grenoble",        700, 250);
        Lieu bu         = lieu("BU Sciences",               TypeLieu.BIBLIOTHEQUE,
                               "Domaine Universitaire, 38400 St-Martin", 580, 420);
        Lieu biocoop    = lieu("Bio Coop Grenoble",         TypeLieu.SUPERMARCHE,
                               "6 rue Servan, 38000 Grenoble",           400, 320);
        Lieu bastille   = lieu("Parc de la Bastille",       TypeLieu.PARC,
                               "Fort de la Bastille, 38000 Grenoble",    250, 150);
        Lieu piscine    = lieu("Piscine Olympique",         TypeLieu.PISCINE,
                               "39 rue Paul Janet, 38000 Grenoble",      750, 450);
        Lieu lycee      = lieu("Lycée Stendhal",            TypeLieu.LYCEE,
                               "6 rue Ernest Carnot, 38000 Grenoble",    420, 280);
        Lieu yoga       = lieu("Studio Yoga Zen",           TypeLieu.YOGA_BIENETRE,
                               "18 rue Chenoise, 38000 Grenoble",        500, 380);
        Lieu ru         = lieu("Restaurant Universitaire",  TypeLieu.CAFE_RESTAURANT,
                               "Domaine Universitaire, 38400 St-Martin", 610, 410);
        Lieu marche     = lieu("Marché des Halles",         TypeLieu.MARCHE,
                               "Halles Sainte-Claire, 38000 Grenoble",   460, 340);
        Lieu gare       = lieu("Gare de Grenoble",          TypeLieu.GARE,
                               "Pl. de la Gare, 38000 Grenoble",         550, 300);

        // Domiciles
        Lieu domMartin  = lieu("14 rue Championnet",        TypeLieu.DOMICILE,
                               "14 rue Championnet, 38000 Grenoble",     480, 260);
        Lieu domDubois  = lieu("7 rue Beyle Stendhal",      TypeLieu.DOMICILE,
                               "7 rue Beyle Stendhal, 38000 Grenoble",   520, 290);
        Lieu domLefevre = lieu("3 impasse des Tilleuls",    TypeLieu.DOMICILE,
                               "3 impasse des Tilleuls, 38000 Grenoble", 430, 370);
        Lieu domMoreau  = lieu("22 av. Alsace-Lorraine",    TypeLieu.DOMICILE,
                               "22 av. Alsace-Lorraine, 38000 Grenoble", 540, 320);
        Lieu domHugo    = lieu("Résidence Vaucanson",       TypeLieu.DOMICILE,
                               "Résidence Vaucanson, 38000 Grenoble",    640, 390);
        Lieu domInes    = lieu("32 rue Hector Berlioz",     TypeLieu.DOMICILE,
                               "32 rue H. Berlioz, 38000 Grenoble",      470, 430);
        Lieu domVacParis     = new Lieu("Paris",                TypeLieu.DESTINATION_VACANCES, "Paris");
        Lieu domVacMontagne  = new Lieu("Les Deux Alpes",       TypeLieu.DESTINATION_VACANCES, "Les Deux Alpes – station de ski");
        Lieu domVacMer       = new Lieu("Biarritz",             TypeLieu.DESTINATION_VACANCES, "Biarritz – côte basque");
        Lieu domVacEtranger  = new Lieu("Barcelone",            TypeLieu.DESTINATION_VACANCES, "Barcelone – Espagne");

        // Ajouter tous les lieux
        for (Lieu l : new Lieu[]{iut, edf, caf, cafe, escalade, salleConcert,
                bu, biocoop, bastille, piscine, lycee, yoga, ru, marche, gare,
                domMartin, domDubois, domLefevre, domMoreau, domHugo, domInes,
                domVacParis, domVacMontagne, domVacMer, domVacEtranger}) {
            ville.ajouterLieu(l);
        }

        // ── 2. CRÉER LES BAVARDS ───────────────────────────────────────────

        // ─ Famille Martin ──────────────────────────────────────────────────
        Bavard pierre   = new Bavard("Pierre Martin",   PersonnaliteRationnelle.RATIONNEL);
        Bavard sophie   = new Bavard("Sophie Martin",   PersonnaliteBienveillant.BIENVEILLANT);
        Bavard lucas    = new Bavard("Lucas Martin",    PersonnaliteExtrovert.EXTRAVERTI);
        Bavard emma     = new Bavard("Emma Martin",     PersonnaliteEmotive.SENSIBLE);

        ville.ajouterBavard(pierre, domMartin, 48, "Ingénieur EDF",          TraitCaractere.RATIONNEL);
        ville.ajouterBavard(sophie, domMartin, 46, "Assistante sociale CAF", TraitCaractere.GENEREUX);
        ville.ajouterBavard(lucas,  domMartin, 22, "Étudiant IUT info",      TraitCaractere.SOCIABLE);
        ville.ajouterBavard(emma,   domMartin, 19, "Étudiante IUT GEA",      TraitCaractere.SENSIBLE);

        // ─ Famille Dubois ──────────────────────────────────────────────────
        Bavard robert    = new Bavard("Robert Dubois",   PersonnaliteDepressive.MELANCOLIQUE);
        Bavard mireille  = new Bavard("Mireille Dubois", PersonnaliteMediateur.MEDIATEUR);
        Bavard marc      = new Bavard("Marc Dubois",     PersonnaliteInfluenceur.INFLUENCEUR);
        Bavard isabelle  = new Bavard("Isabelle Dubois", PersonnaliteBienveillant.OPTIMISTE);
        Bavard theo      = new Bavard("Théo Dubois",     PersonnaliteEmotive.TRES_EMOTIF);

        ville.ajouterBavard(robert,   domDubois, 72, "Retraité",             TraitCaractere.PESSIMISTE);
        ville.ajouterBavard(mireille, domDubois, 70, "Retraitée institutrice",TraitCaractere.EMPATHIQUE);
        ville.ajouterBavard(marc,     domDubois, 45, "Commercial",           TraitCaractere.CHARISMATIQUE);
        ville.ajouterBavard(isabelle, domDubois, 43, "Infirmière",           TraitCaractere.ALTRUISTE);
        ville.ajouterBavard(theo,     domDubois, 17, "Lycéen",               TraitCaractere.IMPULSIF);

        // ─ Coloc Lefèvre ───────────────────────────────────────────────────
        Bavard antoine  = new Bavard("Antoine Lefèvre", PersonnaliteExtrovert.TRES_EXTRAVERTI);
        Bavard lea      = new Bavard("Léa Lefèvre",     PersonnaliteIntrovert.RESERVE);

        ville.ajouterBavard(antoine, domLefevre, 26, "Développeur web",      TraitCaractere.CREATIF);
        ville.ajouterBavard(lea,     domLefevre, 24, "Graphiste freelance",  TraitCaractere.SOLITAIRE);

        // ─ Couple Moreau ───────────────────────────────────────────────────
        Bavard louis    = new Bavard("Louis Moreau",    PersonnaliteInfluenceur.MICRO_INFLUENCEUR);
        Bavard chloe    = new Bavard("Chloé Moreau",    PersonnaliteAnxieux.ANXIEUX);

        ville.ajouterBavard(louis, domMoreau, 32, "Chef de projet",          TraitCaractere.LEADER);
        ville.ajouterBavard(chloe, domMoreau, 30, "Professeure de yoga",     TraitCaractere.PRUDENT);

        // ─ Individus ───────────────────────────────────────────────────────
        Bavard hugo     = new Bavard("Hugo Bernard",    PersonnaliteExtrovert.SOCIABLE);
        Bavard ines     = new Bavard("Inès Karim",      PersonnaliteRationnelle.PRAGMATIQUE);
        Bavard nathan   = new Bavard("Nathan Silva",    PersonnaliteInfluenceur.MEGA_INFLUENCEUR);
        Bavard eva      = new Bavard("Eva Rousseau",    PersonnaliteEmotive.TRES_EMOTIF);
        Bavard tom      = new Bavard("Tom Girard",      PersonnaliteCynique.CYNIQUE);
        Bavard zoe      = new Bavard("Zoé Laurent",     PersonnaliteBienveillant.TRES_BIENVEILLANT);
        Bavard adrien   = new Bavard("Adrien Petit",    PersonnaliteComplotiste.COMPLOTISTE);
        Bavard claire   = new Bavard("Claire Fontaine", PersonnaliteMediateur.TRES_PACIFICATEUR);
        Bavard rayan    = new Bavard("Rayan Makhlouf",  PersonnaliteExtrovert.EXTRAVERTI);
        Bavard camille  = new Bavard("Camille Lebrun",  PersonnaliteDepressive.DEPRESSIF);
        Bavard julien   = new Bavard("Julien Faure",    PersonnaliteCynique.ACIDE);
        Bavard manon    = new Bavard("Manon Dupont",    PersonnaliteEmotive.EMOTIF);
        Bavard baptiste = new Bavard("Baptiste Rolland",PersonnaliteRationnelle.TRES_RATIONNEL);
        Bavard yasmine  = new Bavard("Yasmine Benali",  PersonnaliteAnxieux.INQUIET);
        Bavard leo      = new Bavard("Léo Durand",      PersonnaliteIntrovert.TRES_INTROVERTI);

        ville.ajouterBavard(hugo,     domHugo, 23, "Étudiant master info",   TraitCaractere.SOCIABLE);
        ville.ajouterBavard(ines,     domInes, 25, "Ingénieure CNRS",        TraitCaractere.RATIONNEL);
        ville.ajouterBavard(nathan,   domHugo, 27, "Youtubeur / influenceur",TraitCaractere.CHARISMATIQUE);
        ville.ajouterBavard(eva,      domInes, 22, "Étudiante arts",         TraitCaractere.SENSIBLE);
        ville.ajouterBavard(tom,      domMoreau, 29,"Journaliste",           TraitCaractere.PESSIMISTE);
        ville.ajouterBavard(zoe,      domLefevre, 24,"Éducatrice",           TraitCaractere.ALTRUISTE);
        ville.ajouterBavard(adrien,   domHugo, 31, "Électricien",            TraitCaractere.CONVENTIONNEL);
        ville.ajouterBavard(claire,   domInes, 28, "Médiatrice culturelle",  TraitCaractere.EMPATHIQUE);
        ville.ajouterBavard(rayan,    domDubois, 25,"Cuisinier",             TraitCaractere.GENEREUX);
        ville.ajouterBavard(camille,  domMartin, 26,"Bibliothécaire",        TraitCaractere.SOLITAIRE);
        ville.ajouterBavard(julien,   domMoreau, 33,"Avocat",                TraitCaractere.MANIPULATEUR);
        ville.ajouterBavard(manon,    domLefevre, 21,"Étudiante psycho",     TraitCaractere.EMPATHIQUE);
        ville.ajouterBavard(baptiste, domHugo, 24, "Technicien réseau",      TraitCaractere.PRUDENT);
        ville.ajouterBavard(yasmine,  domInes, 27, "Doctorante chimie",      TraitCaractere.PRUDENT);
        ville.ajouterBavard(leo,      domDubois, 20,"Étudiant CPGE",         TraitCaractere.SOLITAIRE);

        // ── 3. RELATIONS SOCIALES ──────────────────────────────────────────

        // Famille Martin (cohabitation)
        ville.ajouterRelation(pierre,  sophie,  TypeRelation.EPOUX);
        ville.ajouterRelation(pierre,  lucas,   TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(pierre,  emma,    TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(sophie,  lucas,   TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(sophie,  emma,    TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(lucas,   emma,    TypeRelation.FRERE_SOEUR);

        // Famille Dubois
        ville.ajouterRelation(robert,   mireille, TypeRelation.EPOUX);
        ville.ajouterRelation(robert,   marc,     TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(mireille, marc,     TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(marc,     isabelle, TypeRelation.EPOUX);
        ville.ajouterRelation(marc,     theo,     TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(isabelle, theo,     TypeRelation.PARENT_ENFANT);
        ville.ajouterRelation(robert,   theo,     TypeRelation.GRAND_PARENT);

        // Coloc Lefèvre
        ville.ajouterRelation(antoine, lea,      TypeRelation.FRERE_SOEUR);

        // Couple Moreau
        ville.ajouterRelation(louis,   chloe,    TypeRelation.COUPLE);

        // Amis proches
        ville.ajouterRelation(lucas,   hugo,     TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(lucas,   nathan,   TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(emma,    eva,      TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(emma,    manon,    TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(antoine, rayan,    TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(lea,     claire,   TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(chloe,   zoe,      TypeRelation.AMI_PROCHE);
        ville.ajouterRelation(hugo,    ines,     TypeRelation.AMI_PROCHE);

        // Collègues
        ville.ajouterRelation(pierre, ines,     TypeRelation.COLLEGUE);
        ville.ajouterRelation(pierre, baptiste, TypeRelation.COLLEGUE);
        ville.ajouterRelation(sophie, claire,   TypeRelation.COLLEGUE);
        ville.ajouterRelation(marc,   louis,    TypeRelation.COLLEGUE);
        ville.ajouterRelation(marc,   julien,   TypeRelation.COLLEGUE);
        ville.ajouterRelation(isabelle, yasmine, TypeRelation.COLLEGUE);

        // Camarades (IUT/lycée)
        ville.ajouterRelation(lucas,   eva,     TypeRelation.CAMARADE);
        ville.ajouterRelation(lucas,   baptiste,TypeRelation.CAMARADE);
        ville.ajouterRelation(emma,    leo,     TypeRelation.CAMARADE);
        ville.ajouterRelation(theo,    leo,     TypeRelation.CAMARADE);
        ville.ajouterRelation(hugo,    manon,   TypeRelation.CAMARADE);
        ville.ajouterRelation(ines,    yasmine, TypeRelation.CAMARADE);

        // Voisins
        ville.ajouterRelation(pierre,  marc,    TypeRelation.VOISIN);
        ville.ajouterRelation(antoine, chloe,   TypeRelation.VOISIN);

        // Mentor
        ville.ajouterRelation(pierre,  lucas,   TypeRelation.MENTOR);
        ville.ajouterRelation(ines,    yasmine, TypeRelation.MENTOR);
        ville.ajouterRelation(claire,  manon,   TypeRelation.MENTOR);

        // Club / sport
        ville.ajouterRelation(lucas,   hugo,    TypeRelation.PARTENAIRE_SPORT);  // escalade
        ville.ajouterRelation(chloe,   zoe,     TypeRelation.PARTENAIRE_CLUB);   // yoga
        ville.ajouterRelation(rayan,   adrien,  TypeRelation.PARTENAIRE_SPORT);  // piscine

        // Connaissances légères
        ville.ajouterRelation(tom,     julien,  TypeRelation.CONNAISSANCE);
        ville.ajouterRelation(adrien,  tom,     TypeRelation.CONNAISSANCE);
        ville.ajouterRelation(camille, yasmine, TypeRelation.CONNAISSANCE);
        ville.ajouterRelation(leo,     baptiste,TypeRelation.CONNAISSANCE);

        // ── 4. ROUTINES JOURNALIÈRES ───────────────────────────────────────

        // Pierre — Ingénieur EDF (lundi–vendredi 8h–18h)
        ville.setRoutine(pierre, new RoutineJournaliere(domMartin)
            .ajouterTravail(edf, 8, 12)
            .ajouterPauses(cafe)
            .ajouterTravail(edf, 14, 18)
            .ajouterLoisirWeekEnd(bastille, 9, 11)
            .ajouterLoisir(bastille, 18, 20, 0.4f)
        );

        // Sophie — Assistante sociale CAF
        ville.setRoutine(sophie, new RoutineJournaliere(domMartin)
            .ajouterTravail(caf, 9, 12)
            .ajouterPauses(marche)
            .ajouterTravail(caf, 13, 17)
            .ajouterLoisir(yoga, 17, 19, 0.5f)
            .ajouterLoisirWeekEnd(marche, 9, 11)
        );

        // Lucas — Étudiant IUT
        ville.setRoutine(lucas, new RoutineJournaliere(domMartin)
            .ajouterTravail(iut, 8, 12)
            .ajouterPauses(ru)
            .ajouterTravail(bu, 14, 17)
            .ajouterLoisir(escalade, 17, 19, 0.6f)
            .ajouterLoisir(cafe, 19, 21, 0.5f)
            .ajouterLoisirWeekEnd(escalade, 10, 13)
        );

        // Emma — Étudiante IUT GEA
        ville.setRoutine(emma, new RoutineJournaliere(domMartin)
            .ajouterTravail(iut, 9, 12)
            .ajouterPauses(ru)
            .ajouterTravail(bu, 13, 16)
            .ajouterLoisir(salleConcert, 20, 22, 0.3f)
            .ajouterLoisirWeekEnd(marche, 10, 12)
        );

        // Robert — Retraité
        ville.setRoutine(robert, new RoutineJournaliere(domDubois)
            .ajouterLoisir(bastille, 9, 11, 0.5f)
            .ajouterLoisir(cafe, 15, 17, 0.4f)
        );

        // Mireille — Retraitée
        ville.setRoutine(mireille, new RoutineJournaliere(domDubois)
            .ajouterLoisir(marche, 9, 11, 0.6f)
            .ajouterLoisir(biocoop, 11, 12, 0.4f)
            .ajouterLoisir(yoga, 15, 16, 0.5f)
        );

        // Marc — Commercial
        ville.setRoutine(marc, new RoutineJournaliere(domDubois)
            .ajouterTravail(edf, 9, 12)
            .ajouterPauses(cafe)
            .ajouterTravail(edf, 13, 18)
            .ajouterLoisir(salleConcert, 20, 22, 0.3f)
        );

        // Isabelle — Infirmière (travail décalé)
        ville.setRoutine(isabelle, new RoutineJournaliere(domDubois)
            .ajouterTravail(caf, 7, 15)  // hôpital simulé par CAF
            .ajouterLoisir(piscine, 16, 17, 0.5f)
            .ajouterLoisirWeekEnd(bastille, 10, 12)
        );

        // Théo — Lycéen
        ville.setRoutine(theo, new RoutineJournaliere(domDubois)
            .ajouterTravail(lycee, 8, 12)
            .ajouterPauses(cafe)
            .ajouterTravail(lycee, 13, 17)
            .ajouterLoisir(escalade, 17, 19, 0.5f)
            .ajouterLoisirWeekEnd(salleConcert, 20, 22)
        );

        // Antoine — Développeur web (télétravail + coworking imaginé)
        ville.setRoutine(antoine, new RoutineJournaliere(domLefevre)
            .ajouterTravail(bu, 9, 12)          // "coworking" = BU
            .ajouterPauses(cafe)
            .ajouterTravail(bu, 14, 18)
            .ajouterLoisir(escalade, 18, 20, 0.5f)
            .ajouterLoisirWeekEnd(salleConcert, 20, 23)
        );

        // Léa — Graphiste
        ville.setRoutine(lea, new RoutineJournaliere(domLefevre)
            .ajouterTravail(bu, 10, 12)
            .ajouterTravail(bu, 14, 17)
            .ajouterLoisir(marche, 17, 18, 0.4f)
            .ajouterLoisirWeekEnd(bastille, 11, 13)
        );

        // Louis — Chef de projet
        ville.setRoutine(louis, new RoutineJournaliere(domMoreau)
            .ajouterTravail(edf, 8, 12)
            .ajouterPauses(cafe)
            .ajouterTravail(edf, 14, 19)
            .ajouterLoisir(piscine, 19, 20, 0.4f)
        );

        // Chloé — Professeure yoga
        ville.setRoutine(chloe, new RoutineJournaliere(domMoreau)
            .ajouterTravail(yoga, 7, 9)
            .ajouterTravail(yoga, 18, 20)
            .ajouterLoisir(biocoop, 10, 11, 0.6f)
            .ajouterLoisirWeekEnd(bastille, 9, 11)
        );

        // Individuels
        ville.setRoutine(hugo, new RoutineJournaliere(domHugo)
            .ajouterTravail(iut, 8, 12)
            .ajouterPauses(ru)
            .ajouterTravail(bu, 14, 17)
            .ajouterLoisir(escalade, 17, 19, 0.5f)
            .ajouterLoisir(cafe, 20, 22, 0.5f)
        );

        ville.setRoutine(ines, new RoutineJournaliere(domInes)
            .ajouterTravail(iut, 8, 18)
            .ajouterPauses(bu)
            .ajouterLoisirWeekEnd(bastille, 9, 12)
        );

        ville.setRoutine(nathan, new RoutineJournaliere(domHugo)
            .ajouterLoisir(cafe, 10, 12, 0.7f)
            .ajouterLoisir(salleConcert, 20, 23, 0.5f)
            .ajouterLoisirWeekEnd(salleConcert, 14, 17)
        );

        ville.setRoutine(eva, new RoutineJournaliere(domInes)
            .ajouterTravail(iut, 9, 12)
            .ajouterPauses(cafe)
            .ajouterTravail(bu, 14, 16)
            .ajouterLoisir(salleConcert, 20, 22, 0.4f)
        );

        ville.setRoutine(tom, new RoutineJournaliere(domMoreau)
            .ajouterTravail(caf, 9, 17)
            .ajouterPauses(cafe)
            .ajouterLoisir(cafe, 17, 19, 0.6f)
        );

        ville.setRoutine(zoe, new RoutineJournaliere(domLefevre)
            .ajouterTravail(caf, 8, 16)
            .ajouterPauses(marche)
            .ajouterLoisir(yoga, 18, 19, 0.5f)
        );

        ville.setRoutine(adrien, new RoutineJournaliere(domHugo)
            .ajouterTravail(edf, 7, 15)
            .ajouterLoisir(piscine, 16, 17, 0.5f)
            .ajouterLoisirWeekEnd(bastille, 9, 11)
        );

        ville.setRoutine(claire, new RoutineJournaliere(domInes)
            .ajouterTravail(caf, 9, 17)
            .ajouterLoisir(bu, 17, 19, 0.4f)
            .ajouterLoisirWeekEnd(salleConcert, 15, 17)
        );

        ville.setRoutine(rayan, new RoutineJournaliere(domDubois)
            .ajouterTravail(cafe, 10, 14)
            .ajouterTravail(cafe, 18, 22)
            .ajouterLoisir(piscine, 14, 16, 0.5f)
        );

        ville.setRoutine(camille, new RoutineJournaliere(domMartin)
            .ajouterTravail(bu, 9, 18)
            .ajouterLoisir(marche, 18, 19, 0.3f)
        );

        ville.setRoutine(julien, new RoutineJournaliere(domMoreau)
            .ajouterTravail(caf, 9, 18)
            .ajouterPauses(cafe)
            .ajouterLoisir(cafe, 19, 21, 0.5f)
        );

        ville.setRoutine(manon, new RoutineJournaliere(domLefevre)
            .ajouterTravail(iut, 8, 12)
            .ajouterPauses(ru)
            .ajouterTravail(bu, 14, 16)
            .ajouterLoisir(yoga, 16, 17, 0.4f)
        );

        ville.setRoutine(baptiste, new RoutineJournaliere(domHugo)
            .ajouterTravail(edf, 8, 17)
            .ajouterPauses(cafe)
            .ajouterLoisirWeekEnd(escalade, 10, 12)
        );

        ville.setRoutine(yasmine, new RoutineJournaliere(domInes)
            .ajouterTravail(iut, 9, 18)
            .ajouterPauses(bu)
            .ajouterLoisir(piscine, 18, 19, 0.3f)
        );

        ville.setRoutine(leo, new RoutineJournaliere(domDubois)
            .ajouterTravail(lycee, 8, 12)
            .ajouterTravail(bu, 14, 18)
            .ajouterLoisir(piscine, 18, 20, 0.4f)
        );

        // ── 5. VACANCES ────────────────────────────────────────────────────

        // Pierre et Sophie partent à Paris (jours 14–21 du cycle)
        Vacances vacPierre = new Vacances(domVacParis,
            "Week-end à Paris avec Sophie", 14, 21, "France", false);
        vacPierre.ajouterCompagnon(sophie);
        ville.setVacances(pierre, vacPierre);
        ville.setVacances(sophie, new Vacances(domVacParis,
            "Week-end à Paris avec Pierre", 14, 21, "France", false));

        // Lucas aux Deux Alpes (ski, jours 0–7)
        ville.setVacances(lucas, new Vacances(domVacMontagne,
            "Ski aux Deux Alpes", 0, 7, "France", true));

        // Nathan à Barcelone (jours 5–12)
        ville.setVacances(nathan, new Vacances(domVacEtranger,
            "Voyage à Barcelone", 5, 12, "Espagne", false));

        // Marc et Isabelle à Biarritz (jours 20–28)
        ville.setVacances(marc, new Vacances(domVacMer,
            "Vacances en famille à Biarritz", 20, 28, "France", false));
        ville.setVacances(isabelle, new Vacances(domVacMer,
            "Vacances en famille à Biarritz", 20, 28, "France", false));

        return ville;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ══════════════════════════════════════════════════════════════════════════

    /** Crée et configure un lieu avec adresse simplifiée. */
    private static Lieu lieu(String nom, TypeLieu type,
                              String adresseStr, double x, double y) {
        Adresse adresse = new Adresse(0, adresseStr, "38000", "Grenoble");
        return new Lieu(nom, type, adresse, x, y);
    }

}
