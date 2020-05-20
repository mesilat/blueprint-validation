package com.mesilat.vbp.impl;

import com.atlassian.confluence.pages.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.PageInfo;
import com.mesilat.vbp.api.DataService;

public interface DataServiceEx extends DataService {
    PageInfo getPageInfo(Page page);
    ObjectNode getPageInfo(Long pageId);
    void createPageInfo(Page page, String templateKey, Boolean isValid, String message, String data);
    void updatePageInfo(Page page, Boolean isValid, String message, String data);
    void deletePageInfo(Page page);
    void undeletePageInfo(Page page);
}
