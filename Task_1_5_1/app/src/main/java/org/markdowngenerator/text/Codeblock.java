package org.markdowngenerator.text;

import org.markdowngenerator.MarkdownException;

public class Codeblock extends Text {

    private String language;

    public Codeblock(String str) {
        super(str);
    }

    public Codeblock(String str, String language) {
        super(str);
        this.language = language;
    }

    @Override
    public String serialize() {
        try {
            if (language == null) {
                return "```" + System.lineSeparator() + super.serialize() + System.lineSeparator() + "```";
            } else {
                return "```" + language + System.lineSeparator() + super.serialize() + System.lineSeparator() + "```";
            }
        } catch (MarkdownException e) {
            System.err.println("in serialize" + e.toString());
            return "";
        }
        
        
    }
}