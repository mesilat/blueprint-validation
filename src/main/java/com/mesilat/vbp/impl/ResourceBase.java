package com.mesilat.vbp.impl;

import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;

public class ResourceBase {
    private static final Pattern DECIMAL = Pattern.compile("^\\d+$");

    private final PermissionManager permissionManager;
    private final I18nResolver resolver;
    private final PageTemplateManager pageTemplateManager;
    private final SpacePermissionManager spacePermissionManager;

    protected boolean isConfluenceAdmin() {
        return permissionManager.isConfluenceAdministrator(AuthenticatedUserThreadLocal.get());
    }
    protected Response checkUserCanAdministerTemplate(String templateKey) {
        if (isGlobalTemplate(templateKey)) {
            if (!isConfluenceAdmin()) {
                return Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(resolver.getText("com.mesilat.general.error.permission.rest"))
                    .build();
            }
        } else {
            PageTemplate pageTemplate = pageTemplateManager.getPageTemplate(Long.parseLong(templateKey));
            if (pageTemplate == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(resolver.getText("com.mesilat.vbp.template.error.notfound"))
                    .build();
            }
            Space space = pageTemplate.getSpace();
            if (space == null) {
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(resolver.getText("com.mesilat.vbp.template.error.nospace"))
                    .build();
            }
            if (!spacePermissionManager.hasPermission(SpacePermission.ADMINISTER_SPACE_PERMISSION, space, AuthenticatedUserThreadLocal.get())) {
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(resolver.getText("com.mesilat.vbp.template.error.notspaceadmin"))
                    .build();
            }
        }

        return null;
    }
    protected boolean isGlobalTemplate(String templateKey) {
        return !DECIMAL.matcher(templateKey).matches();
    }

    public ResourceBase(
        PermissionManager permissionManager, I18nResolver resolver,
        PageTemplateManager pageTemplateManager, SpacePermissionManager spacePermissionManager
    ){
        this.permissionManager = permissionManager;
        this.resolver = resolver;
        this.pageTemplateManager = pageTemplateManager;
        this.spacePermissionManager = spacePermissionManager;
    }
}
