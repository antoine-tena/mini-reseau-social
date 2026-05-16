# INFO641-SocialNetwork

Mini-projet de POO autour d'un reseau social emotionnel en Java. Le projet simule la propagation de messages entre des bavards relies en graphe, avec modification de la bienveillance selon leur personnalite, affichage Swing, statistiques, analyses et outils d'export.

## Objectif du projet

Le coeur du projet est de modeliser un reseau de bavards capables :

- d'envoyer et recevoir des messages
- de modifier la bienveillance d'un message selon leur personnalite
- de propager l'information a leurs amis sans boucles infinies
- d'afficher le reseau et les messages dans une interface graphique
- d'analyser ce qui se passe dans le reseau avec des statistiques et des vues dediees

## Pourquoi avoir separe le projet en front et back

On a volontairement separe le projet en deux grandes parties :

- le `back` contient toute la logique metier
- le `front` contient tout ce qui concerne l'affichage Swing et l'interaction utilisateur

Cette separation a plusieurs avantages :

- elle evite de melanger la logique de propagation avec le code d'interface
- elle rend le projet plus lisible quand le nombre de classes augmente
- elle facilite les evolutions : on peut enrichir les analyses ou l'interface sans casser le coeur du modele
- elle permet de tester le comportement du reseau sans dependre de la GUI
- elle donne une architecture plus propre et plus defendable a l'oral

En pratique, le `back` repond a la question "comment le reseau fonctionne ?" et le `front` repond a la question "comment on le voit et comment on interagit avec lui ?".

## Technologies utilisees

- Java
- Swing pour l'interface graphique
- aucune dependance externe
- module Java `SocialNetwork`

## Point d'entree

L'application se lance via la classe :

- `socialnetwork.front.ApplicationGraphique`

Cette classe ouvre la fenetre principale du reseau :

- creation des bavards
- connexions entre bavards
- propagation de messages
- vues statistiques et graphiques
- acces aux simulations et analyses

## Arborescence du projet

```text
SocialNetwork/
├── README.md
├── WORKFLOW.md
├── sources.txt
├── bin/
├── out/
└── src/
	├── module-info.java
	└── socialnetwork/
		├── back/
		│   ├── analyse/
		│   ├── export/
		│   ├── model/
		│   ├── observer/
		│   ├── personnalite/
		│   ├── stats/
		│   ├── test/
		│   └── topologie/
		└── front/
			├── ApplicationGraphique.java
			├── fenetre/
			└── vue/
```

## Detail des dossiers

### `src/`

Contient tout le code source Java du projet.

### `src/module-info.java`

Declare le module Java `SocialNetwork` et exporte les packages utilises par le projet.

### `src/socialnetwork/back/`

Contient toute la logique metier du reseau. C'est la partie independante de l'interface graphique.

### `src/socialnetwork/back/model/`

Contient les classes de base du modele.

- `Bavard.java` : represente un utilisateur du reseau, ses amis, ses messages recus, sa personnalite et sa logique de propagation
- `MessageEvent.java` : represente un message qui circule, avec son contenu, sa bienveillance, son chemin, sa profondeur et ses donnees de propagation
- `Reseau.java` : represente le graphe global des bavards et les connexions entre eux

### `src/socialnetwork/back/observer/`

Contient les interfaces d'observation utilisees pour decoupler le modele du reste.

- `MessageListener.java` : interface pour recevoir un message
- `BavardObserver.java` : interface pour observer les changements d'un bavard

Cette partie permet de brancher facilement des vues, des stats ou des journaux sur le modele sans le surcharger.

### `src/socialnetwork/back/personnalite/`

Contient les differentes manieres de modifier la bienveillance d'un message.

- `Personnalite.java` : base commune des personnalites
- `PersonnaliteForte.java` : amplification forte des effets positifs ou negatifs
- `PersonnaliteContextuelle.java` : prise en compte du contenu du message
- `PersonnaliteAdaptative.java` : evolution de la reponse selon les messages recus
- `PersonnaliteOscillante.java` : variation cyclique dans le temps
- `PersonnaliteVirale.java` : modele lie au buzz et a la probabilite de retransmission

Ce dossier sert a rendre les comportements des bavards plus riches qu'un simple coefficient fixe.

### `src/socialnetwork/back/stats/`

Contient les outils de suivi et de mesure du reseau.

