package com.mesilat.vbp.converter;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.api.TextConverterService;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 Preserved for future use

async function convertContentToStorageFormat(html) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/converter/to-storage-format`,
    type: 'POST',
    data: html,
    dataType: 'text',
    contentType: 'text/plain',
    processData: false,
    timeout: 30000
  });
}
*/
@Path("/converter")
public class TextConverterResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final TextConverterService converter;

    @POST
    @Path("/to-storage-format")
    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    @XsrfProtectionExcluded
    public Response post(String editorFormat) {
        try {
            LOGGER.debug(String.format("Convert to Confluence storage format"));
            return Response.ok(converter.convertToStorage(editorFormat)).build();
        } catch (XhtmlException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
    
    @Inject
    public TextConverterResource(TextConverterService converter) {
        this.converter = converter;
    }
}
