package org.markdowngenerator;

public abstract class Element {

    public abstract String serialize() throws MarkdownException;

    public String toString() {
        try {
            return serialize();
        } catch (MarkdownException e) {
            System.err.println("in toString" + e.toString());
            return "";
        }
    }


    public abstract boolean equals(Object obj);
    // {
    //     return this.equals(obj);
    // }
}