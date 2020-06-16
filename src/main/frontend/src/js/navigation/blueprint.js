import { trace } from "../util";
import { get } from "../api";
import { X_VBP_TEMPLATE, REST_API_PATH } from "../constants";

// const getTemplateSetting = async templateKey => get(`${REST_API_PATH}/template/${templateKey}`);
// const getBlueprintInfo = async id/* blueprint id */ => get(`${REST_API_PATH}/blueprint`, { id });

export async function checkResponse(response, request) {
  trace("blueprint::checkResponse", response, request);
  const spaceKey = response.spaceKey;
  const draftId = response.draftId;
  if (!draftId) {
    trace("blueprint::checkResponse: no draftId found in response, aborting...");
    return;
  }
  trace(`blueprint::checkResponse: draftId=${draftId}`);

  /*
  Safari does not allow queries from ajax event handler, so the following code
  does not work:
  if (!templateKey) {
    trace(`blueprint::checkResponse: get templateKey for blueprint ${request.contentBlueprintId}`);
    try {

      const blueprint = await getBlueprintInfo(request.contentBlueprintId);
      if (blueprint.contentTemplateRefs && blueprint.contentTemplateRefs.length > 0) {
        templateKey = blueprint.contentTemplateRefs[0].moduleCompleteKey;
      }
    } catch (err) {
      trace(`blueprint::checkResponse: getBlueprintInfo(${request.contentBlueprintId}) failed`);
      console.error(err);
    }
  }
  if (!templateKey) {
    trace("blueprint::checkResponse failed to obtain templateKey");
    return;
  }
  trace(`blueprint::checkResponse: templateKey=${templateKey}`);
  */

  const data = {
    draftId, spaceKey, blueprintId: request.contentBlueprintId
  };
  const templateKey = request.contentTemplateId || request.contentTemplateKey;
  if (templateKey) {
    data.templateKey = templateKey;
  }
  // const templateSettings = await getTemplateSetting(templateKey);
  // $.extend(data, templateSettings);
  window.localStorage[X_VBP_TEMPLATE] = JSON.stringify(data);
  trace("blueprint::checkResponse window.localStorage[X_VBP_TEMPLATE]=", data);
}
