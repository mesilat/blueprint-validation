import { SHOW_WARNING } from "../constants";

function DateValidator(options) {
  this.title = options.name;
}

DateValidator.prototype.focusin = function($td, empty, ed) {
  if (empty){
    ed.execCommand("mceConfInsertDateAutocomplete", false, {}, {skip_undo: true});
  }
}
DateValidator.prototype.focusout = function($td, empty, ed) {
  if (empty)
    return;

  setTimeout(() => {
    if ($td.find("time[datetime]").length === 0) {
      $td.addClass(SHOW_WARNING);
    }
  }, 100);
}

export default DateValidator;
