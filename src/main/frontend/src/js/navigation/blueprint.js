import { trace } from "../util";
import { get } from "../api";
import { X_VBP_TEMPLATE, REST_API_PATH } from "../constants";

const getTemplateSetting = async templateKey => get(`${REST_API_PATH}/template/${templateKey}`);
const getBlueprintInfo = async contentBlueprintId => get(`/rest/create-dialog/1.0/blueprints/get/${contentBlueprintId}`);

export async function checkResponse(response, request) {
  trace("blueprint::checkResponse");
  const spaceKey = response.spaceKey;
  const draftId = response.draftId;
  if (!draftId) {
    return;
  }
  let templateKey = request.contentTemplateId || request.contentTemplateKey;
  if (!templateKey) {
    const blueprint = await getBlueprintInfo(request.contentBlueprintId);
    if (blueprint.contentTemplateRefs && blueprint.contentTemplateRefs.length > 0) {
      templateKey = blueprint.contentTemplateRefs[0].moduleCompleteKey;
    }
  }
  if (!templateKey) {
    trace("blueprint::checkResponse failed to obtain templateKey");
    return;
  }

  const data = {
    draftId, templateKey, spaceKey
  };
  const templateSettings = await getTemplateSetting(templateKey);
  $.extend(data, templateSettings);
  window.localStorage[X_VBP_TEMPLATE] = JSON.stringify(data);
  trace("blueprint::checkResponse", data);
}
