package com.mesilat.vbp.impl;

import com.atlassian.confluence.content.render.xhtml.XmlEventReaderFactory;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.plugins.createcontent.ContentBlueprintManager;
import com.atlassian.confluence.plugins.createcontent.extensions.ContentTemplateModuleDescriptor;
import com.atlassian.confluence.util.UserAgentUtil;
import com.atlassian.confluence.xml.XhtmlEntityResolver;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.EXPIRES;
import static com.google.common.net.HttpHeaders.PRAGMA;
import static com.mesilat.vbp.Constants.PLUGIN_KEY;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import me.bvn.confluence.parser.SchemaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Path("/template")
@Scanned
public class TemplateResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);
    private static final Logger EXTRA_TRACE = LoggerFactory.getLogger("extratrace");
    private static final Pattern DECIMAL = Pattern.compile("^\\d+$");

    @ComponentImport
    private final PluginAccessor pluginAccessor;
    @ComponentImport
    private final XmlEventReaderFactory xmlEventReaderFactory;
    @ComponentImport
    private final ContentBlueprintManager contentBlueprintManager;
    @ComponentImport
    private final I18nResolver resolver;
    @ComponentImport
    private final PageTemplateManager pageTemplateManager;

    private final TemplateManager service;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@QueryParam("schema") Boolean schema) {
        LOGGER.debug("List template settings");
        return Response.ok(service.list(schema == null? false: schema)).build();
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("key") String templateKey) {
        LOGGER.debug(String.format("Get template settings for %s", templateKey));

        Template template = service.get(templateKey);
        if (template == null) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity("Template settings could not be found")
                .build();
        } else {
            return Response.ok(template).build();
        }
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response post(Template template){
        LOGGER.debug(String.format("Post template settings"));

        service.create(template);
        return get(template.getTemplateKey());
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response put(@PathParam("key") String templateKey, Template template){
        LOGGER.debug(String.format("Update template settings"));

        if (templateKey == null) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("Template key is not defined")
                .build();
        } else {
            service.update(templateKey, template);
            return get(template.getTemplateKey());
        }
    }
    
    @DELETE
    @Path("/{key}")
    public Response delete(@PathParam("key") String templateKey){
        service.delete(templateKey);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Path("/{key}/content")
    @Produces(MediaType.TEXT_XML + ";charset=utf-8")
    public Response getContent(@PathParam("key") String templateKey) {
        LOGGER.debug(String.format("Get content of template %s", templateKey));
        
        PageTemplate pageTemplate = getPageTemplateByModuleKey(templateKey);
        if (pageTemplate == null) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(String.format("Page template could not be found: %s", templateKey))
                .build();
        } else {
            String prettyPrinted = makePretty(pageTemplate.getContent());
            String fileName = String.format("%s.json", pageTemplate.getTitle());
            return Response
                .status(Response.Status.OK)
                .header(CONTENT_DISPOSITION, "attachment; filename*=Base64''" + Base64.getEncoder().encodeToString(fileName.getBytes()))
                .header(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(PRAGMA, "no-cache")
                .header(EXPIRES, "0")
                .entity(prettyPrinted)
                .build();
        }
    }

    @PUT
    @Path("/{key}/content")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.TEXT_XML)
    public Response putContent(@PathParam("key") String templateKey, String content) {
        LOGGER.debug(String.format("Put content of template %s", templateKey));
        
        ValidationError error = this.validate(content);
        if (error != null) {
            return Response.status(Status.BAD_REQUEST).entity(error).build();
        } else {
            try {
                PageTemplate pageTemplate = getPageTemplateByModuleKey(templateKey);
                if (pageTemplate == null) {
                    return Response.status(Status.NOT_FOUND).build();
                }
                PageTemplate originalPageTemplate = (PageTemplate)pageTemplate.clone();
                pageTemplate.setContent(content);
                pageTemplateManager.savePageTemplate(pageTemplate, originalPageTemplate);
                return Response.ok().build();
            } catch (CloneNotSupportedException ex) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
            }
        }
    }

    @GET
    @Path("/{key}/generate-schema")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response schema(@PathParam("key") String templateKey){
        LOGGER.debug(String.format("Create JSON schema for template %s", templateKey));

        PageTemplate pageTemplate = getPageTemplateByModuleKey(templateKey);
        if (pageTemplate == null) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(String.format("Page template could not be found: %s", templateKey))
                .build();
        }
        
        try (StringReader sr = new StringReader(pageTemplate.getContent())) {
            XMLEventReader reader = xmlEventReaderFactory.createStorageXmlEventReader(sr);
            ObjectNode schema = SchemaParser.parse(reader, EXTRA_TRACE);
            ObjectMapper mapper = new ObjectMapper();
            String serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
            String fileName = String.format("%s.json", pageTemplate.getTitle());

            return Response.status(Response.Status.OK)
                .header(CONTENT_DISPOSITION, "attachment; filename*=Base64''" + Base64.getEncoder().encodeToString(fileName.getBytes()))
                .header(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(PRAGMA, "no-cache")
                .header(EXPIRES, "0")
                .entity(serialized)
                .build();
        } catch (XMLStreamException | JsonProcessingException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/list-blueprint-templates")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response list() throws JsonProcessingException {
        LOGGER.debug("List available blueprint templates");

        ArrayNode templates = mapper.createArrayNode();
        contentBlueprintManager.getAll().forEach(blueprint -> {
            ObjectNode bp = mapper.createObjectNode();
            bp.put("uuid", blueprint.getId().toString());
            bp.put("id", blueprint.getModuleCompleteKey());
            bp.put("name", blueprint.getI18nNameKey() == null
                ? blueprint.getModuleCompleteKey()
                : resolver.getText(blueprint.getI18nNameKey())
            );

            blueprint.getContentTemplateRefs().forEach(contentTemplateRef -> {
                ObjectNode template = mapper.createObjectNode();
                template.put("uuid", contentTemplateRef.getId().toString());
                template.put("id", contentTemplateRef.getModuleCompleteKey());
                template.put("name", contentTemplateRef.getI18nNameKey() == null
                    ? contentTemplateRef.getModuleCompleteKey()
                    : resolver.getText(contentTemplateRef.getI18nNameKey())
                );
                template.set("blueprint", bp);
                templates.add(template);
            });
        });
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(templates)).build();
    }

    @PUT
    @Path("/{key}/schema")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putSchema(@PathParam("key") String templateKey, String schema) {
        LOGGER.debug(String.format("Upload schema for template %s", templateKey));

        Template template = service.get(templateKey);
        if (template == null) {
            PageTemplate pageTemplate = getPageTemplateByModuleKey(templateKey);
            if (pageTemplate == null) {
                return Response.status(Status.NOT_FOUND).entity("Page template could not be found").build();
            }
            template = new Template(Long.toString(pageTemplate.getId()), pageTemplate.getTitle(), Template.ValidationMode.NONE);
            template.setSchema(schema);
            service.create(template);
        } else {
            template.setSchema(schema);
            service.update(template.getTemplateKey(), template);
        }
        return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Path("/{key}/schema")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getSchema(@PathParam("key") String templateKey) throws UnsupportedEncodingException{
        LOGGER.debug(String.format("Get JSON schema for template %s", templateKey));

        Template template = service.get(templateKey);
        if (template == null || template.getSchema() == null) {
            return schema(templateKey);
        } else {
            String fileName = String.format("%s.json", template.getTemplateName());
            return Response.status(Response.Status.OK)
                .header(CONTENT_DISPOSITION, 
                    getBrowserFamily() == UserAgentUtil.BrowserFamily.FIREFOX
                        ? "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName,"UTF-8")
                        : "attachment; filename=" + URLEncoder.encode(fileName,"UTF-8")
                )
                .header(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(PRAGMA, "no-cache")
                .header(EXPIRES, "0")
                .entity(template.getSchema())
                .build();
        }
    }

    private PageTemplate getPageTemplateByModuleKey(String moduleCompleteKey) {
        if (DECIMAL.matcher(moduleCompleteKey).matches()) {
            Long templateId = Long.parseLong(moduleCompleteKey);
            return pageTemplateManager.getPageTemplate(templateId);
        } else {
            ModuleDescriptor contentTemplateModuleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
            if (contentTemplateModuleDescriptor instanceof ContentTemplateModuleDescriptor) {
                try {
                    PageTemplate e = ((ContentTemplateModuleDescriptor) contentTemplateModuleDescriptor).getModule();
                    if (e != null) {
                        return (PageTemplate)e.clone();
                    }
                } catch (CloneNotSupportedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return null;
    }
    private String makePretty(String storageFormat) {
        try {
            Document doc = this.getStorageDocument(storageFormat);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
            NodeList children = doc.getDocumentElement().getChildNodes();
            
            for (int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
                transformer.transform(new DOMSource(child), result);
            }
            
            return writer.toString();
        } catch (TransformerException | SAXException | ParserConfigurationException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    private Document getStorageDocument(String storageFormat) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setExpandEntityReferences(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        XhtmlEntityResolver xmlResolver = new XhtmlEntityResolver();
        String entityDTD = xmlResolver.createDTD();
        String xmlStorageFormat = "<!DOCTYPE xml [ " + entityDTD + "]><storage-format>" + storageFormat + "</storage-format>";

        db.setEntityResolver(xmlResolver);
        InputSource is = new InputSource(new StringReader(xmlStorageFormat));
        return db.parse(is);
    }
    private ValidationError validate(String storageFormat) {
        try {
            this.getStorageDocument(storageFormat);
            return null;
        } catch (SAXParseException ex) {
            return new ValidationError(ex.getMessage(), ex.getLineNumber(), ex.getColumnNumber());
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            return new ValidationError(ex.getMessage());
        }
    }
    private UserAgentUtil.BrowserFamily getBrowserFamily() {
        UserAgentUtil.UserAgent userAgent = UserAgentUtil.getCurrentUserAgent();
        if (userAgent == null) {
            return UserAgentUtil.BrowserFamily.CHROME;
        }
        UserAgentUtil.Browser browser = userAgent.getBrowser();
        if (browser == null) {
            return UserAgentUtil.BrowserFamily.CHROME;
        }
        return browser.getBrowserFamily() == null
            ? UserAgentUtil.BrowserFamily.CHROME
            : browser.getBrowserFamily();
    }

    @Inject
    public TemplateResource(PluginAccessor pluginAccessor, XmlEventReaderFactory xmlEventReaderFactory,
            ContentBlueprintManager contentBlueprintManager, I18nResolver resolver, TemplateManager service,
            PageTemplateManager pageTemplateManager){
        this.pluginAccessor = pluginAccessor;
        this.xmlEventReaderFactory = xmlEventReaderFactory;
        this.contentBlueprintManager = contentBlueprintManager;
        this.resolver = resolver;
        this.service = service;
        this.pageTemplateManager = pageTemplateManager;
    }

    @XmlRootElement(name = "response")
    private class ValidationError {
        @XmlAttribute
        private final String message;
        @XmlAttribute
        private final Integer line;
        @XmlAttribute
        private final Integer column;

        public String getMessage() {
            return this.message;
        }

        private ValidationError(String message) {
            this(message, null, null);
        }
        private ValidationError(String message, Integer line, Integer column) {
            this.message = message;
            this.line = line;
            this.column = column;
        }
    }
}