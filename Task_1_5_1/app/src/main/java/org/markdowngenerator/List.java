package org.markdowngenerator;

import java.util.ArrayList;

import org.markdowngenerator.text.Text;

public class List extends Element {

    private ArrayList<Element> elements;

    public List() {
        this.elements = new ArrayList<>();
    }



    public void add(Element element) {
        elements.add(element);
    }

    public static class Builder implements org.markdowngenerator.Builder {
        protected List list;
        
        public Builder() {
            list = new List();
        }

        public Builder add(String text) {
            list.add(new Text(text));
            return this;
        }

        public Builder add(Element element) {
            list.add(element);
            return this;
        }

        public List build() {
            return list;
        }
    }

    @Override
    public String serialize() throws MarkdownException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            result.append((i + 1)).append(". ").append(elements.get(i)).append("\n");
        }
        return result.toString().trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        
        List other = (List) obj;
        if (elements.size() != other.elements.size()) {
            return false;
        }
        
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).equals(other.elements.get(i))) {
                return false;
            }
        }
        return true;
    }
}