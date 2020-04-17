package com.mesilat.vbp.xwork;

import com.atlassian.confluence.spaces.actions.SpaceAdminAction;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import static com.opensymphony.xwork.Action.SUCCESS;
import javax.inject.Inject;

@Scanned
public class ConfigureSpaceAction extends SpaceAdminAction {
    private final I18nResolver resolver;

    public String getPageTitle(){
        return resolver.getText("com.mesilat.vbp.admin.title");
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @Inject
    public ConfigureSpaceAction(final @ComponentImport I18nResolver resolver){
        this.resolver = resolver;
    }
}
