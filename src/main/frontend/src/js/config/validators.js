import $ from "jquery";
import _ from "lodash";
import { showConfirmationDialog, notify, notifyError, notifySuccess } from "../util";

async function listValidators() {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/validator`,
    type: 'GET',
    dataType: 'json',
    timeout: 30000
  });
}
async function getValidatorDetail(code) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`,
    type: 'GET',
    dataType: 'json',
    timeout: 30000
  });
}
async function createValidator(data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/validator`,
    type: 'POST',
    data: JSON.stringify(data),
    dataType: 'json',
    contentType: 'application/json',
    processData: false,
    timeout: 30000
  });
}
async function updateValidator(code, data) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`,
    type: 'PUT',
    data: JSON.stringify(data),
    dataType: 'json',
    contentType: 'application/json',
    processData: false,
    timeout: 30000
  });
}
async function deleteValidator(code) {
  return $.ajax({
    url: `${AJS.contextPath()}/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`,
    type: 'DELETE',
    timeout: 30000
  });
}

function validate($form) {
  const data = {};
  data.type = $form.find("aui-select[name='type']").val();
  data.code = $form.find("input[name='code']").val();
  data.name = $form.find("input[name='name']").val();
  data.prompt = $form.find("input[name='prompt']").val();
  data.warning = $form.find("input[name='warning']").val();
  data.text = $form.find("textarea[name='text']").val();
  data.module = $form.find("input[name='module']").val();
  return data;
}
function updateForm($form, data) {
  $form.find("aui-select[name='type']").val(data.type? data.type: "");
  $form.find("input[name='code']").val(data.code? data.code: "");
  $form.find("input[name='name']").val(data.name? data.name: "");
  $form.find("input[name='prompt']").val(data.prompt? data.prompt: "");
  $form.find("input[name='warning']").val(data.warning? data.warning: "");
  $form.find("textarea[name='text']").val(data.text? data.text: "");
  $form.find("input[name='module']").val(data.module? data.module: "");
}
function disableButton($form, id){
  $form.find(`#${id}`).prop("disabled", true);
}
function enableButton($form, id){
  $form.find(`#${id}`).prop("disabled", false);
}

function onDataError(err) {
  console.error(err);
  notifyError("Error", err.responseText);
}
async function onSelected($form, code) {
  console.debug("config::onSelected", code);
  try {
    const data = await getValidatorDetail(code);
    updateForm($form, data);
    enableButton($form, "vbp-config-delete");
    onTypeChange($form);
  } catch (err) {
    onDataError(err);
  }
}
async function onSave($form) {
  let data = validate($form);
  console.debug("config::onSave", data);

  const $select = $form.find("select[name='list-of-names']");
  const code = $select.val();

  try {
    if (code) {
      const $option = $select.find(`option[value='${code}']`);
      data = await updateValidator(code, data);
      $option.prop("value", data.code);
      $option.text(data.name);
    } else {
      data = await createValidator(data);
      const $option = $("<option>")
        .prop("value", data.code)
        .text(data.name)
        .appendTo($select);
    }
    $select.val(data.code).trigger("change");
    notifySuccess("Success", "Validator was created successfully");
  } catch (err) {
    onDataError(err);
  }
}
function onCreate($form) {
  $form.find("select[name='list-of-names']").val("");
  updateForm($form, { type: "LOFV", code: "newval", name: "New validator" });
  disableButton($form, "vbp-config-delete");
  onTypeChange($form);
}
async function onDelete($form) {
  const $select = $form.find("select[name='list-of-names']");
  const code = $select.val();
  const $option = $select.find(`option[value='${code}']`);
  const name = $option.text();

  if (await showConfirmationDialog({
    header: "Delete validator",
    message: `You are about to delete validator "${name}". This can not be undone`,
    proceed: "Proceed",
    cancel: "Cancel"
  })) {
    console.debug("config::onDelete", code);
    try {
      await deleteValidator(code);
      $option.remove();
      notify("Deleted", `Validator "${name}" was deleted`);
      updateForm($form, { });
      disableButton($form, "vbp-config-delete");
    } catch (err) {
      onDataError(err);
    }
  }
}
function onTypeChange($form) {
  const type = $form.find("aui-select[name='type']").val();
  $form.find("input[name='module']").prop("disabled", type !== "MODL");
}

export default async ($div) => {
  const VT = window.require('com.mesilat.vbp.validator-types');
  const VALIDATOR_TYPES = _.values(VT.getValidatorTypes())
    .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));

  try {
    const validators = await listValidators();
    const $form = $(Mesilat.BlueprintValidation.validators({
      validatorTypes: VALIDATOR_TYPES,
      validators
    }));
    $form.appendTo($div);
    $form.find("select[name='list-of-names']").on("change", (e) => onSelected($form, $(e.target).val()));
    $form.find("#vbp-config-save").on("click", (e) => { e.preventDefault(); onSave($form); });
    $form.find("#vbp-config-create").on("click", (e) => { e.preventDefault(); onCreate($form); });
    $form.find("#vbp-config-delete").on("click", (e) => { e.preventDefault(); onDelete($form); });
    $form.find("aui-select[name='type']").on("change", () => onTypeChange($form));
  } catch (err) {
    onDataError(err);
  }
};
