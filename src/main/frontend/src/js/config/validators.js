import $ from "jquery";
import _ from "lodash";
import { showConfirmationDialog, notify, notifyError, notifySuccess, trace, error } from "../util";
import { get, post, put, del, downloadFile } from "../api";

const listValidators = async () => get(`/rest/blueprint-validation/1.0/validator`);
const getValidatorDetail = async code => get(`/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`);
const createValidator = async data => post(`/rest/blueprint-validation/1.0/validator`, data);
const updateValidator = async (code, data) => put(`/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`, data);
const deleteValidator = async code => del(`/rest/blueprint-validation/1.0/validator/${encodeURIComponent(code)}`);
const uploadValidators = async (data) => post(`/rest/blueprint-validation/1.0/validator/upload`, {}, { data });

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
  trace(err);
  notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
}
function onCreate($form) {
  $form.find("select[name='list-of-names']").val("");
  updateForm($form, { type: "LOFV", code: "newval", name: "New validator" });
  disableButton($form, "com-mesilat-vbp-config-delete");
  onTypeChange($form);
}
function onTypeChange($form) {
  const type = $form.find("aui-select[name='type']").val();
  $form.find("input[name='module']").prop("disabled", type !== "MODL");
}
async function onSelected($form, code) {
  trace(`config::onSelected(${code})`);
  try {
    const data = await getValidatorDetail(code);
    updateForm($form, data);
    enableButton($form, "com-mesilat-vbp-config-delete");
    onTypeChange($form);
  } catch (err) {
    onDataError(err);
  }
}
async function onSave($form) {
  let data = validate($form);
  trace("validators::onSave", data);

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
    notifySuccess(
      AJS.I18n.getText("com.mesilat.general.success"),
      AJS.I18n.getText("com.mesilat.vbp.validator.create.success")
    );
  } catch (err) {
    onDataError(err);
  }
}
async function onDelete($form) {
  const $select = $form.find("select[name='list-of-names']");
  const code = $select.val();
  const $option = $select.find(`option[value='${code}']`);
  const name = $option.text();

  if (await showConfirmationDialog({
    header: AJS.I18n.getText("com.mesilat.vbp.validator.delete.caption"),
    message: AJS.I18n.getText("com.mesilat.vbp.validator.delete.prompt").replace("{}", name),
    proceed: AJS.I18n.getText("com.mesilat.general.proceed"),
    cancel: AJS.I18n.getText("com.mesilat.general.cancel")
  })) {
    trace("validators::onDelete", code);
    try {
      await deleteValidator(code);
      $option.remove();
      notify(
        AJS.I18n.getText("com.mesilat.general.deleted"),
        AJS.I18n.getText("com.mesilat.vbp.validator.deleted.message").replace("{}", name)
      );
      updateForm($form, { });
      disableButton($form, "com-mesilat-vbp-config-delete");
    } catch (err) {
      onDataError(err);
    }
  }
}
async function onExport(href) {
  try {
    await downloadFile(href, "validators.json");
  } catch(err) {
    notifyError(AJS.I18n.getText("com.mesilat.general.error"), err.message);
  }
}
async function onImport($form, e) {
  try {
    const reader = new FileReader();
    reader.onload = async (e) => {
      if (e.target.readyState != 2)
          return;
      if (e.target.error) {
        throw e.target.error;
      }
      try {
        await uploadValidators(e.target.result);
        const list = await listValidators();
        const $select = $form.find(`select[name="list-of-names"]`);
        const newValidators = list.filter(validator => $select.find(`option[value="${validator.code}"]`).length === 0);
        trace("New validators: ", newValidators);
        if (newValidators.length === 0) {
          notifySuccess(
            AJS.I18n.getText("com.mesilat.general.success"),
            AJS.I18n.getText("com.mesilat.vbp.validators.import.nochange")
          );
        } else {
          notifySuccess(
            AJS.I18n.getText("com.mesilat.general.success"),
            AJS.I18n.getText("com.mesilat.vbp.validators.import.success").replace("{}", newValidators.length)
          );
        }
        newValidators.forEach(validator => {
          $("<option>").attr("value", validator.code).text(validator.name).appendTo($select);
        });
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

export default async ($form) => {
  try {
    $form.find("select[name='list-of-names']").on("change", (e) => onSelected($form, $(e.target).val()));
    $form.find("#com-mesilat-vbp-config-save").on("click", (e) => { e.preventDefault(); onSave($form); });
    $form.find("#com-mesilat-vbp-config-create").on("click", (e) => { e.preventDefault(); onCreate($form); });
    $form.find("#com-mesilat-vbp-config-delete").on("click", (e) => { e.preventDefault(); onDelete($form); });
    $form.find("aui-select[name='type']").on("change", () => onTypeChange($form));
    $form.find("#com-mesilat-vbp-templates-export").on("click", (e) => { e.preventDefault(); onExport($(e.target).attr("href")); });
    $form.find("#com-mesilat-vbp-templates-import").on("change", (e) => onImport($form, e));
  } catch (err) {
    onDataError(err);
  }
};
