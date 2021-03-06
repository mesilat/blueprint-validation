package com.mesilat.vbp.xwork;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.plugins.createcontent.ContentBlueprintManager;
import com.atlassian.confluence.plugins.createcontent.extensions.ContentTemplateModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import static com.mesilat.vbp.Constants.REST_API_PATH;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.ValidatorManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Scanned
public class GlobalConfigAction extends ConfluenceActionSupport {
    private final static String[] VALIDATOR_TYPES = { "LOFV", "REXP", "NUMR", "USER", "PAGE", "DATE", "MODL" };

    private TemplateManager templateManager;
    private ValidatorManager validatorManager;
    @ComponentImport
    private ContentBlueprintManager contentBlueprintManager;
    @ComponentImport
    private PageTemplateManager pageTemplateManager;

    private String title;
    private String message;
    private String cssClass;

    public String getTitle() {
        return title;
    }
    public String getMessage() {
        return message;
    }
    public String getCssClass() {
        return cssClass;
    }
    public String getPluginEndpoint() {
        return String.format("%s%s", this.getCurrentRequest().getContextPath(), REST_API_PATH);
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }
    public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    public ValidatorManager getValidatorManager() {
        return validatorManager;
    }
    public void setValidatorManager(ValidatorManager validatorManager) {
        this.validatorManager = validatorManager;
    }
    public ContentBlueprintManager getContentBlueprintManager() {
        return contentBlueprintManager;
    }
    public void setContentBlueprintManager(ContentBlueprintManager contentBlueprintManager) {
        this.contentBlueprintManager = contentBlueprintManager;
    }
    public PageTemplateManager getPageTemplateManager() {
        return pageTemplateManager;
    }
    public void setPageTemplateManager(PageTemplateManager pageTemplateManager) {
        this.pageTemplateManager = pageTemplateManager;
    }

    @Override
    public String doDefault() {
        if (getPermissionManager().isSystemAdministrator(getAuthenticatedUser())) {
            return INPUT;
        } else {
            title = getText("title.not.permitted");
            message = getText("not.permitted.description");
            cssClass = "not-permitted-background-image";
            return ERROR;
        }
    }

    public List getPageTemplates() {
        String baseUrl = getGlobalSettings().getBaseUrl();
        List<TemplateWrapper> templates = new ArrayList<>();
        contentBlueprintManager.getAll().forEach(blueprint -> {
            blueprint.getContentTemplateRefs().forEach(contentTemplateRef -> {
                String key = contentTemplateRef.getModuleCompleteKey();
                Template template = getTemplateManager().get(key);
                ModuleDescriptor contentTemplateModuleDescriptor = pluginAccessor.getEnabledPluginModule(key);
                TemplateWrapper templateWrapper;
                if (contentTemplateModuleDescriptor instanceof ContentTemplateModuleDescriptor) {
                    PageTemplate pageTemplate = ((ContentTemplateModuleDescriptor) contentTemplateModuleDescriptor).getModule();
                    templateWrapper = new TemplateWrapper(key, pageTemplate.getName(),
                        template == null? Template.ValidationMode.NONE: template.getValidationMode()
                    );
                } else {
                    return;
                }
                String url = String.format("%s/plugins/createcontent/edit-template.action?key=&contentTemplateRefId=%s", baseUrl, contentTemplateRef.getId().toString());
                templateWrapper.setUrl(url);
                templateWrapper.setUploadEnabled(false);
                templates.add(templateWrapper);
            });
        });
        getPageTemplateManager().getGlobalPageTemplates().forEach(t -> {
            PageTemplate pageTemplate = (PageTemplate)t;
            String key = Long.toString(pageTemplate.getId());
            Template template = getTemplateManager().get(key);
            TemplateWrapper templateWrapper = new TemplateWrapper(key, pageTemplate.getName(),
                template == null? Template.ValidationMode.NONE: template.getValidationMode()
            );
            String url = String.format("%s/pages/templates2/editpagetemplate.action?entityId=%d", baseUrl, pageTemplate.getId());
            templateWrapper.setUrl(url);
            templateWrapper.setUploadEnabled(false);
            templates.add(templateWrapper);
        });
                
        return templates;
    }
    public List getValidators() {
        return getValidatorManager().list(false);        
    }
    public List getValidatorTypes() {
        List<ValidatorType> validatorTypes = new ArrayList<>();
        Arrays.asList(VALIDATOR_TYPES).forEach(key -> validatorTypes.add(
            new ValidatorType(key, getText(String.format("com.mesilat.vbp.types.%s", key)))
        ));
        return validatorTypes;
    }        

    public static class ValidatorType {
        private final String id;
        private final String name;

        public String getId() {
            return id;
        }
        public String getName() {
            return name;
        }

        public ValidatorType(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}