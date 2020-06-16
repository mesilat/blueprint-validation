package com.mesilat.vbp.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServletFilter implements Filter {
    // private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);
    // private ServletContext context;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        // context = fc.getServletContext();
    }
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;

        String target = request.getRequestURI().replace("/data-share/", "/blueprint-validation/");
        // LOGGER.warn(String.format("Redirecting %s to %s", request.getRequestURI(), target));
        response.sendRedirect(target);
        // ServletContext context = request.getSession().getServletContext();
        // RequestDispatcher requestDispatcher = context.getRequestDispatcher(target);
        // requestDispatcher.forward(request, response);
    }
}
