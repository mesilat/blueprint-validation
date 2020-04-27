package com.mesilat.vbp.xwork;

import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.actions.SpaceAdminAction;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@Scanned
public class SpaceConfigAction extends SpaceAdminAction {
    @ComponentImport
    private final PageTemplateManager pageTemplateManager;
    private final TemplateManager manager;
    
    @Override
    public String doDefault() {
        return INPUT;
    }

    public List getPageTemplates() {
        String baseUrl = getGlobalSettings().getBaseUrl();
        Space space = spaceManager.getSpace(this.getKey());
        if (space == null) {
            return Collections.EMPTY_LIST;
        }
        List pageTemplates = pageTemplateManager.getPageTemplates(space);
        List<TemplateWrapper> templates = new ArrayList<>();
        pageTemplates.forEach(pt -> {
            PageTemplate pageTemplate = (PageTemplate)pt;
            if (pageTemplate.getModuleKey() != null || pageTemplate.isGlobalPageTemplate()) {
                return; // global templates are configured elsewhere
            }
            Template template = manager.get(Long.toString(pageTemplate.getId()));
            TemplateWrapper templateWrapper = (template == null)?
                new TemplateWrapper(Long.toString(pageTemplate.getId()), pageTemplate.getName(), Template.ValidationMode.NONE):
                new TemplateWrapper(template, pageTemplate.getName());
            String url = String.format("%s/pages/templates2/viewpagetemplate.action?entityId=%d&key=%s", baseUrl, pageTemplate.getId(), space.getKey());
            templateWrapper.setUrl(url);
            templateWrapper.setUploadEnabled(true);
            templates.add(templateWrapper);
        });
                
        return templates;
    }
    
    @Inject
    public SpaceConfigAction(PageTemplateManager pageTemplateManager, TemplateManager manager) {
        this.pageTemplateManager = pageTemplateManager;
        this.manager = manager;
    }
}
