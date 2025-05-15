package org.markdowngenerator.text;

import org.markdowngenerator.Element;
import org.markdowngenerator.MarkdownException;
import org.markdowngenerator.StringUtil;

public class Text extends Element {

    private String str;

    public Text(String str) {
        this.str = str;
    }
    
    @Override
    public String serialize() throws MarkdownException {
        if (str == null) {
            throw new MarkdownException("Value is null");
        }
        return str.toString();
    }

    public static class Builder implements org.markdowngenerator.Builder {
        protected Text text;
        
        public Builder() {
            text = new Text("");
        }

        public Builder(Text text) throws MarkdownException {
            this.text = text;
            
        }

        public Builder setTextType(Text text) {
            this.text = text;
            return this;
        }

        public Builder setStr(String str) {
            this.text.str = str;
            return this;
        }
        
        public Text build() {
            return text;
        }
    }
    
    public static class Bold extends Text {
        
        public Bold(String text) {
            super(text);
        }

        
        @Override
        public String serialize() {
            try {
                return StringUtil.surroundValueWith(super.serialize(), "**");
            } catch (MarkdownException e) {
                System.err.println("in serialize" + e.toString());
                return "";
            }
            
        }
    }

    
    public static class Italic extends Text {

        public Italic(String text) {
            super(text);
        }

        @Override
        public String serialize() {
            try {
                return StringUtil.surroundValueWith(super.serialize(), "*");
            } catch (MarkdownException e) {
                System.err.println("in serialize" + e.toString());
                return "";
            }
        }
    }

    public static class Strikethrough extends Text {
        
        public Strikethrough(String text) {
            super(text);
        }

        @Override
        public String serialize() {
            try {
                return StringUtil.surroundValueWith(super.serialize(), "~~");
            } catch (MarkdownException e) {
                System.err.println("in serialize" + e.toString());
                return "";
            }
        }
    }

    // one line code
    public static class Code extends Text {
        
        public Code(String text) throws MarkdownException {
            super(text);
            if (text.contains("\n")) {
                throw new MarkdownException("String contains \\n (more then one line), use Codeblock instead");
            }
        }

        @Override
        public String serialize() {
            try {
                return StringUtil.surroundValueWith(super.serialize(), "`");
            } catch (MarkdownException e) {
                System.err.println("in serialize" + e.toString());
                return "";
            }
            
        }
    }
    
    public String getString() {
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Text) {
            Text other = (Text) obj;

            return str.equals(other.toString());
        }

        return false;
        
    }
}