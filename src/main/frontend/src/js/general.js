import $ from "jquery";
import { trace } from "./util";
import { checkResponse } from "./navigation/blueprint";

async function init() {
  trace("general::init()");

  // AJS.bind("blueprint.before-create", (e) => {
  // });

  $(document).ajaxSuccess((e, xhr, options) => {
    if (
        options.type === "POST"
      && options.url.indexOf("/rest/create-dialog/1.0/content-blueprint/create-draft") >= 0
    ) {
      checkResponse(xhr.responseJSON, JSON.parse(options.data));
    }
  });
}

export default () => AJS.toInit(() => init());
