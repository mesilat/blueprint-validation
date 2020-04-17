package com.mesilat.vbp.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.impl.ValidationServiceImpl;

public interface ValidationService {
    void validate(String templateKey, ObjectNode data) throws ValidationException;
    void registerValidationTask(String uuid, Long pageId, String pageTitle);
    ValidationServiceImpl.ValidationTask getValidationTask(String uuid);
    void runValidationTask(String uuid, String templateKey, ObjectNode data) throws ValidationException;
}
