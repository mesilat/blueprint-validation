package com.mesilat.vbp.servlet;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION_TASK;
import com.mesilat.vbp.api.DataService;
import com.mesilat.vbp.api.DataValidateEvent;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.TextConverterService;
import com.mesilat.vbp.api.ValidationService;
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

        if (!("POST".equals(request.getMethod())) && !("PUT".equals(request.getMethod()))) {
            chain.doFilter(request, resp);
            return;
        }
        
        Matcher m = CONTENT_ID.matcher(request.getPathInfo());
        if (m.matches()) {
            Long pageId = Long.parseLong(m.group(1));
            doFilterEdit(request, response, chain, pageId);
            return;
        }

        String xTemplate = request.getHeader(X_VBP_TEMPLATE);
        if (xTemplate == null) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);
            return;
        }

        ObjectNode templateInfo = (ObjectNode)mapper.readTree(xTemplate);
        if (templateInfo.has("pageId")) {
            Long pageId = templateInfo.get("pageId").isNumber()?
                templateInfo.get("pageId").asLong():
                Long.parseLong(templateInfo.get("pageId").asText());
            doFilterEdit(request, response, chain, pageId);

        } else if (templateInfo.has("validationMode") && templateInfo.has("templateKey")) {
            String templateKey = templateInfo.get("templateKey").asText();
            Template.ValidationMode mode = Template.ValidationMode.valueOf(templateInfo.get("validationMode").asText());
            String spaceKey = templateInfo.has("spaceKey")? templateInfo.get("spaceKey").asText(): null;
            doFilterCreate(request, response, chain, templateKey, mode, spaceKey);

        } else {
            LOGGER.trace("Cannot validate template");
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);
        }
    }    

    private void doFilterCreate(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain,
        String templateKey,
        Template.ValidationMode mode,
        String spaceKey
    ) throws IOException, ServletException {
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

                    Long pageId = super.doProcess(request, response, chain);
                    Page page = pageManager.getPage(pageId);
                    if (page == null) {
                        LOGGER.error("Failed to get page object");
                        return;
                    }   super.setPageProperty(page, templateKey);
                    validationService.registerValidationTask(uuid, page.getId(), page.getTitle());
                    Thread t = new Thread(() -> super.postValidate(uuid, page, templateKey));
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
                    
                    String storageFormat = getStorageFormat(obj);
                    String data = parserService.parse(storageFormat, spaceKey);
                    validationService.validate(templateKey, data);
                    eventPublisher.publish(new DataValidateEvent(data));
                    response.setHeader(X_BLUEPRINT_VALIDATION, "valid");
                    
                    Long pageId = super.doProcess(wrappedRequest, response, chain);
                    Page page = pageManager.getPage(pageId);
                    if (page == null) {
                        LOGGER.error("Failed to get page object");
                        return;
                    }
                    super.setPageProperty(page, templateKey);
                    transactionTemplate.execute(() -> {
                        try {
                            dataService.createPageInfo(
                                page,
                                templateKey,
                                true,
                                null,
                                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
                            );
                        } catch (JsonProcessingException ex) {
                            LOGGER.warn("Failed to serialize JSON data", ex);
                        }
                        return null;
                    });
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
        Long pageId
    ) throws IOException, ServletException {
        Page page = pageManager.getPage(pageId);
        if (page == null) {
            LOGGER.warn(String.format("Page not found: %d", pageId));
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, response);
            return;
        }

        String templateKey = page.getProperties().getStringProperty(PROPERTY_TEMPLATE);
        if (templateKey == null) {
            LOGGER.warn(String.format("Template key not found for page: %d", pageId));
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, response);
            return;
        }

        Template template = templateManager.get(templateKey);
        if (template == null) {
            LOGGER.trace("Not validating template");
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

                    super.doProcess(request, response, chain);
                    validationService.registerValidationTask(uuid, page.getId(), page.getTitle());
                    Thread t = new Thread(() -> super.postValidate(uuid, page, templateKey));
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
                    
                    String storageFormat = getStorageFormat(obj);
                    String data = parserService.parse(storageFormat, page.getSpaceKey());
                    validationService.validate(templateKey, data);
                    eventPublisher.publish(new DataValidateEvent(data));
                    response.setHeader(X_BLUEPRINT_VALIDATION, "valid");
                    
                    super.doProcess(wrappedRequest, response, chain);

                    transactionTemplate.execute(() -> {
                        try {
                            dataService.updatePageInfo(
                                page,
                                true,
                                null,
                                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
                            );
                        } catch (JsonProcessingException ex) {
                            LOGGER.warn("Failed to serialize JSON data", ex);
                        }
                        return null;
                    });
                } catch (Throwable ex) {
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
        ValidationService validationService,
        ParserService parserService,
        DataService dataService,
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
