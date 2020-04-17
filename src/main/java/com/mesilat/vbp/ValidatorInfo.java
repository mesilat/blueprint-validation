package com.mesilat.vbp;

import com.mesilat.vbp.api.Validator;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Unique;

//@JsonIgnoreProperties({"entityManager", "entityProxy"})
//@JsonAutoDetect
public interface ValidatorInfo extends RawEntity<String> {
    public static enum TYPES { LOFV, REXP, NUMR, USER, PAGE, MODL };

    @NotNull
    @StringLength(30)
    @PrimaryKey(value = "CODE")
    public String getCode();
    void setCode(String code);

    @NotNull
    @StringLength(64)
    @Unique
    String getName();
    void setName(String name);

    @NotNull
    @StringLength(4)
    String getType();
    void setType(String type);
    
    @StringLength(128)
    String getPrompt();
    void setPrompt(String prompt);

    @StringLength(128)
    String getWarning();
    void setWarning(String warning);

    @StringLength(StringLength.UNLIMITED)
    String getText();
    void setText(String text);

    @StringLength(128)
    String getModule();
    void setModule(String module);

    public static Validator toValidator(ValidatorInfo info) {
        Validator validator = new Validator();
        validator.setCode(info.getCode());
        validator.setName(info.getName());
        validator.setType(info.getType());
        validator.setPrompt(info.getPrompt());
        validator.setWarning(info.getWarning());
        validator.setText(info.getText());
        validator.setModule(info.getModule());
        return validator;
    }
}