package com.mesilat.vbp.impl;

import com.mesilat.vbp.api.ValidationException;
import com.mesilat.vbp.api.ValidationService;

public interface ValidationServiceEx extends ValidationService {
    void registerValidationTask(String uuid, Long pageId, String pageTitle);
    ValidationServiceImpl.ValidationTask getValidationTask(String uuid);
    void runValidationTask(String uuid, String templateKey, String data) throws ValidationException;
}
