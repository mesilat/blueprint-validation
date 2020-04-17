package com.mesilat.vbp.api;

import com.atlassian.confluence.api.service.exceptions.ServiceException;

public class ValidationException extends ServiceException {
    public ValidationException(String text) {
        super(text);
    }
    public ValidationException(String text, Throwable cause) {
        super(text, cause);
    }
    public ValidationException(Throwable cause) {
        super(cause);
    }
}