- `StatistiquesReseau.java` : calcul des mesures globales comme le nombre total de messages, la bienveillance moyenne ou le taux de propagation
- `JournalReseau.java` : journal chronologique des messages recus avec filtres et export
- `EntreeJournal.java` : structure d'une entree dans le journal

Cette partie est utile pour comprendre ce qui se passe dans le reseau pendant une simulation.

### `src/socialnetwork/back/analyse/`

Contient les extensions d'analyse avancee.

- `AnalyseurSentiment.java` : analyse simple du ton d'un message
- `Emotion.java` et `ResultatSentiment.java` : representation du resultat de l'analyse
- `AlgorithmeInfluence.java` : calculs d'influence et de centralite
- `DetecteurCommunautes.java` : detection de groupes dans le graphe
- `HistoriqueReseau.java` : conservation de cliches successifs du reseau
- `ModeleEpidemique.java` et `SnapshotSIR.java` : modele SIR applique a la diffusion d'information

Ce dossier sert a aller plus loin que la simple propagation et a fournir une vraie lecture analytique du reseau.

### `src/socialnetwork/back/topologie/`

Contient la generation de reseaux et les scenarios preconfigures.

- `GenerateurTopologie.java` : creation automatique de graphes comme anneau, etoile, grille, petit monde ou graphe complet
- `ScenarioReseau.java` : scenarios de demonstration ou de simulation avec comportements predefinis

Cela permet de tester rapidement des situations interessantes sans tout construire a la main.

### `src/socialnetwork/back/export/`

Contient l'export du reseau vers plusieurs formats.

- `ExportateurReseau.java` : export en DOT, JSON, matrice CSV et GEXF

Ce dossier permet de reutiliser les donnees du projet dans d'autres outils ou de conserver les resultats d'une simulation.

### `src/socialnetwork/back/test/`

Contient les classes de test et de demonstration.

- `JeuDeTest.java` : jeux d'essais manuels pour verifier la propagation et certains comportements

### `src/socialnetwork/front/`

Contient toute la partie interface graphique Swing.

### `src/socialnetwork/front/ApplicationGraphique.java`

Point d'entree de l'application. Cette classe initialise Swing et ouvre la fenetre principale.

### `src/socialnetwork/front/fenetre/`

Contient les fenetres principales de l'application.

- `FenetreReseau.java` : fenetre principale pour construire le reseau, lancer des propagations et piloter l'application
- `FenetreBavard.java` : fenetre detaillee d'un bavard
- `FenetreSimulation.java` : interface de simulation et de suivi dynamique
- `FenetreAnalyse.java` : fenetre dediee aux analyses avancees
- `MessageRenderer.java` : rendu visuel des messages dans les listes Swing

Ce dossier gere surtout la logique d'interaction entre l'utilisateur et le modele.

### `src/socialnetwork/front/vue/`

Contient les composants graphiques specialises.

- `VueReseau.java` : affichage du graphe du reseau
- `VueJournal.java` : affichage du journal des messages
- `VueStatistiquesGraphique.java` : affichage graphique des statistiques
- `VueEvolution.java` : evolution temporelle du reseau
- `VueCommunautes.java` : affichage colore des communautes detectees

Ce dossier sert a separer les composants purement visuels des fenetres qui les utilisent.

## Organisation generale du projet

Le projet suit une logique simple :

1. le `model` contient les objets de base du reseau
2. les `personnalite` definissent comment les messages evoluent
3. les `observer` permettent de notifier le reste du systeme
4. les `stats`, `analyse`, `topologie` et `export` ajoutent des services autour du coeur metier
5. le `front` affiche ces donnees et permet a l'utilisateur de piloter les interactions

## Compilation et lancement

Depuis la racine du projet :

```bash
find src -name "*.java" | xargs javac -d bin/socialnetwork
java -cp bin/socialnetwork socialnetwork.front.ApplicationGraphique
```

## Ce que cette structure apporte

Cette arborescence a ete choisie pour que le projet reste clair meme apres l'ajout de nombreuses fonctionnalites.

Elle permet notamment :

- de reperer rapidement ou modifier une fonctionnalite
- de comprendre tout de suite quelles classes relevent du metier ou de l'affichage
- d'ajouter de nouvelles analyses ou de nouvelles vues sans casser le reste
- de presenter plus facilement le projet a l'oral

En resume, l'architecture front/back et la separation en sous-dossiers nous ont permis de faire grossir le projet sans perdre totalement en lisibilite.