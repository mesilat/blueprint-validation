package com.mesilat.vbp;

import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.Template.ValidationMode;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;

//@JsonIgnoreProperties({"entityManager", "entityProxy"})
//@JsonAutoDetect
public interface TemplateInfo extends RawEntity<String> {
    @NotNull
    @StringLength(StringLength.MAX_LENGTH)
    @PrimaryKey(value = "TEMPLATE_KEY")
    public String getTemplateKey();
    void setTemplateKey(String templateKey);

    @StringLength(4)
    String getValidationMode();
    void setValidationMode(String validationMode);
    
    @StringLength(StringLength.UNLIMITED)
    String getSchema();
    void setSchema(String schema);

    public static Template toTemplate(TemplateInfo info) {
        if (info == null) {
            return null;
        }
        Template template = new Template();
        template.setTemplateKey(info.getTemplateKey());
        template.setValidationMode(info.getValidationMode() == null? null: ValidationMode.valueOf(info.getValidationMode()));
        template.setSchema(info.getSchema());
        return template;
    }
}