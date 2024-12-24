package org.markdowngenerator;

public class Link extends Element {
    private String url;
    private String text;


    Link() {
        this.url = "";
        this.text = "";
    }


    Link(String text, String url) {
        this.url = url;
        this.text = text;
    }

    public static class Builder implements org.markdowngenerator.Builder {
        protected Link link;

        public Builder () {
            link = new Link("", "");
        }

        public Builder (String text, String url) {
            link = new Link(text, url);
        }


        public Builder setUrl(String url) {
            link.url = url;
            return this;
        }

 
        public Builder setText(String text) {
            link.text = text;
            return this;
        }


        public Link build() {
            return link;
        }
    }

    public String getUrl() {
        return url;
    }


    public String getText() {
        return text;
    }

 
    public void setText(String text) {
        this.text = text;
    }

 
    public void setUrl(String url) {
        this.url = url;
    }

 
    @Override
    public String serialize() {
        String result = "[" + text + "](" + url + ")\n";
        return result;
    }

   
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Link) {
            Link other = (Link) obj;
            if (!text.equals(other.text)) {
                return false;
            }
            if (!url.equals(other.url)) {
                return false;
            }
        }
        
        return false;
    }
}