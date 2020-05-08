package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.plugins.createcontent.extensions.ContentTemplateModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.TemplateInfo;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.ao.DBParam;
import net.java.ao.Query;

@ExportAsService ({TemplateManager.class})
@Named ("blueprintTemplateValidationManager")
public class TemplateManagerImpl implements TemplateManager {
    private static final Pattern DECIMAL = Pattern.compile("^\\d+$");

    @ComponentImport
    private final ActiveObjects ao;
    @ComponentImport
    private final PluginAccessor pluginAccessor;
    @ComponentImport
    private final PageTemplateManager pageTemplateManager;

    @Override
    public List<Template> list(boolean schemas) {
        return Arrays.asList(ao.find(TemplateInfo.class, schemas? Query.select(): Query.select("TEMPLATE_KEY")))
            .stream()
            .map(
                info -> schemas
                ? TemplateInfo.toTemplate(info)
                : new Template(info.getTemplateKey(), info.getValidationMode())
            )
            .collect(Collectors.toList());
    }
    @Override
    public Template get(String templateKey) {
        return TemplateInfo.toTemplate(ao.get(TemplateInfo.class, templateKey));
    }
    @Override
    public void setValidationMode(String templateKey, Template.ValidationMode validationMode) {
        TemplateInfo info = ao.get(TemplateInfo.class, templateKey);
        if (info == null) {
            ao.create(TemplateInfo.class, new DBParam("TEMPLATE_KEY", templateKey), new DBParam("VALIDATION_MODE", validationMode.toString()));
        } else {
            info.setValidationMode(validationMode.toString());
            info.save();
        }
    }
    @Override
    public void setSchema(String templateKey, String schema) {
        TemplateInfo info = ao.get(TemplateInfo.class, templateKey);
        if (info == null) {
            ao.create(TemplateInfo.class, new DBParam("TEMPLATE_KEY", templateKey), new DBParam("SCHEMA", schema));
        } else {
            info.setSchema(schema);
            info.save();
        }
    }
    @Override
    public void clear(String templateKey) {
        ao.deleteWithSQL(TemplateInfo.class, "TEMPLATE_KEY = ?", templateKey);
    }
    @Override
    public String getTemplateTitle(String templateKey) {
        if (DECIMAL.matcher(templateKey).matches()) {
            PageTemplate pageTemplate = pageTemplateManager.getPageTemplate(Long.parseLong(templateKey));
            if (pageTemplate != null) {
                return pageTemplate.getName();
            }
        } else {
            try {
                ModuleDescriptor contentTemplateModuleDescriptor = pluginAccessor.getEnabledPluginModule(templateKey);
                if (contentTemplateModuleDescriptor instanceof ContentTemplateModuleDescriptor) {
                    PageTemplate pageTemplate = ((ContentTemplateModuleDescriptor) contentTemplateModuleDescriptor).getModule();
                    return pageTemplate.getName();
                }
            } catch(Throwable ignore) {
            }
        }

        return null;
    }

    @Inject
    public TemplateManagerImpl(ActiveObjects ao, PluginAccessor pluginAccessor,
        PageTemplateManager pageTemplateManager
    ){
        this.ao = ao;
        this.pluginAccessor = pluginAccessor;
        this.pageTemplateManager = pageTemplateManager;
    }
}