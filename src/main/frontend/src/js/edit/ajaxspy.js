import $ from "jquery";
import { X_VBP_TEMPLATE } from "../constants";
import { trace, notifyError } from "../util";
import { post } from "../api";
import { REST_API_PATH } from "../constants";

const INSTALLED = "bvn-ajax-hook-installed";
const registerDraft = async draft => post(`${REST_API_PATH}/draft`, draft);

function init() {
  if ($(document).data(INSTALLED))
    return;

  $(document).data(INSTALLED, true);

  $(document).ajaxSend((e,xhr,options) => {
    if (
        options.type === "POST" && options.url.indexOf("/rest/api/content?") >= 0
      || options.type === "PUT" && options.url.indexOf("/rest/api/content/") >= 0
      || options.type === "POST" && options.url.indexOf("/rest/api/content/blueprint/instance/") >= 0
    ){
      if ($(document).data(X_VBP_TEMPLATE)) {
        xhr.setRequestHeader(X_VBP_TEMPLATE, JSON.stringify($(document).data(X_VBP_TEMPLATE)));
      }
    }
  });

  // Install ajax hook to track certain API calls
  $(document).ajaxError((e,xhr,options) => {
    if (
      (
          options.type === "POST" && options.url.indexOf("/rest/api/content?") >= 0
        || options.type === "PUT" && options.url.indexOf("/rest/api/content/") >= 0
        || options.type === "POST" && options.url.indexOf("/rest/api/content/blueprint/instance/") >= 0
      )
      && xhr.status === 500
      && xhr.responseJSON
      && xhr.responseJSON.data
      && xhr.responseJSON.data.reason === "JSON Validation"
    ){
      // Replace default error popup with validation result;
      // for details take a look at reliable-save.js method _makeRequest
      // in Confluence sources
      setTimeout(() => {
        if ($(".aui-flag .server-offline").length > 0) {
          const Message = window.require("confluence-editor/editor/page-editor-message");
          Message.closeMessages(["server-offline"]);
        }
        notifyError(
          AJS.I18n.getText("com.mesilat.vbp.validation.error.title"),
          xhr.responseJSON.data.message
        );
      });
    }
  });

  $(document).ajaxSuccess((e,xhr,options) => {
    if (
        options.type === "POST"
      && options.url.indexOf("/rest/create-dialog/1.0/content-blueprint/create-draft") >= 0
    ){
      // trace("from template", xhr.responseJSON. options);
    } else if (
        options.type === "POST"
      && options.url.indexOf("/rest/tinymce/1/drafts") >= 0
      && xhr.status === 200
      && xhr.responseJSON
      && xhr.responseJSON.draftId
      && window.location.pathname.indexOf("/pages/createpage-entervariables.action") >= 0
    ){
      const query = new URLSearchParams(window.location.search);
      if (!query.has("templateId"))
        return;

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
    } else if (
      options.type === "GET" && options.url.indexOf(`/rest/api/content/${AJS.Meta.get("page-id")}?`) >= 0
    ) {
      const templateKey = xhr.getResponseHeader(X_VBP_TEMPLATE);
      if (templateKey && !$(document).data(X_VBP_TEMPLATE))
        $(document).data(X_VBP_TEMPLATE, templateKey);
    }
  });
}

export default () => init();
