package com.mesilat.vbp.api;

import java.util.List;

public interface TemplateManager {
    List<Template> list(boolean schemas);
    Template get(String templateKey);
    void delete(String templateKey);
    void create(Template template);
    void create(List<Template> templates);
    void update(String templateKey, Template template);
}