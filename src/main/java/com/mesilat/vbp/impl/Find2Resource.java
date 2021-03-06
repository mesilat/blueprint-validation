package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.config.db.HibernateConfig;
import com.atlassian.config.util.BootstrapUtils;
import com.atlassian.confluence.core.persistence.hibernate.HibernateSessionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.sf.hibernate.HibernateException;

@Path("/find2")
public class Find2Resource extends FindResourceBase {
    private static final Integer LIMIT = 1000000;

    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response find2(@QueryParam("q") String q, @QueryParam("label") List<String> labels, @QueryParam("limit") Integer limit){
        int _limit = limit == null? LIMIT: limit;
        HibernateSessionManager hibernateSessionManager = (HibernateSessionManager)ContainerManager.getComponent("hibernateSessionManager");
        return ao.executeInTransaction(()->{
            try {
                Connection conn = hibernateSessionManager.getSession().connection();
                switch (databaseType) {
                    case POSTGRESQL:
                        HibernateConfig hibernateConfig = BootstrapUtils.getBootstrapManager().getHibernateConfig();
                        String schema = getSchema(hibernateConfig.getHibernateProperties().getProperty("hibernate.connection.url"));
                        if (conn.getSchema() == null) {
                            conn.setSchema(schema == null? "public": schema);
                        }
                        break;
                    default:
                }

                StringBuilder sb = new StringBuilder();
                if (labels == null || labels.isEmpty()){
                    return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(find(conn, q, _limit))).build();
                } else {
                    return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(find2(conn, q, labels, _limit))).build();
                }
            } catch(SQLException | IOException | HibernateException ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
            }
        });
    }

    @Inject
    public Find2Resource(@ComponentImport ActiveObjects ao, @ComponentImport SettingsManager settingsManager){
        super(settingsManager.getGlobalSettings().getBaseUrl(), ao.moduleMetaData().getDatabaseType());
        this.ao = ao;
    }
}
