package com.mesilat.vbp.impl;

import com.mesilat.vbp.ValidatorInfo;
import com.mesilat.vbp.api.Validator;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mesilat.vbp.Constants;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@ExportAsService ({ValidatorManager.class})
@Named ("vbpValidatorManager")
public class ValidatorManagerImpl implements ValidatorManager, InitializingBean, DisposableBean {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Constants.PLUGIN_KEY);

    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread t = new Thread(() -> installValidators());
        t.start();
    }
    @Override
    public void destroy() throws Exception {
    }
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
    public boolean contains(String code) {
        return ao.count(ValidatorInfo.class, "CODE = ?", code) > 0;
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

    private void installValidators() {
        try {
            ao.moduleMetaData().awaitInitialization();
        } catch (ExecutionException | InterruptedException ex) {
            LOGGER.warn("Error waiting for AO to start", ex);
        }
        ao.executeInTransaction(() -> {
            int count = ao.count(ValidatorInfo.class);
            if (count == 0) {
                try (InputStream in = this.getClass().getResourceAsStream("/validators.json")) {
                    ArrayNode arr = (ArrayNode)mapper.readTree(in);
                    arr.forEach(obj -> {
                        Validator validator = new Validator(obj.get("code").asText(), obj.has("name")? obj.get("name").asText(): null);
                        validator.setType(obj.get("type").asText());
                        validator.setPrompt(obj.has("prompt")? obj.get("prompt").asText(): null);
                        validator.setWarning(obj.has("warning")? obj.get("warning").asText(): null);
                        validator.setText(obj.has("text")? obj.get("text").asText(): null);
                        validator.setModule(obj.has("module")? obj.get("module").asText(): null);
                        this.create(validator);
                    });
                } catch (IOException ex) {
                    LOGGER.warn("Failed to read validators from resource \"/validators.json\"", ex);
                }
            }
            return null;
        });
    }

    @Inject
    public ValidatorManagerImpl(final @ComponentImport ActiveObjects ao){
        this.ao = ao;
    }
}