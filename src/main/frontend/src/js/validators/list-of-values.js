import { SHOW_WARNING, VALUE_MULTI } from "../constants";

function ListOfValuesValidator(options) {
  this.title = options.name;
  this.values = options.text.split(/\n/);
}

ListOfValuesValidator.prototype.focusin = function($td, empty, ed) {
  if ($td.hasClass(VALUE_MULTI)) {
    if ($td.data(`${VALUE_MULTI}-initialized`)){
      return;
    }
    $td.data(`${VALUE_MULTI}-initialized`, true);
    $td.on("click", () => setTimeout(() => this.onClickMulti($td, ed)));
    $td.trigger("click");
  } else {
    if (empty) {
      this.showAutocomplete(ed);
    }
  }
}

ListOfValuesValidator.prototype.focusout = function($td, empty, ed) {
  if (!empty){
    setTimeout(() => {
      if ($td.hasClass(VALUE_MULTI)) {
        $td[0].textContent.split(",").forEach(text => {
          text = text.trim();
          this.showWarningIfNotValidOption($td, text);
        });
      } else {
        const text = $td[0].textContent.trim();
        this.showWarningIfNotValidOption($td, text);
      }
    }, 100);
  }
}

ListOfValuesValidator.prototype.onClickMulti = function($td, ed) {
  const text = $td[0].textContent.trim();
  if (text === "" || text.endsWith(",")) {
    this.showAutocomplete(ed);
  }
}

ListOfValuesValidator.prototype.showAutocomplete = function(ed) {
  ed.execCommand("MesilatAutocomplete", false, {
    id: "_DATA_",
    title: this.title,
    data: this.values
  }, {
    skip_undo: true
  });
}

ListOfValuesValidator.prototype.showWarningIfNotValidOption = function($td, text) {
  if (this.values.filter(value => value === text).length === 0) {
    $td.addClass(SHOW_WARNING);
  }
}

export default ListOfValuesValidator;
