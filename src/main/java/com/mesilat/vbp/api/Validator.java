package com.mesilat.vbp.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Validator {
    @XmlElement
    private String code;
    @XmlElement
    private String name;
    @XmlElement
    private String type;
    @XmlElement
    private String prompt;
    @XmlElement
    private String warning;
    @XmlElement
    private String text;
    @XmlElement
    private String module;

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getPrompt() {
        return prompt;
    }
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    public String getWarning() {
        return warning;
    }
    public void setWarning(String warning) {
        this.warning = warning;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getModule() {
        return module;
    }
    public void setModule(String module) {
        this.module = module;
    }

    public Validator() {
    }
    public Validator(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
