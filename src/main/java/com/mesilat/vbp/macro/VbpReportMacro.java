package com.mesilat.vbp.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.template.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import static com.mesilat.vbp.Constants.PLUGIN_KEY;
import java.util.Map;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scanned
public class VbpReportMacro implements Macro {
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);

    @ComponentImport
    private final TemplateRenderer renderer;

    @Override
    public String execute(Map<String, String> map, String str, ConversionContext cc) throws MacroExecutionException {
        try {
            if (!map.containsKey("columns")){
                map.put("columns", "page;template;valid");

                if (!map.containsKey("captions")){
                    map.put("captions", "Page;Template;Valid");
                }
            }
            if (!map.containsKey("captions")){
                map.put("captions", "");
            }
            return renderFromSoy("macro-resources", "Mesilat.BlueprintValidation.macroReport.soy", map);
        } catch(Throwable ex) {
            throw new MacroExecutionException(ex);
        }
    }
    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }
    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    @Inject
    public VbpReportMacro(TemplateRenderer renderer){
        this.renderer = renderer;
    }

    public String renderFromSoy(String key, String soyTemplate, Map soyContext) {
        StringBuilder output = new StringBuilder();
        renderer.renderTo(output, String.format("%s:%s", PLUGIN_KEY, key), soyTemplate, soyContext);
        return output.toString();
    }
}
