import $ from "jquery";
import { notifyError, notifySuccess, trace, flash } from "../util";
import { get, post, put, putXml, downloadFile } from "../api";

let VALIDATION_MODES; // import available validation modes in user locale
window.require("confluence/module-exporter")
.safeRequire("com.mesilat.vbp.validation-modes", provider => {
  VALIDATION_MODES = provider.getValidationModes();
});

const getTemplateSetting = async templateKey => get(`/rest/blueprint-validation/1.0/template/${templateKey}`);
const createTemplateSetting = async data => post("/rest/blueprint-validation/1.0/template", data);
const updateTemplateSetting = async (templateKey, data) => put(`/rest/blueprint-validation/1.0/template/${templateKey}`, data);
const uploadTemplateContent = async (templateKey, data) => putXml(`/rest/blueprint-validation/1.0/template/${templateKey}/content`, data);
const uploadTemplateSchema = async (templateKey, data) => put(`/rest/blueprint-validation/1.0/template/${templateKey}/schema`, {}, { data });

async function updateValidationMode(newVal, oldVal, $tr) {
  if (oldVal.validationMode !== newVal.validationMode) {
    try {
      const val = (!oldVal.templateKey)
        ? await createTemplateSetting(newVal)
        : await updateTemplateSetting(oldVal.templateKey, newVal);

      const $td = $tr.find("td:last-child a").text(VALIDATION_MODES[val.validationMode].name);
      flash($td); // flash to confirm mode had changed
      return val;
    } catch(err) {
      notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
      return oldVal;
    }
  } else {
    return oldVal;
  }
}
async function uploadContent(templateKey, e) {
  return upload(templateKey, e, uploadTemplateContent);
}
async function uploadSchema(templateKey, e) {
  return upload(templateKey, e, uploadTemplateSchema);
}
async function upload(templateKey, e, loaderFunc) {
  try {
    const reader = new FileReader();
    reader.onload = async (e) => {
      if (e.target.readyState != 2)
          return;
      if (e.target.error) {
        throw e.target.error;
      }
      await loaderFunc(templateKey, e.target.result);
      notifySuccess(
        AJS.I18n.getText("com.mesilat.general.success"),
        AJS.I18n.getText("com.mesilat.vbp.template.upload.success")
      );
    };
    reader.readAsText(e.target.files[0]);
  } catch(err) {
    notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
  } finally {
    $(e.target).val("");
  }
}
async function download(e, fileName) {
  e.preventDefault();
  try {
    await downloadFile($(e.target).attr("href"), fileName);
  } catch(err) {
    notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
  }
}

export default function initTemplatesTable($table){
  trace("templates::initTemplatesTable()");

  $table.find("tbody").on("click", "tr[data-template-key] .com-mesilat-vbp-validation-mode", async (e) => {
    const $tr = $(e.currentTarget).closest("tr");
    $table.find(".com-mesilat-vbp-template-actions").remove();
    const templateKey = $tr.data("template-key");
    const uploadEnabled = $tr.data("upload-enabled");
    const templateName = $tr.find("td:first-child").text().trim();
    let settings = { validationMode: "NONE" };
    try {
      settings = await getTemplateSetting(templateKey);
    } catch (err) {
      //trace(err);
    }

    // Show actions pane
    const $actions = $(Mesilat.BlueprintValidation.spaceTemplateActions({ templateKey, uploadEnabled }));
    $actions.find(`aui-option[value="${settings.validationMode}"]`).attr("selected", true);
    $actions.find("aui-select").on("change", async (e) => {
      const validationMode = $(e.target).val();
      settings = await updateValidationMode({ templateKey, templateName, validationMode }, settings, $tr);
    });
    $actions.find("#com-mesilat-vbp-template-upload-content").on("change", (e) => {
      uploadContent(templateKey, e);
    });
    $actions.find("#com-mesilat-vbp-template-upload-schema").on("change", (e) => {
      uploadSchema(templateKey, e);
    });
    $actions.find(".com-mesilat-vbp-download-as-xml").on("click", (e) => {
      download(e, `${templateName}.xml`);
    });
    $actions.find(".com-mesilat-vbp-download-as-json").on("click", (e) => {
      download(e, `${templateName}.json`);
    });
    $actions.insertAfter($tr);
  });
}
