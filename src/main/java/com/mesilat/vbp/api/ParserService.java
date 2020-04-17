package com.mesilat.vbp.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ParserService {
    ObjectNode parse(String storgeFormat, String spaceKey) throws ParseException;
    ObjectNode generateSchema(String storgeFormat) throws ParseException;
    ObjectNode generateSchemaForTemplate(String templateKey) throws ParseException;
}
