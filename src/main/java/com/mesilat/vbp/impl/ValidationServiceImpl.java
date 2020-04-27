package com.mesilat.vbp.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.mesilat.vbp.api.ParseException;
import com.mesilat.vbp.api.ParserService;
import com.mesilat.vbp.api.Template;
import com.mesilat.vbp.api.TemplateManager;
import com.mesilat.vbp.api.ValidationException;
import com.mesilat.vbp.api.ValidationService;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import net.java.ao.schema.StringLength;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@ExportAsService ({ValidationService.class})
@Named ("vbpValidationService")
public class ValidationServiceImpl implements ValidationService, InitializingBean, DisposableBean, Runnable {
    private final JsonSchemaFactory factory;
    private final TemplateManager templateManager;
    private final ParserService parserService;
    @ComponentImport
    private final I18nResolver resolver;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String,ValidationTask> tasks = new HashMap<>();
    private Thread thread;

    @Override
    public void afterPropertiesSet() throws Exception {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        // Do some cleaning once in 10 minutes
        while (true) {
            try {
                Thread.sleep(600000l);
                List<String> tasksToRemove = new ArrayList<>();
                Long threshold = System.currentTimeMillis() - 3600000l; // one hour ago
                synchronized(tasks) {
                    tasks.forEach((uuid, task) -> {
                        if (task.getTimestamp() < threshold) {
                            tasksToRemove.add(uuid);
                        }
                    });
                    tasksToRemove.forEach(uuid -> tasks.remove(uuid));
                }
            } catch(InterruptedException ignore) {
                break;
            }
        }
    }

    @Override
    public void validate(String templateKey, String data) throws ValidationException {
        try {
            Template template = templateManager.get(templateKey);
            JsonNode schema = JsonLoader.fromString(
                template.getSchema() == null?
                parserService.generateSchemaForTemplate(templateKey):
                template.getSchema()
            );

            JsonValidator validator = factory.getValidator();
            ProcessingReport report = validator.validate(schema, mapper.readTree(data));
            if (!report.isSuccess()) {
                StringBuilder sb = new StringBuilder();
                report.forEach(processingMessage -> {
                    sb.append(processingMessage.getMessage()).append("\n");
                });
                String message = sb.toString();
                if (message.length() > StringLength.MAX_LENGTH) {
                    message = message.substring(0, StringLength.MAX_LENGTH - 4).concat("...");
                }
                throw new ValidationException(message);
            }
        } catch (IOException | ProcessingException | ParseException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    public void registerValidationTask(String uuid, Long pageId, String pageTitle) {
        synchronized(tasks) {
            tasks.put(uuid, new ValidationTask(uuid, pageId, pageTitle));
        }
    }

    @Override
    public ValidationTask getValidationTask(String uuid) {
        synchronized(tasks) {
            return tasks.get(uuid);
        }
    }

    @Override
    public void runValidationTask(String uuid, String templateKey, String data) throws ValidationException {
        ValidationTask task;
        synchronized(tasks) {
            task = tasks.get(uuid);
        }
        if (task == null) {
            throw new ValidationException("Validation task not registered");            
        }
        if (!"pending".equals(task.getStatus())) {
            throw new ValidationException("Invalid validation task status");
        }
        task.setStatus("validating");
        try {
            validate(templateKey, data);
            task.setStatus("valid");
        } catch (Throwable ex) {
            task.setStatus("invalid");
            String message = MessageFormat.format(resolver.getText("com.mesilat.vbp.validation.error.message"), ex.getMessage());
            task.setMessage(message);
            throw ex;
        }
    }

    @Inject
    public ValidationServiceImpl(
        TransactionTemplate transactionTemplate, TemplateManager templateManager,
        ParserService parserService, I18nResolver resolver
    ) {
        this.templateManager = templateManager;
        this.parserService = parserService;
        this.resolver = resolver;
        this.factory = transactionTemplate.execute(() -> {
            return JsonSchemaFactory.byDefault();
        });
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ValidationTask {
        @XmlElement
        private final String uuid;
        @XmlElement
        private final Long pageId;
        @XmlElement
        private final String pageTitle;
        private final Long timestamp;
        @XmlElement
        private String status;
        @XmlElement
        private String message;

        public Long getTimestamp() {
            return timestamp;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }

        public ValidationTask(String uuid, Long pageId, String pageTitle) {
            this.uuid = uuid;
            this.pageId = pageId;
            this.pageTitle = pageTitle;
            this.timestamp = System.currentTimeMillis();
            status = "pending";
        }

        public String getUuid() {
            return uuid;
        }
    }
}
