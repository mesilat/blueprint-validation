package com.mesilat.vbp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.DataService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
public class DataResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long pageId) throws JsonProcessingException {
        LOGGER.debug(String.format("Get data for page %d", pageId));

        ObjectNode info = dataService.getPageInfo(pageId);
        if (info == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(info)).build();
        }
    }
    
    public DataResource(DataService dataService) {
        this.dataService = dataService;
    }
}