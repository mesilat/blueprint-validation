package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.TemplateInfo;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.ao.DBParam;
import net.java.ao.Query;

@ExportAsService ({TemplateManager.class})
@Named ("blueprintTemplateValidationManager")
public class TemplateManagerImpl implements TemplateManager {
    @ComponentImport
    private final ActiveObjects ao;

    @Override
    public List<Template> list(boolean schemas) {
        return Arrays.asList(ao.find(TemplateInfo.class, schemas? Query.select(): Query.select("TEMPLATE_KEY")))
            .stream()
            .map(
                info -> schemas
                ? TemplateInfo.toTemplate(info)
                : new Template(info.getTemplateKey(), info.getTemplateName(), info.getValidationMode())
            )
            .collect(Collectors.toList());
    }

    @Override
    public Template get(String templateKey) {
        return TemplateInfo.toTemplate(ao.get(TemplateInfo.class, templateKey));
    }

    @Override
    public void delete(String templateKey) {
        ao.deleteWithSQL(TemplateInfo.class, "TEMPLATE_KEY = ?", templateKey);
    }

    @Override
    public void create(Template template) {
        ao.executeInTransaction(() -> {
            _create(template);
            return null;
        });
    }

    @Override
    public void create(List<Template> templates) {
        ao.executeInTransaction(() -> {
            templates.forEach(template -> {
                if (ao.count(TemplateInfo.class, "TEMPLATE_KEY = ?", template.getTemplateKey()) == 0) {
                    _create(template);
                }
            });
            return null;
        });
    }
    private void _create(Template template) {
        TemplateInfo info = ao.create(TemplateInfo.class,
            new DBParam("TEMPLATE_KEY", template.getTemplateKey()),
            new DBParam("TEMPLATE_NAME", template.getTemplateName())
        );
        info.setValidationMode(template.getValidationMode() == null? null: template.getValidationMode().toString());
        info.setSchema(template.getSchema());
        info.save();        
    }

    @Override
    public void update(String templateKey, Template template) {
        ao.executeInTransaction(() -> {
            TemplateInfo info = ao.get(TemplateInfo.class, templateKey);
            if (info == null) {
                throw new RuntimeException("Template info could not be found");
            }
            if (templateKey.equals(template.getTemplateKey())) {
                info.setTemplateName(template.getTemplateName());
                info.setValidationMode(template.getValidationMode() == null? null: template.getValidationMode().toString());
                info.setSchema(template.getSchema());
                info.save();        
            } else {
                delete(templateKey);
                _create(template);
            }
            return null;
        });
    }
  
    @Inject
    public TemplateManagerImpl(final @ComponentImport ActiveObjects ao){
        this.ao = ao;
    }
}