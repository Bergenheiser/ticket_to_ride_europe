package fr.umontpellier.iut.rails;

import org.junit.jupiter.api.Disabled;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;

public class Joueur {

    /**
     * Les couleurs possibles pour les joueurs (pour l'interface graphique)
     */
    public enum Couleur {
        JAUNE, ROUGE, BLEU, VERT, ROSE
    }

    /**
     * Jeu auquel le joueur est rattaché
     */
    private final Jeu jeu;
    /**
     * Nom du joueur
     */
    private final String nom;
    /**
     * CouleurWagon du joueur (pour représentation sur le plateau)
     */
    private final Couleur couleur;
    /**
     * Nombre de gares que le joueur peut encore poser sur le plateau
     */
    private int nbGares;
    /**
     * Nombre de wagons que le joueur peut encore poser sur le plateau
     */
    private int nbWagons;
    /**
     * Liste des missions à réaliser pendant la partie
     */
    private final List<Destination> destinations;
    /**
     * Liste des cartes que le joueur a en main
     */
    private final List<CouleurWagon> cartesWagon;
    /**
     * Liste temporaire de cartes wagon que le joueur est en train de jouer pour
     * payer la capture d'une route ou la construction d'une gare
     */
    private final List<CouleurWagon> cartesWagonPosees;
    /**
     * Score courant du joueur (somme des valeurs des routes capturées)
     */
    private int score;

