package com.mesilat.vbp.servlet;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION;
import static com.mesilat.vbp.Constants.X_BLUEPRINT_VALIDATION_TASK;
import com.mesilat.vbp.api.DataService;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.Template.ValidationMode;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.TextConverterService;
import com.mesilat.vbp.api.ValidationException;
import com.mesilat.vbp.api.ValidationService;
import com.mesilat.vbp.drafts.DraftService;
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
public class CreateFromTemplateServletFilter extends PageServletBase implements Filter {
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
        if (!"POST".equals(request.getMethod())) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(request, resp);
            return;
        }

        // Inspect request paylaod to figure out the template id; there are
        // two possible cases: page is created from existing draft or it may
        // contain direct indication of a source template
        GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
        ObjectNode obj;
        try (InputStream in = wrappedRequest.getInputStream()){
            obj = (ObjectNode)mapper.readTree(in);
        }

        String templateKey = null;
        String spaceKey = null;
        if (
               obj.has("type")
            && obj.has("id")
            && "page".equals(obj.get("type").asText())
        ) {
            // A page was created from draft
            Long draftId = obj.get("id").asLong();
            if (DraftService.hasDraftKey(draftId)) {
                DraftService.Draft draft = DraftService.getDraft(draftId);
                templateKey = draft.getTemplateKey();
                spaceKey = draft.getSpaceKey();
            }
        }
        
        if (
            templateKey == null
            && obj.has("extensions")
            && obj.get("extensions").has("sourceTemplateId")
        ) {
            templateKey = obj.get("extensions").get("sourceTemplateId").asText();
            spaceKey = obj.has("space") && obj.get("space").has("key")?
                obj.get("space").get("key").asText(): null;
        }

        if (templateKey == null) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        Template template = templateManager.get(templateKey);
        if (
               template == null
            || template.getValidationMode() == null
            || template.getValidationMode() == ValidationMode.NONE
        ) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        if (template.getValidationMode() == ValidationMode.FAIL) {
            // prevalidation required
            try {
                // X-Blueprint-Validation header is set according to validation
                // result be the method
                preCreateValidate(obj, spaceKey, templateKey, wrappedRequest, response, chain);
            } catch (ParseException | ValidationException ex) {
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
            response.setHeader(X_BLUEPRINT_VALIDATION, "pending validation");
            String uuid = UUID.randomUUID().toString();
            response.setHeader(X_BLUEPRINT_VALIDATION_TASK, uuid);
            Long pageId = doProcess(wrappedRequest, resp, chain);
            if (pageId == null) {
                LOGGER.error("Failed to get page id from response");
                return;
            }
            Page page = pageManager.getPage(pageId);
            if (page == null) {
                LOGGER.error(String.format("Page does not exist: %d", pageId));
                return;
            }
            String _templateKey = templateKey;
            transactionTemplate.execute(() -> {
                page.getProperties().setStringProperty(PROPERTY_TEMPLATE, _templateKey);
                return null;
            });
            validationService.registerValidationTask(uuid, page.getId(), page.getTitle());

            Thread t = new Thread(() -> {
                // Do validation
                this.postValidate(uuid, page, template.getTemplateKey());
            });
            t.start();
        }
    }

    @Inject
    public CreateFromTemplateServletFilter(
        PageManager pageManager,
        TransactionTemplate transactionTemplate,
        TemplateManager templateManager,
        TextConverterService textConverterService,
        ValidationService validationService,
        ParserService parserService,
        DataService dataService,
        I18nResolver resolver
    ) {
        super(textConverterService, parserService, validationService,
            pageManager, dataService, transactionTemplate, resolver);
        this.templateManager = templateManager;
    }
}
