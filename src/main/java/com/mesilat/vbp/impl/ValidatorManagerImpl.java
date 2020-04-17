package com.mesilat.vbp.impl;

import com.mesilat.vbp.ValidatorInfo;
import com.mesilat.vbp.api.Validator;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.ao.DBParam;
import net.java.ao.Query;
import com.mesilat.vbp.api.ValidatorManager;

@ExportAsService ({ValidatorManager.class})
@Named ("vbpValidatorManager")
public class ValidatorManagerImpl implements ValidatorManager {
    private final ActiveObjects ao;

    @Override
    public List<Validator> list(boolean extensive) {
        return Arrays.asList(ao.find(ValidatorInfo.class, Query.select("CODE, NAME")))
            .stream()
            .map(info -> extensive? ValidatorInfo.toValidator(info) :new Validator(info.getCode(), info.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public Validator get(String code) {
        return ValidatorInfo.toValidator(ao.get(ValidatorInfo.class, code));
    }

    @Override
    public void delete(String code) {
        ao.deleteWithSQL(ValidatorInfo.class, "CODE = ?", code);
    }

    @Override
    public void create(Validator validator) {
        ao.executeInTransaction(() -> {
            _create(validator);
            return null;
        });
    }
    @Override
    public void create(List<Validator> validators) {
        ao.executeInTransaction(() -> {
            validators.forEach(validator -> {
                if (ao.count(ValidatorInfo.class, "CODE = ?", validator.getCode()) == 0) {
                    _create(validator);
                }
            });
            return null;
        });
    }
    private void _create(Validator validator) {
        ValidatorInfo info = ao.create(ValidatorInfo.class,
            new DBParam("CODE", validator.getCode()),
            new DBParam("NAME", validator.getName()),
            new DBParam("TYPE", validator.getType())
        );
        info.setPrompt(validator.getPrompt());
        info.setWarning(validator.getWarning());
        info.setText(validator.getText());
        info.setModule(validator.getModule());
        info.save();        
    }

    @Override
    public void update(String code, Validator validator) {
        ao.executeInTransaction(() -> {
            ValidatorInfo info = ao.get(ValidatorInfo.class, code);
            if (info == null) {
                throw new RuntimeException("Validator could not be found");
            }
            if (code.equals(validator.getCode())) {
                info.setName(validator.getName());
                info.setType(validator.getType());
                info.setPrompt(validator.getPrompt());
                info.setWarning(validator.getWarning());
                info.setText(validator.getText());
                info.setModule(validator.getModule());
                info.save();        
            } else {
                delete(code);
                _create(validator);
            }
            return null;
        });
    }

    @Override
    public String css() {
        Map<String,String> prompts = new HashMap<>();
        Map<String,String> warnings = new HashMap<>();
        Map<String,String> warningsAfter = new HashMap<>();
        Arrays.asList(ao.find(ValidatorInfo.class, Query.select("CODE, PROMPT, WARNING")))
            .stream()
            .forEach(validator -> {
                if (validator.getPrompt() != null && !validator.getPrompt().isEmpty()) {
                    prompts.put(String.format("td.show-prompt.dsvalidate-%s:before", validator.getCode()), validator.getPrompt());
                }
                if (validator.getWarning()!= null && !validator.getWarning().isEmpty()) {
                    warnings.put(String.format("td.show-warning.dsvalidate-%s", validator.getCode()), null);
                    warningsAfter.put(String.format("td.show-warning.dsvalidate-%s:after", validator.getCode()), validator.getWarning());
                }
            });

        StringBuilder sb = new StringBuilder();
        if (prompts.size() > 0) {
            sb.append(String.join(",\n", prompts.keySet()))
            .append(" {\n")
            .append("  color: #707070;\n")
            .append("  background-color: #f5f5f5;\n")
            .append("  font-style: italic;\n")
            .append("  min-width: 10px;\n")
            .append("}\n\n");
        }
        prompts.forEach((key, prompt) -> {
            sb.append(key)
            .append(" {\n")
            .append("  content: \"")
            .append(prompt)
            .append("\";\n")
            .append("}\n\n");
        });

        if (warnings.size() > 0) {
            sb.append(String.join(",\n", warnings.keySet()))
            .append(" {\n")
            .append("  position: relative;\n")
            .append("  padding-bottom: 0.85rem;\n")
            .append("}\n\n");

            sb.append(String.join(",\n", warningsAfter.keySet()))
            .append(" {\n")
            .append("  color: red;\n")
            .append("  position: absolute;\n")
            .append("  bottom: 0;\n")
            .append("  left: 0.65rem;\n")
            .append("  font-size: 0.65rem;\n")
            .append("}\n\n");
        }
        
        warningsAfter.forEach((key, prompt) -> {
            sb.append(key)
            .append(" {\n")
            .append("  content: \"")
            .append(prompt)
            .append("\";\n")
            .append("}\n\n");
        });

        return sb.toString();
    }
   
    @Inject
    public ValidatorManagerImpl(final @ComponentImport ActiveObjects ao){
        this.ao = ao;
    }
}