import $ from "jquery";

export const PLUGIN_KEY = "com.mesilat.blueprint-validation";

export function trace(){
  const traceEnabled = !!window.localStorage["vbp-trace-enabled"];
  if (traceEnabled){
    const args = [PLUGIN_KEY];
    for (let i = 0; i < arguments.length; i++) {
      args.push(arguments[i]);
    }
    console.debug.apply(console, args);
  }
}
export function error(){
  const args = [PLUGIN_KEY];
  for (let i = 0; i < arguments.length; i++) {
    args.push(arguments[i]);
  }
  console.error.apply(console, args);
}

export async function showConfirmationDialog(data/*header, message, proceed, cancel*/) {
  return new Promise(resolve => {
    const $dialog = $(Mesilat.BlueprintValidation.confirmationDialog(data));
    const dialog = AJS.dialog2($dialog);
    $dialog.find(".aui-button-primary").on("click", () => {
      resolve(true);
      dialog.hide();
    });
    $dialog.find(".aui-button-cancel").on("click", () => {
      resolve(false);
      dialog.hide();
    });
    dialog.show();
  });
}

export function notify(title, message) {
  AJS.flag({
    type: "info",
    title: title||"Info",
    body: $("<p>").text(message).html()
  });
}
export function notifyError(title, message) {
  AJS.flag({
    type: "error",
    title: title||"Error",
    body: $("<p>").text(message).html()
  });
}
export function notifySuccess(title, message) {
  AJS.flag({
    type: "success",
    title: title||"Success",
    body: $("<p>").text(message).html()
  });
}
