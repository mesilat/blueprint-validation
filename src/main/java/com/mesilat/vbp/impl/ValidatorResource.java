package com.mesilat.vbp.impl;

import com.mesilat.vbp.api.Validator;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.io.IOException;
import java.util.List;

@Path("/validator")
@Scanned
public class ValidatorResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.vbp");

    private final ValidatorManager service;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@QueryParam("extensive") Boolean extensive) {
        LOGGER.debug("List validators");
        try {
            List<Validator> validators = service.list(extensive == null? false: extensive);
            String serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(validators);
            return Response.ok(serialized).build();
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
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

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upload(String serialized) throws IOException{
        LOGGER.debug(String.format("Upload validators"));
        ArrayNode arr = (ArrayNode)mapper.readTree(serialized);
        arr.forEach(obj -> {
            Validator validator = new Validator(obj.get("code").asText(), obj.has("name")? obj.get("name").asText(): null);
            validator.setType(obj.get("type").asText());
            validator.setPrompt(obj.has("prompt")? obj.get("prompt").asText(): null);
            validator.setWarning(obj.has("warning")? obj.get("warning").asText(): null);
            validator.setText(obj.has("text")? obj.get("text").asText(): null);
            validator.setModule(obj.has("module")? obj.get("module").asText(): null);
            if (!service.contains(validator.getCode())) {
                service.create(validator);
            }
        });

        return Response.status(Response.Status.ACCEPTED).build();
    }
    
    @Inject
    public ValidatorResource(ValidatorManager service){
        this.service = service;
    }
}