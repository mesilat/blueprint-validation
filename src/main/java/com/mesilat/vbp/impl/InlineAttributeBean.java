package com.mesilat.vbp.impl;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class InlineAttributeBean {
    @XmlElement
    private long pageId;
    @XmlElement
    private long lastFetchTime;
    @XmlElement
    private String selectedText;
    @XmlElement
    private int numMatches;
    @XmlElement
    private int index;
    @XmlElement
    private String attributeName;

    public long getPageId() {
        return pageId;
    }
    public void setPageId(long pageId) {
        this.pageId = pageId;
    }
    public long getLastFetchTime() {
        return lastFetchTime;
    }
    public void setLastFetchTime(long lastFetchTime) {
        this.lastFetchTime = lastFetchTime;
    }
    public String getSelectedText() {
        return selectedText;
    }
    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }
    public int getNumMatches() {
        return numMatches;
    }
    public void setNumMatches(int numMatches) {
        this.numMatches = numMatches;
    }
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public String getAttributeName() {
        return attributeName;
    }
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
}