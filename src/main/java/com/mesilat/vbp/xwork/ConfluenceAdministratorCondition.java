package com.mesilat.vbp.xwork;

import com.atlassian.confluence.plugin.descriptor.web.WebInterfaceContext;
import com.atlassian.confluence.plugin.descriptor.web.conditions.BaseConfluenceCondition;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.spring.container.ContainerManager;

public class ConfluenceAdministratorCondition extends BaseConfluenceCondition {
    @Override
    public boolean shouldDisplay(WebInterfaceContext context) {
        PermissionManager permissionManager = (PermissionManager)ContainerManager.getComponent("permissionManager");
        return permissionManager.isConfluenceAdministrator(context.getCurrentUser());
    }
}
