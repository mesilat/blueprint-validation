/*
import $ from "jquery";

function initGlobalTemplates($div) {
  $div.find("table.plugin-pagetemplates-table tr:first-child").each(function(){
    const $tr = $(this);
    $("<a>")
      .attr("href", `${AJS.contextPath()}/plugins/blueprint-validation/configure-template.action`)
      .text("Validate")
      .appendTo($tr.find("td.template-operations"));
  });
}

function initSpaceTemplates($div) {
  // console.debug("Space templates are not supported (yet)");
}

export default () => {
  console.debug("validating-blueprints", "admin.js::init()");

  if (window.location.pathname.endsWith("/pages/templates2/listglobaltemplates.action")) {
    $("#content-blueprint-templates").each(function(){
      const $div = $(this);
      if ($div.data("vbp-initialized")) {
        return;
      }
      $div.data("vbp-initialized", true);
      initGlobalTemplates($div);
    });
  } else if (window.location.pathname.endsWith("/pages/templates2/listpagetemplates.action")) {
    $("#content-blueprint-templates").each(function(){
      const $div = $(this);
      if ($div.data("vbp-initialized")) {
        return;
      }
      $div.data("vbp-initialized", true);
      initSpaceTemplates($div);
    });
  }
}
*/
