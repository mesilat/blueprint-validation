package com.mesilat.vbp.xwork;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import javax.inject.Inject;

@Scanned
public class ConfigureValidatorsAction extends ConfluenceActionSupport {
    private final I18nResolver resolver;

    public String getPageTitle(){
        return resolver.getText("com.mesilat.vbp.config.title");
    }

    @Override
    public String execute() throws Exception {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (!permissionManager.hasPermission(user, Permission.ADMINISTER, PermissionManager.TARGET_APPLICATION)) {
            return "no-permission";
        } else {
            return "success";
        }
    }

    @Inject
    public ConfigureValidatorsAction(final @ComponentImport I18nResolver resolver){
        this.resolver = resolver;
    }
}