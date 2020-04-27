package com.mesilat.vbp.api;

public interface ParserService {
    String parse(String storgeFormat, String spaceKey) throws ParseException;
    String generateSchema(String storgeFormat) throws ParseException;
    String generateSchemaForTemplate(String templateKey) throws ParseException;
}
