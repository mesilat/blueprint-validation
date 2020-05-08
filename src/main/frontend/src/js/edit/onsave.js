import $ from "jquery";
import { OBJECTID_PREFIX, OBJECT_CLASS } from "../constants";
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

function init() {
  Confluence.Editor.addSaveHandler(() => fixDataObjectIds());
}

export default () => init();
