package socialnetwork.back.monde;

import socialnetwork.back.model.Bavard;
import socialnetwork.back.model.Reseau;
import socialnetwork.back.personnalite.*;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de grand échantillon de population aléatoire.
 *
 * Crée une VilleSociale avec N bavards, des lieux variés, des relations
 * réalistes et des routines diversifiées.
 *
 * Utilisé par le bouton "▶ Simuler 500 personnes" de FenetreCarteVille.
 */
public final class SimulateurGrandPopulation {

    private SimulateurGrandPopulation() {}

    // ──────────────────────────────────────────────────────────────────────────
    //  POOLS DE DONNÉES
    // ──────────────────────────────────────────────────────────────────────────

    private static final String[] PRENOMS_F = {
        "Emma","Léa","Chloé","Manon","Inès","Camille","Zoé","Julie","Sarah","Laura",
        "Marie","Lucie","Clara","Alice","Charlotte","Anaïs","Clémence","Margot","Eva","Océane",
        "Mathilde","Pauline","Elisa","Noemie","Justine","Marion","Jade","Axelle","Mélanie","Sophie",
        "Isabelle","Claire","Aurélie","Florence","Stéphanie","Laure","Céline","Valérie","Nathalie","Brigitte",
        "Yasmine","Fatima","Myriam","Rania","Leila","Aicha","Nora","Kaoutar","Samia","Wafa",
        "Amina","Zineb","Houda","Nawal","Siham","Ikram","Hajar","Safaa","Kenza","Sanae",
        "Mei","Lin","Yuki","Hana","Sakura","Mia","Sofia","Lena","Ava","Nina"
    };

    private static final String[] PRENOMS_M = {
        "Lucas","Hugo","Nathan","Tom","Adrien","Julien","Baptiste","Rayan","Léo","Antoine",
        "Pierre","Marc","Louis","Paul","Thomas","Nicolas","Maxime","Théo","Axel","Clément",
        "Arthur","Raphaël","Ethan","Mathieu","Valentin","Guillaume","Alexis","Kévin","Florian","Damien",
        "Mehdi","Karim","Samir","Youssef","Amine","Rachid","Hamza","Iliasse","Samy","Zakaria",
        "Mohamed","Omar","Bilal","Sofiane","Nassim","Hicham","Khalid","Nabil","Reda","Aziz",
        "Wei","Jian","Takeshi","Hiroshi","Kenji","Marco","Diego","Carlos","Pablo","Ivan",
        "James","Ryan","Dylan","Brandon","Tyler","Kevin","Adam","Liam","Noah","Ethan"
    };

    private static final String[] NOMS = {
        "Martin","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau","Simon",
        "Laurent","Lefebvre","Michel","Garcia","David","Bertrand","Roux","Vincent","Fournier","Morel",
        "Girard","Andre","Lefevre","Mercier","Dupont","Lambert","Bonnet","François","Martinez","Legrand",
        "Garnier","Faure","Rousseau","Blanc","Guerin","Muller","Henry","Roussel","Nicolas","Perrin",
        "Morin","Mathieu","Clement","Gauthier","Dumont","Lopez","Fontaine","Chevalier","Robin","Masson",
        "Sanchez","Gerard","Nguyen","Diallo","Mbaye","Traore","Coulibaly","Diop","Sow","Fall",
        "Chen","Wang","Zhang","Li","Liu","Yang","Huang","Zhao","Wu","Zhou",
        "Karim","Benali","Makhlouf","Hadj","Aomar","Benzema","Zidane","Cisse","Diouf","Camara"
    };

    private static final String[] RUES = {
        "rue Championnet","avenue des Martyrs","rue des Alpes","boulevard des Alpes",
        "avenue Alsace-Lorraine","impasse des Tilleuls","rue Hector Berlioz","rue Fantin Latour",
        "rue des Eaux-Claires","avenue du Grésivaudan","rue Ernest Carnot","rue Chenoise",
        "place Grenette","boulevard Gambetta","rue Paul Janet","rue de Strasbourg",
        "allée des Mouilles","chemin des Acacias","rue de la Paix","rue de la Liberté",
        "avenue de Vizille","rue Ampère","rue Servan","rue des Bergers","voie Ferrée"
    };

