package com.mesilat.vbp.api;

import com.atlassian.confluence.event.events.ConfluenceEvent;

public class DataValidateEvent extends ConfluenceEvent {
    private final String data;

    public String getData() {
        return data;
    }

    public DataValidateEvent(String data){
        super(data);
        this.data = data;
    }
}