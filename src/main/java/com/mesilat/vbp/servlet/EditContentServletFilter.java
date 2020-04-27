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
import com.mesilat.vbp.api.DataService;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.Template.ValidationMode;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.TextConverterService;
import com.mesilat.vbp.api.ValidationService;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;
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
public class EditContentServletFilter extends PageServletBase implements Filter {
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
        if (!"PUT".equals(request.getMethod())) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);
            return;
        }

        GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
        ObjectNode obj;
        try (InputStream in = wrappedRequest.getInputStream()){
            obj = (ObjectNode)mapper.readTree(in);
        }

        if (!obj.has("type") || !"page".equals(obj.get("type").asText())) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }
        if (!obj.has("id")) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }
        String pageId = obj.get("id").asText();
        if (pageId == null) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }
        Page page = pageManager.getPage(Long.parseLong(pageId));
        if (page == null) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        String templateKey = null;
        Template template = null;
        if (obj.has("extensions") && obj.get("extensions").has("sourceTemplateId")) {
            templateKey = obj.get("extensions").get("sourceTemplateId").asText();
            if (templateKey == null) {
                response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
                chain.doFilter(wrappedRequest, resp);
                return;
            }
            template = templateManager.get(templateKey);
        } else {
            templateKey = page.getProperties().getStringProperty(PROPERTY_TEMPLATE);
            if (templateKey == null) {
                response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
                chain.doFilter(wrappedRequest, resp);
                return;
            }
            template = templateManager.get(templateKey);
        }

        if (template == null || template.getValidationMode() == null || template.getValidationMode() == ValidationMode.NONE) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        if (template.getValidationMode() == ValidationMode.FAIL) {
            // prevalidation required
            String spaceKey = obj.has("space") && obj.get("space").has("key")? obj.get("space").get("key").asText(): null;
            try {
                preUpdateValidate(page, obj, spaceKey, templateKey, wrappedRequest, response, chain);
            } catch (Throwable ex) {
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
        } else /* template.getValidationMode() == ValidationMode.WARN */ {
            // postvalidation
            String uuid = UUID.randomUUID().toString();
            response.setHeader(X_BLUEPRINT_VALIDATION, "pending validation");
            response.setHeader(X_BLUEPRINT_VALIDATION_TASK, uuid);
            chain.doFilter(wrappedRequest, resp);
            validationService.registerValidationTask(uuid, page.getId(), page.getTitle());

            String _templateKey = template.getTemplateKey();
            Thread t = new Thread(() -> {
                // Do validation
                this.postValidate(uuid, page, _templateKey);
            });
            t.start();
        }
    }

    @Inject
    public EditContentServletFilter(
        PageManager pageManager,
        TransactionTemplate transactionTemplate,
        TemplateManager templateManager,
        TextConverterService textConverterService,
        ValidationService validationService,
        ParserService parserService,
        DataService dataService,
        I18nResolver resolver,
        EventPublisher eventPublisher
    ) {
        super(
            textConverterService, parserService, validationService,
            pageManager, dataService, transactionTemplate, resolver,
            eventPublisher
        );
        this.templateManager = templateManager;
    }
}
