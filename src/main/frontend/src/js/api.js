import $ from "jquery";
import _ from "lodash";

const DEFAULT_TIMEOUT = 30000;

function toError(xhr) {
  if (xhr.responseJSON && xhr.responseJSON.message) {
    return new Error(xhr.responseJSON.message);
  } else if (xhr.responseText) {
    return new Error(xhr.responseText.substr(0, 1000));
  } else {
    return new Error("API call failed");
  }
}

export async function get(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'GET',
        data,
        dataType: 'json',
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function post(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(_.extend({
        url: `${AJS.contextPath()}${url}`,
        type: 'POST',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
