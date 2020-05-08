import { SHOW_WARNING } from "../constants";

function UserValidator(options) {
  this.title = options.name;
}

UserValidator.prototype.focusin = function($td, empty, ed) {
  if (empty){
    AJS.EventQueue.push({name:'confluencementioninsert'});
    Confluence.Editor.Autocompleter.Manager.shortcutFired("@",true);
  }
}
UserValidator.prototype.focusout = function($td, empty, ed) {
  if (empty)
    return;

  setTimeout(() => {
    if ($td.find('a[data-linked-resource-type="userinfo"]').length === 0) {
      $td.addClass(SHOW_WARNING);
    }
  }, 100);
}

export default UserValidator;
