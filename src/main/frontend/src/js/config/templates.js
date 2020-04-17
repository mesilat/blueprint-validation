import $ from "jquery";
import { showConfirmationDialog, notify, notifyError, notifySuccess } from "../util";

async function listBlueprintTemplates() {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/list-blueprint-templates`,
    type: 'GET',
    dataType: 'json',
    timeout: 30000
  });
}

async function listTemplateSettings() {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template`,
    type: 'GET',
    dataType: 'json',
    timeout: 30000
  });
}

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

async function clearTemplateSetting(templateKey) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/template/${templateKey}`,
    type: 'DELETE',
    timeout: 30000
  });
}


function onDataError(err) {
  console.error(err);
  notifyError("Error", err.responseText);
}

async function showTemplateSettingsDialog(templates, setting) {
  return new Promise(resolve => {
    const params = {templates};
    if (setting && setting.validationMode) {
      params.validationMode = setting.validationMode;
    }
    if (setting && setting.templateKey) {
      params.templateKey = setting.templateKey;
    }
    const $dialog = $(Mesilat.BlueprintValidation.templateDialog(params));
    const dialog = AJS.dialog2($dialog);
    dialog.on("show", () => {
      $dialog.find("select[name='template']").auiSelect2();
    });
    $dialog.find(".aui-button-primary").on("click", () => {
      const templateKey = $dialog.find("select[name='template']").val();
      const data = {
        templateKey,
        validationMode: $dialog.find("aui-select[name='mode']").val()
      }
      const template = templates.filter(template => template.id === templateKey)
      .forEach(template => {
        data.templateName = template.name;
      });
      resolve(data);
      dialog.hide();
    });
    $dialog.find(".aui-button-cancel").on("click", () => {
      resolve();
      dialog.hide();
    });
    dialog.show();
  });
}

async function onCreateSetting(link) {
  const $link = $(link);
  $link.attr('aria-disabled', 'true');
  $link.spin();
  let templates;
  try {
    templates = await listBlueprintTemplates();
    templates.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
  } catch(err) {
    onDataError(err);
    return;
  } finally {
    $link.attr('aria-disabled', 'false');
    $link.spinStop();
  }

  let setting = await showTemplateSettingsDialog(templates);
  if (!setting) {
    return;
  }
  try {
    setting = await createTemplateSetting(setting);
    const $tr = $(Mesilat.BlueprintValidation.template(setting));
    $(link).closest("#com-mesilat-configure-templates").find("tbody").append($tr);
    notifySuccess("Success", "Template settings saved successfully");
  } catch(err) {
    onDataError(err);
  }
}

async function onEditSetting(link) {
  const templateKey = $(link).closest("tr").data("template-key");
  try {
    const [ setting, templates ] = await Promise.all([
      getTemplateSetting(templateKey),
      listBlueprintTemplates()
    ]);

    templates.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
    const modifiedSetting = await showTemplateSettingsDialog(templates, setting);
    const updatedSetting = await updateTemplateSetting(setting.templateKey, modifiedSetting);
    const $tr = $(Mesilat.BlueprintValidation.template(updatedSetting));
    $(link).closest("tr").replaceWith($tr);
    notifySuccess("Success", "Template settings updated successfully");
  } catch(err) {
    onDataError(err);
  }
}

async function onDeleteSetting(link) {
  const $tr = $(link).closest("tr");
  const templateKey = $tr.data("template-key");
  const templateName = $tr.find("td:first-child").text();
  try {
    if (await showConfirmationDialog({
      header: "Delete template settings",
      message: `You are about to delete settings for template "${templateName}". This can not be undone`,
      proceed: "Proceed",
      cancel: "Cancel"
    })) {
      await clearTemplateSetting(templateKey);
      $tr.remove();
      notifySuccess("Success", "Template settings deleted");
    }
  } catch(err) {
    onDataError(err);
  }
}

export default async ($div) => {
  try {
    const settings = await listTemplateSettings();
    console.debug("Settings", settings);
    const $view = $(Mesilat.BlueprintValidation.templates({settings}));
    $view.find("#vbp-add-template-setting").on("click", e => onCreateSetting(e.target));
    $view.on("click", ".vbp-template-edit", e => onEditSetting(e.target));
    $view.on("click", ".vbp-template-delete", e => onDeleteSetting(e.target));
    $view.appendTo($div);
  } catch (err) {
    onDataError(err);
  }
}
