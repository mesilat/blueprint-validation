package com.mesilat.vbp.servlet;

import com.atlassian.confluence.plugins.createcontent.ContentBlueprintManager;
import com.atlassian.confluence.plugins.createcontent.impl.ContentBlueprint;
import com.atlassian.plugin.ModuleCompleteKey;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.Constants;
import com.mesilat.vbp.impl.DraftService;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDraftServletFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    @ComponentImport
    private final ContentBlueprintManager contentBlueprintManager;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }
    @Override
    public void destroy() {
    }    

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        GenericRequestWrapper wrappedRequest = new GenericRequestWrapper(request);

        ObjectNode obj;
        try (InputStream in = wrappedRequest.getInputStream()){
            obj = (ObjectNode)mapper.readTree(in);
        }
        String spaceKey = obj.has("spaceKey")? obj.get("spaceKey").asText(): null;
        String blueprintKey = getBlueprintKey(obj);;
        if (blueprintKey == null) {
            LOGGER.warn("Failed to get blueprintKey");
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        String templateKey = getTemplateKey(blueprintKey);
        if (templateKey == null) {
            LOGGER.warn(String.format("Failed to get templateKey for blueprint %s", blueprintKey));
            chain.doFilter(wrappedRequest, resp);
            return;
        }

        HttpServletResponse response = (HttpServletResponse)resp;
        GenericResponseWrapper wrappedResponse = new GenericResponseWrapper(response);
        chain.doFilter(wrappedRequest, wrappedResponse);
        String data = wrappedResponse.getCaptureAsString();
        try (PrintWriter w = resp.getWriter()) {
            w.write(data);
        }

        try {
            ObjectNode draft = (ObjectNode)mapper.readTree(data);
            if (draft.has("draftId")) {
                Long draftId = draft.get("draftId").asLong();
                LOGGER.debug(String.format("Draft %d template key: %s", draftId, templateKey));
                DraftService.addDraftKey(draftId, spaceKey, templateKey);
            }
        } catch(Throwable ex) {
            LOGGER.warn("Failed to parse response", ex);
        }
    }
    
    
    private String getBlueprintKey(ObjectNode obj) throws IOException {
        JsonNode bpk = obj.has("context") && obj.get("context").has("blueprintModuleCompleteKey")
            ? obj.path("context").path("blueprintModuleCompleteKey")
            : null;

        if (bpk == null || bpk.isMissingNode() || bpk.isNull()) {
            return null;
        } else {
            return bpk.asText();
        }
    }
    private String getTemplateKey(String blueprintKey) {
        ContentBlueprint blueprint = contentBlueprintManager.getPluginBlueprint(new ModuleCompleteKey(blueprintKey));
        if (blueprint == null) {
            return null;
        }
        if (blueprint.getContentTemplateRefs().isEmpty()) {
            return null;
        }
        return blueprint.getContentTemplateRefs().get(0).getModuleCompleteKey();
    }

    public CreateDraftServletFilter(ContentBlueprintManager contentBlueprintManager) {
        this.contentBlueprintManager = contentBlueprintManager;
    }
}
