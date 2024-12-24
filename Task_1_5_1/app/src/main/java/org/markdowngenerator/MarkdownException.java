package org.markdowngenerator;

public class MarkdownException extends Exception {
    public MarkdownException() {
        super();
    }

    public MarkdownException(String s) {
        super(s);
    }

    public MarkdownException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public MarkdownException(Throwable throwable) {
        super(throwable);
    }
}
