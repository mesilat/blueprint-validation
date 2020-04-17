package com.mesilat.vbp.impl;

import com.mesilat.vbp.api.Validator;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mesilat.vbp.api.ValidatorManager;


@Path("/validator")
@Scanned
public class ValidatorResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.vbp");

    private final ValidatorManager service;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@QueryParam("extensive") Boolean extensive) {
        LOGGER.debug("List validators");
        return Response.ok(service.list(extensive == null? false: extensive)).build();
    }

    @GET
    @Path("/css")
    @Produces("text/css;charset=utf-8")
    public Response css() {
        LOGGER.debug("List validators");
        return Response.ok(service.css()).build();
    }
    
    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("code") String code) {
        LOGGER.debug(String.format("Get validator info for %s", code));

        Validator validator = service.get(code);
        if (validator == null) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity("Validator could not be found")
                .build();
        } else {
            return Response.ok(validator).build();
        }
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response post(Validator validator){
        LOGGER.debug(String.format("Post validator info"));

        service.create(validator);
        return get(validator.getCode());
    }

    @PUT
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response put(@PathParam("code") String code, Validator validator){
        LOGGER.debug(String.format("Update validator info"));

        if (code == null) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Validator code is not defined")
                .build();
        } else {
            service.update(code, validator);
            return get(validator.getCode());
        }
    }
    
    @DELETE
    @Path("/{code}")
    public Response delete(@PathParam("code") String code){
        service.delete(code);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Inject
    public ValidatorResource(ValidatorManager service){
        this.service = service;
    }
}