    private static final String[] PROFESSIONS = {
        "Étudiant(e) IUT","Étudiant(e) master","Ingénieur(e)","Développeur/se web",
        "Infirmier(e)","Médecin","Enseignant(e)","Chef de projet","Graphiste",
        "Comptable","Commercial(e)","Juriste","Architecte","Plombier/ère",
        "Cuisinier(e)","Boulanger(e)","Pharmacien(ne)","Kinésithérapeute","Psychologue",
        "Assistante sociale","Éducateur/trice","Journaliste","Photographe","Musicien(ne)",
        "Chercheur/se CNRS","Post-doctorant(e)","Doctorant(e)","Data scientist","UX designer",
        "Chef cuisinier","Électricien(ne)","Maçon","Carreleur/se","Peintre en bâtiment",
        "Retraité(e)","Auto-entrepreneur","Freelance","Sans emploi"
    };

    // ──────────────────────────────────────────────────────────────────────────
    //  MÉTHODE PRINCIPALE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Génère une ville avec N bavards aléatoires.
     * @param nbPersonnes nombre de bavards à créer (suggéré : 500)
     * @param seed        graine aléatoire (pour reproductibilité)
     * @return VilleSociale complète
     */
    public static VilleSociale generer(int nbPersonnes, long seed) {
        Random rng = new Random(seed);
        VilleSociale ville = new VilleSociale("Grenoble (" + nbPersonnes + " habitants)");

        // ── Générer les lieux ──────────────────────────────────────────────
        List<Lieu> lieux = genererLieux(ville, rng, nbPersonnes);

        // Séparer par type
        List<Lieu> domiciles    = filterType(lieux, TypeLieu.DOMICILE);
        List<Lieu> universites  = filterType(lieux, TypeLieu.UNIVERSITE);
        List<Lieu> entreprises  = filterType(lieux, TypeLieu.ENTREPRISE, TypeLieu.CENTRALE_ENERGIE,
                                             TypeLieu.ADMINISTRATION, TypeLieu.ASSOCIATION);
        List<Lieu> lycees       = filterType(lieux, TypeLieu.LYCEE);
        List<Lieu> loisirs      = filterType(lieux, TypeLieu.SALLE_ESCALADE, TypeLieu.PISCINE,
                                             TypeLieu.SALLE_SPORT, TypeLieu.YOGA_BIENETRE,
                                             TypeLieu.CINEMA, TypeLieu.SALLE_CONCERT,
                                             TypeLieu.PARC, TypeLieu.MONTAGNE);
        List<Lieu> restaurants  = filterType(lieux, TypeLieu.CAFE_RESTAURANT,
                                             TypeLieu.MARCHE, TypeLieu.BOULANGERIE);
        List<Lieu> bibliotheques= filterType(lieux, TypeLieu.BIBLIOTHEQUE);

        // Assurer des listes non vides
        if (domiciles.isEmpty())     domiciles.add(lieux.get(0));
        if (universites.isEmpty())   universites.add(lieux.get(0));
        if (entreprises.isEmpty())   entreprises.add(lieux.get(0));
        if (loisirs.isEmpty())       loisirs.add(lieux.get(0));
        if (restaurants.isEmpty())   restaurants.add(lieux.get(0));
        if (bibliotheques.isEmpty()) bibliotheques.add(lieux.get(0));

        // ── Générer les bavards ────────────────────────────────────────────
        List<Bavard> bavards = new ArrayList<>();

        for (int i = 0; i < nbPersonnes; i++) {
            String prenom  = tirerPrenom(rng);
            String nom     = NOMS[rng.nextInt(NOMS.length)];
            String nomComplet = prenom + " " + nom;
            int age        = 16 + rng.nextInt(60); // 16–75 ans

            Personnalite perso = personaliteAleatoire(rng);
            Bavard b = new Bavard(nomComplet, perso);

            Lieu domicile = domiciles.get(rng.nextInt(domiciles.size()));
            TraitCaractere trait = TraitCaractere.aleatoire(rng);
            String profession = tirerProfession(age, rng);

            ville.ajouterBavard(b, domicile, age, profession, trait);
            bavards.add(b);

            // Routine selon âge et profession
            RoutineJournaliere routine = creerRoutine(b, domicile, age, profession,
                universites, lycees, entreprises, restaurants, loisirs, bibliotheques, rng);
            ville.setRoutine(b, routine);
        }

        // ── Générer les relations ──────────────────────────────────────────
        genererRelations(ville, bavards, domiciles, rng);

        // ── Quelques vacances aléatoires ───────────────────────────────────
        genererVacances(ville, bavards, rng);

        return ville;
    }

