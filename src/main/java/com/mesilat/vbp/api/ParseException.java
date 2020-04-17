package com.mesilat.vbp.api;

public class ParseException extends Exception {
    public ParseException(String text) {
        super(text);
    }
    public ParseException(String text, Throwable cause) {
        super(text, cause);
    }
    public ParseException(Throwable cause) {
        super(cause);
    }
}
