package org.blackjack;

// koloda kart na odin round
// deleate task 111
// more direct/logical command approach
// make abstract human for test
// hidden in card

public class Game {

    private int round;
    private boolean devMode;
    private Human player;
    private Human dealer;
    private Deck deck;
    private Receiver receiver;


    public Game (Receiver rec) {
        this.devMode = false;
        round = 1;
        receiver = rec;
        player = new Player();
        dealer = new Dealer();
        deck = new Deck();
        System.out.println("WELCOME TO BLACKJACK");
    }

    public Game (String devMode, Receiver rec) {
        round = 1;
        player = new Player();
        dealer = new Dealer();
        receiver = rec;
        deck = new Deck();
        System.out.println("WELCOME TO BLACKJACK");
        if (devMode == "-d") {
            this.devMode = true;
            System.out.println("OPENED IN DEV MODE");
        }
    }

    public Game (String devMode, Receiver rec, Player player, Dealer dealer) {
        round = 1;
        this.player = player;
        this.dealer = dealer;
        receiver = rec;
        deck = new Deck();
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

        deck.fill();
        
        Dealer.dealCards(deck, player, dealer);
        //dealer.dealCards();

        printAllDecks(true);

        if (winCheck()) {
            
        } else {
            playerTurn();
            //player.turn();

            if (winCheck()) {
                
            } else {
                dealerTurn();
                
            }
        }

        compareScoreWins();

        printScore();

        player.clear();
        dealer.clear();
        deck.clear();
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
