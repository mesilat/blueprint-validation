package com.mesilat.vbp.servlet;

import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.Constants;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION;
import com.mesilat.vbp.api.DataValidateEvent;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.TextConverterService;
import com.mesilat.vbp.api.ValidationException;
import com.mesilat.vbp.impl.DataServiceEx;
import com.mesilat.vbp.impl.ValidationServiceEx;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageServletBase {
    public static final String PROPERTY_TEMPLATE = "com.mesilat.vbp.template";

    protected static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    protected final TextConverterService textConverterService;
    protected final ParserService parserService;
    protected final ValidationServiceEx validationService;
    protected final PageManager pageManager;
    protected final DataServiceEx dataService;
    protected final TransactionTemplate transactionTemplate;
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final I18nResolver resolver;
    protected final EventPublisher eventPublisher;

    protected void preCreateValidate(
        ObjectNode obj, String spaceKey, String templateKey,
        ServletRequest req, HttpServletResponse resp, FilterChain chain
    ) throws ParseException, ValidationException, IOException, ServletException {
        String storageFormat = getStorageFormat(obj);

        String data = parserService.parse(storageFormat, spaceKey);
        validationService.validate(templateKey, data);
        eventPublisher.publish(new DataValidateEvent(templateKey, spaceKey, data, null));
        resp.setHeader(X_BLUEPRINT_VALIDATION, "valid");

        Long pageId = doProcess(req, resp, chain);
        if (pageId == null) {
            LOGGER.warn("Failed to get page id from response");
            return;
        }

        Page page = pageManager.getPage(pageId);
        if (page == null) {
            LOGGER.warn(String.format("Page not found: %d", pageId));
            return;
        }
        transactionTemplate.execute(() -> {
            page.getProperties().setStringProperty(PROPERTY_TEMPLATE, templateKey);
            return null;
        });

        transactionTemplate.execute(() -> {
            try {
                dataService.createPageInfo(page, templateKey, true, null, mapper.writeValueAsString(data));
            } catch (JsonProcessingException ex) {
                LOGGER.warn("Failed to serialize JSON data", ex);
            }
            return null;
        });
    }
    protected void preUpdateValidate(
        Page page, ObjectNode obj, String spaceKey, String templateKey,
        ServletRequest req, HttpServletResponse resp, FilterChain chain
    ) throws ParseException, ValidationException, IOException, ServletException {
        String storageFormat = getStorageFormat(obj);

        String data = parserService.parse(storageFormat, spaceKey);
        validationService.validate(templateKey, data);
        eventPublisher.publish(new DataValidateEvent(templateKey, spaceKey, data, page));

        chain.doFilter(req, resp);

        transactionTemplate.execute(() -> {
            try {
                dataService.createPageInfo(page, templateKey, true, null, mapper.writeValueAsString(data));
            } catch (JsonProcessingException ex) {
                LOGGER.warn("Failed to serialize JSON data", ex);
            }
            return null;
        });
    }
    protected String postValidate(String uuid, Page page, String templateKey) {
        String storageFormat = page.getBodyContent().getBody();
        String data;
        try {
            data = parserService.parse(storageFormat, page.getSpaceKey());
        } catch (ParseException ex) {
            LOGGER.error(String.format("Failed to parse page: %d", page.getId()), ex);
            return null;
        }

        transactionTemplate.execute(() -> {
            try {
                validationService.runValidationTask(uuid, templateKey, data);
                eventPublisher.publish(new DataValidateEvent(templateKey, page.getSpaceKey(), data, page));
                dataService.createPageInfo(page, templateKey, true, null, data);
            } catch (Throwable ex) {
                dataService.createPageInfo(page, templateKey, false, ex.getMessage(), data);
            }
            return null;
        });

        return data;
    }
    protected Long doProcess(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)resp;
        GenericResponseWrapper wrappedResponse = new GenericResponseWrapper(response);
        chain.doFilter(req, wrappedResponse);

        String data = wrappedResponse.getCaptureAsString();
        try (PrintWriter w = resp.getWriter()) {
            w.write(data);
        }

        ObjectNode page = (ObjectNode)mapper.readTree(data);
        return page.has("id")? page.get("id").asLong(): null;
    }
    protected String getStorageFormat(ObjectNode obj) {
        if (obj.has("body") && obj.get("body").has("editor") && obj.get("body").get("editor").has("value")) {
            try {
                String text = obj.get("body").get("editor").get("value").asText();
                String representation = obj.get("body").get("editor").get("representation").asText();
                return "editor".equals(representation)? textConverterService.convertToStorage(text): text;
            } catch (XhtmlException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return null;
        }
    }
    protected ObjectNode toValidationErrorResult(String message) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("statusCode", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ObjectNode data = mapper.createObjectNode();
        data.put("authorized", false);
        data.put("valid", false);
        data.put("allowedInReadOnlyMode", false);
        data.set("errors", mapper.createArrayNode());
        data.put("message", MessageFormat.format(resolver.getText("com.mesilat.vbp.validation.error.message"), message));
        data.put("reason", "JSON Validation");
        obj.set("data", data);
        return obj;
    }
    protected void setPageProperty(Page page, String templateKey) {
        transactionTemplate.execute(() -> {
            page.getProperties().setStringProperty(PROPERTY_TEMPLATE, templateKey);
            return null;
        });
    }

    public PageServletBase(
        TextConverterService textConverterService,
        ParserService parserService,
        ValidationServiceEx validationService,
        PageManager pageManager,
        DataServiceEx dataService,
        TransactionTemplate transactionTemplate,
        I18nResolver resolver,
        EventPublisher eventPublisher
    ) {
        this.textConverterService = textConverterService;
        this.parserService = parserService;
        this.validationService = validationService;
        this.pageManager = pageManager;
        this.dataService = dataService;
        this.transactionTemplate = transactionTemplate;
        this.resolver = resolver;
        this.eventPublisher = eventPublisher;
    }
}
