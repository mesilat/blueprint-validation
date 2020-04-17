package com.mesilat.vbp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;

//@JsonIgnoreProperties({"entityManager", "entityProxy"})
//@JsonAutoDetect
public interface PageInfo extends RawEntity<Long> {
    @NotNull
    @PrimaryKey(value = "PAGE_ID")
    public Long getPageId();
    void setPageId(Long pageId);

    @StringLength(255)
    public String getPageTitle();
    void setPageTitle(String pageTitle);

    @StringLength(255)
    @NotNull
    public String getTemplateKey();
    void setTemplateKey(String templateKey);

    public Boolean isValid();
    void setValid(Boolean valid);

    @StringLength(StringLength.MAX_LENGTH)
    public String getValidationMessage();
    void setValidationMessage(String validationMessage);

    @StringLength(StringLength.UNLIMITED)
    String getData();
    void setData(String data);

    public static ObjectNode toObjectNode(ObjectMapper mapper, PageInfo info) {
        try {
            ObjectNode obj = mapper.createObjectNode();
            ObjectNode page = mapper.createObjectNode();
            page.put("id", info.getPageId());
            page.put("title", info.getPageTitle());
            obj.set("page", page);
            obj.set("data", mapper.readTree(info.getData()));
            obj.put("valid", info.isValid());
            obj.put("validationMessage", info.getValidationMessage());
            return obj;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}