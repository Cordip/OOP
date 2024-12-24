package org.markdowngenerator;

public class Image extends Element {
    private String url;
    private String alt;


    public static class Builder {
        protected Image image;


        public Builder() {
            image = new Image("", "");
        }


        public Builder setUrl(String url) {
            image.url = url;
            return this;
        }


        public Builder setAlt(String alt) {
            image.alt = alt;
            return this;
        }


        public Image build() {
            return image;
        }
    }
    

    public Image(String url, String alt) {
        this.url = url;
        this.alt = alt;
    }


    public String getUrl() {
        return url;
    }


    public String getAlt() {    
        return alt;    
    }   

    public void setUrl(String url) {
        this.url = url;
    }


    public void setAlt(String alt) {
        this.alt = alt;
    }
    
    @Override
    public String serialize() {
        return String.format("![%s](%s)\n", alt, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Image) {
            Image other = (Image) obj;
            if (!alt.equals(other.alt)) {
                return false;
            }
            if (!url.equals(other.url)) {
                return false;
            }
        }
        
        return false;
    }

}
