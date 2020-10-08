package com.mesilat.vbp.impl;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.PathService;
import com.mesilat.vbp.api.ValidationService;
import static com.mesilat.vbp.servlet.PageServletBase.PROPERTY_TEMPLATE;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
public class DataResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);
    private final PathService pathService;
    private final ValidationService validationService;
    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response post(PathService.Args args) {
        return Response.ok(pathService.evalCached(args)).build();
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@QueryParam("path") String path) {
        LOGGER.trace(String.format("Get all data, path %s", path));

        return Response.ok(new Gson().toJson(pathService.evaluate(path))).build();
    }

    @GET
    @Path("/template")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@QueryParam("templateKey") String templateKey, @QueryParam("path") String path) {
        LOGGER.trace(String.format("Get data for template %s, path %s", templateKey, path));

        return Response.ok(new Gson().toJson(pathService.evaluate(templateKey, path))).build();
    }

    /**
     * Return page validation info and JSON data.
     *
     * Using with curl:
     * curl -v -H "X-Atlassian-Token: no-check" \
     * -u "${WIKI_USER}:${WIKI_PASSWORD}" \
     * "${WIKI_HOME}${REST_API_PATH}/data/${pageId}"
     *
     * @param pageId
     * @param path JSON path to extract
     * @return JSON object
     * @throws JsonProcessingException
     * @throws IOException
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long pageId, @QueryParam("path") String path) throws JsonProcessingException, IOException {
        LOGGER.trace(String.format("Get data for page %d, path %s", pageId, path));

        return Response.ok(new Gson().toJson(pathService.evaluate(pageId, path))).build();
    }

    @POST
    @Path("/validate/{id}")
    public Response validate(@PathParam("id") Long pageId, @QueryParam("templateKey") String templateKey) {
        LOGGER.trace(String.format("Convert and validate page %d and template %s", pageId, templateKey));

        Page page = pageManager.getPage(pageId);
        if (page == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Page could not be found").build();
        }
        try {
            validationService.validate(templateKey == null? page.getProperties().getStringProperty(PROPERTY_TEMPLATE): templateKey, page);
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (ParseException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/template/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getTemplate(@PathParam("id") Long pageId) {
        Page page = pageManager.getPage(pageId);
        if (page == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(String.format("The page %d could not be found", pageId)).build();
        }
        return Response.ok(String.format("\"%s\"", page.getProperties().getStringProperty(PROPERTY_TEMPLATE))).build();
    }

    @POST
    @Path("/template/{id}")
    public Response setTemplate(@PathParam("id") Long pageId, @QueryParam("templateKey") String templateKey) {
        LOGGER.trace(String.format("Set page %d template key to %s", pageId, templateKey));

        Page page = pageManager.getPage(pageId);
        return transactionTemplate.execute(() -> {
            page.getProperties().setStringProperty(PROPERTY_TEMPLATE, templateKey);
            return Response.status(Response.Status.ACCEPTED).build();
        });
    }

    @Inject
    public DataResource(PathService pathService, ValidationService validationService, PageManager pageManager,
        TransactionTemplate transactionTemplate
    ) {
        this.pathService = pathService;
        this.validationService = validationService;
        this.pageManager = pageManager;
        this.transactionTemplate = transactionTemplate;
    }
}
