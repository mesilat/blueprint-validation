package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;
import com.atlassian.confluence.pages.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataValidateEvent extends ConfluenceEvent {
    private final Page page;
    private final ObjectNode data;

    public Page getPage() {
        return page;
    }
    public ObjectNode getData() {
        return data;
    }

    public DataValidateEvent(Page page, ObjectNode data){
        super(page);
        this.page = page;
        this.data = data;
    }
}