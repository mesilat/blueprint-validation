import { SHOW_WARNING } from "../constants";

function CountryValidator(options) {
  this.title = options.name;
}

CountryValidator.prototype.focusin = function($td, empty, ed) {
}
CountryValidator.prototype.focusout = function($td, empty, ed) {
}

export default CountryValidator;
