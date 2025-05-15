package org.markdowngenerator.text;

import org.markdowngenerator.MarkdownException;

public class Quote extends Text {

    Quote(String text) {
        super(text);
    }

 
    @Override
    public String serialize() {
        try {
            return "> " + super.serialize();
        } catch (MarkdownException e) {
            System.err.println("in serialize" + e.toString());
            return "";
        }
        
    }


}