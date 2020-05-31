package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.atlassian.confluence.pages.Page;
import com.mesilat.vbp.Constants;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataValidateEvent extends ConfluenceEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final String data;
    private final String templateKey;
    private final String spaceKey;
    private final Page page;
    private boolean valid = true;
    private final List<String> messages = new ArrayList<>();

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
    public boolean isValid() {
        return valid;
    }
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    public List<String> getMessages() {
        return messages;
    }
    public void addMessage(String message) {
        this.messages.add(message);
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