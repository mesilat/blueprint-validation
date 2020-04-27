package com.mesilat.vbp.api;

import com.atlassian.confluence.pages.Page;

public interface DataService {
    String getPageData(Long pageId);
    boolean isPageValid(Long pageId);
    String getPageValidationMessage(Long pageId);
    void createPageInfo(Page page, String templateKey, Boolean isValid, String message, String data);
    void updatePageInfo(Page page, Boolean isValid, String message, String data);
}
