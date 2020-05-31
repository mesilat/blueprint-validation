package com.mesilat.vbp.impl;

import com.mesilat.vbp.api.Validator;
import com.mesilat.vbp.api.ValidatorManager;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/refdata")
public class RefDataResource {
    private final ValidatorManager manager;

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("code") String code){
        Validator validator = manager.get(code);
        Map<String,Object> response = new HashMap<>();
        response.put("code", validator.getCode());
        response.put("name", validator.getName());
        response.put("data", validator.getText());
        response.put("type", 0);
        response.put("status", 2);
        return Response.ok(response).build();
    }

    public RefDataResource(ValidatorManager manager) {
        this.manager = manager;
    }
}