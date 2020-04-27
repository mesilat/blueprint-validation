import $ from "jquery";
import { SHOW_PROMPT, SHOW_WARNING, VALIDATE_PREFIX } from "./constants";
import { get } from "./api";
import { trace, error } from "./util";

import ListOfValuesValidator from "./validators/list-of-values";
import RegularExpressionValidator from "./validators/regular-expression";
import NumberValidator from "./validators/number";
import PageValidator from "./validators/page";
import UserValidator from "./validators/user";
import DateValidator from "./validators/date";

const getValidators = async () => get(`/rest/blueprint-validation/1.0/validator?extensive=true`);

function isEmpty($td){
  if ($td.text().replace(/\s/g, "") !== ""){
    return false;
  }
  const $descendants = $td.find("*");
  for (let i = 0; i < $descendants.length; i++){
    switch ($descendants[i].nodeName){
      case "P":
      case "SPAN":
      case "BR":
      case "DIV":
        break;
      default:
        return false;
    }
  }
  return true;
}

const registry = {
  classes: {},
  focusin: ($td, ed) => {
    trace("registry::focusin");
    const empty = isEmpty($td);
    $td[0].className.split(/\s+/).forEach((className) => {
      if (className.indexOf(VALIDATE_PREFIX) !== 0)
        return;

      className = className.substr(VALIDATE_PREFIX.length);
      if (className in registry.classes){
        $td.removeClass(`${SHOW_PROMPT} ${SHOW_WARNING}`);
        if (registry.classes[className].focusin) {
          registry.classes[className].focusin($td, empty, ed);
        }
      }
    });
  },
  focusout: ($td, ed, lastTdHolder) => {
    trace("registry::focusout");
    const empty = isEmpty($td);
    let hasValidationClassName = false;
    $td[0].className.split(/\s+/).forEach((className) => {
      if (className.indexOf(VALIDATE_PREFIX) !== 0)
        return;

      className = className.substr(VALIDATE_PREFIX.length);
      if (className in registry.classes){
        hasValidationClassName = true;
        if (registry.classes[className].focusout) {
          registry.classes[className].focusout($td, empty, ed, lastTdHolder);
        }
      }
    });
    if (hasValidationClassName && empty) {
      $td.addClass(SHOW_PROMPT);
    }
  },
  isEmpty: isEmpty
};

async function loadValidators() {
  trace("registry::loadValidators");
  try {
    const validators = await getValidators();
    validators.forEach(validator => {
      trace("registry::loadValidators() validator=", validator);
      try {
        switch (validator.type) {
          case "LOFV":
            registry.classes[validator.code] = new ListOfValuesValidator(validator);
            break;
          case "REXP":
            registry.classes[validator.code] = new RegularExpressionValidator(validator);
            break;
          case "NUMR":
            registry.classes[validator.code] = new NumberValidator(validator);
            break;
          case "PAGE":
            registry.classes[validator.code] = new PageValidator(validator);
            break;
          case "USER":
            registry.classes[validator.code] = new UserValidator(validator);
            break;
          case "DATE":
            registry.classes[validator.code] = new DateValidator(validator);
            break;
          case "MODL":
            const Module = window.require(validator.module);
            if (!Module) {
              error(`Module ${Module} could not be found`);
            } else {
              try {
                registry.classes[validator.code] = new Module(validator);
              } catch(err) {
                error(`Module ${Module} instantiation failed`, err);
              }
            }
            break;
          default:
            // TODO
        }
      } catch (err) {
        error(`Failed to init validator "${validator.code}"`, err);
      }
    });
  } catch(err) {
    error("Failed to get validators data", err);
  }
}
loadValidators();

export default registry;
