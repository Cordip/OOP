package org.blackjack;

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;

public abstract class Human {

    private List<Card> deck;
    private int winPoints;

    public Human () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }

    // public void addCardToDeck () {
    //     deck.add(new Card());
    // }

    public void addCardToDeck (Deck deck) {
        this.deck.add(deck.get());
    }



    public void printDeck (String name) {
        System.out.printf("\t%s deck: [ ", name);
        int cnt = 0;
        for (Card i : deck) {
            cnt += 1;
            if (deck.size() > cnt) {
                System.out.printf("%s, ", i.cardToString());
            } else {
                System.out.printf("%s", i.cardToString());
            }
            
            
        }
        System.out.print("] ");
        //System.out.print(System.lineSeparator());
    }

    public void printDeck () {
        System.out.print("[ ");
        int cnt = 0;
        for (Card i : deck) {
            cnt += 1;
            if (deck.size() > cnt) {
                System.out.printf("%s", i.cardToString());
            } else {
                System.out.printf(", %s", i.cardToString());
            }
            
        }
        System.out.print("] ");
        //System.out.print(System.lineSeparator());
    }

    public static void printHiddenDeck (Human human, String name) {
        try {
            if (name != "dealer") {
                throw new Exception("Wrong use of function \" printHiddenDeck \". Use this only for dealer and after deck had dealted");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 

        System.out.printf("\t%s deck: [ ", name);
        int cnt = 0;
        
        for (Card i : human.deck) {
            cnt += 1;
            if (human.deck.size() > cnt) {
                System.out.printf("%s, ", i.cardToString());
            } else {
                System.out.print("<closed card> ");
            }
            
        }
        System.out.print("] ");
        //System.out.print(System.lineSeparator());
    }

    public void printScoreOfDeck () {
        int totalWeight = 0;
        for (Card i : deck) {
            totalWeight += i.getWeight();
        }
        System.out.print(totalWeight);
    }

    public boolean isDeckOverflow () {
        if (getScore() > 21) {
            scoreDownAces();
            if (getScore() > 21) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void scoreDownAces () {
        Card tranCard;
        if (getScore() > 21) {
            for (int index = 0; index < deck.size(); ++index) {
                tranCard = deck.get(index);
                if (tranCard.getWeight() == 11) {
                    tranCard.changeWeight(1);
                    deck.set(index, tranCard);
                    break;
                }
            }
        }
    }

    public int getScore () {
        int score = 0;
        for (Card i : deck) {
            score += i.getWeight();
        }
        return score;
    }

    public void clear() {
        this.deck.clear();
    }

    public void plusWinPoints () {
        this.winPoints += 1;
    }

    public int getWinPoints () {
        return this.winPoints;
    }

    private static void completePrintDeck (Human human, String name) {
        human.isDeckOverflow();
        human.printDeck(name);
        System.out.print("=> ");
        human.printScoreOfDeck();
        System.out.print(System.lineSeparator());
    }

    

    private static void completePrintDeck (Human human, String name, boolean hiddenDealer) {
        if (hiddenDealer) {
            printHiddenDeck(human, name);
        } else {
            human.isDeckOverflow();
            human.printDeck(name);
            System.out.print("=> ");
            human.printScoreOfDeck();
        }
        System.out.print(System.lineSeparator());
    }

    public static void printAllDecks (Human player, Human dealer, boolean hiddenDealer) {
        completePrintDeck(player, "player");
        completePrintDeck(dealer, "dealer", hiddenDealer);
    }


}


class Player extends Human {
    private List<Card> deck;
    private int winPoints;

    public Player () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }

    private void turn (Receiver receiver, Deck deck) {
        int num = 0;
        while(!isDeckOverflow() && getScore() <= 21) {
            if (winCheck()) {
                printAllDecks(this, true);
                break;
            }
            System.out.println("1 for new card, 0 to stop");
            num = receiver.collectIntFromCmd();
            if (num == 1) {
                addCardToDeck(deck);
                printAllDecks(true);
            } else if (num == 0) {
                printAllDecks(true);
                break;
            } else {
                System.out.println("wrong number");
            }
        }
    }
}

class Dealer extends Human {

    private List<Card> deck;
    private int winPoints;

    public Dealer () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }


    public void dealCards(Deck deck, Human player) {
        player.addCardToDeck(deck);
        player.addCardToDeck(deck);

        addCardToDeck(deck);
        addCardToDeck(deck);

        System.out.println("dealer dealed cards");
    }

    private void turn (Deck deck, Human player) {
        int num = 0;
        System.out.println("dealer opens his hand");
        printAllDecks(false);
        while(!isDeckOverflow() && getScore() <= 21) {
            
            if (winCheck()) {
                printAllDecks(false);
                break;
            }
            if (player.getScore() < getScore()) {
                num = 0;
            } else if (getScore() < 18 || player.getScore() >= getScore()) {
                num = 1;
            } else {
                num = 0;
            }
            if (num == 1) {
                System.out.println("dealer takes card");
                addCardToDeck(deck);
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
}

class DevPlayer extends Human {
    private List<Card> deck;
    private int winPoints;

    public DevPlayer () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }
}

class DevDealer extends Human {
    private List<Card> deck;
    private int winPoints;

    public DevDealer () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }
}

