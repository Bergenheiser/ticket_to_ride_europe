package fr.umontpellier.iut.rails;

import com.google.gson.Gson;
import fr.umontpellier.iut.gui.GameServer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Jeu implements Runnable {
    /**
     * Liste des joueurs
     */
    private final List<Joueur> joueurs;

    /**
     * Le joueur dont c'est le tour
     */
    private Joueur joueurCourant;
    /**
     * Liste des villes représentées sur le plateau de jeu
     */
    private Joueur vainqueur;

    private final List<Ville> villes;
    /**
     * Liste des routes du plateau de jeu
     */
    private final List<Route> routes;
    /**
     * Pile de pioche (face cachée)
     */
    private final List<CouleurWagon> pileCartesWagon;
    /**
     * Cartes de la pioche face visible (normalement il y a 5 cartes face visible)
     */
    private final List<CouleurWagon> cartesWagonVisibles;
    /**
     * Pile de cartes qui ont été défaussée au cours de la partie
     */
    private final List<CouleurWagon> defausseCartesWagon;

    public Joueur getVainqueur() {
        return vainqueur;
    }

    /**
     * Pile des cartes "Destination" (uniquement les destinations "courtes", les
     * destinations "longues" sont distribuées au début de la partie et ne peuvent
     * plus être piochées après)
     */
    private List<Destination> pileDestinations;
    /**
     * File d'attente des instructions recues par le serveur
     */
    private final BlockingQueue<String> inputQueue;
    /**
     * Messages d'information du jeu
     */
    private final List<String> log;

    public Jeu(String[] nomJoueurs) {
        // initialisation des entrées/sorties
        inputQueue = new LinkedBlockingQueue<>();
        log = new ArrayList<>();

        // création des cartes
        pileCartesWagon = new ArrayList<>();
        cartesWagonVisibles = new ArrayList<>();
        defausseCartesWagon = new ArrayList<>();
        pileDestinations = new ArrayList<>();
        //initialisation des piles cartesWagon
        for (int i = 0; i < 12; i++) {
            pileCartesWagon.add(CouleurWagon.ROUGE);
            pileCartesWagon.add(CouleurWagon.BLEU);
            pileCartesWagon.add(CouleurWagon.BLANC);
            pileCartesWagon.add(CouleurWagon.NOIR);
            pileCartesWagon.add(CouleurWagon.ROSE);
            pileCartesWagon.add(CouleurWagon.ORANGE);
            pileCartesWagon.add(CouleurWagon.VERT);
            pileCartesWagon.add(CouleurWagon.JAUNE);
            pileCartesWagon.add(CouleurWagon.LOCOMOTIVE);
        }
        pileCartesWagon.add(CouleurWagon.LOCOMOTIVE);
        pileCartesWagon.add(CouleurWagon.LOCOMOTIVE);
        Collections.shuffle(pileCartesWagon);

        //cartesdestinations
        pileDestinations = Destination.makeDestinationsEurope();

        //cartesWagonVisibles
        for (int i = 0; i < 5; i++) {
            cartesWagonVisibles.add(this.piocherCarteWagon());
        }
        // création des joueurs
        ArrayList<Joueur.Couleur> couleurs = new ArrayList<>(Arrays.asList(Joueur.Couleur.values()));
        Collections.shuffle(couleurs);
        joueurs = new ArrayList<>();
        for (String nom : nomJoueurs) {
            Joueur joueur = new Joueur(nom, this, couleurs.remove(0));
            joueurs.add(joueur);
        }
        joueurCourant = joueurs.get(0);

        // création des villes et des routes
        Plateau plateau = Plateau.makePlateauEurope();
        villes = plateau.getVilles();
        routes = plateau.getRoutes();
    }

    public List<CouleurWagon> getPileCartesWagon() {
        return pileCartesWagon;
    }

    public List<CouleurWagon> getCartesWagonVisibles() {
        return cartesWagonVisibles;
    }

    public List<Ville> getVilles() {
        return villes;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Destination> getPileDestinations() {
        return pileDestinations;
    }

    public Joueur getJoueurCourant() {
        return joueurCourant;
    }

    /**
     * Instancie le joueurCourant en fonctions des règles de l'aventurier du rail, pour le joueur qui joue en premier.
     *
     * @return
     */


    /**
     * Exécute la partie
     */
    public void run() {
        /*
         * ATTENTION : Cette méthode est à réécrire.
         *
         * Cette méthode doit :
         * - faire choisir à chaque joueur les destinations initiales qu'il souhaite
         * garder : on pioche 3 destinations "courtes" et 1 destination "longue", puis
         * le joueur peut choisir des destinations à défausser ou passer s'il ne veut plus
         * en défausser. Il doit en garder au moins 2.
         * - exécuter la boucle principale du jeu qui fait jouer le tour de chaque
         * joueur à tour de rôle jusqu'à ce qu'un des joueurs n'ait plus que 2 wagons ou
         * moins
         * - exécuter encore un dernier tour de jeu pour chaque joueur après
         */

        /**
         * Le code proposé ici n'est qu'un exemple d'utilisation des méthodes pour
         * interagir avec l'utilisateur, il n'a rien à voir avec le code de la partie et
         * doit donc être entièrement réécrit.
         */

        // Distribution des cartes premier tour
        ArrayList<Destination> destinationsLongues = Destination.makeDestinationsLonguesEurope();
        Collections.shuffle(destinationsLongues);

        for (Joueur j : joueurs) {
            joueurCourant = j;
            //log(joueurCourant.getNom() + " doit sélectionner les destinations qu'il souhaite défausser ou passer...");
            // Main distribuée de routes normales
            ArrayList<Destination> destinationsPossibles = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                destinationsPossibles.add(piocherDestination());
            }

            //ajouter une destinationsLongues a la liste des destinations possible
            destinationsPossibles.add(destinationsLongues.get(0));
            destinationsLongues.remove(destinationsLongues.get(0));

            //faire choisir des destinations aux joueurs
            j.choisirDestinations(destinationsPossibles, 2);
        }
        boolean continuer = true;
        Joueur vainqueur = joueurCourant;
        while (continuer) {
            joueurCourant.jouerTour();
                if (joueurs.indexOf(joueurCourant) == joueurs.size() - 1) {
                    joueurCourant = joueurs.get(0);
                } else {
                    joueurCourant = joueurs.get(joueurs.indexOf(joueurCourant) + 1);
                }
                if(vainqueur.getScore()<joueurCourant.getScore()){
                    vainqueur=joueurCourant;
                }
                if (joueurCourant.getNbWagons()<=2){
                continuer = false;
            }
        }
        joueurCourant.jouerTour();
        prompt("Fin de partie \n Le vainqueur est : "+vainqueur.getNom(), new ArrayList<>(), false);
        //Dernier tour
       /* Joueur vainqueur = joueurCourant;
        for(int i =0;i<joueurs.size()-1;i++){
            if (joueurs.indexOf(joueurCourant) == joueurs.size() - 1) {
                joueurCourant = joueurs.get(0);
            } else {
                joueurCourant = joueurs.get(joueurs.indexOf(joueurCourant) + 1);
            }
            joueurCourant.jouerTour();
            if(vainqueur.getScore()<joueurCourant.getScore()){
                vainqueur=joueurCourant;
            }
        }*/
        this.vainqueur=vainqueur;
        System.out.println("Le vainqueur est :");
        System.out.println(vainqueur);
        log("Le vainqueur est :"+vainqueur);
    }

    /**
     * Ajoute une carte dans la pile de défausse.
     * Dans le cas peu probable, où il y a moins de 5 cartes wagon face visibles
     * (parce que la pioche
     * et la défausse sont vides), alors il faut immédiatement rendre cette carte
     * face visible.
     *
     * @param c carte à défausser
     */

    public void defausserCarteWagon(CouleurWagon c) {

        //Ajouter pré-requis : le joueur possède cette carte wagon.
        if (cartesWagonVisibles.size() < 5 && pileCartesWagon.isEmpty()) {
            cartesWagonVisibles.add(0, c);
        } else {
            defausseCartesWagon.add(c);
        }
    }

    /**
     * Pioche une carte de la pile de pioche
     * Si la pile est vide, les cartes de la défausse sont replacées dans la pioche
     * puis mélangées avant de piocher une carte
     *
     * @return la carte qui a été piochée (ou null si aucune carte disponible)
     */
    public CouleurWagon piocherCarteWagon() {
        if (getPileCartesWagon().isEmpty()) {
            rafraichirPioche();
        }
        CouleurWagon result = null;
        if (pileCartesWagon.size() > 0) {
            result = pileCartesWagon.get(0);
            pileCartesWagon.remove(0);
        }
        return result;
    }

    /**
     * Retire une carte wagon de la pile des cartes wagon visibles.
     * Si une carte a été retirée, la pile de cartes wagons visibles est recomplétée
     * (remise à 5, éventuellement remélangée si 3 locomotives visibles)
     */
    public void retirerCarteWagonVisible(CouleurWagon c) {
        cartesWagonVisibles.remove(c);
        if (getPileCartesWagon().isEmpty()) {
            rafraichirPioche();
        }
        CouleurWagon couleur = piocherCarteWagon();
        if (couleur != null) {
            cartesWagonVisibles.add(couleur);
        }

        if (Collections.frequency(cartesWagonVisibles, CouleurWagon.LOCOMOTIVE) == 3) {
            for (CouleurWagon carte : cartesWagonVisibles) {
                defausserCarteWagon(carte);
            }
            cartesWagonVisibles.clear();
            if (pileCartesWagon.isEmpty()) {
                rafraichirPioche();
            }
            if (cartesWagonVisibles.size() < 5) {
                int manque = 5 - cartesWagonVisibles.size();
                for (int i = 0; i < manque; i++) {
                    cartesWagonVisibles.add(piocherCarteWagon());
                }
            }
        }
    }

    public void rafraichirPioche() {
        if (!defausseCartesWagon.isEmpty()) {
            Collections.shuffle(defausseCartesWagon);
            pileCartesWagon.addAll(defausseCartesWagon);
            defausseCartesWagon.clear();
        }
    }

    /**
     * Pioche et renvoie la destination du dessus de la pile de destinations.
     *
     * @return la destination qui a été piochée (ou `null` si aucune destination
     * disponible)
     */
    public Destination piocherDestination() {
        Destination piocheActDes = null;
        if (pileDestinations.size() > 0) {
            piocheActDes = pileDestinations.get(0);
        }
        pileDestinations.remove(0);
        return piocheActDes;
    }

    public List<CouleurWagon> getDefausseCartesWagon() {
        return defausseCartesWagon;
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("\n");
        for (Joueur j : joueurs) {
            joiner.add(j.toString());
        }
        return joiner.toString();
    }

    /**
     * Ajoute un message au log du jeu
     */
    public void log(String message) {
        log.add(message);
    }

    /**
     * Ajoute un message à la file d'entrées
     */
    public void addInput(String message) {
        inputQueue.add(message);
    }

    /**
     * Lit une ligne de l'entrée standard
     * C'est cette méthode qui doit être appelée à chaque fois qu'on veut lire
     * l'entrée clavier de l'utilisateur (par exemple dans {@code Player.choisir})
     *
     * @return une chaîne de caractères correspondant à l'entrée suivante dans la
     * file
     */
    public String lireLigne() {
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Envoie l'état de la partie pour affichage aux joueurs avant de faire un choix
     *
     * @param instruction l'instruction qui est donnée au joueur
     * @param boutons     labels des choix proposés s'il y en a
     * @param peutPasser  indique si le joueur peut passer sans faire de choix
     */
    public void prompt(String instruction, Collection<String> boutons, boolean peutPasser) {
        System.out.println();
        System.out.println(this);
        if (boutons.isEmpty()) {
            System.out.printf(">>> %s: %s <<<%n", joueurCourant.getNom(), instruction);
        } else {
            StringJoiner joiner = new StringJoiner(" / ");
            for (String bouton : boutons) {
                joiner.add(bouton);
            }
            System.out.printf(">>> %s: %s [%s] <<<%n", joueurCourant.getNom(), instruction, joiner);
        }

        Map<String, Object> data = Map.ofEntries(
                new AbstractMap.SimpleEntry<String, Object>("prompt", Map.ofEntries(
                        new AbstractMap.SimpleEntry<String, Object>("instruction", instruction),
                        new AbstractMap.SimpleEntry<String, Object>("boutons", boutons),
                        new AbstractMap.SimpleEntry<String, Object>("nomJoueurCourant", getJoueurCourant().getNom()),
                        new AbstractMap.SimpleEntry<String, Object>("peutPasser", peutPasser))),
                new AbstractMap.SimpleEntry<>("villes",
                        villes.stream().map(Ville::asPOJO).collect(Collectors.toList())),
                new AbstractMap.SimpleEntry<>("routes",
                        routes.stream().map(Route::asPOJO).collect(Collectors.toList())),
                new AbstractMap.SimpleEntry<String, Object>("joueurs",
                        joueurs.stream().map(Joueur::asPOJO).collect(Collectors.toList())),
                new AbstractMap.SimpleEntry<String, Object>("piles", Map.ofEntries(
                        new AbstractMap.SimpleEntry<String, Object>("pileCartesWagon", pileCartesWagon.size()),
                        new AbstractMap.SimpleEntry<String, Object>("pileDestinations", pileDestinations.size()),
                        new AbstractMap.SimpleEntry<String, Object>("defausseCartesWagon", defausseCartesWagon),
                        new AbstractMap.SimpleEntry<String, Object>("cartesWagonVisibles", cartesWagonVisibles))),
                new AbstractMap.SimpleEntry<String, Object>("log", log));
        GameServer.setEtatJeu(new Gson().toJson(data));
    }


}
