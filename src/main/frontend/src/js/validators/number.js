import validator from "validator";
import numeral from "numeral";
import { SHOW_WARNING, VALUE_MULTI } from "../constants";

/*
 * NumberFormats are specified using numeral.js formatting http://numeraljs.com
 * <format>; <color>; <min value>; <max value>
 */
function NumberValidator(options) {
  this.title = options.name;
  try {
    this.format = JSON.parse(options.text);
  } catch(ignore) {
    this.format = options.text;
  }
}

NumberValidator.prototype.focusout = function($td, empty, ed) {
  if (empty)
    return;

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

NumberValidator.prototype.showWarningIfNotValid = function($td, text) {
  if (!validator.isDecimal(text)) {
    $td.addClass(SHOW_WARNING);
  }
}

NumberValidator.prototype.formatValue = function($td) {
  function applyFormat($td, obj) {
    if (obj.style && _.isObject(obj.style)) {
      _.keys(obj.style).forEach(key => $td.css(key, obj.style[key]));
    }
    if (obj.format) {
      let text = $td[0].textContent.trim();
      text = numeral(text).format(obj.format);
      if ("group-separator" in obj) {
        text = text.replace(/\,/g, obj["group-separator"]);
      }
      $td.empty().text(text);
    }
  }

  if (_.isObject(this.format)) {
    if (_.isArray(this.format)) {
      const val = parseFloat($td[0].textContent.trim());
      this.format.forEach(format => {
        if (("gt" in format) && val <= format.gt) {
          return;
        }
        if (("gte" in format) && val < format.gte) {
          return;
        }
        if (("eq" in format) && val !== format.eq) {
          return;
        }
        if (("lte" in format) && val > format.lte) {
          return;
        }
        if (("lt" in format) && val >= format.lt) {
          return;
        }
        applyFormat($td, format);
      });
    } else {
      applyFormat($td, this.format);
    }
  } else {
    let text = $td[0].textContent.trim();
    text = numeral(text).format(this.format);
    $td.empty().css("text-align", "right").text(text);
  }
}

export default NumberValidator;
