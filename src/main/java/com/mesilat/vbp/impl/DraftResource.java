package com.mesilat.vbp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.drafts.DraftService;
import com.mesilat.vbp.drafts.DraftService.Draft;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/draft")
public class DraftResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long draftId) throws JsonProcessingException {
        LOGGER.debug(String.format("Get template key for draft %d", draftId));

        DraftService.Draft draft = DraftService.getDraft(draftId);
        if (draft == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(draft).build();
        }
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(Draft draft){
        DraftService.addDraft(draft);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}