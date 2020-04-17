package com.mesilat.vbp.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Template {
    public static enum ValidationMode { NONE, WARN, FAIL };

    @XmlElement
    private String templateKey;
    @XmlElement
    private String templateName;
    @XmlElement
    private ValidationMode validationMode;
    @XmlElement
    private String schema;

    public String getTemplateKey() {
        return templateKey;
    }
    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }
    public String getTemplateName() {
        return templateName;
    }
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    public ValidationMode getValidationMode() {
        return validationMode;
    }
    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }
    public String getSchema() {
        return schema;
    }
    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Template() {
    }
    public Template(String templateKey, String templateName, String validationMode) {
        this.templateKey = templateKey;
        this.templateName = templateName;
        this.validationMode = ValidationMode.valueOf(validationMode);
    }
    public Template(String templateKey, String templateName, ValidationMode validationMode) {
        this.templateKey = templateKey;
        this.templateName = templateName;
        this.validationMode = validationMode;
    }
}
