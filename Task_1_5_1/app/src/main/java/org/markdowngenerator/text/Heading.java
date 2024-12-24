package org.markdowngenerator.text;

import org.markdowngenerator.MarkdownException;

public class Heading extends Text {
    private int level;

    public Heading(String text, int level) {
        super(text);
        this.level = level;
    }

    public static class Builder extends Text.Builder { 
        protected Heading heading;

        public Builder setLevel(int level) {
            if (level < 0 || level > 6) {
                throw new IllegalArgumentException("Level must be between 0 and 6");
            }
            this.heading.level = level;
            return this;
        }

        public Heading build() {
            return heading;
        }
    }

    @Override
    public String serialize() {
        try {
            return System.lineSeparator() + "#".repeat(level) + " " + super.serialize() + System.lineSeparator();
        } catch (MarkdownException e) {
            System.err.println("in serialize" + e.toString());
            return "";
        }
        
    }

}
