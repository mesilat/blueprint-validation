package com.mesilat.vbp.impl;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.websudo.WebSudoNotRequired;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.Validator;
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
@WebSudoRequired
public class ValidatorResource extends ResourceBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final I18nResolver resolver;
    private final ValidatorManager service;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @WebSudoNotRequired
    public Response get(@QueryParam("extensive") Boolean extensive) {
        LOGGER.trace("List validators");
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
    @WebSudoNotRequired
    public Response css() {
        LOGGER.trace("Get validators CSS");
        return Response.ok(service.css()).build();
    }
    
    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @WebSudoNotRequired
    public Response get(@PathParam("code") String code) {
        LOGGER.trace(String.format("Get validator %s", code));

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
        if (!isConfluenceAdmin()) {
            return Response
                .status(Response.Status.FORBIDDEN)
                .entity(resolver.getText("com.mesilat.general.error.permission.rest"))
                .build();
        }

        LOGGER.trace(String.format("Create validator %s", validator.getCode()));
        service.create(validator);
        return get(validator.getCode());
    }

    @PUT
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response put(@PathParam("code") String code, Validator validator){
        if (!isConfluenceAdmin()) {
            return Response
                .status(Response.Status.FORBIDDEN)
                .entity(resolver.getText("com.mesilat.general.error.permission.rest"))
                .build();
        }

        if (code == null) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Validator code is not defined")
                .build();
        }

        LOGGER.trace(String.format("Update validator %s", code));
        service.update(code, validator);
        return get(validator.getCode());
    }
    
    @DELETE
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response delete(@PathParam("code") String code){
        if (!isConfluenceAdmin()) {
            return Response
                .status(Response.Status.FORBIDDEN)
                .entity(resolver.getText("com.mesilat.general.error.permission.rest"))
                .build();
        }

        LOGGER.trace(String.format("Delete validator %s", code));
        service.delete(code);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upload(String serialized) throws IOException {
        if (!isConfluenceAdmin()) {
            return Response
                .status(Response.Status.FORBIDDEN)
                .entity(resolver.getText("com.mesilat.general.error.permission.rest"))
                .build();
        }

        LOGGER.trace("Import validators");
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
    public ValidatorResource(
        ValidatorManager service,
        @ComponentImport PermissionManager permissionManager,
        @ComponentImport I18nResolver resolver
    ){
        super(permissionManager, resolver, null, null);
        this.service = service;
        this.resolver = resolver;
    }
}