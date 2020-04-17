package com.mesilat.vbp.impl;

import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/space")
@Scanned
public class SpaceResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.vbp");

    @ComponentImport
    private final PageTemplateManager pageTemplateManager;
    @ComponentImport
    private final SpaceManager spaceManager;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/{space}/templates")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listTemplates(@PathParam("space")String spaceKey) throws JsonProcessingException {
        ArrayNode arr = mapper.createArrayNode();
        LOGGER.debug("List available space templates");
        Space space = spaceManager.getSpace(spaceKey);
        List pageTemplates = pageTemplateManager.getPageTemplates(space);

        pageTemplates.forEach(item -> {
            if (item instanceof PageTemplate) {
                PageTemplate pageTemplate = (PageTemplate)item;
                ObjectNode template = mapper.createObjectNode();
                template.put("id", pageTemplate.getId());
                template.put("title", pageTemplate.getTitle());
                template.put("key", pageTemplate.getModuleKey());
                template.put("pluginKey", pageTemplate.getPluginKey());
                arr.add(template);
            }
        });
        
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arr)).build();
    }

    @GET
    @Path("/templates")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listTemplates() throws JsonProcessingException {
        ArrayNode arr = mapper.createArrayNode();
        LOGGER.debug("List available global templates");
        List pageTemplates = pageTemplateManager.getGlobalPageTemplates();

        pageTemplates.forEach(item -> {
            if (item instanceof PageTemplate) {
                PageTemplate pageTemplate = (PageTemplate)item;
                ObjectNode template = mapper.createObjectNode();
                template.put("id", pageTemplate.getId());
                template.put("title", pageTemplate.getTitle());
                template.put("key", pageTemplate.getModuleKey());
                template.put("pluginKey", pageTemplate.getPluginKey());
                arr.add(template);
            }
        });

        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arr)).build();
    }

    @Inject
    public SpaceResource(PageTemplateManager pageTemplateManager, SpaceManager spaceManager){
        this.pageTemplateManager = pageTemplateManager;
        this.spaceManager = spaceManager;
    }
}