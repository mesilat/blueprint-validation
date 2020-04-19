import $ from "jquery";
import { PLUGIN_KEY, TRACE_ENABLED } from "./constants";

export function trace(){
  const traceEnabled = !!window.localStorage[TRACE_ENABLED];
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

export async function waitForElement(selector) {
  return new Promise(resolve => {
    function check(count) {
      if (count > 50) {
        trace(`could not find: ${selector}`)
        resolve(false);
        return;
      }
      const $elt = $(selector);
      if ($elt.length > 0) {
        if ($elt.data("com-mesilat-vbp-initialized")) {
          trace(`already initialized: ${selector}`)
          resolve(false);
        } else {
          $elt.data("com-mesilat-vbp-initialized", true);
          resolve($elt);
        }
      } else {
        setTimeout(count => check(count), 50, count + 1);
      }
    }
    check(0);
  });
}

export async function flash($elt) {
  return new Promise(resolve => {
    $elt
      .addClass('animated flash')
      .one('webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend', () => {
        $elt.removeClass('animated flash');
        resolve();
      });
  });
};
