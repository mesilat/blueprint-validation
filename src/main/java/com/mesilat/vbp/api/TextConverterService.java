package com.mesilat.vbp.api;

import com.atlassian.confluence.content.render.xhtml.XhtmlException;

public interface TextConverterService {
    String convertToStorage(String editorFormat) throws XhtmlException;
}
