package com.mesilat.vbp.servlet;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION_TASK;
import com.mesilat.vbp.api.DataCreateEvent;
import com.mesilat.vbp.api.DataUpdateEvent;
import com.mesilat.vbp.api.DataValidateEvent;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.TextConverterService;
import com.mesilat.vbp.api.ValidationException;
import com.mesilat.vbp.impl.DataServiceEx;
import com.mesilat.vbp.impl.ValidationServiceEx;
import static com.mesilat.vbp.servlet.PageServletBase.LOGGER;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Scanned
public class ValidatingServletFilter extends PageServletBase implements Filter {
    private final static String X_VBP_TEMPLATE = "X-Blueprint-Validation-Template";
    private final static Pattern CONTENT_ID = Pattern.compile("^/api/content/(\\d+)$");

    private final TemplateManager templateManager;

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;

        if ("GET".equals(request.getMethod())) {
            Matcher m = CONTENT_ID.matcher(request.getPathInfo());
            if (m.matches()) {
                try {
                    long pageId = Long.parseLong(m.group(1));
                    Page page = pageManager.getPage(pageId);
                    if (page != null && page.getProperties().getStringProperty(PROPERTY_TEMPLATE) != null) {
                        response.addHeader(X_VBP_TEMPLATE, page.getProperties().getStringProperty(PROPERTY_TEMPLATE));
                    }
                } catch(Throwable ignore) {}
            }
        }

        if (!("POST".equals(request.getMethod())) && !("PUT".equals(request.getMethod()))) {
            chain.doFilter(request, resp);
            return;
        }

        String xTemplate = request.getHeader(X_VBP_TEMPLATE);
        ObjectNode templateInfo = null;
        try {
            if (xTemplate != null && xTemplate.charAt(0) == '{')
                templateInfo = (ObjectNode)mapper.readTree(xTemplate);
        } catch(Throwable ignore) {}

        Matcher m = CONTENT_ID.matcher(request.getPathInfo());
        if (m.matches()) {
            Long pageId = Long.parseLong(m.group(1));
            doFilterEdit(request, response, chain, templateInfo, pageId);
            return;
        }

