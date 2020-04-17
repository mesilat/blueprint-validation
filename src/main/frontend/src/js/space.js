import $ from "jquery";
import FileSaver from "file-saver";
import { notifyError, notifySuccess } from "./util";

let validationModes;
window.require('confluence/module-exporter')
.safeRequire('com.mesilat.vbp.validation-modes', (VM) => {
  validationModes = VM.getValidationModes();
});

let localeStrings;
window.require('confluence/module-exporter')
.safeRequire('com.mesilat.vbp.locale-strings', (LS) => {
  localeStrings = LS.getLocaleStrings();
});

async function getTemplateSetting(templateKey) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/${templateKey}`,
    type: 'GET',
    dataType: 'json',
    timeout: 30000
  });
}
async function createTemplateSetting(data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template`,
    type: 'POST',
    data: JSON.stringify(data),
    dataType: 'json',
    contentType: 'application/json',
    processData: false,
    timeout: 30000
  });
}
async function updateTemplateSetting(templateKey, data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/${templateKey}`,
    type: 'PUT',
    data: JSON.stringify(data),
    dataType: 'json',
    contentType: 'application/json',
    processData: false,
    timeout: 30000
  });
}
async function uploadTemplateContent(templateKey, data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/${templateKey}/content`,
    type: 'PUT',
    data,
    dataType: 'json',
    contentType: 'text/xml',
    processData: false,
    timeout: 30000
  });
}
async function uploadTemplateSchema(templateKey, data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/${templateKey}/schema`,
    type: 'PUT',
    data,
    dataType: 'json',
    contentType: 'application/json',
    processData: false,
    timeout: 30000
  });
}
async function downloadFile(url, fileName) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'blob';
    xhr.onload = function (e) {
      if (this.status === 200) {
        FileSaver.saveAs(e.target.response, fileName);
        resolve(fileName);
      } else {
        const reader = new FileReader();
        reader.onload = () => {
          try {
            reject(JSON.parse(reader.result));
          } catch(err) {
            reject(reader.result);
          }
        };
        reader.readAsText(this.response.valueOf());
      }
    };
    xhr.send();
  });
}

async function changeValidationMode(newVal, oldVal, $tr) {
  if (oldVal.validationMode !== newVal.validationMode) {
    try {
      const val = (!oldVal.templateKey)
        ? await createTemplateSetting(newVal)
        : await updateTemplateSetting(oldVal.templateKey, newVal);

      $tr.find("td:last-child").text(validationModes[val.validationMode].name);
      notifySuccess(localeStrings["vbp.msg.common.success"], localeStrings["vbp.msg.template.save.success"]);
      return val;
    } catch(err) {
      notifyError(localeStrings["vbp.msg.common.error"], err.message);
      return oldVal;
    }
  } else {
    return oldVal;
  }
}
async function uploadContent(templateKey, e) {
  try {
    const loader = new FileReader();
    loader.onload = async (e) => {
      if (e.target.readyState != 2)
          return;
      if (e.target.error) {
        throw e.target.error;
      }
      await uploadTemplateContent(templateKey, e.target.result);
      notifySuccess(localeStrings["vbp.msg.common.success"], localeStrings["vbp.msg.template.upload.success"]);
    };

    loader.readAsText(e.target.files[0]);
  } catch(err) {
    notifyError(localeStrings["vbp.msg.common.error"], err.message);
  } finally {
    $(e.target).val("");
  }
}
async function uploadSchema(templateKey, e) {
  try {
    const loader = new FileReader();
    loader.onload = async (e) => {
      if (e.target.readyState != 2)
          return;
      if (e.target.error) {
        throw e.target.error;
      }
      await uploadTemplateSchema(templateKey, e.target.result);
      notifySuccess(localeStrings["vbp.msg.common.success"], localeStrings["vbp.msg.template.upload.success"]);
    };

    loader.readAsText(e.target.files[0]);
  } catch(err) {
    notifyError(localeStrings["vbp.msg.common.error"], err.message);
  } finally {
    $(e.target).val("");
  }
}

function initTemplates($table){

  console.debug("validating-blueprints", "space.js::initTemplates()");
  $table.find("tbody").on("click", "tr[data-template-key]", async (e) => {
    const $tr = $(e.currentTarget);
    $table.find(".vbp-space-template-actions").remove();
    const templateKey = $tr.data("template-key");
    const templateName = $tr.find("td:first-child").text().trim();
    let settings = { validationMode: "NONE" };
    try {
      settings = await getTemplateSetting(templateKey);
    } catch (err) {
      console.debug(err);
    }
    const $actions = $(Mesilat.BlueprintValidation.spaceTemplateActions({ templateKey }));
    $actions.find(`aui-option[value="${settings.validationMode}"]`).attr("selected", true);
    $actions.find("aui-select").on("change", async (e) => {
      const validationMode = $(e.target).val();
      settings = await changeValidationMode({
        templateKey,
        templateName,
        validationMode
      }, settings, $tr);
    });
    $actions.find("#vbp-template-upload-content").on("change", async (e) => {
      uploadContent(templateKey, e);
    });
    $actions.find("#vbp-template-upload-schema").on("change", async (e) => {
      uploadSchema(templateKey, e);
    });
    $actions.find(".vbp-download-as-xml").on("click", async (e) => {
      e.preventDefault();
      try {
        downloadFile($(e.target).attr("href"), `${templateName}.xml`);
      } catch(err) {
        notifyError(localeStrings["vbp.msg.common.error"], err.message);
      }
    });
    $actions.find(".vbp-download-as-json").on("click", async (e) => {
      e.preventDefault();
      try {
        downloadFile($(e.target).attr("href"), `${templateName}.json`);
      } catch(err) {
        notifyError(localeStrings["vbp.msg.common.error"], err.message);
      }
    });
    $actions.insertAfter($tr);
  });
}

function init(count) {
  count = count||0;
  if (count > 50) {
    return; // give up
  }
  // console.debug("validating-blueprints", "space.js::init()");
  if ($("#vbp-space-templates").length === 0) {
    setTimeout(init, 50, count + 1);
    return;
  }

  $("#vbp-space-templates").each(async function(){
    const $table = $(this);
    if ($table.data("initialized")) {
      return;
    }
    $table.data("initialized", true);
    initTemplates($table);
  });
}

export default () => init();
