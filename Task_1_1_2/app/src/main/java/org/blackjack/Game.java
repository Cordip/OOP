package org.blackjack;


public class Game {

    private int round;
    private boolean devMode;
    private Human player;
    private Human dealer;
    private Receiver receiver;


    public Game (Receiver rec) {
        this.devMode = false;
        round = 1;
        receiver = rec;
        player = new Human();
        dealer = new Human();
        System.out.println("WELCOME TO BLACKJACK");
    }

    public Game (String devMode, Receiver rec) {
        round = 1;
        player = new Human();
        dealer = new Human();
        receiver = rec;
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

        printAllDecks(true);

        if (winCheck()) {
            
        } else {
            playerTurn();

            if (winCheck()) {
                
            } else {
                dealerTurn();
                
            }
        }

        compareScoreWins();

        printScore();

        player.clear();
        dealer.clear();
    }

    private void dealCards() {
        player.addCardToDeck();
        player.addCardToDeck();

        dealer.addCardToDeck();
        dealer.addCardToDeck();

        System.out.println("dealer dealed cards");
    }

    private void completePrintDeck (Human human, String name) {
        human.isDeckOverflow();
        human.printDeck(name);
        System.out.print("=> ");
        human.printScoreOfDeck();
        System.out.print(System.lineSeparator());
    }

    private void completePrintDeck (Human human, String name, boolean hiddenDealer) {
        if (hiddenDealer) {
            human.printHiddenDeck(name);
        } else {
            human.isDeckOverflow();
            human.printDeck(name);
            System.out.print("=> ");
            human.printScoreOfDeck();
        }
        System.out.print(System.lineSeparator());
    }

    private void printAllDecks (boolean hiddenDealer) {
        completePrintDeck(player, "player");
        completePrintDeck(dealer, "dealer", hiddenDealer);
    }

    private void playerTurn () {
        int num = 0;
        while(!player.isDeckOverflow() && player.getScore() <= 21) {
            if (winCheck()) {
                printAllDecks(true);
                break;
            }
            System.out.println("1 for new card, 0 to stop");
            num = receiver.collectIntFromCmd();
            if (num == 1) {
                player.addCardToDeck();
                printAllDecks(true);
            } else if (num == 0) {
                printAllDecks(true);
                break;
            } else {
                System.out.println("wrong number");
            }
        }
    }

    private void dealerTurn () {
        int num = 0;
        System.out.println("dealer opens his hand");
        printAllDecks(false);
        while(!dealer.isDeckOverflow() && dealer.getScore() <= 21) {
            
            if (winCheck()) {
                printAllDecks(false);
                break;
            }
            if (player.getScore() < dealer.getScore()) {
                num = 0;
            } else if (dealer.getScore() < 18 || player.getScore() >= dealer.getScore()) {
                num = 1;
            } else {
                num = 0;
            }
            if (num == 1) {
                System.out.println("dealer takes card");
                dealer.addCardToDeck();
                printAllDecks(false);
            } else if (num == 0) {
                System.out.println("dealer stops");
                printAllDecks(false);
                break;
            } else {
                System.out.println("wrong number");
            }
        }
    }

    private boolean winCheck () {
        if (player.getScore() == 21 || dealer.getScore() > 21) {
            return true;
        } else if (dealer.getScore() == 21 || player.getScore() > 21) {;
            return true;
        }
        return false;
    }

    private void compareScoreWins () {
        int playerScore = player.getScore();
        int dealerScore = dealer.getScore();
        if (playerScore == 21 || dealerScore > 21) {
            playerWins();
        } else if (dealerScore == 21 || playerScore > 21) {
            dealerWins();
        } else if (playerScore <= 21 && playerScore >= dealerScore ) {
            playerWins();
        } else if (dealerScore <= 21 && dealerScore > playerScore) {
            dealerWins();
        }
    }

    private boolean overflowCheck () {
        if (player.isDeckOverflow()) {
            return true;
        } else if (dealer.isDeckOverflow()) {
            return true;
        }
        return false;
    }

    private void playerWins () {
        System.out.println("player wins");
        player.plusWinPoints();
    }

    private void dealerWins () {
        System.out.println("dealer wins");
        dealer.plusWinPoints();
    }

    private void printScore() {
        System.out.printf("Player points: %d ; Dealer points: %d\n", player.getWinPoints(), dealer.getWinPoints());
    }
}
