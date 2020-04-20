package com.mesilat.vbp.xwork;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.plugins.createcontent.ContentBlueprintManager;
import com.atlassian.confluence.plugins.createcontent.extensions.ContentTemplateModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.ValidatorManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

@Scanned
public class GlobalConfigAction extends ConfluenceActionSupport {
    private final static String[] VALIDATOR_TYPES = { "LOFV", "REXP", "NUMR", "USER", "PAGE", "DATE", "MODL" };

    @ComponentImport
    private final ContentBlueprintManager contentBlueprintManager;
    private final TemplateManager templateManager;
    private final ValidatorManager validatorManager;

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
                Template template = templateManager.get(key);
                TemplateWrapper templateWrapper;
                if (template == null) {
                    ModuleDescriptor contentTemplateModuleDescriptor = pluginAccessor.getEnabledPluginModule(key);
                    if (contentTemplateModuleDescriptor instanceof ContentTemplateModuleDescriptor) {
                        PageTemplate e = ((ContentTemplateModuleDescriptor) contentTemplateModuleDescriptor).getModule();
                        templateWrapper = new TemplateWrapper(key, e.getName(), Template.ValidationMode.NONE);
                    } else {
                        return;
                    }
                } else {
                    templateWrapper = new TemplateWrapper(template);
                }
                String url = String.format("%s/plugins/createcontent/edit-template.action?key=&contentTemplateRefId=%s", baseUrl, contentTemplateRef.getId().toString());
                templateWrapper.setUrl(url);
                templateWrapper.setUploadEnabled(false);
                templates.add(templateWrapper);
            });
        });
                
        return templates;
    }
    public List getValidators() {
        return validatorManager.list(false);        
    }
    public List getValidatorTypes() {
        List<ValidatorType> validatorTypes = new ArrayList<>();
        Arrays.asList(VALIDATOR_TYPES).forEach(key -> validatorTypes.add(
            new ValidatorType(key, getText(String.format("com.mesilat.vbp.types.%s", key)))
        ));
        return validatorTypes;
    }        

    @Inject
    public GlobalConfigAction(
        ContentBlueprintManager contentBlueprintManager,
        TemplateManager templateManager,
        ValidatorManager validatorManager
    ){
        this.contentBlueprintManager = contentBlueprintManager;
        this.templateManager = templateManager;
        this.validatorManager = validatorManager;
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