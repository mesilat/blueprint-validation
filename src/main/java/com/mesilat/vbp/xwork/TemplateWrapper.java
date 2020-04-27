package com.mesilat.vbp.xwork;

import com.mesilat.vbp.api.Template;

public class TemplateWrapper extends Template {
    private String url;
    private String templateName;
    private boolean uploadEnabled = true;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public boolean isUploadEnabled() {
        return uploadEnabled;
    }
    public void setUploadEnabled(boolean uploadEnabled) {
        this.uploadEnabled = uploadEnabled;
    }
    public String getTemplateName() {
        return templateName;
    }
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public TemplateWrapper() {
    }
    public TemplateWrapper(Template template, String templateName) {
        super(template.getTemplateKey(), template.getValidationMode());
        this.templateName = templateName;
    }
    public TemplateWrapper(String templateKey, String templateName, String validationMode) {
        super(templateKey, validationMode);
        this.templateName = templateName;
    }
    public TemplateWrapper(String templateKey, String templateName, ValidationMode validationMode) {
        super(templateKey, validationMode);
        this.templateName = templateName;
    }
}
