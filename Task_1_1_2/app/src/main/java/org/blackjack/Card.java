package org.blackjack;

import java.util.Random;

public class Card {
    private int weight;
    private Suits suit;
    private Face face;
    private static Random rnd = new Random();

    public enum Face {
        ONE, 
        TWO, 
        THREE, 
        FOUR, 
        FIVE, 
        SIX, 
        SEVEN, 
        EIGHT, 
        NINE, 
        TEN, 
        JACK, 
        QUEEN, 
        KING,
        ACE;
    }
    
    public enum Suits {
        CLUBS, 
        DIAMONDS, 
        HEARTS, 
        SPADES;
    }

    Card () {
        this.suit = generateSuit();
        this.face = generateFace();
        this.weight = calculateWeight();
    }

    private static Suits generateSuit () {
        Suits suit = Suits.values()[rnd.nextInt(Suits.values().length)];
        return suit;
    }

    private static Face generateFace () {
        Face face = Face.values()[rnd.nextInt(Face.values().length)];
        return face;
    }

    private int calculateWeight () {
        int cnt = 0;
        for (Face i : Face.values()) {
            if (cnt < 10) {
                cnt += 1;
            }
            if (i == this.face) {
                break;
            }
        }
        if (this.face.name() == "ACE") {
            cnt = 11;
        }
        return cnt;
    }

    public void printCard () {
        System.out.println(this.suit + " " + this.face + " " + this.weight);
    }

    public void printCard (Card card) {
        System.out.println(card.suit + " " + card.face + " " + card.weight);
    }

    public String cardToString () {
        return(String.format("%s %s %s ", this.suit.toString(), this.face.toString(), Integer.toString(this.weight)));
    }

    public String cardToString (Card card) {
        return(String.format("%s %s %s ", card.suit.toString(), card.face.toString(), Integer.toString(card.weight)));
    }

    public int getWeight () {
        return this.weight;
    }

    public int getWeight (Card card) {
        return card.weight;
    }

}
