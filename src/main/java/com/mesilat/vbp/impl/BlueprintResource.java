package com.mesilat.vbp.impl;

import com.atlassian.confluence.plugins.createcontent.ContentBlueprintManager;
import com.atlassian.confluence.plugins.createcontent.impl.ContentBlueprint;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/blueprint")
@Scanned
public class BlueprintResource {
    @ComponentImport
    private final ContentBlueprintManager contentBlueprintManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public ContentBlueprint get(@QueryParam("id") UUID id) {
       return this.contentBlueprintManager.getById(id);
    }

    public BlueprintResource(ContentBlueprintManager contentBlueprintManager) {
        this.contentBlueprintManager = contentBlueprintManager;
    }
}