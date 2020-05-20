package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.atlassian.confluence.pages.Page;
import com.mesilat.vbp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataValidateEvent extends ConfluenceEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final String data;
    private final String templateKey;
    private final String spaceKey;
    private final Page page;

    public String getData() {
        return data;
    }
    public String getTemplateKey() {
        return templateKey;
    }
    public String getSpaceKey() {
        return spaceKey;
    }
    public Page getPage() {
        return page;
    }

    public DataValidateEvent(String templateKey, String spaceKey, String data, Page page){
        super(data);
        this.templateKey = templateKey;
        this.data = data;
        this.spaceKey = spaceKey;
        this.page = page;
        LOGGER.trace(String.format("DataValidateEvent(%s,%s)", templateKey, spaceKey));
    }
}