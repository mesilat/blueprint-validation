import $ from "jquery";
import MutationObserver from "mutation-observer";
import registry from "./registry";
import { notifyError, notifySuccess, trace } from "./util";
import { checkLocation } from "./navigation/nav";
import installTableHook from "./edit/table";
import installAjaxSpy from "./edit/ajaxspy";
import installOnSaveHook from "./edit/onsave";
import { X_VBP_TEMPLATE, REST_API_PATH } from "./constants";
import { get } from "./api";

const getTemplateSetting = async templateKey => get(`${REST_API_PATH}/template/${templateKey}`);
const getBlueprintInfo = async id/* blueprint id */ => get(`${REST_API_PATH}/blueprint`, { id });

async function init(){

  const query = new URLSearchParams(window.location.search);
  if (query.has("draftId") && window.localStorage[X_VBP_TEMPLATE]) {
    do {
      const docTemplate = JSON.parse(window.localStorage[X_VBP_TEMPLATE]);
      if (`${docTemplate.draftId}` === query.get("draftId")) {
        if (!docTemplate.templateKey) {
          if (!docTemplate.blueprintId)
            break;

          try {
            const blueprint = await getBlueprintInfo(docTemplate.blueprintId);
            if (blueprint && blueprint.contentTemplateRefs && blueprint.contentTemplateRefs.length > 0)
              docTemplate.templateKey = blueprint.contentTemplateRefs[0].moduleCompleteKey;
            else
              break;
          } catch (err) {
            trace(`edit::init: getBlueprintInfo(${docTemplate.blueprintId}) failed`);
            console.error(err);
            break;;
          }
        }

        try {
          const templateSettings = await getTemplateSetting(docTemplate.templateKey);
          $.extend(docTemplate, templateSettings);
          $(document).data(X_VBP_TEMPLATE, docTemplate);
          trace("edit::init restored X_VBP_TEMPLATE", docTemplate);
        } catch (err) {
          // trace(err);
        }
      }
    } while (false);
/*
  } else {
    setTimeout(() => {
      const query = new URLSearchParams(window.location.search);
      $(document).data(X_VBP_TEMPLATE, { pageId: query.get("pageId") });
    });
*/
  }

  checkLocation();

  const ed = AJS.Rte.getEditor();

  // Install CSS for prompts and warnings
  const $link = $(`<link rel="stylesheet" type="text/css" href="${AJS.contextPath()}${REST_API_PATH}/validator/css">`,
    ed.contentDocument);
  $link.appendTo($(ed.contentDocument.head));

  const lastTdHolder = { lastTd: null };

  function checkTd() {
    const td = ed.selection.getNode().closest("td.confluenceTd");
    if (lastTdHolder.lastTd !== null && lastTdHolder.lastTd !== td){
      registry.focusout($(lastTdHolder.lastTd), ed, lastTdHolder);
    }
    if (td !== null && td !== lastTdHolder.lastTd){
      registry.focusin($(td), ed);
    }
    lastTdHolder.lastTd = td;
  }

  ed.dom.bind(ed.getDoc(), "keyup", (e) => {
    if (e.key === "Tab"){
      setTimeout(() => {
        checkTd();
      });
    }
  });
  ed.dom.bind(ed.getDoc(), "focusout", (e) => {
    setTimeout(() => {
      checkTd();
    });
  });
  $(ed.getDoc()).on("click", () => {
    setTimeout(() => {
      checkTd();
    });
  });

  $(ed.getDoc()).find("td.confluenceTd").each(function(){
    registry.focusout($(this), ed, lastTdHolder);
  });

  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeName === "TR" && $(node).hasClass("dsobject")){
          setTimeout(() => {
            $(node).find("td").each(function(){
              const $td = $(this);
              this.className.split(/\s+/).forEach((className) => {
                if (className in registry.classes && typeof registry.classes[className].reset === "function"){
                  registry.classes[className].reset($td, registry.isEmpty($td), ed);
                }
              });
            });
          });
        }
      });
    });
  });
  observer.observe(ed.getDoc(), {
    childList: true,
    subtree: true
  });

  // Manage table rows
  installTableHook();
  // Fix object ids on save
  installOnSaveHook();
}

export default () => {
  // It is possible that the "init.rte" event was already fired
  // so we need to check "early hook" for this
  const preinit = window.require("com.mesilat.vbp.preinit");
  if (preinit.getInitRteCalled()){
    init();
  } else {
    AJS.bind("init.rte", () => init());
  }

  installAjaxSpy();
};
