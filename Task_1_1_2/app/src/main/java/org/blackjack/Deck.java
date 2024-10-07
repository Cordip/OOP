package org.blackjack;

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;

import java.util.Random;

public class Deck {
    private List<Card> deck;
    Deck() {
        deck = new ArrayList<Card>();
    }

    public void fill() {
        for (var i : Card.Suits.values()) {
            for (var j : Card.Face.values()) {
                deck.add(new Card(i, j));
            }
        }

        Random rnd = new Random();
        // 14 * 4 = 56
        for (int i = 0; i < 56; i++) {
            swap(i, rnd.nextInt(56-i)+i);
        }

    }

    private void swap (int frst, int scnd) {
        Card tmp = deck.get(frst);
        deck.set(frst, deck.get(scnd));
        deck.set(scnd, tmp);
    }

    public Card get() {
        Card card = deck.getLast();
        deck.removeLast();
        return card;
    }

    public void clear() {
        deck.clear();
    }
}
