package io.ezto.extensions.helpers;

public enum OutputFormat {
    HTML("html"),
    TEXT("text");

    private final String format;

    OutputFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
