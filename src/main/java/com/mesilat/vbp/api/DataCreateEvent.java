package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.mesilat.vbp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCreateEvent extends ConfluenceEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final String templateKey;
    private final String data;

    public String getTemplateKey() {
        return templateKey;
    }
    public String getData(){
        return data;
    }

    public DataCreateEvent(Object src, String data, String templateKey){
        super(src);
        this.data = data;
        this.templateKey = templateKey;
        LOGGER.trace(String.format("DataCreateEvent(%s)", templateKey));
    }
}