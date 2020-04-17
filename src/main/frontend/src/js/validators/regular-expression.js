import { SHOW_WARNING, VALUE_MULTI } from "../constants";

function RegularExpressionValidator(options) {
  this.title = options.name;
  this.regexps = [];
  options.text.split(/\n/).forEach(re => {
    try {
      this.regexps.push(new RegExp(re));
    } catch(err) {
      console.error("RegularExpressionValidator::constructor", err.message);
    }
  });
}

RegularExpressionValidator.prototype.focusout = function($td, empty, ed) {
  if (!empty) {
    setTimeout(() => {
      if ($td.hasClass(VALUE_MULTI)) {
        $td[0].textContent.split(",").forEach(text => {
          text = text.trim();
          this.showWarningIfNotValid($td, text);
        });
      } else {
        const text = $td[0].textContent.trim();
        this.showWarningIfNotValid($td, text);
      }
    }, 100);
  }
}

RegularExpressionValidator.prototype.showWarningIfNotValid = function($td, text) {
  let valid = false;
  this.regexps.forEach(regexp => {
    if (text.match(regexp)) {
      valid = true;
    }
  });
  if (!valid) {
    $td.addClass(SHOW_WARNING);
  }
}

export default RegularExpressionValidator;
