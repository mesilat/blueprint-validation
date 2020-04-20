package com.mesilat.vbp.impl;

import com.atlassian.sal.api.websudo.WebSudoNotRequired;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.ValidationService;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mesilat.vbp.impl.ValidationServiceImpl.ValidationTask;


@Path("/validation")
@WebSudoNotRequired
public class ValidationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);
    private final ValidationService service;

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("uuid") String uuid) {
        LOGGER.trace("Get validation task status");
        ValidationTask task = service.getValidationTask(uuid);
        return task == null
            ? Response.status(Response.Status.NOT_FOUND).build()
            : Response.ok(task).build();
    }

    @Inject
    public ValidationResource(ValidationService service){
        this.service = service;
    }
}