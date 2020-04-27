package com.mesilat.vbp.api;

import java.util.List;

public interface TemplateManager {
    List<Template> list(boolean schemas);
    Template get(String templateKey);
    void setValidationMode(String templateKey, Template.ValidationMode validationMode);
    void setSchema(String templateKey, String schema);
    void clear(String templateKey);
}