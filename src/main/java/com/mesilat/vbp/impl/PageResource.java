package com.mesilat.vbp.impl;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.Constants;
import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/page")
public class PageResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    @ComponentImport
    private final PageManager pageManager;

    @PUT
    @Path("/{id}")
    public Response put(@PathParam("id") Long pageId, @QueryParam("suppress-events") boolean suppressEvents, String body){
        Page page = pageManager.getPage(pageId);
        if (page == null)
            return Response.status(Response.Status.NOT_FOUND).entity(String.format("Page could not be found: %d", pageId)).build();

        if (body == null || body.isEmpty())
            return Response.status(Response.Status.NO_CONTENT).entity("Page content cannot be empty").build();

        if (body.equals(page.getBodyAsString()))
            return Response.status(Response.Status.CONFLICT).entity("Page content has not changed").build();

        DefaultSaveContext sc = new DefaultSaveContext(true/*suppressNotifications*/, true/*updateLastModifier*/, suppressEvents);
        pageManager.saveNewVersion(page, (Page p) -> {
            p.setBodyAsString(body);
            LOGGER.warn(String.format("Created new version of page %d", pageId));
        }, sc);

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Inject
    public PageResource(PageManager pageManager) {
        this.pageManager = pageManager;
    }
}