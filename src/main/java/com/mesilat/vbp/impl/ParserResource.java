package com.mesilat.vbp.impl;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.ParserService;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/parser")
@Scanned
public class ParserResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final ParserService parserService;
    private final ObjectMapper mapper = new ObjectMapper();
    
    @POST
    @Path("/parse")
    @XsrfProtectionExcluded
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response parse(String storageFormat, @QueryParam("space-key") String spaceKey) {
        try {
            LOGGER.trace(String.format("Parse data using space %s", spaceKey));
            ObjectNode data = parserService.parse(storageFormat, spaceKey);
            return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)).build();
        } catch (ParseException | JsonProcessingException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
    
    @Inject
    public ParserResource(ParserService parserService) {
        this.parserService = parserService;
    }
}
