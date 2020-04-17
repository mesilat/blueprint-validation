package com.mesilat.vbp.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;

public class GenericRequestWrapper extends HttpServletRequestWrapper {
    private final Map customHeaders;
    private final Map customParameters;
    private byte[] body;

    public GenericRequestWrapper(HttpServletRequest request) {
        super(request);
        this.cachePayload(request);
        this.customHeaders = new HashMap();
        this.customParameters = new HashMap();
    }

    private void cachePayload(HttpServletRequest request) {
        try {
            this.body = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException ignore) {
            this.body = new byte[0];
        }

    }

    public void putHeader(String name, String value) {
        this.customHeaders.put(name, value);
    }
    public void setBody(byte[] body){
        this.body = body;
    }

    @Override
    public String getHeader(String name) {
        String headerValue = (String) this.customHeaders.get(name);
        return headerValue != null ? headerValue : ((HttpServletRequest) this.getRequest()).getHeader(name);
    }

    @Override
    public Enumeration getHeaderNames() {
        HashSet set = new HashSet(this.customHeaders.keySet());
        Enumeration e = ((HttpServletRequest) this.getRequest()).getHeaderNames();

        while (e.hasMoreElements()) {
            String n = (String) e.nextElement();
            set.add(n);
        }

        return Collections.enumeration(set);
    }

    public void setParameter(String name, String value) {
        this.customParameters.put(name, value);
    }

    @Override
    public String getParameter(String name) {
        String value = (String) this.customParameters.get(name);
        return value != null ? value : super.getParameter(name);
    }

    @Override
    public Map getParameterMap() {
        HashMap map = new HashMap();
        this.customParameters.forEach(map::put);
        super.getParameterMap().forEach(map::putIfAbsent);
        return map;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            private ByteArrayInputStream bais;

            {
                this.bais = new ByteArrayInputStream(GenericRequestWrapper.this.body);
            }

            @Override
            public int read() throws IOException {
                return this.bais.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }
}