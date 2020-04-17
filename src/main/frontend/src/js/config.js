import $ from "jquery";
import initValidators from "./config/validators";
import initTemplates from "./config/templates";

function init(count) {
  count = count||0;
  if (count > 50) {
    return; // give up
  }
  console.debug("validating-blueprints", "config.js::init()");
  if (
    $("#com-mesilat-configure-validators").length === 0 &&
    $("#com-mesilat-configure-templates").length === 0
  ){
    setTimeout(init, 50, count + 1);
    return;
  }

  $("#com-mesilat-configure-validators").each(async function(){
    const $div = $(this);
    if ($div.data("initialized")) {
      return;
    }
    $div
      .data("initialized", true)
      .empty();

    initValidators($div);
  });

  $("#com-mesilat-configure-templates").each(async function(){
    const $div = $(this);
    if ($div.data("initialized")) {
      return;
    }
    $div
      .data("initialized", true)
      .empty();

    initTemplates($div);
  });
}

export default () => init();
