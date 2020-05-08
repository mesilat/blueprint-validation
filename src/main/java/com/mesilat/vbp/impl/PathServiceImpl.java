package com.mesilat.vbp.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.confluence.event.events.content.page.PageRemoveEvent;
import com.atlassian.confluence.event.events.content.page.PageRestoreEvent;
import com.atlassian.confluence.event.events.content.page.PageTrashedEvent;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import static com.mesilat.vbp.Constants.PLUGIN_KEY;
import com.mesilat.vbp.PageInfo;
import com.mesilat.vbp.api.DataValidateEvent;
import com.mesilat.vbp.api.PathService;
import com.mesilat.vbp.api.TemplateManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@ExportAsService ({PathService.class})
@Named
public class PathServiceImpl implements PathService, InitializingBean, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);

    private final Cache<String, String> cache;

    @ComponentImport
    private final ActiveObjects ao;
    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final EventPublisher eventPublisher;
    private final TemplateManager templateManager;
    private final DataServiceEx dataService;
    private final String baseUrl;
    private final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

    @Override
    public Object evaluate(String path) {
        LOGGER.trace(String.format("Evaluate path=%s", path));

        ArrayList arr = new ArrayList();
        Arrays.asList(ao.find(PageInfo.class)).forEach(page -> {
            if (Boolean.TRUE.equals(page.isDeleted())) {
                return;
            }
            try {
                Object o = evaluateText(page.getData(), path);
                if (o != null && !(o instanceof JsonNull)){
                    JsonObject val = formatResult(o, page);
                    if (val != null) {
                        arr.add(val);
                    }
                }
            } catch(Throwable ex){
                LOGGER.warn(String.format("Evaluate pageId=%d, path=%s failed", page.getPageId(), path), ex);
            }
        });

        return arr.isEmpty()? null: arr;
    }

    @Override
    public Object evaluate(Long pageId, String path) {
        LOGGER.trace(String.format("Evaluate pageId=%d, path=%s", pageId, path));

        PageInfo page = ao.get(PageInfo.class, pageId);
        if (page == null){
            return null;
        } else {
            Object o = evaluateText(page.getData(), path);
            if (o == null || o instanceof JsonNull){
                return null;
            } else {
                return formatResult(o, page);
            }
        }
    }
    @Override
    public Object evaluate(String templateKey, String path) {
        LOGGER.trace(String.format("Evaluate templateKey=%s, path=%s", templateKey, path));

        ArrayList arr = new ArrayList();
        PageInfo[] pages = ao.find(PageInfo.class, "TEMPLATE_KEY = ?", templateKey);
        Arrays.asList(pages).forEach(page -> {
            if (Boolean.TRUE.equals(page.isDeleted())) {
                return;
            }
            try {
                Object o = evaluateText(page.getData(), path);
                if (o != null && !(o instanceof JsonNull)){
                    JsonObject val = formatResult(o, page);
                    if (val != null) {
                        arr.add(val);
                    }
                }
            } catch(Throwable ex){
                LOGGER.warn(String.format("Evaluate pageId=%d, path=%s failed", page.getPageId(), path), ex);
            }
        });

        return arr.isEmpty()? null: arr;
    }
    @Override
    public String evalCached(Args args) {
        return cache.get(gson.toJson(args));
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
        cache.removeAll();
    }

    private Object evaluateText(String text, String path){
        try {
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(text);
            if (path == null || path.isEmpty()){
                return JsonPath.read(document, "$");
            } else {
                return JsonPath.read(document, "$." + path);
            }
        } catch(Throwable ex) {
            LOGGER.error(String.format("Error evaluating JSON path: %s in %s", path, text), ex);
            return null;
        }
    }
    private JsonObject formatResult(Object val, PageInfo info){
        if (val instanceof JsonElement){
            JsonElement elt = (JsonElement)val;
            if (elt.isJsonArray()) {
                JsonArray arr = (JsonArray)elt;
                if (arr.size() == 0) {
                    return null;
                } else if (arr.size() == 1) {
                    val = arr.get(0);
                }
            }
        }
        
        Page page = pageManager.getPage(info.getPageId());
        if (page == null) {
            return null;
        }

        JsonObject result = new JsonObject();
        JsonObject pageObject = new JsonObject();
        pageObject.addProperty("id", info.getPageId());
        pageObject.addProperty("title", page.getTitle());
        JsonArray labels = new JsonArray();
        page.getLabels().forEach(l -> labels.add(new JsonPrimitive(l.getDisplayTitle())));
        if (labels.size() != 0){
            pageObject.add("labels", labels);
        }
        pageObject.addProperty("href", String.format("%s/pages/viewpage.action?pageId=%d", baseUrl, info.getPageId()));
        if (page.isDeleted()) {
            pageObject.addProperty("deleted", Boolean.TRUE);
        }
        result.add("page", pageObject);

        JsonObject validation = new JsonObject();
        JsonObject template = new JsonObject();
        template.addProperty("key", info.getTemplateKey());
        template.addProperty("title", templateManager.getTemplateTitle(info.getTemplateKey()));
        validation.add("template", template);
        validation.addProperty("valid", info.isValid());
        validation.addProperty("message", info.getValidationMessage());
        validation.addProperty("href", String.format("%s/rest/blueprint-validation/1.0/data/%d", baseUrl, info.getPageId()));
        result.add("validation", validation);

        if (val instanceof JsonPrimitive){
            JsonObject data = new JsonObject();
            data.add("value", (JsonPrimitive)val);
            result.add("data", data);
        } else if (val instanceof JsonElement){
            result.add("data", (JsonElement)val);
        } else if (val instanceof String){
            JsonObject data = new JsonObject();
            data.addProperty("value", (String)val);
            result.add("data", data);
        } else if (val instanceof Number){
            JsonObject data = new JsonObject();
            data.addProperty("value", (Number)val);
            result.add("data", data);
        } else if (val instanceof Boolean){
            JsonObject data = new JsonObject();
            data.addProperty("value", (Boolean)val);
            result.add("data", data);
        } else if (val instanceof Character){
            JsonObject data = new JsonObject();
            data.addProperty("value", (Character)val);
            result.add("data", data);
        } else {
            result.addProperty("data", String.format("#Unexpected value type: %s", val.getClass().getName()));
        }
        return result;
    }
    /*
    @EventListener
    public void onPageCreateEvent(PageCreateEvent event) {
        PageInfo info = dataService.getPageInfo(event.getPage());
        if (info == null) {
            return;
        }
        resetCache(info.getTemplateKey());
    }
    @EventListener
    public void onPageUpdateEvent(PageUpdateEvent event) {
        PageInfo info = dataService.getPageInfo(event.getPage());
        if (info == null) {
            return;
        }
        resetCache(info.getTemplateKey());
    }
    */
    @EventListener
    public void validateEvent(DataValidateEvent event) {
        resetCache(event.getTemplateKey(), event.getPage());
    }
    @EventListener
    public void pageRestoreEvent(PageRestoreEvent event) {
        PageInfo info = dataService.getPageInfo(event.getPage());
        if (info == null) {
            return;
        }
        dataService.undeletePageInfo(event.getPage());
        resetCache(info.getTemplateKey(), event.getPage());
    }
    @EventListener
    public void pageTrashedEvent(PageTrashedEvent event) {
        PageInfo info = dataService.getPageInfo(event.getPage());
        if (info == null) {
            return;
        }
        dataService.deletePageInfo(event.getPage());
        resetCache(info.getTemplateKey(), event.getPage());
    }
    @EventListener
    public void pageRemoveEvent(PageRemoveEvent event) {
        PageInfo info = dataService.getPageInfo(event.getPage());
        if (info == null) {
            return;
        }
        dataService.deletePageInfo(event.getPage());
        resetCache(info.getTemplateKey(), event.getPage());
    }
    private void resetCache(String templateKey, Page page) {
        if (templateKey == null) {
            LOGGER.trace("Not resetting cache");
            return;
        }

        List<String> keys = new ArrayList<>();
        cache.getKeys().forEach(key -> {
            Args args = gson.fromJson(key, Args.class);
            if (args.getPageId() != null) {
                if (page != null && args.getPageId().equals(page.getId())) {
                    keys.add(key);
                }
            } else if (args.getTemplateKey() != null) {
                if (args.getTemplateKey().equals(templateKey)) {
                    keys.add(key);
                }
            } else {
                keys.add(key);
            }
        });
        LOGGER.trace(String.format("Resetting cache: %s", keys.toString()));
        keys.forEach(key -> cache.remove(key));
    }

    @Inject
    public PathServiceImpl(ActiveObjects ao, PageManager pageManager,
        @ComponentImport SettingsManager settingsManager,
        TemplateManager templateManager,
        @ComponentImport CacheManager cacheManager,
        EventPublisher eventPublisher, DataServiceEx dataService
    ){
        this.ao = ao;
        this.baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        this.pageManager = pageManager;
        this.templateManager = templateManager;
        this.eventPublisher = eventPublisher;
        this.dataService = dataService;

        this.cache = cacheManager.getCache(PathServiceImpl.class.getName() + ".cache",
            new DataCacheLoader(),
            new CacheSettingsBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build()
        );         
    }

    private class DataCacheLoader implements CacheLoader<String,String> {
        @Override
        public String load(String json) {
            Args args = gson.fromJson(json, Args.class);
            if (args.getPageId() != null) {
                Object data = evaluate(args.getPageId(), args.getPath());
                return gson.toJson(data);
            } else if (args.getTemplateKey() != null) {
                Object data = evaluate(args.getTemplateKey(), args.getPath());
                return gson.toJson(data);
            } else {
                Object data = evaluate(args.getPath());
                return gson.toJson(data);
            }
        }
    }

    static {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final GsonJsonProvider jsonProvider = new GsonJsonProvider();
            private final MappingProvider mappingProvider = new GsonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }
}