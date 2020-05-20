import $ from "jquery";
import { notifyError, notifySuccess, trace, flash } from "../util";
import { get, post, put, putXml, downloadFile } from "../api";
import { REST_API_PATH } from "../constants";

let VALIDATION_MODES; // import available validation modes in user locale
window.require("confluence/module-exporter")
.safeRequire("com.mesilat.vbp.validation-modes", provider => {
  VALIDATION_MODES = provider.getValidationModes();
});

const getTemplateSetting = async templateKey => get(`${REST_API_PATH}/template/${templateKey}`);
const setValidationMode = async data => post(`${REST_API_PATH}/template`, data);
const uploadTemplateContent = async (templateKey, data) => putXml(`${REST_API_PATH}/template/${templateKey}/content`, data);
const uploadTemplateSchema = async (templateKey, data) => put(`${REST_API_PATH}/template/${templateKey}/schema`, {}, { data });

async function updateValidationMode(templateKey, validationMode, $tr) {
  try {
    const val = await setValidationMode({ templateKey, validationMode });
    const $td = $tr.find("td:last-child a").text(VALIDATION_MODES[val.validationMode].name);
    flash($td); // flash to confirm mode had changed
    return val;
  } catch(err) {
    notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
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
      try {
        await loaderFunc(templateKey, e.target.result);
        notifySuccess(
          AJS.I18n.getText("com.mesilat.general.success"),
          AJS.I18n.getText("com.mesilat.vbp.template.upload.success")
        );
      } catch(err) {
        notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
      }
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
    const $actions = $(Mesilat.BlueprintValidation.spaceTemplateActions({ templateKey, uploadEnabled, restApiPath: REST_API_PATH }));
    $actions.find(`aui-option[value="${settings.validationMode}"]`).attr("selected", true);
    $actions.find("aui-select").on("change", async (e) => {
      const validationMode = $(e.target).val();
      settings = await updateValidationMode(templateKey, validationMode, $tr);
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
