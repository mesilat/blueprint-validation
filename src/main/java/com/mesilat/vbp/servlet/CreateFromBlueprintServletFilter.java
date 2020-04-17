package com.mesilat.vbp.servlet;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class CreateFromBlueprintServletFilter extends PageServletBase implements Filter {
    private static final Pattern PATH_INFO = Pattern.compile("^/api/content/blueprint/instance/(\\d+)$");

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
        Matcher m = PATH_INFO.matcher(request.getPathInfo());
        if (!m.matches()) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(req, resp);
            return;
        }

        Long draftId = Long.parseLong(m.group(1));
        if (!DraftService.hasDraftKey(draftId)) {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(req, resp);
            return;
        }

        final DraftService.Draft draft = DraftService.getDraft(draftId);
        Template template = templateManager.get(draft.getTemplateKey());
        if (template == null || template.getValidationMode() == ValidationMode.NONE)  {
            response.setHeader(X_BLUEPRINT_VALIDATION, "not validated");
            chain.doFilter(req, resp);
            return;
        }

        if (template.getValidationMode() == ValidationMode.FAIL) {
            // prevalidation required
            GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);
            ObjectNode obj;
            try (InputStream in = wrappedRequest.getInputStream()){
                obj = (ObjectNode)mapper.readTree(in);
            }
            try {
                preCreateValidate(obj, draft.getSpaceKey(), draft.getTemplateKey(), wrappedRequest, response, chain);
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
            Long pageId = doProcess(req, resp, chain);
            if (pageId == null) {
                LOGGER.warn("Failed to get page id from response");
                return;
            }
            Page page = pageManager.getPage(pageId);
            if (page == null) {
                LOGGER.error(String.format("Page does not exist: %d", pageId));
                return;
            }
            page.getProperties().setStringProperty(PROPERTY_TEMPLATE, template.getTemplateKey());
            validationService.registerValidationTask(uuid, page.getId(), page.getTitle());

            Thread t = new Thread(() -> {
                // Do validation
                this.postValidate(uuid, page, template.getTemplateKey());
            });
            t.start();
        }
    }

    @Inject
    public CreateFromBlueprintServletFilter(
        @ComponentImport PageManager pageManager,
        TemplateManager templateManager,
        TextConverterService textConverterService,
        ValidationService validationService,
        ParserService parserService,
        DataService dataService,
        @ComponentImport I18nResolver resolver,
        @ComponentImport TransactionTemplate transactionTemplate
    ) {
        super(textConverterService, parserService, validationService,
            pageManager, dataService, transactionTemplate, resolver);
        this.templateManager = templateManager;
    }
}
