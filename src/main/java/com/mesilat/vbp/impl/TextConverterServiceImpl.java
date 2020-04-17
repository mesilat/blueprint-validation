package com.mesilat.vbp.impl;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlCleaner;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.editor.EditorConverter;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.vbp.api.TextConverterService;
import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService ({TextConverterService.class})
@Named ("vbpTextConverterService")
public class TextConverterServiceImpl implements TextConverterService {
    @ComponentImport
    private final EditorConverter editConverter;
    @ComponentImport
    private final XhtmlCleaner storageFormatCleaner;

    @Override
    public String convertToStorage(String editorFormat) throws XhtmlException {
        PageContext renderContext = new PageContext();
        ConversionContext conversionContext = new DefaultConversionContext(renderContext);
        String storageFormat = convertContentToStorageFormat(editorFormat, conversionContext);
        return storageFormatCleaner.cleanQuietly(storageFormat, conversionContext);
    }

    private String convertContentToStorageFormat(String wysiwygContent, ConversionContext conversionContext) throws XhtmlException {
        return editConverter.convert(wysiwygContent, conversionContext);
    }

    @Inject
    public TextConverterServiceImpl(EditorConverter editConverter, XhtmlCleaner storageFormatCleaner) {
        this.editConverter = editConverter;
        this.storageFormatCleaner = storageFormatCleaner;
    }
}
