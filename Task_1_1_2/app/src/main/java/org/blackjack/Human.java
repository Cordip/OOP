package org.blackjack;

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;

public class Human {

    private List<Card> deck;
    private int winPoints;

    public Human () {
        deck = new ArrayList<Card>();
        winPoints = 0;

    }

    public void addCardToDeck () {
        deck.add(new Card());
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

    public void printHiddenDeck (String name) {
        try {
            if (name != "dealer") {
                throw new Exception("Wrong use of function \" printHiddenDeck \". Use this only for dealer and after deck had dealted");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 

        System.out.printf("\t%s deck: [ ", name);
        int cnt = 0;
        
        for (Card i : deck) {
            cnt += 1;
            if (deck.size() > cnt) {
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


}
