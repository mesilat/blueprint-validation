import { SHOW_WARNING } from "../constants";

function CurrencyValidator(options) {
  this.title = options.name;
}

CurrencyValidator.prototype.focusin = function($td, empty, ed) {
}
CurrencyValidator.prototype.focusout = function($td, empty, ed) {
}

export default CurrencyValidator;
