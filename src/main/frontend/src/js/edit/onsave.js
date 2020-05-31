import $ from "jquery";
import { OBJECTID_PREFIX, OBJECT_CLASS, X_VBP_TEMPLATE } from "../constants";
import { generateUUID, trace, error } from "../util";
import tinymce from "tinymce";

function fixDataObjectIds () {
  trace("onsave::fixDataObjectIds");

  const ids = {};
  try {
    $("#wysiwygTextarea_ifr").contents().find(`.${OBJECT_CLASS}`).each(function(){
      const classNames = [];
      let id;
      this.className.split(/\s+/).forEach(className => {
        if (className.startsWith(OBJECTID_PREFIX))
          id = className;
        else
          classNames.push(className);
      });

      if (!id || id in ids){
        id = `${OBJECTID_PREFIX}${generateUUID()}`;
        classNames.push(id);
        this.className = classNames.join(" ");
      }
      ids[id] = this;
    });
  } catch (err) {
    error(err);
  }
}

function repairMarkup() {
  let templateKey = $(document).data(X_VBP_TEMPLATE);
  if (!templateKey)
    return;
  try {
    if ("templateKey" in templateKey)
      templateKey = templateKey.templateKey;
  } catch(ignore) {}
  try {
    const mod = window.require(templateKey);
    if (mod && typeof mod.repairMarkup === "function") {
      trace(`Repair markup for template ${templateKey}`);
      mod.repairMarkup($("#wysiwygTextarea_ifr").contents());
    }
  } catch(ignore) {}
}

function init() {
  Confluence.Editor.addSaveHandler(() => {
    fixDataObjectIds();
    repairMarkup();
  });
}

export default () => init();
