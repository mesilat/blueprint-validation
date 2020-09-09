package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.pages.Page;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mesilat.vbp.DataObject;
import com.mesilat.vbp.PageInfo;
import com.mesilat.vbp.api.DataService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Named;
import net.java.ao.DBParam;

@ExportAsService ({DataService.class, DataServiceEx.class})
@Named ("vbpDataService")
public class DataServiceImpl implements DataServiceEx {
    private static final Pattern RE_OBJID = Pattern.compile("dsobjid-([0-9a-f\\-]+)");

    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getPageData(Long pageId) {
        PageInfo info = ao.get(PageInfo.class, pageId);
        return info == null? null: info.getData();
    }
    @Override
    public boolean isPageValid(Long pageId) {
        PageInfo info = ao.get(PageInfo.class, pageId);
        return info == null? null: info.isValid();
    }
    @Override
    public String getPageValidationMessage(Long pageId) {
        PageInfo info = ao.get(PageInfo.class, pageId);
        return info == null? null: info.getValidationMessage();
    }
    @Override
    public ObjectNode getPageInfo(Long pageId) {
        PageInfo info = ao.get(PageInfo.class, pageId);
        return info == null? null: PageInfo.toObjectNode(mapper, info);
    }
    @Override
    public PageInfo getPageInfo(Page page) {
        return ao.get(PageInfo.class, page.getId());
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
            pageInfo.setTemplateKey(templateKey);
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
    @Override
    public void deletePageInfo(Page page) {
        PageInfo pageInfo = ao.get(PageInfo.class, page.getId());
        if (pageInfo != null) {
            pageInfo.setDeleted(Boolean.TRUE);
            pageInfo.save();
        }
    }
    @Override
    public void undeletePageInfo(Page page) {
        PageInfo pageInfo = ao.get(PageInfo.class, page.getId());
        if (pageInfo != null) {
            pageInfo.setDeleted(Boolean.FALSE);
            pageInfo.save();
        }
    }

    @Override
    public void registerDataObjectIds(long pageId, String storageFormat) {
        ao.deleteWithSQL(DataObject.class, "PAGE_ID = ?", pageId);
        Matcher m = RE_OBJID.matcher(storageFormat);
        while (m.find()){
            String objid = m.group(1);
            ao.create(DataObject.class, new DBParam("ID", objid), new DBParam("PAGE_ID", pageId));
        }
    }
    @Override
    public Long getDataObjectPage(String objid) {
        DataObject dataObject = ao.get(DataObject.class, objid);
        return dataObject == null? null: dataObject.getPageId();
    }

    public DataServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }
}
