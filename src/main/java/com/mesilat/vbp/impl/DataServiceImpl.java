package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.pages.Page;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.PageInfo;
import com.mesilat.vbp.api.DataService;
import javax.inject.Named;
import net.java.ao.DBParam;

@ExportAsService ({DataService.class})
@Named ("vbpDataService")
public class DataServiceImpl implements DataService {
    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ObjectNode getPageInfo(Long pageId) {
        PageInfo info = ao.get(PageInfo.class, pageId);
        return (info == null)? null: PageInfo.toObjectNode(mapper, info);
    }
    @Override
    public void createPageInfo(Page page, String templateKey, Boolean isValid, String message, String data) {
        PageInfo pageInfo = ao.get(PageInfo.class, page.getId());
        if (pageInfo == null) {
            pageInfo = ao.create(PageInfo.class,
                new DBParam("PAGE_ID", page.getId()),
                new DBParam("PAGE_TITLE", page.getTitle()),
                new DBParam("TEMPLATE_KEY", templateKey)
            );
            pageInfo.setData(data);
            pageInfo.setValid(isValid);
            pageInfo.setValidationMessage(message);
            pageInfo.save();
        } else {
            if (!pageInfo.getTemplateKey().equals(templateKey)) {
                throw new RuntimeException("Invalid page template key");
            }
            pageInfo.setPageTitle(page.getTitle());
            pageInfo.setData(data);
            pageInfo.setValid(isValid);
            pageInfo.setValidationMessage(message);
            pageInfo.save();
        }
    }
    @Override
    public void updatePageInfo(Page page, Boolean isValid, String message, String data) {
        PageInfo pageInfo = ao.get(PageInfo.class, page.getId());
        pageInfo.setPageTitle(page.getTitle());
        pageInfo.setData(data);
        pageInfo.setValid(isValid);
        pageInfo.setValidationMessage(message);
        pageInfo.save();
    }

    public DataServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }
}