    public Joueur(String nom, Jeu jeu, Joueur.Couleur couleur) {
        this.nom = nom;
        this.jeu = jeu;
        this.couleur = couleur;
        nbGares = 3;
        nbWagons = 45;
        cartesWagon = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cartesWagon.add(jeu.piocherCarteWagon());
        }
        cartesWagonPosees = new ArrayList<>();
        destinations = new ArrayList<>();
        score = 12; // chaque gare non utilisée vaut 4 points
    }

    public String getNom() {
        return nom;
    }

    public Couleur getCouleur() {
        return couleur;
    }

    public int getNbWagons() {
        return nbWagons;
    }

    public void setNbWagons(int nbWagons) {
        this.nbWagons = nbWagons;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public List<CouleurWagon> getCartesWagonPosees() {
        return cartesWagonPosees;
    }

    public List<CouleurWagon> getCartesWagon() {
        return cartesWagon;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public int getNbGares() {
        return nbGares;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Attend une entrée de la part du joueur (au clavier ou sur la websocket) et
     * renvoie le choix du joueur.
     * <p>
     * Cette méthode lit les entrées du jeu ({@code Jeu.lireligne()}) jusqu'à ce
     * qu'un choix valide (un élément de {@code choix} ou de {@code boutons} ou
     * éventuellement la chaîne vide si l'utilisateur est autorisé à passer) soit
     * reçu.
     * Lorsqu'un choix valide est obtenu, il est renvoyé par la fonction.
     * <p>
     * Si l'ensemble des choix valides ({@code choix} + {@code boutons}) ne comporte
     * qu'un seul élément et que {@code canPass} est faux, l'unique choix valide est
     * automatiquement renvoyé sans lire l'entrée de l'utilisateur.
     * <p>
     * Si l'ensemble des choix est vide, la chaîne vide ("") est automatiquement
     * renvoyée par la méthode (indépendamment de la valeur de {@code canPass}).
     * <p>
     * Exemple d'utilisation pour demander à un joueur de répondre à une question
     * par "oui" ou "non" :
     * <p>
     * e     * {@code
     * List<String> choix = Arrays.asList("Oui", "Non");
     * String input = choisir("Voulez vous faire ceci ?", choix, new ArrayList<>(), false);
     * }
     * <p>
     * <p>
     * Si par contre on voulait proposer les réponses à l'aide de boutons, on
     * pourrait utiliser :
     * <p>
     * {@code
     * List<String> boutons = Arrays.asList("1", "2", "3");
     * String input = choisir("Choisissez un nombre.", new ArrayList<>(), boutons, false);
     * }
     *
     * @param instruction message à afficher à l'écran pour indiquer au joueur la
     *                    nature du choix qui est attendu
     * @param choix       une collection de chaînes de caractères correspondant aux
     *                    choix valides attendus du joueur
     * @param boutons     une collection de chaînes de caractères correspondant aux
     *                    choix valides attendus du joueur qui doivent être
     *                    représentés par des boutons sur l'interface graphique.
     * @param peutPasser  booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix. S'il est autorisé à passer, c'est la
     *                    chaîne de caractères vide ("") qui signifie qu'il désire
     *                    passer.
     * @return le choix de l'utilisateur (un élément de {@code choix}, ou de
     * {@code boutons} ou la chaîne vide)
     */
    public String choisir(String instruction, Collection<String> choix, Collection<String> boutons,
                          boolean peutPasser) {
        // on retire les doublons de la liste des choix
        HashSet<String> choixDistincts = new HashSet<>();
        choixDistincts.addAll(choix);
        choixDistincts.addAll(boutons);

        // Aucun choix disponible
        if (choixDistincts.isEmpty()) {
            return "";
        } else {
            // Un seul choix possible (renvoyer cet unique élément)
            if (choixDistincts.size() == 1 && !peutPasser)
                return choixDistincts.iterator().next();
            else {
                String entree;
                // Lit l'entrée de l'utilisateur jusqu'à obtenir un choix valide
                while (true) {
                    jeu.prompt(instruction, boutons, peutPasser);
                    entree = jeu.lireLigne();
                    // si une réponse valide est obtenue, elle est renvoyée
                    if (choixDistincts.contains(entree) || (peutPasser && entree.equals("")))
                        return entree;
                }
            }

        }
    }

    /**
     * Affiche un message dans le log du jeu (visible sur l'interface graphique)
     *
     * @param message le message à afficher (peut contenir des balises html pour la
     *                mise en forme)
     */
    public void log(String message) {
        jeu.log(message);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(String.format("=== %s (%d pts) ===", nom, score));
        joiner.add(String.format("  Gares: %d, Wagons: %d", nbGares, nbWagons));
        joiner.add("  Destinations: "
                + destinations.stream().map(Destination::toString).collect(Collectors.joining(", ")));
        joiner.add("  Cartes wagon: " + CouleurWagon.listToString(cartesWagon));
        return joiner.toString();
    }

    /**
     * @return une chaîne de caractères contenant le nom du joueur, avec des balises
     * HTML pour être mis en forme dans le log
     */
    public String toLog() {
        return String.format("<span class=\"joueur\">%s</span>", nom);
    }

    /**
     * Renvoie une représentation du joueur sous la forme d'un objet Java simple
     * (POJO)
     */
    public Object asPOJO() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("nom", nom);
        data.put("couleur", couleur);
        data.put("score", score);
        data.put("nbGares", nbGares);
        data.put("nbWagons", nbWagons);
        data.put("estJoueurCourant", this == jeu.getJoueurCourant());
        data.put("destinations", destinations.stream().map(Destination::asPOJO).collect(Collectors.toList()));
        data.put("cartesWagon", cartesWagon.stream().sorted().map(CouleurWagon::name).collect(Collectors.toList()));
        data.put("cartesWagonPosees",
                cartesWagonPosees.stream().sorted().map(CouleurWagon::name).collect(Collectors.toList()));
        return data;
    }

    /**
     * Propose une liste de cartes destinations, parmi lesquelles le joueur doit en
     * garder un nombre minimum n.
     * <p>
     * Tant que le nombre de destinations proposées est strictement supérieur à n,
     * le joueur peut choisir une des destinations qu'il retire de la liste des
     * choix, ou passer (en renvoyant la chaîne de caractères vide).
     * <p>
     * Les destinations qui ne sont pas écartées sont ajoutées à la liste des
     * destinations du joueur. Les destinations écartées sont renvoyées par la
     * fonction.
     *
     * @param destinationsPossibles liste de destinations proposées parmi lesquelles
     *                              le joueur peut choisir d'en écarter certaines
     * @param n                     nombre minimum de destinations que le joueur
     *                              doit garder
     * @return liste des destinations qui n'ont pas été gardées par le joueur
     */
    public List<Destination> choisirDestinations(List<Destination> destinationsPossibles, int n) {
        boolean veuxPasser = false;
        List<String> boutons = new ArrayList<String>();
        List<Destination> veuxDeffausser = new ArrayList<Destination>();

        for (Destination d : destinationsPossibles) {
            boutons.add(d.getNom());
        }

        do {
            String choix = choisir("Choisissez la/les cartes Destinations à défausser", new ArrayList<>(), boutons, true);
            if (choix.isEmpty()) {
                veuxPasser = true;
            } else {
                for (Destination d : destinationsPossibles) {
                    if (d.getNom().equals(choix)) {
                        veuxDeffausser.add(d);
                        destinationsPossibles.remove(d);
                        boutons.remove(choix);
                        break;

                    }
                }

            }

        } while (destinationsPossibles.size() > n && !veuxPasser);
        this.destinations.addAll(destinationsPossibles);
        return veuxDeffausser;
    }

    /**
     * Exécute un tour de jeu du joueur.
     * <p>
     * Cette méthode attend que le joueur choisisse une des options suivantes :
     * - le nom d'une carte wagon face visible à prendre ;
     * - le nom "GRIS" pour piocher une carte wagon face cachée s'il reste des
     * cartes à piocher dans la pile de pioche ou dans la pile de défausse ;
     * - la chaîne "destinations" pour piocher des cartes destination ;
     * - le nom d'une ville sur laquelle il peut construire une gare (ville non
     * prise par un autre joueur, le joueur a encore des gares en réserve et assez
     * de cartes wagon pour construire la gare) ;
     * - le nom d'une route que le joueur peut capturer (pas déjà capturée, assez de
     * wagons et assez de cartes wagon) ;
     * - la chaîne de caractères vide pour passer son tour
     * <p>
     * Lorsqu'un choix valide est reçu, l'action est exécutée (il est possible que
     * l'action nécessite d'autres choix de la part de l'utilisateur, comme "choisir les cartes wagon à défausser pour capturer une route" ou
     * "construire une gare", "choisir les destinations à défausser", etc.)
     */

    public void jouerTour() {

        if (jeu.getPileCartesWagon().isEmpty()) {
            jeu.rafraichirPioche();
        }
        // Generation des paramètres de choisir();
        List<String> boutons = new ArrayList<>();
        List<String> choix = new ArrayList<>();

        //Cartes Wagons
        HashMap<String, CouleurWagon> cartesWagonsVisibles = new HashMap<>();
        if (!jeu.getCartesWagonVisibles().isEmpty()) {
            for (CouleurWagon c : this.jeu.getCartesWagonVisibles()) {
                choix.add(c.toString().toUpperCase());
                cartesWagonsVisibles.put(c.toString().toUpperCase(), c);
            }
        }

        //Ajout de l'option de piocheCarteWagon
        if (jeu.getPileCartesWagon().size() > 0) {
            choix.add("GRIS");
        }

        //Ajout du choix de piocher une carte destination
        choix.add("destinations");

        //Ajout des routes que le joueur peux acheter.
        HashMap<String, Route> routesPossible = new HashMap();
        ArrayList listeTunnel = new ArrayList<>();
        for (Route route : this.jeu.getRoutes()) {
            if (!(route.getCouleur() == CouleurWagon.GRIS) && route.getLongueur() <= this.nbWagons && (Collections.frequency(this.cartesWagon, route.getCouleur()) + Collections.frequency(this.cartesWagon, CouleurWagon.LOCOMOTIVE)) >= route.getLongueur() && route.getProprietaire() == null && verifProprio(route)) {
                routesPossible.put(route.getNom(), route);
                choix.add(route.getNom());
            } else if (route.getCouleur() == CouleurWagon.GRIS && this.peuxAcheterGris(route) && verifProprio(route) && route.getProprietaire() == null) {
                if (getAttribute(route, "nbLocomotives") != null) {
                    if (this.peuxAcheterFerry(route)) {
                        routesPossible.put(route.getNom(), route);
                        choix.add(route.getNom());
                    }

                } else if (getAttribute(route, "nbLocomotives") == null) {
                    routesPossible.put(route.getNom(), route);
                    choix.add(route.getNom());
                }
            }
        }
        //Ajout des gares que le joueur pourrait bâtir, si il lui en reste à bâtir.
        HashMap<String, Ville> garePossibles = new HashMap<>();
        if (this.peuxAcheterGare()) {
            for (Ville v : this.jeu.getVilles()) {
                if (v.getProprietaire() == null) {
                    choix.add(v.toString());
                    garePossibles.put(v.toString(), v);
                }

            }
        }
        //////////////////Le joueur prend la décision///////////////////
        String decision = choisir(" veuillez exécuter l'unique action de votre tour", choix, boutons, true);
        /////////////////////////////Nous évaluons sa réponse./////////


        if (decision.equals("destinations")) {
            log("Vous venez de piocher 3 nouvelles cartes Destinations, choisissez celle à défausser");
            List<Destination> mainDestinations = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                mainDestinations.add(this.jeu.piocherDestination());
            }
            List<Destination> aRendre = choisirDestinations(mainDestinations, 1);
            this.jeu.getPileDestinations().addAll(aRendre);
        }
        //Il choisit de piocher une carte wagon face cachée de la pile
        else if (decision.equals("GRIS")) {
            this.cartesWagon.add(jeu.piocherCarteWagon());
            log("Vous avez pioché une carte wagon");
            deuxiemeCarte();
        }

        //Il a choisi de prendre une carteWagonVisible
        else if (!garePossibles.containsKey(decision) && cartesWagonsVisibles.containsKey(decision)) {
            CouleurWagon carteClick = cartesWagonsVisibles.get(decision);
            log("Vous venez de prendre une carte wagon visible");
            this.cartesWagon.add(carteClick);
            jeu.retirerCarteWagonVisible(carteClick);
            choix.remove(decision);

            if (!carteClick.equals(CouleurWagon.LOCOMOTIVE)) {
                deuxiemeCarte();
            }

            //Le Joueur à choisis de bâtir une gare.
        } else if (garePossibles.containsKey(decision)) {
            Ville v = garePossibles.get(decision);
            log("Vous avez choisis de batir la gare: " + v.toString());
            List<String> choixCartesAchatGare = new ArrayList<>();
            HashMap<String, CouleurWagon> mapMainCourante = new HashMap<>();

            for (CouleurWagon c : this.cartesWagon) {
                if (Collections.frequency(cartesWagon, c) + Collections.frequency(cartesWagon, CouleurWagon.LOCOMOTIVE) >= (4 - this.nbGares) || c.equals(CouleurWagon.LOCOMOTIVE)) {
                    mapMainCourante.put(c.toString().toUpperCase(), c);
                    choixCartesAchatGare.add(c.toString().toUpperCase());
                }
            }
            CouleurWagon carteChoisie = null;
            String choixCouleur = this.choisir("Choisir couleur de cartesWagons à défausser pour acheter la gare", choixCartesAchatGare, new ArrayList<>(), false);
            if (choixCouleur.equals("")) {
                decision = "";
                for (CouleurWagon c : cartesWagonPosees) {
                    cartesWagon.add(c);
                    cartesWagonPosees.remove(c);
                }
            } else {
                carteChoisie = mapMainCourante.get(choixCouleur);
                cartesWagonPosees.add(carteChoisie);
                cartesWagon.remove(carteChoisie);
                choixCartesAchatGare.remove(choixCouleur);
            }

            while (cartesWagonPosees.size() < (4 - this.nbGares)) {
                actualiserMain(choixCartesAchatGare, mapMainCourante, carteChoisie);
                choixCouleur = this.choisir("Choisir couleur de cartesWagons à défausser pour acheter la gare", choixCartesAchatGare, new ArrayList<>(), true);
                if (choixCouleur.equals("")) {
                    decision = "";
                    for (CouleurWagon c : cartesWagonPosees) {
                        cartesWagon.add(c);
                    }
                    cartesWagonPosees.clear();
                    break;
                } else {
                    carteChoisie = mapMainCourante.get(choixCouleur);
                    cartesWagonPosees.add(carteChoisie);
                    cartesWagon.remove(carteChoisie);
                    choixCartesAchatGare.remove(choixCouleur);
                }

            }
            for (CouleurWagon c : cartesWagonPosees) {
                jeu.defausserCarteWagon(c);
            }
            if (!decision.equals("")) {
                cartesWagonPosees.clear();
                v.setProprietaire(this);
                this.nbGares -= 1;
                this.score -= 4;
            } else {
                cartesWagonPosees.clear();
            }

        } else if (routesPossible.containsKey(decision)) {
            Route r = routesPossible.get(decision);
            if (r.getClass().equals((new Tunnel(r.getVille1(), r.getVille2(), r.getLongueur(), r.getCouleur()).getClass()))) {
                log("Vous souhaitez acheter le tunnel " + r.getNom());
                acheterTunnel(r);
            } else {
                log("Vous souhaitez acheter " + r.getNom());
                acheterRoute(r);
                for (CouleurWagon c : cartesWagonPosees) {
                    jeu.defausserCarteWagon(c);
                }
                cartesWagonPosees.clear();
                r.setProprietaire(this);
                nbWagons -= r.getLongueur();
                this.score += donnerPoints(r.getLongueur());
            }
        } else if (decision.equals("")) {
            log("Vous avez choisi de passer votre tour...");
        }
    }


    private void acheterTunnel(Route r) {
        acheterRoute(r);
        ArrayList<CouleurWagon> carteAdefaussTunnel2 = new ArrayList<>();
        boolean test = true;
        /////////////////
        for (int i = 0; i < 3; i++) {
            if (!jeu.getPileCartesWagon().isEmpty()) {
                //log(jeu.getPileCartesWagon().get(0).toString());
                carteAdefaussTunnel2.add(jeu.getPileCartesWagon().get(0));
                //jeu.defausserCarteWagon(jeu.getPileCartesWagon().get(0));
                jeu.getPileCartesWagon().remove(0);
            } else if (!jeu.getDefausseCartesWagon().isEmpty()) {
                //log(jeu.getDefausseCartesWagon().get(0).toString());
                carteAdefaussTunnel2.add(jeu.getDefausseCartesWagon().get(0));
                //jeu.defausserCarteWagon(jeu.getDefausseCartesWagon().get(0));
                jeu.getDefausseCartesWagon().remove(0);
            }
        }
        for (CouleurWagon c :carteAdefaussTunnel2 ){
            jeu.defausserCarteWagon(c);
        }
        log(carteAdefaussTunnel2.toString());
        System.out.println(carteAdefaussTunnel2.toString());
        //////////////////
        List<String> choixCarte = new ArrayList<>();
        List<CouleurWagon> choixCarteObj = new ArrayList<>();
        List<CouleurWagon> choixCarteObj1 = new ArrayList<>();
        List<CouleurWagon> choixCarteObj2 = new ArrayList<>();
        List<String> choixCarte1 = new ArrayList<>();
        List<String> choixCarte2 = new ArrayList<>();
        //choixCarte.add("");
        for (CouleurWagon c : cartesWagonPosees) {
            if (c.equals(CouleurWagon.LOCOMOTIVE)) {
                //choixCarte1.add(c.toString().toUpperCase());
                choixCarteObj1.add(c);
                choixCarte.add(c.toString().toUpperCase());
            } else {
                //choixCarte2.add(c.toString().toUpperCase());
                choixCarte.add(c.toString().toUpperCase());
                choixCarteObj2.add(c);
            }
        }
        if (cartesWagon.contains(CouleurWagon.LOCOMOTIVE)) {
            choixCarte.add(CouleurWagon.LOCOMOTIVE.toString().toUpperCase());
        }
        if (choixCarteObj2.size() != 0) {
            //choixCarte.addAll(choixCarte2);
            choixCarteObj.addAll(choixCarteObj2);
        } else {
            //choixCarte.addAll(choixCarte1);
            choixCarteObj.addAll(choixCarteObj1);
        }
        //log("les cartes choisie"+choixCarte.toString());
        ////////////////////
        for (CouleurWagon cartePiocherPourAchatTunnel : carteAdefaussTunnel2) {
            test = false;
            ///si la carte piocher est une loco et la personne a la possibilité de payer c.a.d sa main contient une carte posee choisie Précédemment
            if ((Objects.equals(cartePiocherPourAchatTunnel.toString(), "Locomotive") || cartesWagonPosees.contains(cartePiocherPourAchatTunnel)) && (cartesWagon.contains(choixCarteObj.get(0)) || cartesWagon.contains(CouleurWagon.LOCOMOTIVE))) {
                String choixAchatTunnel = this.choisir("Alors ? ", choixCarte, new ArrayList<>(), true);
                if (choixAchatTunnel.equals("")) {
                    // log("t'abandonne deja ! ");
                    test = false;
                    break;
                } else {
                    cartesWagonPosees.add(CouleurWagon.valueOf(choixAchatTunnel));
                    cartesWagon.remove(CouleurWagon.valueOf(choixAchatTunnel));
                    test = true;
                    //log("tu paies ! ");
                }
                ///sinon si la carte piocher est une couleur et cette couleur existe dans les cates posee choisie Précédemment et dans sa main
            } /*else if (cartesWagonPosees.contains(cartePiocherPourAchatTunnel) && cartesWagon.contains(cartePiocherPourAchatTunnel) && cartePiocherPourAchatTunnel != (CouleurWagon.LOCOMOTIVE)) {
                String choixAchatTunnel = this.choisir("Alors ? ", choixCarte, new ArrayList<>(), true);
                if (choixAchatTunnel.equals("")) {
                    //log("t'abandonne deja ! ");
                    test = false;
                    break;
                } else {
                    cartesWagonPosees.add(CouleurWagon.valueOf(choixAchatTunnel));
                    cartesWagon.remove(CouleurWagon.valueOf(choixAchatTunnel));
                    test = true;
                    //log("tu paies ! ");
                }
                }*/
            ///sinon si il a de la chance c.a.d la carte piocher n'existe pas dans les cartes posee
            else if (!cartesWagonPosees.contains(cartePiocherPourAchatTunnel) && cartePiocherPourAchatTunnel != (CouleurWagon.LOCOMOTIVE)) {
                //log("la chance!!");
                test = true;
            }
            ///sinon si il abandonne l'achat avec le bouton passer
            else {
                /*for (CouleurWagon c : cartesWagonPosees) {
                    cartesWagon.add(c);
                }
                cartesWagonPosees.clear();*/
                test = false;
                break;
            }
        }
        ///////////
        ///si il a abandonné ou il ne peut simplement pas payer
        if (!test) {
            for (CouleurWagon c : cartesWagonPosees) {
                cartesWagon.add(c);
            }
            cartesWagonPosees.clear();
            Collections.shuffle(cartesWagon);
            log("vous n'assumez pas le payement du tunnel !");
            System.out.println("vous n'assumez pas le payement du tunnel !");
        } else {
            for (CouleurWagon c : cartesWagonPosees) {
                jeu.defausserCarteWagon(c);
            }
            cartesWagonPosees.clear();
            r.setProprietaire(this);
            nbWagons -= r.getLongueur();
            this.score += donnerPoints(r.getLongueur());
            log("félicitation vous aves acheté un tunnel !");
            System.out.println("vous n'assumez pas le payement du tunnel !");
        }
    }

    private void acheterRoute(Route r) {
        List<String> choixCarteAchat = new ArrayList<>();
        HashMap<String, CouleurWagon> mapMainCourante = new HashMap<>();
        int trigger = r.getLongueur();
        if (r.getCouleur() == CouleurWagon.GRIS) {
            // Il s'agit d'un Ferry que le joueur souhaite acheter
            if (getAttribute(r, "nbLocomotives") != null) {
                int loco = (int) getAttribute(r, "nbLocomotives");
                trigger-=loco;
                while (Collections.frequency(this.cartesWagonPosees, CouleurWagon.LOCOMOTIVE) != loco) {
                    System.out.println("Prix du tunnel soustrait");
                    this.cartesWagon.remove(CouleurWagon.LOCOMOTIVE);
                    this.cartesWagonPosees.add(CouleurWagon.LOCOMOTIVE);
                }
            }

            for (CouleurWagon carteMainCourante : this.cartesWagon) {
                if (Collections.frequency(this.cartesWagon, carteMainCourante) + (Collections.frequency(this.cartesWagon, CouleurWagon.LOCOMOTIVE)) >= trigger || carteMainCourante.equals(CouleurWagon.LOCOMOTIVE)) {
                    mapMainCourante.put(carteMainCourante.toString().toUpperCase(), carteMainCourante);
                    choixCarteAchat.add(carteMainCourante.toString().toUpperCase());
                }
            }

        }
        ////////////////
        else {
            for (CouleurWagon carteMainCourante : this.cartesWagon) {
                if (carteMainCourante.equals(r.getCouleur()) || carteMainCourante.equals(CouleurWagon.LOCOMOTIVE)) {
                    mapMainCourante.put(carteMainCourante.toString().toUpperCase(), carteMainCourante);
                    choixCarteAchat.add(carteMainCourante.toString().toUpperCase());
                }
            }
        }

        while (cartesWagonPosees.size() != r.getLongueur()) {
            log(this.getNom() + "choisir les cartes wagon à défausser pour capturer la route");
            String choixCouleur = this.choisir("Choisir couleur de cartesWagons à défausser pour acheter la route", choixCarteAchat, new ArrayList<>(), false);
            CouleurWagon carteChoisie = mapMainCourante.get(choixCouleur);
            cartesWagonPosees.add(carteChoisie);
            this.cartesWagon.remove(carteChoisie);
            if (carteChoisie == CouleurWagon.LOCOMOTIVE) {
                choixCarteAchat.remove(choixCouleur);
            } else {
                actualiserMain(choixCarteAchat, mapMainCourante, carteChoisie);
            }
        }


    }

    private boolean peuxAcheterFerry(Route route) {
        boolean test = false;
        int nbLoco = (int) getAttribute(route, "nbLocomotives");
        if (Collections.frequency(this.cartesWagon, CouleurWagon.LOCOMOTIVE) >= nbLoco) {
            int routeLongueur = route.getLongueur() - nbLoco;
            for (CouleurWagon c : this.cartesWagon) {
                if ((Collections.frequency(this.cartesWagon, c) >= routeLongueur)) {
                    return true;
                }
            }
        }

        return test;
    }


    private void actualiserMain(List<String> choixCarteAchat, HashMap<String, CouleurWagon> mapMainCourante, CouleurWagon carte) {
        if (!(carte == CouleurWagon.LOCOMOTIVE) && !cartesWagonPosees.isEmpty()) {
            mapMainCourante.clear();
            choixCarteAchat.clear();
            for (CouleurWagon c : cartesWagon) {
                if (c.equals(carte) || c.equals(CouleurWagon.LOCOMOTIVE)) {
                    mapMainCourante.put(c.toString().toUpperCase(), c);
                    choixCarteAchat.add(c.toString().toUpperCase());
                }
            }
        }
    }

    private boolean peuxAcheterGris(Route route) {
        int routeLongueur = route.getLongueur();
        boolean test = false;
        for (CouleurWagon c : cartesWagon) {
            if (Collections.frequency(cartesWagon, c) + (Collections.frequency(cartesWagon, CouleurWagon.LOCOMOTIVE)) >= routeLongueur) {
                return true;
            }
        }
        return test;
    }

    private boolean peuxAcheterGare() {
        if (this.nbGares != 0) {
            for (CouleurWagon c : cartesWagon) {
                if (Collections.frequency(cartesWagon, c) + Collections.frequency(cartesWagon, CouleurWagon.LOCOMOTIVE) >= (4 - this.nbGares)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int donnerPoints(int longueur) {
        switch (longueur) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 7;
            case 6:
                return 15;
            case 8:
                return 21;
            default:
                return 0;
        }
    }

    private boolean verifProprio(Route route) {
        boolean test = true;
        for (Route r : this.jeu.getRoutes()) {
            if (r.getVille1() == (route.getVille1()) && r.getVille2() == (route.getVille2()) && r.getCouleur() != route.getCouleur() && r.getProprietaire() == this) {
                test = false;
            }
            //a revoir pour ferry!!!!
            if (r.getVille1() == (route.getVille1()) && r.getVille2() == (route.getVille2()) && r.getCouleur() == route.getCouleur() && r.getProprietaire() == this && getAttribute(r, "nbLocomotives") != null) {
                test = false;
            }
        }
        return test;
    }

    private void deuxiemeCarte() {
        HashMap<String, CouleurWagon> mapChoix = new HashMap();
        List<String> choix = new ArrayList<String>();
        for (CouleurWagon c : jeu.getCartesWagonVisibles()) {
            if (!(c.equals(CouleurWagon.LOCOMOTIVE))) {
                mapChoix.put(c.toString().toUpperCase(), c);
                choix.add(c.toString().toUpperCase());
            }
        }
        if (jeu.getPileCartesWagon().size() > 0) {
            choix.add("GRIS");
        }
        // Proposition du choix //
        String deuxiemeChoix = this.choisir("Veuillez choisir votre seconde carte", choix, new ArrayList<>(), true);
        // Analyse de la décision //
        switch (deuxiemeChoix) {
            case "GRIS":
                this.cartesWagon.add(jeu.piocherCarteWagon());
                break;

            case "":
                break;

            default:
                if (mapChoix.containsKey(deuxiemeChoix)) {
                    CouleurWagon cartePrise = mapChoix.get(deuxiemeChoix);
                    this.cartesWagon.add(cartePrise);
                    jeu.retirerCarteWagonVisible(cartePrise);
                    break;
                }
        }


    }

    public static Object getAttribute(Object obj, String name) {
        Class c = obj.getClass();
        while (c != null) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
                continue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }


}






