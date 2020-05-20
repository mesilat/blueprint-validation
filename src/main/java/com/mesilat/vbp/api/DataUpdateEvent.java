package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.mesilat.vbp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataUpdateEvent extends ConfluenceEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final Boolean pageRenamed;
    private final String templateKey;
    private final String data;

    public String getTemplateKey() {
        return templateKey;
    }
    public String getData(){
        return data;
    }
    public Boolean getPageRenamed(){
        return pageRenamed;
    }

    public DataUpdateEvent(Object src, String data, String templateKey, Boolean pageRenamed){
        super(src);
        this.data = data;
        this.templateKey = templateKey;
        this.pageRenamed = pageRenamed;
        LOGGER.trace(String.format("DataUpdateEvent(%s,%s)", templateKey, Boolean.toString(pageRenamed)));
    }
}