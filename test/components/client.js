const ConfluenceClient = require('confluence-client');
const REST_API_PATH = "/rest/blueprint-validation/1.0";

function Client(options) {
    ConfluenceClient.apply(this, [options]);
}
Client.prototype = Object.create(ConfluenceClient.prototype);
Client.prototype.constructor = Client;
Client.prototype.setTemplateValidationMode = async function(data /*{templateKey, templateName, validationMode}*/) {
  return this.post(`${REST_API_PATH}/template`, data);
}
Client.prototype.listSpaceTemplates = async function(spaceKey) {
  return this.get(`/rest/experimental/template/page`, { spaceKey });
}
Client.prototype.getBlueprintInfo = async function(blueprintModuleCompleteKey) {
  return this.get(`/rest/create-dialog/1.0/blueprints/get`, { blueprintModuleCompleteKey });
}
Client.prototype.createDraftFromBlueprint = async function(spaceKey, title, blueprint) {
  return this.post(`/rest/create-dialog/1.0/content-blueprint/create-draft`, {
    spaceKey,
    contentBlueprintId: blueprint.id,
    parentPageId: null,
    title,
    context:{
      blueprintModuleCompleteKey: blueprint.moduleCompleteKey,
      contentBlueprintId: blueprint.id,
      new: true,
      spaceKey
    }
  });
}
Client.prototype.validatePage = async function(pageId, templateKey) {
  if (templateKey) {
    return this.post(`${REST_API_PATH}/data/validate/${pageId}?templateKey=${encodeURIComponent(templateKey)}`);
  } else {
    return this.post(`${REST_API_PATH}/data/validate/${pageId}`);
  }
}
Client.prototype.getPageData = async function(pageId) {
  return this.get(`${REST_API_PATH}/data/${pageId}`);
}
Client.prototype.queryData = async function(params) {
  return this.post(`${REST_API_PATH}/data`, params);
}
Client.prototype.getBVPageTemplate = async function(pageId) {
  return this.get(`${REST_API_PATH}/data/template/${pageId}`);
}

module.exports = Client;
