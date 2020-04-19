import { trace, waitForElement } from "./util";
import initTemplatesTable from "./config/templates";

function init(count) {
  trace("space::init()");

  waitForElement("#com-mesilat-vbp-templates")
  .then($elt => $elt? initTemplatesTable($elt): null);
}

export default () => init();
