package com.mesilat.vbp.api;

import com.atlassian.confluence.pages.Page;

public interface ValidationService {
    void validate(String templateKey, String data) throws ValidationException;
    void validate(String templateKey, Page page) throws ParseException;
    void validate(String templateKey, Page page, boolean transaction) throws ParseException;
}
