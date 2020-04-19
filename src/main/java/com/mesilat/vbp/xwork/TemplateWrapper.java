package com.mesilat.vbp.xwork;

import com.mesilat.vbp.api.Template;

public class TemplateWrapper extends Template {
    private String url;
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

    public TemplateWrapper() {
    }
    public TemplateWrapper(Template template) {
        super(template.getTemplateKey(), template.getTemplateName(), template.getValidationMode());
        this.setSchema(template.getSchema());
    }
    public TemplateWrapper(String templateKey, String templateName, String validationMode) {
        super(templateKey, templateName, validationMode);
    }
    public TemplateWrapper(String templateKey, String templateName, ValidationMode validationMode) {
        super(templateKey, templateName, validationMode);
    }
}
