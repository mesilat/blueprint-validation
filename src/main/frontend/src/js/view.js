import $ from "jquery";
import _ from "lodash";
import { get } from "./api";
import { trace, error, notifyError } from "./util";
import NumberValidator from "./validators/number";
import { VALIDATE_PREFIX } from "./constants";
import initMacroReport from "./macro/report";

const getValidators = async () => get(`/rest/blueprint-validation/1.0/validator?extensive=true`);
const getValidationTask = async uuid => get(`/rest/blueprint-validation/1.0/validation/${uuid}`);

// Load number validators
async function loadValidators() {
  try {
    const validators = await getValidators();
    const validatorsClasses = {};
    validators.forEach(validator => {
      switch (validator.type) {
        case "NUMR":
          validatorsClasses[validator.code] = new NumberValidator(validator);
          break;
        default:
          // TODO?
      }
    });
    return validatorsClasses;
  } catch(err) {
    error("Failed to get validators data", err);
  }
}

// For post-validated page templates: show notification message if page
// validation against JSON schema failed
function checkForPendingTasks() {
  if (!window.localStorage.vbpPendingTasks) {
    return;
  }

  const pendingTasks = JSON.parse(window.localStorage.vbpPendingTasks);
  const threshold = Date.now() - 180000; // 3 min max for pending validation tasks
  _.keys(pendingTasks).forEach(uuid => {
    if (pendingTasks[uuid] >= threshold) {
      checkTask(uuid);
    } else {
      delete pendingTasks[uuid];
    }
  });
  updatePendingTasks(pendingTasks) ;
}
function checkTask(uuid) {
  trace(`check validation task: ${uuid}`);

  async function doCheckTask(uuid, count) {
    if (count > 15) {
      return; // stop monitoring validation task after approx. 2.5 min
    }
    try {
      const task = await getValidationTask(uuid);
      trace(`Page ${task.pageId} [${task.pageTitle}] validation`, task);
      switch (task.status) {
        case "valid":
          // do we need to notify success as well?
          break;
        case "invalid":
          notifyError("Page validation failed", task.message);
          break;
        case "validating":
        case "pending":
          setTimeout(count => doCheckTask(uuid, count), Math.pow(1.4, count) * 400, count + 1);
          return;
      }

      // this task is over, so remove it from localStorage
      let pendingTasks = window.localStorage.vbpPendingTasks;
      pendingTasks = pendingTasks? JSON.parse(pendingTasks): {};
      delete pendingTasks[uuid];
      updatePendingTasks(pendingTasks) ;
    } catch(err) {
      error("Failed to get validation task status", err);
    }
  }

  doCheckTask(uuid, 0);
}
function updatePendingTasks(pendingTasks) {
  if (_.keys(pendingTasks).length > 0) {
    window.localStorage.vbpPendingTasks = JSON.stringify(pendingTasks);
  } else {
    delete window.localStorage.vbpPendingTasks;
  }
}

function checkForMacros() {
  trace("view::checkForMacros");
  $(".conf-macro.com-mesilat-vbp-report").each(function(){
    initMacroReport($(this));
  });
}

async function init() {
  trace("view::init()");
  try {
    const validatorsClasses = await loadValidators();

    _.keys(validatorsClasses).forEach(code => {
      $(`td.${VALIDATE_PREFIX}${code}`).each(function(){
        if (validatorsClasses[code].format) {
          validatorsClasses[code].formatValue($(this));
        }
      });
    });
  } catch (err) {
    error("Failed to get validators data", err);
  }

  checkForPendingTasks();
  checkForMacros();
}

export default () => AJS.toInit(() => init());
