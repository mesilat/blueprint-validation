package com.mesilat.vbp.impl;

import com.atlassian.confluence.content.render.xhtml.DefaultXmlEventReaderFactory;
import com.atlassian.confluence.content.render.xhtml.XmlEventReaderFactory;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.confluence.pages.templates.PageTemplateManager;
import com.atlassian.confluence.plugins.createcontent.extensions.ContentTemplateModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.blueprints.parser.DefaultPageResolver;
import com.mesilat.blueprints.parser.DefaultUserResolver;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.ParserService;
import java.io.StringReader;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import me.bvn.confluence.parser.DataParser;
import me.bvn.confluence.parser.SchemaParser;
import me.bvn.confluence.parser.model.PageResolver;
import me.bvn.confluence.parser.model.UserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExportAsService ({ParserService.class})
@Named ("vbpParserService")
public class ParserServiceImpl implements ParserService {
    private static final Logger EXTRA_TRACE = LoggerFactory.getLogger("extratrace");
    private static final Pattern DECIMAL = Pattern.compile("^\\d+$");
    
    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final PageTemplateManager pageTemplateManager;
    @ComponentImport
    private final PluginAccessor pluginAccessor;
    private final XmlEventReaderFactory xmlEventReaderFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String parse(String storgeFormat, String spaceKey) throws ParseException {
        try (StringReader sr = new StringReader(storgeFormat)) {
            XMLEventReader reader = xmlEventReaderFactory.createStorageXmlEventReader(sr);
            UserResolver userResolver = new DefaultUserResolver(userManager);
            PageResolver pageResolver = new DefaultPageResolver(pageManager, spaceKey);
            ObjectNode node = DataParser.parse(reader, userResolver, pageResolver, EXTRA_TRACE);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (XMLStreamException | JsonProcessingException ex) {
            throw new ParseException(ex);
        }
    }

    @Override
    public String generateSchema(String storgeFormat) throws ParseException {
        try (StringReader sr = new StringReader(storgeFormat)) {
            XMLEventReader reader = xmlEventReaderFactory.createStorageXmlEventReader(sr);
            ObjectNode node = SchemaParser.parse(reader, EXTRA_TRACE);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (XMLStreamException | JsonProcessingException ex) {
            throw new ParseException(ex);
        }
    }

    @Override
    public String generateSchemaForTemplate(String templateKey) throws ParseException {
        PageTemplate pageTemplate = getPageTemplateByModuleKey(templateKey);
        if (pageTemplate == null) {
            return null;
        }

        try (StringReader sr = new StringReader(pageTemplate.getContent())) {
            XMLEventReader reader = xmlEventReaderFactory.createStorageXmlEventReader(sr);
            ObjectNode node = SchemaParser.parse(reader, EXTRA_TRACE);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (XMLStreamException | JsonProcessingException ex) {
            throw new ParseException(ex);
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


    @Inject
    public ParserServiceImpl(
        UserManager userManager, PageManager pageManager, TransactionTemplate transactionTemplate,
        PageTemplateManager pageTemplateManager, PluginAccessor pluginAccessor
    ) {
        this.userManager = userManager;
        this.pageManager = pageManager;
        this.pageTemplateManager = pageTemplateManager;
        this.pluginAccessor = pluginAccessor;
        // we need the root class loader for that to work
        this.xmlEventReaderFactory = transactionTemplate.execute(() -> {
            return new DefaultXmlEventReaderFactory();
        });
    }
}