        if (templateInfo == null) {
            LOGGER.debug("No X_VBP_TEMPLATE header, not validating");
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);

        } else if (templateInfo.has("pageId")) {
            Long pageId = templateInfo.get("pageId").isNumber()?
                templateInfo.get("pageId").asLong():
                Long.parseLong(templateInfo.get("pageId").asText());
            doFilterEdit(request, response, chain, templateInfo, pageId);

        } else if (templateInfo.has("validationMode") && templateInfo.has("templateKey")) {
            doFilterCreate(request, response, chain, templateInfo);

        } else {
            LOGGER.debug("Cannot validate template");
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);
        }
    }    

    private void doFilterCreate(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain,
        ObjectNode templateInfo
    ) throws IOException, ServletException {
        String templateKey = templateInfo.get("templateKey").asText();
        Template.ValidationMode mode = Template.ValidationMode.valueOf(templateInfo.get("validationMode").asText());
        String spaceKey = templateInfo.has("spaceKey")? templateInfo.get("spaceKey").asText(): null;

        switch (mode) {
            case NONE:
                {
                    response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
                    chain.doFilter(request, response);
                }
                break;
            case WARN:
                {
                    String uuid = UUID.randomUUID().toString();
                    response.setHeader(X_BLUEPRINT_VALIDATION, "pending validation");
                    response.setHeader(X_BLUEPRINT_VALIDATION_TASK, uuid);

                    GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
                    ObjectNode obj;
                    try (InputStream in = wrappedRequest.getInputStream()){
                        obj = (ObjectNode)mapper.readTree(in);
                    }
                    normalizeObjectIds(null, wrappedRequest, obj);

                    Long pageId = super.doProcess(wrappedRequest, response, chain);
                    if (pageId == null) {
                        LOGGER.error("Failed to get pageId");
                        return;
                    }

                    Page page = pageManager.getPage(pageId);
                    if (page == null) {
                        LOGGER.error("Failed to get page object");
                        return;
                    }
                    super.setPageProperty(page, templateKey);
                    validationService.registerValidationTask(uuid, page.getId(), page.getTitle());
                    Thread t = new Thread(() -> {
                        String data = postValidate(uuid, page, templateKey);
                        if (data == null)
                            return;
                        eventPublisher.publish(new DataCreateEvent(page, data, templateKey));
                    });
                    t.start();
                }
                break;
            case FAIL:
                try {
                    GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
                    ObjectNode obj;
                    try (InputStream in = wrappedRequest.getInputStream()){
                        obj = (ObjectNode)mapper.readTree(in);
                    }
                    normalizeObjectIds(null, wrappedRequest, obj);

                    String storageFormat = getStorageFormat(obj);
                    String data = parserService.parse(storageFormat, spaceKey);

                    validationService.validate(templateKey, data);
                    DataValidateEvent event = new DataValidateEvent(templateKey, spaceKey, data, null);
                    eventPublisher.publish(event);
                    if (!event.isValid())
                        throw new ValidationException(String.join("\n", event.getMessages()));

                    response.setHeader(X_BLUEPRINT_VALIDATION, "valid");
                    
                    Long pageId = super.doProcess(wrappedRequest, response, chain);
                    Page page = pageManager.getPage(pageId);
                    if (page == null) {
                        LOGGER.error("Failed to get page object");
                        return;
                    }
                    super.setPageProperty(page, templateKey);
                    transactionTemplate.execute(() -> {
                        dataService.createPageInfo(
                            page,
                            templateKey,
                            true,
                            null,
                            data
                        );
                        dataService.registerDataObjectIds(page.getId(), storageFormat);
                        return null;
                    });
                    Thread t = new Thread(() -> eventPublisher.publish(new DataCreateEvent(page, data, templateKey)));
                    t.start();
                } catch (Throwable ex) {
                    sendError(response, ex);
                }
            default:
                break;
        }
    }    

    private void doFilterEdit(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain,
        ObjectNode templateInfo,
        Long pageId
    ) throws IOException, ServletException {
        Page page = pageManager.getPage(pageId);
        if (page == null) {
            LOGGER.warn(String.format("Page not found: %d", pageId));
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, response);
            return;
        }

        String oldPageTitle = page.getTitle();
        String templateKey = page.getProperties().getStringProperty(PROPERTY_TEMPLATE);
        if (templateKey == null) {
            if (templateInfo != null) {
                doFilterCreate(request, response, chain, templateInfo);
            } else {
                LOGGER.warn(String.format("Template key not found for page: %d", pageId));
                response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
                chain.doFilter(request, response);
                return;
            }
        }

        Template template = templateManager.get(templateKey);
        if (template == null) {
            LOGGER.debug("Not validating template");
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, response);
            return;
        }

        Template.ValidationMode mode = template.getValidationMode();
        switch (mode) {
            case NONE:
                {
                    response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
                    chain.doFilter(request, response);
                }
                break;
            case WARN:
                {
                    String uuid = UUID.randomUUID().toString();
                    response.setHeader(X_BLUEPRINT_VALIDATION, "pending validation");
                    response.setHeader(X_BLUEPRINT_VALIDATION_TASK, uuid);

                    GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
                    ObjectNode obj;
                    try (InputStream in = wrappedRequest.getInputStream()){
                        obj = (ObjectNode)mapper.readTree(in);
                    }
                    normalizeObjectIds(page.getId(), wrappedRequest, obj);

                    super.doProcess(wrappedRequest, response, chain);
                    validationService.registerValidationTask(uuid, page.getId(), page.getTitle());
                    Thread t = new Thread(() -> {
                        String data = postValidate(uuid, page, templateKey);
                        eventPublisher.publish(new DataUpdateEvent(page, data, templateKey, !oldPageTitle.equals(page.getTitle())));
                    });
                    t.start();
                }
                break;
            case FAIL:
                try {
                    GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
                    ObjectNode obj;
                    try (InputStream in = wrappedRequest.getInputStream()){
                        obj = (ObjectNode)mapper.readTree(in);
                    }
                    normalizeObjectIds(page.getId(), wrappedRequest, obj);

                    String storageFormat = getStorageFormat(obj);
                    String data = parserService.parse(storageFormat, page.getSpaceKey());

                    validationService.validate(templateKey, data);
                    DataValidateEvent event = new DataValidateEvent(templateKey, page.getSpaceKey(), data, page);
                    eventPublisher.publish(event);
                    if (!event.isValid())
                        throw new ValidationException(String.join("\n", event.getMessages()));

                    response.setHeader(X_BLUEPRINT_VALIDATION, "valid");                    
                    super.doProcess(wrappedRequest, response, chain);
                    transactionTemplate.execute(() -> {
                        dataService.updatePageInfo(page, true, null, data);
                        dataService.registerDataObjectIds(page.getId(), storageFormat);
                        return null;
                    });

                    Thread t = new Thread(() -> eventPublisher.publish(new DataUpdateEvent(page, data, templateKey, !oldPageTitle.equals(page.getTitle()))));
                    t.start();
                } catch (Throwable ex) {
                    LOGGER.error("Hm... something bad happend", ex);
                    sendError(response, ex);
                }
            default:
                break;
        }
    }    

    private void sendError(HttpServletResponse response, Throwable ex) throws IOException {
        ObjectNode err = toValidationErrorResult(ex.getMessage());
        response.reset();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setHeader(X_BLUEPRINT_VALIDATION, "invalid");
        try (PrintWriter pw = response.getWriter()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(pw, err);
        }
    }

    @Inject
    public ValidatingServletFilter(
        PageManager pageManager,
        TransactionTemplate transactionTemplate,
        TextConverterService textConverterService,
        ValidationServiceEx validationService,
        ParserService parserService,
        DataServiceEx dataService,
        I18nResolver resolver,
        EventPublisher eventPublisher,
        TemplateManager templateManager
    ) {
        super(
            textConverterService, parserService, validationService,
            pageManager, dataService, transactionTemplate, resolver,
            eventPublisher
        );
        this.templateManager = templateManager;
    }
}
