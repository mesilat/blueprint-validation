package com.mesilat.vbp.api;

public interface DataService {
    String getPageData(Long pageId);
    boolean isPageValid(Long pageId);
    String getPageValidationMessage(Long pageId);
}