    /** Version par défaut : 500 personnes, seed fixe. */
    public static VilleSociale generer(int nbPersonnes) {
        return generer(nbPersonnes, 12345L);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION DES LIEUX
    // ──────────────────────────────────────────────────────────────────────────

    private static List<Lieu> genererLieux(VilleSociale ville, Random rng, int nbPers) {
        List<Lieu> liste = new ArrayList<>();

        // Nombre proportionnel de chaque type
        int nbDomiciles   = Math.max(10, nbPers / 3);    // ~3 personnes par domicile
        int nbEntreprises = Math.max(5, nbPers / 20);
        int nbUniversites = Math.max(2, nbPers / 100);

        // Lieux fixes (toujours présents)
        ajouterLieu(ville, liste, "IUT Grenoble",            TypeLieu.UNIVERSITE,        600, 400);
        ajouterLieu(ville, liste, "Lycée Stendhal",          TypeLieu.LYCEE,             420, 280);
        ajouterLieu(ville, liste, "EDF Centrale",            TypeLieu.CENTRALE_ENERGIE,  900, 600);
        ajouterLieu(ville, liste, "CAF Grenoble",            TypeLieu.ASSOCIATION,        200, 200);
        ajouterLieu(ville, liste, "Café des Arts",           TypeLieu.CAFE_RESTAURANT,   450, 350);
        ajouterLieu(ville, liste, "Le Bloc Escalade",        TypeLieu.SALLE_ESCALADE,    350, 500);
        ajouterLieu(ville, liste, "La Belle Électrique",     TypeLieu.SALLE_CONCERT,     700, 250);
        ajouterLieu(ville, liste, "BU Sciences",             TypeLieu.BIBLIOTHEQUE,      580, 420);
        ajouterLieu(ville, liste, "Bio Coop",                TypeLieu.SUPERMARCHE,       400, 320);
        ajouterLieu(ville, liste, "Parc Bastille",           TypeLieu.PARC,              250, 150);
        ajouterLieu(ville, liste, "Piscine Olympique",       TypeLieu.PISCINE,           750, 450);
        ajouterLieu(ville, liste, "Studio Yoga Zen",         TypeLieu.YOGA_BIENETRE,     500, 380);
        ajouterLieu(ville, liste, "Restaurant Universitaire",TypeLieu.CAFE_RESTAURANT,   610, 410);
        ajouterLieu(ville, liste, "Marché des Halles",       TypeLieu.MARCHE,            460, 340);
        ajouterLieu(ville, liste, "Gare de Grenoble",        TypeLieu.GARE,              550, 300);
        ajouterLieu(ville, liste, "Cinéma UGC",              TypeLieu.CINEMA,            670, 360);
        ajouterLieu(ville, liste, "Salle de sport municipal",TypeLieu.SALLE_SPORT,       380, 460);
        ajouterLieu(ville, liste, "Labo CNRS",               TypeLieu.LABORATOIRE,       640, 480);
        ajouterLieu(ville, liste, "Mairie de Grenoble",      TypeLieu.ADMINISTRATION,    500, 310);
        ajouterLieu(ville, liste, "Hôpital Nord",            TypeLieu.HOPITAL,           150, 350);

        // Entreprises aléatoires
        String[] typeEntreprises = {"Tech Innovation","Conseil & Co","Industrie Isère",
            "Services Plus","Digital Factory","AlpesInfo","Grenoble Dev","MontBlancSoft"};
        for (int i = 0; i < Math.min(nbEntreprises, 30); i++) {
            String nom = typeEntreprises[rng.nextInt(typeEntreprises.length)] + " " + (i+1);
            ajouterLieuRandom(ville, liste, nom, TypeLieu.ENTREPRISE, rng);
        }

        // Universités/Écoles supplémentaires
        String[] nomUniv = {"Grenoble INP","UGA Sciences","ESAG Design","IEP Grenoble","ESC Grenoble"};
        for (int i = 0; i < Math.min(nbUniversites, 5); i++) {
            ajouterLieuRandom(ville, liste, nomUniv[i % nomUniv.length], TypeLieu.UNIVERSITE, rng);
        }

        // Domiciles aléatoires
        for (int i = 0; i < Math.min(nbDomiciles, 200); i++) {
            int num = 1 + rng.nextInt(99);
            String rue = RUES[rng.nextInt(RUES.length)];
            ajouterLieuRandom(ville, liste, num + " " + rue, TypeLieu.DOMICILE, rng);
        }

        // Loisirs supplémentaires
        String[] nomsParcs = {"Parc Mistral","Parc de l'Île d'Amour","Parc Paul Mistral"};
        for (int i = 0; i < 3; i++) {
            ajouterLieuRandom(ville, liste, nomsParcs[i], TypeLieu.PARC, rng);
        }
        ajouterLieuRandom(ville, liste, "Club de Judo",      TypeLieu.SALLE_SPORT, rng);
        ajouterLieuRandom(ville, liste, "Tennis Club Alpes", TypeLieu.SALLE_SPORT, rng);
        ajouterLieuRandom(ville, liste, "Boulangerie Dupain", TypeLieu.BOULANGERIE, rng);
        ajouterLieuRandom(ville, liste, "Boulangerie Croissant d'Or", TypeLieu.BOULANGERIE, rng);

        // Destinations vacances
        for (String dest : new String[]{"Paris","Lyon","Barcelone","Marseille","Nice",
                "Les Deux Alpes","Chamonix","Biarritz","Montpellier","Bordeaux"}) {
            Lieu l = new Lieu(dest, TypeLieu.DESTINATION_VACANCES, dest);
            ville.ajouterLieu(l);
            liste.add(l);
        }

        return liste;
    }

    private static void ajouterLieu(VilleSociale ville, List<Lieu> liste,
                                     String nom, TypeLieu type, double x, double y) {
        Lieu l = new Lieu(nom, type, new Adresse(0, nom, "38000", "Grenoble"), x, y);
        ville.ajouterLieu(l);
        liste.add(l);
    }

    private static void ajouterLieuRandom(VilleSociale ville, List<Lieu> liste,
                                           String nom, TypeLieu type, Random rng) {
        double x = 50 + rng.nextDouble() * 1100;
        double y = 50 + rng.nextDouble() * 800;
        ajouterLieu(ville, liste, nom, type, x, y);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION DES ROUTINES
    // ──────────────────────────────────────────────────────────────────────────

    private static RoutineJournaliere creerRoutine(Bavard b, Lieu domicile,
            int age, String profession,
            List<Lieu> universites, List<Lieu> lycees, List<Lieu> entreprises,
            List<Lieu> restaurants, List<Lieu> loisirs, List<Lieu> bibliotheques,
            Random rng) {

        RoutineJournaliere r = new RoutineJournaliere(domicile);

        // Choisir le lieu principal (travail/étude)
        if (age <= 18 && !lycees.isEmpty()) {
            // Lycéen
            Lieu lycee = lycees.get(rng.nextInt(lycees.size()));
            r.ajouterTravail(lycee, 8, 12);
            r.ajouterTravail(lycee, 13, 17);
        } else if (age <= 25 && !universites.isEmpty()) {
            // Étudiant
            Lieu univ = universites.get(rng.nextInt(universites.size()));
            r.ajouterTravail(univ, 8 + rng.nextInt(2), 12);
            if (!bibliotheques.isEmpty()) {
                r.ajouterPauses(bibliotheques.get(rng.nextInt(bibliotheques.size())));
            }
            r.ajouterTravail(univ, 14, 17 + rng.nextInt(2));
        } else if (profession.startsWith("Retraité") || age >= 65) {
            // Retraité — pas de travail, loisirs
            if (!loisirs.isEmpty()) {
                r.ajouterLoisir(loisirs.get(rng.nextInt(loisirs.size())), 9, 11, 0.6f);
                r.ajouterLoisirWeekEnd(loisirs.get(rng.nextInt(loisirs.size())), 10, 12);
            }
        } else if (!entreprises.isEmpty()) {
            // Actif
            Lieu travail = entreprises.get(rng.nextInt(entreprises.size()));
            int debut = 7 + rng.nextInt(3);
            int fin   = 16 + rng.nextInt(4);
            r.ajouterTravail(travail, debut, 12);
            if (!restaurants.isEmpty()) {
                r.ajouterPauses(restaurants.get(rng.nextInt(restaurants.size())));
            }
            r.ajouterTravail(travail, 13, fin);
        }

        // Loisirs
        if (!loisirs.isEmpty()) {
            r.ajouterLoisir(loisirs.get(rng.nextInt(loisirs.size())),
                            17 + rng.nextInt(2), 19 + rng.nextInt(2),
                            0.3f + rng.nextFloat() * 0.5f);
            r.ajouterLoisirWeekEnd(loisirs.get(rng.nextInt(loisirs.size())),
                                    9 + rng.nextInt(3), 12 + rng.nextInt(3));
        }

        return r;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION DES RELATIONS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Génère des relations réalistes :
     *  - Cohabitation (même domicile → famille ou coloc)
     *  - Amis proches (3–6 par bavard)
     *  - Collègues (partagent un lieu de travail)
     *  - Connaissances (plus rare)
     */
    private static void genererRelations(VilleSociale ville, List<Bavard> bavards,
                                          List<Lieu> domiciles, Random rng) {
        int nb = bavards.size();

        // Grouper par domicile
        java.util.Map<Lieu, List<Bavard>> parDomicile = new java.util.LinkedHashMap<>();
        for (Bavard b : bavards) {
            Lieu d = ville.getDomicile(b);
            if (d != null) parDomicile.computeIfAbsent(d, x -> new ArrayList<>()).add(b);
        }

        // Relations de cohabitation
        for (var entry : parDomicile.entrySet()) {
            List<Bavard> cohabitants = entry.getValue();
            for (int i = 0; i < cohabitants.size(); i++) {
                for (int j = i + 1; j < cohabitants.size() && j < i + 4; j++) {
                    // Cohabitants → famille ou coloc
                    TypeRelation type = rng.nextFloat() < 0.3f
                        ? TypeRelation.FRERE_SOEUR
                        : TypeRelation.CONNAISSANCE;
                    ville.ajouterRelation(cohabitants.get(i), cohabitants.get(j), type);
                }
            }
        }

        // Amis proches (aléatoires, 2–4 par bavard)
        int nbAmis = Math.max(nb * 2, 50);
        for (int k = 0; k < nbAmis; k++) {
            Bavard a = bavards.get(rng.nextInt(nb));
            Bavard b2 = bavards.get(rng.nextInt(nb));
            if (!a.equals(b2) && ville.getRelationEntre(a, b2) == null) {
                TypeRelation type;
                float r = rng.nextFloat();
                if (r < 0.15f)     type = TypeRelation.AMI_PROCHE;
                else if (r < 0.40f)type = TypeRelation.AMI;
                else if (r < 0.65f)type = TypeRelation.COLLEGUE;
                else               type = TypeRelation.CONNAISSANCE;
                ville.ajouterRelation(a, b2, type);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION DES VACANCES
    // ──────────────────────────────────────────────────────────────────────────

    private static void genererVacances(VilleSociale ville, List<Bavard> bavards, Random rng) {
        List<Lieu> destinations = filterType(ville.getLieux(), TypeLieu.DESTINATION_VACANCES);
        if (destinations.isEmpty()) return;

        // Environ 15% des bavards en vacances
        int nbEnVacances = Math.max(1, bavards.size() / 7);
        for (int i = 0; i < nbEnVacances; i++) {
            Bavard b = bavards.get(rng.nextInt(bavards.size()));
            Lieu dest = destinations.get(rng.nextInt(destinations.size()));
            int jourDebut = rng.nextInt(28);
            int duree     = 3 + rng.nextInt(14);
            Vacances v = new Vacances(dest, "Vacances à " + dest.getNom(),
                jourDebut, jourDebut + duree, "France", rng.nextFloat() < 0.5f);
            ville.setVacances(b, v);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ──────────────────────────────────────────────────────────────────────────

    private static String tirerPrenom(Random rng) {
        boolean femme = rng.nextBoolean();
        if (femme) return PRENOMS_F[rng.nextInt(PRENOMS_F.length)];
        return PRENOMS_M[rng.nextInt(PRENOMS_M.length)];
    }

    private static String tirerProfession(int age, Random rng) {
        if (age <= 18) return "Lycéen(ne)";
        if (age <= 25) return PROFESSIONS[rng.nextInt(4)]; // étudiant-ish
        if (age >= 65) return "Retraité(e)";
        return PROFESSIONS[4 + rng.nextInt(PROFESSIONS.length - 5)];
    }

    private static Personnalite personaliteAleatoire(Random rng) {
        return switch (rng.nextInt(11)) {
            case 0  -> PersonnaliteEmotive.EMOTIF;
            case 1  -> PersonnaliteExtrovert.EXTRAVERTI;
            case 2  -> PersonnaliteIntrovert.INTROVERTI;
            case 3  -> PersonnaliteRationnelle.RATIONNEL;
            case 4  -> PersonnaliteMediateur.MEDIATEUR;
            case 5  -> PersonnaliteInfluenceur.MICRO_INFLUENCEUR;
            case 6  -> PersonnaliteDepressive.MELANCOLIQUE;
            case 7  -> PersonnaliteComplotiste.SCEPTIQUE;
            case 8  -> PersonnaliteBienveillant.BIENVEILLANT;
            case 9  -> PersonnaliteCynique.CYNIQUE;
            default -> PersonnaliteAnxieux.INQUIET;
        };
    }

    private static <T> List<Lieu> filterType(List<Lieu> liste, TypeLieu... types) {
        List<TypeLieu> set = List.of(types);
        List<Lieu> result = new ArrayList<>();
        for (Lieu l : liste) {
            if (set.contains(l.getType())) result.add(l);
        }
        return result;
    }
}
