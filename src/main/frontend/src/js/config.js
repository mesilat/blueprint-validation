import $ from "jquery";
import { trace, waitForElement } from "./util";
import initTemplatesTable from "./config/templates";
import initValidatorsForm from "./config/validators";

function init() {
  trace("config::init()");

  waitForElement("#com-mesilat-vbp-templates")
  .then($elt => $elt? initTemplatesTable($elt): null);

  waitForElement("#com-mesilat-vbp-validators")
  .then($elt => $elt? initValidatorsForm($elt): null);

  waitForElement("#com-mesilat-vbp-config-tab2")
  .then($elt => $elt && location.hash? $(`a[href="${location.hash}"]`).trigger("click"): null);
}

export default () => init();
