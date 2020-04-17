import $ from "jquery";
import _ from "lodash";
import MutationObserver from "mutation-observer";
import registry from "./registry";
import { notifyError, notifySuccess } from "./util";
import { post } from "./api";
import { trace } from "./util";

const registerDraft = async draft => post(`/rest/blueprint-validation/1.0/draft`, draft);


function init(){
  console.debug("validating-blueprints", "edit.js::init()");

  const ed = AJS.Rte.getEditor();

  // Install CSS for prompts and warnings
  const $link = $(`<link rel="stylesheet" type="text/css" href="${AJS.contextPath()}/rest/blueprint-validation/1.0/validator/css">`,
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
        //console.debug('edit::Tab', lastTdHolder.lastTd);
      });
    }
  });
  ed.dom.bind(ed.getDoc(), "focusout", (e) => {
    setTimeout(() => {
      checkTd();
      //console.debug('edit::focusout', lastTdHolder.lastTd);
    });
  });
  $(ed.getDoc()).on("click", () => {
    setTimeout(() => {
      checkTd();
      //console.debug('edit::click', lastTdHolder.lastTd);
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
                if (className in registry.classes && _.isFunction(registry.classes[className].reset)){
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

  if ($(document).data("bvn-ajax-hook-installed")) {
    return;
  } else {
    $(document).data("bvn-ajax-hook-installed", true);
  }

  // Install ajax hook to track certain API calls
  $(document).ajaxError((e,xhr,options) => {
    if (
      (
          options.type === "POST" && options.url.indexOf("/rest/api/content?") >= 0
        || options.type === "PUT" && options.url.indexOf("/rest/api/content/") >= 0
      )
      && xhr.status === 500
      && xhr.responseJSON
      && xhr.responseJSON.data
      && xhr.responseJSON.data.reason === "JSON Validation"
    ){
      console.debug(`vbp-ajax-error`, xhr.responseJSON.data.message);
      // Expect popup message and modify text to desired
      // for details take a look at reliable-save.js method _makeRequest
      // in Confluence sources
      /*
      function hackMessage(count) {
        if (count > 100) {
          return;
        }
        const $msg = $('.aui-flag .server-offline');
        if ($msg.length === 0) {
          setTimeout(count => hackMessage(count), 20, count + 1);
        } else {
          $msg.text(xhr.responseJSON.data.message);
        }
      }
      hackMessage(0);
      */
      setTimeout(() => {
        const $msg = $(".aui-flag .server-offline");
        if ($msg.length > 0) {
          $msg.text(xhr.responseJSON.data.message);
        }
      });
    }
  });

  $(document).ajaxSuccess((e,xhr,options) => {
    if (
        options.type === "POST"
      && options.url.indexOf("/rest/tinymce/1/drafts") >= 0
      && xhr.status === 200
      && xhr.responseJSON
      && xhr.responseJSON.draftId
      && window.location.pathname.indexOf("/pages/createpage-entervariables.action") >= 0
    ){
      const query = new URLSearchParams(window.location.search);
      if (!query.has("templateId")) {
        return;
      }
      const data = {
        "draft-id": parseInt(xhr.responseJSON.draftId, 10),
        "template-key": query.get("templateId"),
        "space-key": query.get("spaceKey")
      };
      registerDraft(data);

    } else if (
        (options.type === "POST" || options.type === "PUT")
      && options.url.indexOf("/rest/api/content") >= 0
    ) {
      // the response to POST /rest/api/content may contain
      // X-Blueprint-Validation and X-Blueprint-Validation-Task headers
      // that can be used to obtain the result of page validation by
      // the plugin.
      const validationResult = xhr.getResponseHeader("X-Blueprint-Validation");
      trace(`Blueprint-Validation: ${validationResult}`);
      if (validationResult === "invalid") {
        // processed above
      } else if (validationResult === "invalid") {
        // do we need to notify success as well?
      } else if (validationResult === "pending validation") {
        const task = xhr.getResponseHeader("X-Blueprint-Validation-Task");
        trace(`Blueprint-Validation-Task: ${task}`);
        if (task) {
          // we can't just show AJS.flag here as the page will be fully refreshed
          // in a matter of (milli)seconds. Thus we must use local storage to
          // delegate notification processing to succeeding (view) page
          let pendingTasks = window.localStorage.vbpPendingTasks;
          pendingTasks = pendingTasks? JSON.parse(pendingTasks): {};
          pendingTasks[task] = Date.now();
          window.localStorage.vbpPendingTasks = JSON.stringify(pendingTasks);
        }
      }
    }
  });

};
