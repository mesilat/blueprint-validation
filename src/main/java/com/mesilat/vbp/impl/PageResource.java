package com.mesilat.vbp.impl;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.plugins.highlight.SelectionModificationException;
import com.atlassian.confluence.plugins.highlight.SelectionStorageFormatModifier;
import com.atlassian.confluence.plugins.highlight.model.TextSearch;
import com.atlassian.confluence.plugins.highlight.model.XMLModification;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.Constants;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Path("/page")
@Scanned
public class PageResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final SelectionStorageFormatModifier selectionStorageFormatModifier;
    private final ValidationServiceEx validationService;

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
    /**
     * Creates "inline" data attribute on page
     * @param attributeBean Attribute data
     * @return Message
     */
    @POST
    @Path("inline")
    public Response inline(InlineAttributeBean attributeBean){
        try {
            if (selectionStorageFormatModifier.markSelection(
                attributeBean.getPageId(),
                attributeBean.getLastFetchTime(),
                new TextSearch(attributeBean.getSelectedText(), attributeBean.getNumMatches(), attributeBean.getIndex()),
                new XMLModification(String.format("<span class=\"dsattr-%s\" />", attributeBean.getAttributeName()))
            )){
                LOGGER.debug(String.format("Successfully added attribute %s to page %d", attributeBean.getAttributeName(), attributeBean.getPageId()));
                Page page = pageManager.getPage(attributeBean.getPageId());
                validationService.registerValidationTask(UUID.randomUUID().toString(), page.getId(), page.getTitle());
                return Response.status(Response.Status.OK).entity("Attribute added successfully").build();
            } else {
                LOGGER.warn(String.format("Failed to add attribute %s to page %d, selectionStorageFormatModifier.markSelection() returned \"false\"", attributeBean.getAttributeName(), attributeBean.getPageId()));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Attribute add failed").build();
            }
        } catch (SAXException | SelectionModificationException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @Inject
    public PageResource(PageManager pageManager,
            SelectionStorageFormatModifier selectionStorageFormatModifier,
            ValidationServiceEx validationService
    ) {
        this.pageManager = pageManager;
        this.selectionStorageFormatModifier = selectionStorageFormatModifier;
        this.validationService = validationService;
    }
}