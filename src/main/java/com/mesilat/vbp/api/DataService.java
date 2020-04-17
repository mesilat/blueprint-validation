package com.mesilat.vbp.api;

import com.atlassian.confluence.pages.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataService {
    ObjectNode getPageInfo(Long pageId);
    void createPageInfo(Page page, String templateKey, Boolean isValid, String message, String data);
    void updatePageInfo(Page page, Boolean isValid, String message, String data);
}
