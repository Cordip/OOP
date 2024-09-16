package org.blackjack;

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;

public class Human {

    private int score;

    private List<Card> deck;

    public Human () {
        score = 0;
        deck = new ArrayList<Card>();

    }

    public void clearDeck () {
        deck.clear();
    }

    public void addCardToDeck () {
        deck.add(new Card());
    }



    public void printDeck (String name) {
        System.out.printf("%s deck: [ ", name);
        boolean firstComma = true;
        for (Card i : deck) {
            if (firstComma == true) {
                System.out.printf("%s", i.cardToString());
                firstComma = false;
            } else {
                System.out.printf(", %s", i.cardToString());
            }
            
        }
        System.out.print("] ");
        //System.out.print(System.lineSeparator());
    }

    public void printDeck () {
        System.out.print("[ ");
        boolean firstComma = true;
        for (Card i : deck) {
            if (firstComma == true) {
                System.out.printf("%s", i.cardToString());
                firstComma = false;
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

        System.out.printf("%s deck: [ ", name);
        boolean firstComma = true;
        
        for (Card i : deck) {
            if (firstComma == true) {
                System.out.printf("%s", i.cardToString());
                firstComma = false;
            } else {
                System.out.print("<closed card>");
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


}
