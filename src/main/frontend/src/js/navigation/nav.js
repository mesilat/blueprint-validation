import $ from "jquery";
import _ from "lodash";
import { trace } from "../util";
import { get } from "../api";
import { X_VBP_TEMPLATE, REST_API_PATH } from "../constants";

const getTemplateSetting = async templateKey => get(`${REST_API_PATH}/template/${templateKey}`);
const getTemplateForPage = async pageId => get(`${REST_API_PATH}/data/template/${pageId}`);

export async function checkLocation() {
  // Create page from blueprint
  if (window.location.pathname.endsWith("/pages/createpage-entervariables.action")) {
    const draftId = $("input#draftId").val();
    if (!draftId) {
      return;
    }
    const query = new URLSearchParams(window.location.search);
    if (!query.has("templateId")) {
      return;
    }
    const templateKey = query.get("templateId");
    const spaceKey = query.get("spaceKey");
    const data = { draftId, templateKey, spaceKey };
    try {
      const templateSettings = await getTemplateSetting(templateKey);
      _.extend(data, templateSettings);
      trace("nav::checkLocation", data);
      $(window.document).data(X_VBP_TEMPLATE, data);
    } catch (err) {
      // trace(err)
    }

  // Create page from copy
  } else if (window.location.pathname.endsWith("/pages/copypage.action")) {
    const query = new URLSearchParams(window.location.search);
    if (!query.has("idOfPageToCopy")) {
      return;
    }
    const idOfPageToCopy = query.get("idOfPageToCopy");
    const spaceKey = query.get("spaceKey");
    try {
      const templateKey = await getTemplateForPage(idOfPageToCopy);
      if (templateKey) {
        const data = { templateKey, spaceKey };
        const templateSettings = await getTemplateSetting(templateKey);
        _.extend(data, templateSettings);
        trace("nav::checkLocation", data);
        $(window.document).data(X_VBP_TEMPLATE, data);
      }
    } catch (err) {
      trace(err);
    }
  }
}
