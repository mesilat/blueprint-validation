package com.mesilat.vbp.servlet;

import com.mesilat.vbp.Constants;
import com.mesilat.vbp.impl.DraftService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class CreateDraftServletFilter2 implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);
    private static final Pattern DRAFT_INFO = Pattern.compile("<meta name=\"ajs-draft-id\" content=\"(\\d+)\">");
    private static final Pattern DRAFT_INFO2 = Pattern.compile("<input id=\"draftId\" type=\"hidden\" name=\"draftId\" value=\"(\\d+)\">");

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }
    @Override
    public void destroy() {
    }    

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        if (!"GET".equals(request.getMethod())) {
            LOGGER.trace("Not a get method");
            chain.doFilter(req, resp);
            return;
        }

        String templateId = request.getParameter("templateId");
        if (templateId == null) {
            LOGGER.trace("Missing templateId parameter");
            chain.doFilter(req, resp);
            return;
        }
        String spaceKey = request.getParameter("spaceKey");
        if (spaceKey == null) {
            LOGGER.trace("Missing spaceKey parameter");
            chain.doFilter(req, resp);
            return;
        }

        HttpServletResponse response = (HttpServletResponse)resp;
        GenericResponseWrapper wrappedResponse = new GenericResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);
        String data = wrappedResponse.getCaptureAsString();
        try (PrintWriter w = resp.getWriter()) {
            w.write(data);
        }

        long draftId = 0;
        Matcher m = DRAFT_INFO.matcher(data);
        if (m.find()) {
            draftId = Long.parseLong(m.group(1));
        }
        if (draftId == 0) {
            m = DRAFT_INFO2.matcher(data);
            if (m.find()) {
                draftId = Long.parseLong(m.group(1));
            }
        }
        if (draftId == 0) {
            LOGGER.error("Failed to find draftId in response to create draft request");
        } else {
            LOGGER.debug(String.format("Draft %d template key: %s", draftId, templateId));
            DraftService.addDraftKey(draftId, spaceKey, templateId);
        }
    }
}
