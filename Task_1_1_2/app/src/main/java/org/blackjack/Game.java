package org.blackjack;

public class Game {

    private int round;
    private boolean devMode;
    private Human player;
    private Human dealer;

    public Game () {
        this.devMode = false;
        round = 1;
        player = new Human();
        dealer = new Human();
        System.out.println("WELCOME TO BLACKJACK");
    }

    public Game (String devMode) {
        round = 1;
        player = new Human();
        dealer = new Human();
        System.out.println("WELCOME TO BLACKJACK");
        if (devMode == "-d") {
            this.devMode = true;
            System.out.println("OPENED IN DEV MODE");
        }
    }

    public void startRound (){
        System.out.println("round " + round);
        round += 1;

        if (devMode == true) {
        
        }

        dealCards();

        completePrintDeck(player, "player");
        completePrintDeck(dealer, "dealer");
        

        // card deal
            // your deck

            // dealer deck

        // your turn

        // plater takes

        // prob results

        // diller turn

        // diller takes

        // result

        player.clearDeck();
        dealer.clearDeck();
    }

    private void dealCards() {
        player.addCardToDeck();
        player.addCardToDeck();

        dealer.addCardToDeck();
        dealer.addCardToDeck();

        System.out.println("dealer dealed cards");
    }

    private void completePrintDeck (Human human, String name) {
        if (name == "player") {
            human.printDeck(name);
            System.out.print("=> ");
            human.printScoreOfDeck();
        } else {
            human.printHiddenDeck(name);
        }
        System.out.print(System.lineSeparator());
    }
}
