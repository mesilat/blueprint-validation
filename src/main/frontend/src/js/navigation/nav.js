import $ from "jquery";
import _ from "lodash";
import { trace } from "../util";
import { get } from "../api";
import { X_VBP_TEMPLATE } from "../constants";

const getTemplateSetting = async templateKey => get(`/rest/blueprint-validation/1.0/template/${templateKey}`);

export async function checkLocation() {
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
    const data = {
      draftId, templateKey, spaceKey
    };
    const templateSettings = await getTemplateSetting(templateKey);
    _.extend(data, templateSettings);
    trace("nav::checkLocation", data);
    $(window.document).data(X_VBP_TEMPLATE, data);
  }
}
